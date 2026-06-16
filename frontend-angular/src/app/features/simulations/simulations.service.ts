import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, map, throwError } from 'rxjs';
import { environment } from '../../../environments/environment';
import {
  CreditPoint,
  CreditProjectionResult,
  CreditScenarioResult,
  ProjectionMilestone,
  ProjectionPoint,
  SavingsProjectionResult,
  SavingsScenarioResult
} from './simulations.models';
import {
  buildCreditMilestones,
  normalizeCreditFormValue,
  normalizeSavingsFormValue,
  projectCredit,
  projectSavings
} from './simulations.engine';
import {
  CreditApiScenarioRequest,
  CreditCalculateApiRequest,
  CreditCalculateRequest,
  CreditCompareApiRequest,
  CreditCompareRequest,
  SavingsApiScenarioRequest,
  SavingsCalculateApiRequest,
  SavingsCalculateRequest,
  SavingsCompareApiRequest,
  SavingsCompareRequest
} from './simulations.api.models';

const MAX_SAVINGS_MONTHS = 240;

export interface SimulationRequestSource {
  component: string;
  method: string;
}

@Injectable({ providedIn: 'root' })
export class SimulationsService {
  private readonly http = inject(HttpClient);
  private readonly apiUrl = `${environment.apiUrl}/simulations`;

  calculateSavings(
    request: Partial<SavingsCalculateRequest>,
    source?: SimulationRequestSource
  ): Observable<SavingsScenarioResult> {
    const normalized = normalizeSavingsFormValue(request);
    const payload = this.toSavingsCalculateRequest(request);
    const endpoint = `${this.apiUrl}/savings/calculate`;

    if (!this.isSavingsCalculatePayloadValid(payload)) {
      this.debugInvalidPayload(source, 'calculate', endpoint, payload);
      return throwError(() => new Error('Savings calculate payload invalid'));
    }

    this.debugSavingsPayload('calculate', payload);

    return this.http.post<unknown>(endpoint, payload).pipe(
      map((response) => this.normalizeSavingsCalculation(response, normalized))
    );
  }

  compareSavings(
    request: Partial<SavingsCompareRequest>,
    source?: SimulationRequestSource
  ): Observable<SavingsProjectionResult> {
    const normalized = normalizeSavingsFormValue(request);
    const payload = this.toSavingsCompareRequest(request);
    const endpoint = `${this.apiUrl}/savings/compare`;

    if (!this.isSavingsComparePayloadValid(payload)) {
      this.debugInvalidPayload(source, 'compare', endpoint, payload);
      return throwError(() => new Error('Savings compare payload invalid'));
    }

    this.debugSavingsPayload('compare', payload);

    return this.http.post<unknown>(endpoint, payload).pipe(
      map((response) => this.normalizeSavingsComparison(response, normalized))
    );
  }

  calculateCredit(
    request: Partial<CreditCalculateRequest>,
    source?: SimulationRequestSource
  ): Observable<CreditScenarioResult> {
    const normalized = normalizeCreditFormValue(request);
    const payload = this.toCreditRequest(normalized);
    const endpoint = `${this.apiUrl}/credit/calculate`;

    if (!this.isCreditPayloadValid(payload)) {
      this.debugInvalidPayload(source, 'calculate', endpoint, payload);
      return throwError(() => new Error('Credit calculate payload invalid'));
    }

    this.debugCreditPayload('calculate', payload);

    return this.http.post<unknown>(endpoint, payload).pipe(
      map((response) => this.normalizeCreditCalculation(response, normalized))
    );
  }

  compareCredit(
    request: Partial<CreditCompareRequest>,
    source?: SimulationRequestSource
  ): Observable<CreditProjectionResult> {
    const normalized = normalizeCreditFormValue(request);
    const payload = this.toCreditCompareRequest(normalized);
    const endpoint = `${this.apiUrl}/credit/compare`;

    if (!this.isCreditComparePayloadValid(payload)) {
      this.debugInvalidPayload(source, 'compare', endpoint, payload);
      return throwError(() => new Error('Credit compare payload invalid'));
    }

    this.debugCreditPayload('compare', payload);

    return this.http.post<unknown>(endpoint, payload).pipe(
      map((response) => this.normalizeCreditComparison(response, normalized))
    );
  }

