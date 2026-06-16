import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, catchError, map, of, tap } from 'rxjs';
import { environment } from '../../../environments/environment';
import { AlertService } from '../../shared/alerts/alert.service';
import {
  BudgetAlertResponse,
  BudgetAlertSeverity,
  BudgetAlertType,
  BudgetTargetCategory,
  BudgetTargetCreateRequest,
  BudgetTargetLevel,
  BudgetTargetMonitoringStatus,
  BudgetTargetResponse,
  BudgetTargetSource,
  BudgetTargetStatus,
  BudgetTargetStatusUpdateRequest,
  getTransactionCategoryLabel,
  normalizeTransactionCategory
} from '../models';

@Injectable({ providedIn: 'root' })
export class BudgetTargetService {
  private readonly http = inject(HttpClient);
  private readonly apiUrl = `${environment.apiUrl}/budget-targets`;
  private readonly alertService = inject(AlertService);

  createBudgetTarget(payload: BudgetTargetCreateRequest): Observable<BudgetTargetResponse> {
    return this.http.post<unknown>(this.apiUrl, payload).pipe(
      map((response) => this.normalizeBudgetTargetResponse(response)),
      tap(() => this.triggerAlertsRefresh())
    );
  }

  getMyBudgetTargets(): Observable<BudgetTargetResponse[]> {
    return this.http.get<unknown>(`${this.apiUrl}/my`).pipe(
      map((response) => this.normalizeBudgetTargetsResponse(response))
    );
  }

  getMyBudgetAlerts(): Observable<BudgetAlertResponse[]> {
    return this.http.get<unknown>(`${this.apiUrl}/my/alerts`).pipe(
      map((response) => this.normalizeBudgetAlertsResponse(response))
    );
  }

  updateBudgetTargetStatus(
    id: number,
    payload: BudgetTargetStatusUpdateRequest
  ): Observable<BudgetTargetResponse> {
    return this.http.patch<unknown>(`${this.apiUrl}/${id}/status`, payload).pipe(
      map((response) => this.normalizeBudgetTargetResponse(response, id)),
      tap(() => this.triggerAlertsRefresh())
    );
  }

  private triggerAlertsRefresh(): void {
    this.alertService.refreshAlertsWithToast().pipe(
      catchError(() => of([]))
    ).subscribe();
  }

  private normalizeBudgetTargetsResponse(response: unknown): BudgetTargetResponse[] {
    const root = this.asRecord(response);
    const rawItems = Array.isArray(response)
      ? response
      : this.pickArray(root, ['content', 'items', 'data', 'budgetTargets', 'results']);

    return rawItems.map((item, index) => this.normalizeBudgetTarget(item, index + 1));
  }

  private normalizeBudgetTargetResponse(response: unknown, fallbackId = 0): BudgetTargetResponse {
    const root = this.asRecord(response);
    const nested =
      this.pickRecord(root, ['data', 'item', 'budgetTarget', 'content', 'result'])
      ?? root;

    return this.normalizeBudgetTarget(nested, fallbackId);
  }

  private normalizeBudgetAlertsResponse(response: unknown): BudgetAlertResponse[] {
    const root = this.asRecord(response);
    const rawItems = Array.isArray(response)
      ? response
      : this.pickArray(root, ['content', 'items', 'data', 'alerts', 'budgetAlerts', 'results']);

    return rawItems.map((item, index) => this.normalizeBudgetAlert(item, index + 1));
  }