  private toSavingsCalculateRequest(request: Partial<SavingsCalculateRequest>): SavingsCalculateApiRequest {
    const normalized = normalizeSavingsFormValue(request);
    const source = request as Partial<SavingsCalculateRequest> & Partial<SavingsCalculateApiRequest>;

    return {
      targetAmount: normalized.targetAmount,
      initialContribution: normalized.initialContribution,
      monthlyContribution:
        this.resolveSavingsNumber(source.monthlyContribution) ??
        this.resolveSavingsNumber(source.recurringContribution) ??
        0,
      contributionFrequency: source.contributionFrequency ?? normalized.frequency,
      oneTimeContribution:
        this.resolveSavingsNumber(source.oneTimeContribution) ??
        this.resolveSavingsNumber(source.exceptionalContribution) ??
        normalized.exceptionalContribution
    };
  }

  private toSavingsCompareRequest(request: Partial<SavingsCompareRequest>): SavingsCompareApiRequest {
    const baseline = this.toSavingsCalculateRequest(request);
    const projection = projectSavings(request);
    const scenarios = projection.scenarios.length
      ? projection.scenarios.map((scenario, index) => this.toSavingsScenarioRequest(scenario.label, scenario.form, index))
      : [this.toSavingsScenarioRequest('Plan actuel', normalizeSavingsFormValue(request), 0)];

    return {
      ...baseline,
      scenarios
    };
  }

  private toSavingsScenarioRequest(
    label: string | null | undefined,
    form: SavingsCalculateRequest,
    index: number
  ): SavingsApiScenarioRequest {
    return {
      scenarioName: this.resolveSavingsScenarioName(label, index),
      targetAmount: form.targetAmount,
      initialContribution: form.initialContribution,
      monthlyContribution: form.recurringContribution,
      contributionFrequency: form.frequency,
      oneTimeContribution: form.exceptionalContribution
    };
  }

  private resolveSavingsScenarioName(label: string | null | undefined, index: number): string {
    const trimmedLabel = typeof label === 'string' ? label.trim() : '';
    return trimmedLabel || `Scenario ${index + 1}`;
  }

  private debugSavingsPayload(
    operation: 'calculate' | 'compare',
    payload: SavingsCalculateApiRequest | SavingsCompareApiRequest
  ): void {
    if (environment.production) {
      return;
    }

    console.debug(`[SimulationsService][Savings][${operation}] payload`, payload);
  }

  private toCreditRequest(request: CreditCalculateRequest): CreditCalculateApiRequest {
    return {
      loanAmount: request.creditAmount,
      downPayment: request.downPayment,
      durationMonths: request.durationMonths,
      annualInterestRate: request.interestRate,
      monthlyIncome: request.monthlyIncome,
      earlyRepaymentAmount: request.earlyRepaymentAmount,
      earlyRepaymentMonth: request.earlyRepaymentMonth
    };
  }

  private toCreditCompareRequest(request: Partial<CreditCompareRequest>): CreditCompareApiRequest {
    const projection = projectCredit(request);
    const scenarios = projection.scenarios.map((scenario, index) => (
      this.toCreditScenarioRequest(scenario.form, scenario.label, index)
    ));

    return {
      scenarios
    };
  }

  private toCreditScenarioRequest(
    form: CreditCalculateRequest,
    label: string | null | undefined,
    index: number
  ): CreditApiScenarioRequest {
    return {
      scenarioName: this.resolveCreditScenarioName(label, index),
      ...this.toCreditRequest(form)
    };
  }

  private resolveCreditScenarioName(label: string | null | undefined, index: number): string {
    const trimmedLabel = typeof label === 'string' ? label.trim() : '';
    return trimmedLabel || `Scenario ${index + 1}`;
  }

  private debugCreditPayload(
    operation: 'calculate' | 'compare',
    payload: CreditCalculateApiRequest | CreditCompareApiRequest
  ): void {
    if (environment.production) {
      return;
    }

    console.debug(`[SimulationsService][Credit][${operation}] payload`, payload);
  }

  private debugInvalidPayload(
    source: SimulationRequestSource | undefined,
    operation: 'calculate' | 'compare',
    endpoint: string,
    payload: SavingsCalculateApiRequest | SavingsCompareApiRequest | CreditCalculateApiRequest | CreditCompareApiRequest
  ): void {
    if (environment.production) {
      return;
    }

    const component = source?.component ?? 'SimulationsService';
    const method = source?.method ?? 'httpPost';

    console.warn(`[${component}][${method}][${operation}] skipped invalid payload for ${endpoint}`, payload);
  }

  private resolveSavingsNumber(value: number | null | undefined): number | null {
    return typeof value === 'number' && Number.isFinite(value) ? value : null;
  }

  private isSavingsCalculatePayloadValid(payload: SavingsCalculateApiRequest): boolean {
    return Number.isFinite(payload.targetAmount)
      && Number.isFinite(payload.initialContribution)
      && Number.isFinite(payload.monthlyContribution)
      && Number.isFinite(payload.oneTimeContribution)
      && payload.monthlyContribution >= 0
      && payload.initialContribution >= 0
      && payload.oneTimeContribution >= 0
      && !!payload.contributionFrequency;
  }

  private isSavingsComparePayloadValid(payload: SavingsCompareApiRequest): boolean {
    return this.isSavingsCalculatePayloadValid(payload)
      && Array.isArray(payload.scenarios)
      && payload.scenarios.length > 0
      && payload.scenarios.every((scenario) => (
        !!scenario.scenarioName?.trim() && this.isSavingsCalculatePayloadValid(scenario)
      ));
  }

  private isCreditPayloadValid(payload: CreditCalculateApiRequest | CreditApiScenarioRequest): boolean {
    return Number.isFinite(payload.loanAmount)
      && Number.isFinite(payload.downPayment)
      && Number.isFinite(payload.annualInterestRate)
      && Number.isFinite(payload.durationMonths)
      && Number.isFinite(payload.monthlyIncome)
      && Number.isFinite(payload.earlyRepaymentAmount)
      && Number.isFinite(payload.earlyRepaymentMonth)
      && payload.loanAmount >= 10_000
      && payload.downPayment >= 0
      && payload.downPayment <= payload.loanAmount
      && payload.annualInterestRate >= 0
      && payload.durationMonths >= 12
      && payload.earlyRepaymentMonth >= 1;
  }

  private isCreditComparePayloadValid(payload: CreditCompareApiRequest): boolean {
    return Array.isArray(payload.scenarios)
      && payload.scenarios.length > 0
      && payload.scenarios.every((scenario) => (
        !!scenario.scenarioName?.trim() && this.isCreditPayloadValid(scenario)
      ));
  }

  private normalizeSavingsCalculation(
    response: unknown,
    request: SavingsCalculateRequest
  ): SavingsScenarioResult {
    const fallback = projectSavings(request);
    const source = this.asRecord(response);
    const scenarioValue =
      source['scenario'] ??
      source['result'] ??
      source['data'] ??
      source['projection'] ??
      response;

    return this.overlaySavingsScenario(scenarioValue, fallback.scenarios[0], fallback.referenceDate);
  }

  private normalizeSavingsComparison(
    response: unknown,
    request: SavingsCompareRequest
  ): SavingsProjectionResult {
    const fallback = projectSavings(request);
    const source = this.asRecord(response);
    const rawScenarios = Array.isArray(response)
      ? response
      : this.pickArray(source, ['scenarios', 'results', 'data', 'items', 'comparisons']);

    if (!rawScenarios.length) {
      return fallback;
    }

    return {
      referenceDate: fallback.referenceDate,
      targetDate: fallback.targetDate,
      scenarios: fallback.scenarios.map((scenario, index) => {
        const rawScenario = this.findMatchingScenario(rawScenarios, scenario.id, index);
        return rawScenario
          ? this.overlaySavingsScenario(rawScenario, scenario, fallback.referenceDate)
          : scenario;
      })
    };
  }