  private normalizeBudgetTarget(value: unknown, fallbackId: number): BudgetTargetResponse {
    const source = this.asRecord(value);
    const category = this.normalizeCategory(this.pickString(source, ['category']) ?? 'AUTRES');
    const selectedLevel = this.normalizeLevel(this.pickString(source, ['selectedLevel']) ?? 'EQUILIBRE');
    const budgetTargetSource = this.normalizeSource(this.pickString(source, ['source']) ?? 'RECOMMENDATION_AI');
    const status = this.normalizeStatus(this.pickString(source, ['status']) ?? 'ACTIVE');
    const targetAmount =
      this.pickNumber(source, ['targetAmount', 'suggestedMonthlyAmount', 'monthlyAmount', 'amount']);
    const suggestedMonthlyAmount =
      this.pickNumber(source, ['suggestedMonthlyAmount', 'targetAmount', 'monthlyAmount', 'amount']) ?? 0;
    const monitoringStatus = this.normalizeMonitoringStatus(this.pickString(source, ['monitoringStatus']));

    return {
      id: this.pickNumber(source, ['id']) ?? fallbackId,
      category,
      categoryLabel:
        this.pickString(source, ['categoryLabel']) ??
        getTransactionCategoryLabel(category),
      selectedLevel,
      suggestedMonthlyAmount,
      targetAmount,
      spentThisMonth: this.pickNumber(source, ['spentThisMonth']),
      remainingAmount: this.pickNumber(source, ['remainingAmount']),
      usagePercent: this.pickNumber(source, ['usagePercent']),
      monitoringStatus,
      monitoringStatusLabel:
        this.pickString(source, ['monitoringStatusLabel']) ??
        (monitoringStatus ? this.humanizeEnum(monitoringStatus) : null),
      source: budgetTargetSource,
      recommendationId: this.pickString(source, ['recommendationId']),
      recommendationTitle: this.pickString(source, ['recommendationTitle']),
      summary: this.pickString(source, ['summary']),
      status,
      createdAt:
        this.pickString(source, ['createdAt']) ??
        new Date().toISOString(),
      updatedAt:
        this.pickString(source, ['updatedAt']) ??
        this.pickString(source, ['createdAt']) ??
        new Date().toISOString(),
      sourceLabel:
        this.pickString(source, ['sourceLabel']) ??
        this.humanizeEnum(budgetTargetSource),
      selectedLevelLabel:
        this.pickString(source, ['selectedLevelLabel']) ??
        this.humanizeEnum(selectedLevel),
      selectedLevelSummary: this.pickString(source, ['selectedLevelSummary']),
      statusLabel:
        this.pickString(source, ['statusLabel']) ??
        this.humanizeEnum(status),
      aiGenerated: this.pickBoolean(source, ['aiGenerated']) ?? (budgetTargetSource === 'RECOMMENDATION_AI')
    };
  }

  private normalizeBudgetAlert(value: unknown, fallbackPriorityRank: number): BudgetAlertResponse {
    const source = this.asRecord(value);
    const alertType = this.normalizeAlertType(this.pickString(source, ['alertType', 'type']) ?? 'BUDGET_QUASI_ATTEINT');
    const severity = this.normalizeAlertSeverity(this.pickString(source, ['severity']) ?? 'INFO');
    const categoryValue = this.pickString(source, ['category']);
    const category = categoryValue ? this.normalizeCategory(categoryValue) : null;

    return {
      alertType,
      severity,
      budgetTargetId: this.pickNumber(source, ['budgetTargetId', 'budgetId', 'targetId', 'id']),
      category,
      categoryLabel:
        this.pickString(source, ['categoryLabel']) ??
        (category ? getTransactionCategoryLabel(category) : null),
      title:
        this.pickString(source, ['title']) ??
        this.fallbackAlertTitle(alertType),
      message:
        this.pickString(source, ['message', 'summary', 'description']) ??
        this.fallbackAlertMessage(alertType),
      usagePercent: this.pickNumber(source, ['usagePercent']),
      targetAmount: this.pickNumber(source, ['targetAmount', 'suggestedMonthlyAmount', 'monthlyAmount', 'amount']),
      spentThisMonth: this.pickNumber(source, ['spentThisMonth']),
      remainingAmount: this.pickNumber(source, ['remainingAmount']),
      generatedAt: this.pickString(source, ['generatedAt', 'createdAt', 'updatedAt']),
      priorityRank: this.pickNumber(source, ['priorityRank', 'priority', 'rank']) ?? fallbackPriorityRank
    };
  }

  private normalizeCategory(value: string): BudgetTargetCategory {
    return normalizeTransactionCategory(value);
  }

  private normalizeLevel(value: string): BudgetTargetLevel {
    const normalized = value.trim().toUpperCase();

    switch (normalized) {
      case 'PRUDENT':
        return 'PRUDENT';
      case 'RENFORCE':
        return 'RENFORCE';
      case 'EQUILIBRE':
      default:
        return 'EQUILIBRE';
    }
  }

  private normalizeSource(value: string): BudgetTargetSource {
    return value.trim().toUpperCase() === 'MANUAL' ? 'MANUAL' : 'RECOMMENDATION_AI';
  }

  private normalizeStatus(value: string): BudgetTargetStatus {
    const normalized = value.trim().toUpperCase();

    switch (normalized) {
      case 'INACTIVE':
        return 'INACTIVE';
      case 'ARCHIVED':
        return 'ARCHIVED';
      case 'ACTIVE':
      default:
        return 'ACTIVE';
    }
  }

  private normalizeMonitoringStatus(value: string | null): BudgetTargetMonitoringStatus | null {
    if (!value?.trim()) {
      return null;
    }

    const normalized = value.trim().toUpperCase();

    switch (normalized) {
      case 'SOUS_CONTROLE':
        return 'SOUS_CONTROLE';
      case 'A_SURVEILLER':
        return 'A_SURVEILLER';
      case 'DEPASSE':
        return 'DEPASSE';
      default:
        return null;
    }
  }