  private normalizeCreditCalculation(
    response: unknown,
    request: CreditCalculateRequest
  ): CreditScenarioResult {
    const fallback = projectCredit(request);
    const source = this.asRecord(response);
    const scenarioValue =
      source['scenario'] ??
      source['result'] ??
      source['data'] ??
      source['projection'] ??
      response;

    return this.overlayCreditScenario(scenarioValue, fallback.scenarios[0], fallback.referenceDate);
  }

  private normalizeCreditComparison(
    response: unknown,
    request: CreditCompareRequest
  ): CreditProjectionResult {
    const fallback = projectCredit(request);
    const source = this.asRecord(response);
    const rawScenarios = Array.isArray(response)
      ? response
      : this.pickArray(source, ['scenarios', 'results', 'data', 'items', 'comparisons']);

    if (!rawScenarios.length) {
      return fallback;
    }

    return {
      referenceDate: fallback.referenceDate,
      scenarios: fallback.scenarios.map((scenario, index) => {
        const rawScenario = this.findMatchingScenario(rawScenarios, scenario.id, index);
        return rawScenario
          ? this.overlayCreditScenario(rawScenario, scenario, fallback.referenceDate)
          : scenario;
      })
    };
  }

  private overlaySavingsScenario(
    rawValue: unknown,
    fallback: SavingsScenarioResult,
    referenceDate: Date
  ): SavingsScenarioResult {
    const source = this.asRecord(rawValue);
    const durationMonths = this.pickNumber(source, ['durationMonths', 'months', 'duration']) ?? fallback.durationMonths;
    const completionDate = this.toDate(
      this.pickString(source, ['completionDate', 'estimatedCompletionDate', 'targetReachedAt', 'date']),
      fallback.completionDate
    );
    const monthlyEquivalent =
      this.pickNumber(source, ['monthlyEquivalent', 'monthlyContribution', 'requiredMonthlyContribution', 'monthlySavings']) ??
      fallback.monthlyEquivalent;
    const startAmount =
      this.pickNumber(source, ['startAmount', 'initialAmount', 'currentAmount', 'initialContribution']) ??
      fallback.startAmount;
    const totalContributed =
      this.pickNumber(source, ['totalContributed', 'projectedAmount', 'projectedTotal', 'totalSaved']) ??
      fallback.totalContributed;
    const remainingAmount =
      this.pickNumber(source, ['remainingAmount', 'remaining', 'amountRemaining']) ??
      Math.max(0, fallback.form.targetAmount - startAmount);
    const hitTargetDate = this.pickBoolean(source, ['hitTargetDate', 'withinTargetDate']) ?? fallback.hitTargetDate;
    const deltaMonthsToTarget =
      this.pickNumber(source, ['deltaMonthsToTarget', 'gapToTargetMonths', 'monthsOffset']) ??
      fallback.deltaMonthsToTarget;
    const points = this.normalizeSavingsPoints(source['points'], fallback, referenceDate, durationMonths, monthlyEquivalent, startAmount);
    const milestones = this.normalizeMilestones(source['milestones'], fallback.milestones, points);

    return {
      ...fallback,
      id: this.pickString(source, ['id', 'scenarioId', 'key']) ?? fallback.id,
      label: this.pickString(source, ['label', 'name', 'title']) ?? fallback.label,
      durationMonths,
      completionDate,
      monthlyEquivalent,
      startAmount,
      totalContributed,
      remainingAmount,
      points,
      milestones,
      hitTargetDate,
      deltaMonthsToTarget
    };
  }

  private overlayCreditScenario(
    rawValue: unknown,
    fallback: CreditScenarioResult,
    referenceDate: Date
  ): CreditScenarioResult {
    const source = this.asRecord(rawValue);
    const points = this.normalizeCreditPoints(source['points'] ?? source['schedule'], fallback.points, referenceDate);
    const earlyRepaymentSource = this.asRecord(source['earlyRepayment']);
    const principal = this.pickNumber(source, ['principal', 'loanAmount', 'borrowedAmount']) ?? fallback.principal;
    const lastPoint = points[points.length - 1] ?? fallback.points[fallback.points.length - 1];
    const totalRepayment = lastPoint?.cumulativePaid ?? fallback.totalRepayment;
    const totalInterest = lastPoint?.cumulativeInterest ?? fallback.totalInterest;
    const endDate = lastPoint?.date ?? fallback.endDate;
    const milestones = Array.isArray(source['milestones']) && source['milestones'].length
      ? this.normalizeMilestones(source['milestones'], fallback.milestones, points)
      : buildCreditMilestones(principal, points);

    return {
      ...fallback,
      id: this.pickString(source, ['id', 'scenarioId', 'key']) ?? fallback.id,
      label: this.pickString(source, ['label', 'name', 'title']) ?? fallback.label,
      principal,
      monthlyPayment:
        this.pickNumber(source, ['monthlyPayment', 'monthlyInstallment', 'mensuality']) ??
        fallback.monthlyPayment,
      totalRepayment,
      totalCost: totalRepayment + fallback.form.downPayment,
      totalInterest,
      debtRatio: this.pickNumber(source, ['debtRatio', 'effortRate', 'incomeRatio']) ?? fallback.debtRatio,
      endDate,
      points,
      milestones,
      earlyRepayment: Object.keys(earlyRepaymentSource).length
        ? {
            month: this.pickNumber(earlyRepaymentSource, ['month']) ?? fallback.earlyRepayment?.month ?? fallback.form.earlyRepaymentMonth,
            amount: this.pickNumber(earlyRepaymentSource, ['amount']) ?? fallback.earlyRepayment?.amount ?? fallback.form.earlyRepaymentAmount,
            newDurationMonths:
              this.pickNumber(earlyRepaymentSource, ['newDurationMonths', 'durationMonths']) ??
              fallback.earlyRepayment?.newDurationMonths ??
              lastPoint?.monthIndex ??
              fallback.form.durationMonths,
            newEndDate: this.toDate(
              this.pickString(earlyRepaymentSource, ['newEndDate', 'endDate']),
              fallback.earlyRepayment?.newEndDate ?? endDate
            ),
            interestSaved:
              this.pickNumber(earlyRepaymentSource, ['interestSaved', 'savedInterest']) ??
              fallback.earlyRepayment?.interestSaved ??
              0,
            termReductionMonths:
              this.pickNumber(earlyRepaymentSource, ['termReductionMonths', 'monthsSaved']) ??
              fallback.earlyRepayment?.termReductionMonths ??
              0
          }
        : fallback.earlyRepayment
    };
  }

  private normalizeSavingsPoints(
    rawValue: unknown,
    fallback: SavingsScenarioResult,
    referenceDate: Date,
    durationMonths: number,
    monthlyEquivalent: number,
    startAmount: number
  ): ProjectionPoint[] {
    const rawPoints = Array.isArray(rawValue) ? rawValue : [];

    if (rawPoints.length > 1) {
      return rawPoints.map((item, index) => {
        const source = this.asRecord(item);
        const date = this.toDate(this.pickString(source, ['date', 'month']), this.addMonths(referenceDate, index));
        return {
          monthIndex: this.pickNumber(source, ['monthIndex', 'index']) ?? index,
          date,
          amount: this.pickNumber(source, ['amount', 'value', 'projectedAmount']) ?? fallback.points[index]?.amount ?? 0
        };
      });
    }

    const horizon = Math.min(Math.max(durationMonths, 6), MAX_SAVINGS_MONTHS);
    return Array.from({ length: horizon + 1 }, (_, month) => ({
      monthIndex: month,
      date: this.addMonths(referenceDate, month),
      amount: Math.min(fallback.form.targetAmount, Math.round((startAmount + monthlyEquivalent * month) * 100) / 100)
    }));
  }