  private normalizeAlertType(value: string): BudgetAlertType {
    const normalized = value.trim().toUpperCase();

    switch (normalized) {
      case 'BUDGET_DEPASSE':
        return 'BUDGET_DEPASSE';
      case 'BUDGET_RESTE_FAIBLE':
        return 'BUDGET_RESTE_FAIBLE';
      case 'BUDGET_SOUS_CONTROLE':
        return 'BUDGET_SOUS_CONTROLE';
      case 'BUDGET_CRITIQUE_PRIORITAIRE':
        return 'BUDGET_CRITIQUE_PRIORITAIRE';
      case 'BUDGET_MAITRISE_GLOBALE':
        return 'BUDGET_MAITRISE_GLOBALE';
      case 'BUDGET_QUASI_ATTEINT':
      default:
        return 'BUDGET_QUASI_ATTEINT';
    }
  }

  private normalizeAlertSeverity(value: string): BudgetAlertSeverity {
    const normalized = value.trim().toUpperCase();

    switch (normalized) {
      case 'CRITICAL':
        return 'CRITICAL';
      case 'WARNING':
        return 'WARNING';
      case 'INFO':
      default:
        return 'INFO';
    }
  }

  private fallbackAlertTitle(alertType: BudgetAlertType): string {
    switch (alertType) {
      case 'BUDGET_DEPASSE':
        return 'Budget depasse';
      case 'BUDGET_QUASI_ATTEINT':
        return 'Budget proche de sa limite';
      case 'BUDGET_RESTE_FAIBLE':
        return 'Reste disponible faible';
      case 'BUDGET_SOUS_CONTROLE':
        return 'Budget sous controle';
      case 'BUDGET_CRITIQUE_PRIORITAIRE':
        return 'Budget prioritaire a traiter';
      case 'BUDGET_MAITRISE_GLOBALE':
      default:
        return 'Maitrise budgetaire globale';
    }
  }

  private fallbackAlertMessage(alertType: BudgetAlertType): string {
    switch (alertType) {
      case 'BUDGET_DEPASSE':
        return 'Ce budget a depasse son cadre mensuel.';
      case 'BUDGET_QUASI_ATTEINT':
        return 'Ce budget approche de sa limite mensuelle.';
      case 'BUDGET_RESTE_FAIBLE':
        return 'Le reste disponible devient limite sur ce budget.';
      case 'BUDGET_SOUS_CONTROLE':
        return 'Ce budget reste bien maitrise pour le moment.';
      case 'BUDGET_CRITIQUE_PRIORITAIRE':
        return 'Ce budget merite une attention immediate.';
      case 'BUDGET_MAITRISE_GLOBALE':
      default:
        return 'Vos budgets restent globalement sous controle.';
    }
  }

  private humanizeEnum(value: string): string {
    return value
      .trim()
      .replace(/[_-]+/g, ' ')
      .toLowerCase()
      .split(' ')
      .filter(Boolean)
      .map((part) => `${part.charAt(0).toUpperCase()}${part.slice(1)}`)
      .join(' ');
  }

  private asRecord(value: unknown): Record<string, unknown> {
    return value && typeof value === 'object' && !Array.isArray(value)
      ? value as Record<string, unknown>
      : {};
  }

  private pickRecord(source: Record<string, unknown>, keys: string[]): Record<string, unknown> | null {
    const value = this.pickValue(source, keys);
    return value && typeof value === 'object' && !Array.isArray(value)
      ? value as Record<string, unknown>
      : null;
  }

  private pickArray(source: Record<string, unknown>, keys: string[]): unknown[] {
    const value = this.pickValue(source, keys);
    return Array.isArray(value) ? value : [];
  }

  private pickString(source: Record<string, unknown>, keys: string[]): string | null {
    const value = this.pickValue(source, keys);
    return typeof value === 'string' && value.trim() ? value.trim() : null;
  }

  private pickNumber(source: Record<string, unknown>, keys: string[]): number | null {
    const value = this.pickValue(source, keys);

    if (typeof value === 'number' && Number.isFinite(value)) {
      return value;
    }

    if (typeof value === 'string' && value.trim()) {
      const parsed = Number(value);
      return Number.isFinite(parsed) ? parsed : null;
    }

    return null;
  }

  private pickBoolean(source: Record<string, unknown>, keys: string[]): boolean | null {
    const value = this.pickValue(source, keys);

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

    return null;
  }

  private pickValue(source: Record<string, unknown>, keys: string[]): unknown {
    for (const key of keys) {
      if (key in source) {
        return source[key];
      }
    }

    return null;
  }
}