  private normalizeCreditPoints(
    rawValue: unknown,
    fallback: CreditPoint[],
    referenceDate: Date
  ): CreditPoint[] {
    const rawPoints = Array.isArray(rawValue) ? rawValue : [];

    if (rawPoints.length > 1) {
      return rawPoints.map((item, index) => {
        const source = this.asRecord(item);
        return {
          monthIndex: this.pickNumber(source, ['monthIndex', 'index']) ?? index,
          date: this.toDate(this.pickString(source, ['date', 'month']), this.addMonths(referenceDate, index)),
          remainingBalance: this.pickNumber(source, ['remainingBalance', 'balance', 'remainingPrincipal']) ?? fallback[index]?.remainingBalance ?? 0,
          cumulativePaid: this.pickNumber(source, ['cumulativePaid', 'amountPaid']) ?? fallback[index]?.cumulativePaid ?? 0,
          cumulativeInterest: this.pickNumber(source, ['cumulativeInterest', 'interestPaid']) ?? fallback[index]?.cumulativeInterest ?? 0
        };
      });
    }

    return fallback;
  }

  private normalizeMilestones<T extends ProjectionPoint | CreditPoint>(
    rawValue: unknown,
    fallback: ProjectionMilestone[],
    points: T[]
  ): ProjectionMilestone[] {
    const rawMilestones = Array.isArray(rawValue) ? rawValue : [];

    if (rawMilestones.length > 0) {
      return rawMilestones.map((item, index) => {
        const source = this.asRecord(item);
        const fallbackMilestone = fallback[index] ?? fallback[fallback.length - 1];
        return {
          label: this.pickString(source, ['label', 'title']) ?? fallbackMilestone?.label ?? `${(index + 1) * 25}%`,
          progress: this.pickNumber(source, ['progress', 'ratio']) ?? fallbackMilestone?.progress ?? 1,
          date: this.toDate(this.pickString(source, ['date']), fallbackMilestone?.date ?? this.extractDate(points[index], new Date())),
          amount: this.pickNumber(source, ['amount', 'value']) ?? fallbackMilestone?.amount ?? 0,
          caption: this.pickString(source, ['caption', 'description']) ?? fallbackMilestone?.caption ?? ''
        };
      });
    }

    return fallback;
  }

  private findMatchingScenario(
    scenarios: unknown[],
    fallbackId: string,
    index: number
  ): unknown | null {
    const direct = scenarios.find((item) => {
      const source = this.asRecord(item);
      const label = this.pickString(source, ['id', 'scenarioId', 'key', 'label', 'name', 'title'])?.toLowerCase() ?? '';
      return label.includes(fallbackId);
    });

    return direct ?? scenarios[index] ?? null;
  }

  private asRecord(value: unknown): Record<string, unknown> {
    return value && typeof value === 'object' && !Array.isArray(value)
      ? value as Record<string, unknown>
      : {};
  }

  private pickArray(source: Record<string, unknown>, keys: string[]): unknown[] {
    for (const key of keys) {
      const value = source[key];
      if (Array.isArray(value)) {
        return value;
      }
    }

    return [];
  }

  private pickString(source: Record<string, unknown>, keys: string[]): string | null {
    for (const key of keys) {
      const value = source[key];
      if (typeof value === 'string' && value.trim()) {
        return value.trim();
      }
    }

    return null;
  }

  private pickNumber(source: Record<string, unknown>, keys: string[]): number | null {
    for (const key of keys) {
      const value = source[key];
      if (typeof value === 'number' && Number.isFinite(value)) {
        return value;
      }
      if (typeof value === 'string' && value.trim()) {
        const parsed = Number(value);
        if (Number.isFinite(parsed)) {
          return parsed;
        }
      }
    }

    return null;
  }

  private pickBoolean(source: Record<string, unknown>, keys: string[]): boolean | null {
    for (const key of keys) {
      const value = source[key];
      if (typeof value === 'boolean') {
        return value;
      }
      if (typeof value === 'string') {
        const normalized = value.trim().toLowerCase();
        if (normalized === 'true') {
          return true;
        }
        if (normalized === 'false') {
          return false;
        }
      }
    }

    return null;
  }

  private toDate(value: string | null, fallback: Date): Date {
    if (!value) {
      return fallback;
    }

    const parsed = new Date(value);
    return Number.isNaN(parsed.getTime()) ? fallback : parsed;
  }

  private addMonths(date: Date, months: number): Date {
    return new Date(date.getFullYear(), date.getMonth() + months, date.getDate());
  }

  private extractDate(point: ProjectionPoint | CreditPoint | undefined, fallback: Date): Date {
    return point?.date ?? fallback;
  }
}
