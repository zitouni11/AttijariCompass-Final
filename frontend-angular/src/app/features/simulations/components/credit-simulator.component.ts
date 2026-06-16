import { CommonModule } from '@angular/common';
import { Component, DestroyRef, computed, effect, inject, signal } from '@angular/core';
import { takeUntilDestroyed, toObservable, toSignal } from '@angular/core/rxjs-interop';
import { AbstractControl, FormBuilder, ReactiveFormsModule, ValidationErrors, ValidatorFn, Validators } from '@angular/forms';
import { catchError, debounceTime, distinctUntilChanged, forkJoin, map, of, startWith, switchMap } from 'rxjs';
import { normalizeCreditFormValue, projectCredit } from '../simulations.engine';
import { formatDuration, formatLongDate, formatMoney, formatMonthLabel, formatPercent } from '../simulations.formatters';
import {
  BuilderHighlight,
  CompareScenarioCard,
  CreditFormValue,
  CreditProjectionResult,
  CreditScenarioResult,
  ProjectionMilestone,
  TimelineItem
} from '../simulations.models';
import { SimulationRequestSource, SimulationsService } from '../simulations.service';
import { environment } from '../../../../environments/environment';

const downPaymentValidator: ValidatorFn = (control: AbstractControl): ValidationErrors | null => {
  const creditAmount = Number(control.get('creditAmount')?.value ?? 0);
  const downPayment = Number(control.get('downPayment')?.value ?? 0);
  return downPayment > creditAmount ? { downPaymentExceeds: true } : null;
};

type CreditNumericControl =
  | 'creditAmount'
  | 'downPayment'
  | 'interestRate'
  | 'durationMonths'
  | 'monthlyIncome'
  | 'earlyRepaymentAmount'
  | 'earlyRepaymentMonth';

const DEFAULT_CREDIT_FORM: CreditFormValue = {
  creditAmount: 220000,
  downPayment: 30000,
  interestRate: 7.1,
  durationMonths: 84,
  monthlyIncome: 6200,
  earlyRepaymentAmount: 15000,
  earlyRepaymentMonth: 24
};

@Component({
  selector: 'app-credit-simulator',
  standalone: true,
  imports: [
    CommonModule,
    ReactiveFormsModule
  ],
  template: `
    <div class="simple-simulator">
      <aside class="input-column">
        <section class="form-card">
          <div class="section-head">
            <span class="section-kicker">Hypothèse</span>
            <h3>Simulateur Crédit</h3>
            <p>Estimez une mensualité, le coût total et la date de fin avec quelques paramètres essentiels.</p>
          </div>

          <form [formGroup]="form" class="simple-form">
            <label class="field">
              <span>Montant du crédit</span>
              <input type="number" formControlName="creditAmount" min="10000" step="1000" />
            </label>

            <label class="field">
              <span>Durée</span>
              <input type="number" formControlName="durationMonths" min="12" max="360" step="12" />
            </label>

            <label class="field">
              <span>Taux d’intérêt</span>
              <input type="number" formControlName="interestRate" min="0" max="30" step="0.1" />
            </label>

            <label class="field">
              <span>Capacité de remboursement</span>
              <input type="number" formControlName="monthlyIncome" min="0" step="100" />
            </label>

            <label class="field field-wide">
              <span>Remboursement anticipé optionnel</span>
              <input type="number" formControlName="earlyRepaymentAmount" min="0" step="1000" />
            </label>
          </form>

          @if (form.hasError('downPaymentExceeds')) {
            <div class="inline-alert">L’apport ne peut pas dépasser le montant total du projet.</div>
          }

          <div class="form-actions">
            <button type="button" class="primary-action" (click)="runSimulation()">Simuler</button>
            <button type="button" class="secondary-action" (click)="resetSimulation()">Réinitialiser</button>
          </div>
        </section>

        <article class="help-card">
          <span class="material-symbols-rounded">help</span>
          <div>
            <h3>Comment lire cette simulation ?</h3>
            <p>Modifiez les montants, lancez la simulation, puis comparez le plan actuel avec un scénario plus rapide. Les résultats sont des estimations destinées à vous aider à décider.</p>
          </div>
        </article>
      </aside>

      <section class="result-column">
        @if (!hasSimulated()) {
          <article class="empty-card">
            <span class="material-symbols-rounded">credit_score</span>
            <h3>Remplissez les champs puis lancez la simulation.</h3>
            <p>La mensualité estimée, les intérêts et la date de fin apparaîtront ici.</p>
          </article>
        } @else {
          @if (renderedScenario(); as scenario) {
            <article class="summary-card">
              <div class="section-head compact">
                <span class="section-kicker">Résultat principal</span>
                <h3>Mensualité estimée : {{ formatMoney(scenario.monthlyPayment) }}</h3>
                <p>{{ simpleStatusMessage() }}</p>
              </div>

              <div class="summary-primary-grid">
                <article class="summary-kpi summary-kpi-primary">
                  <span>Mensualité estimée</span>
                  <strong>{{ formatMoney(scenario.monthlyPayment) }}</strong>
                </article>
                <article class="summary-kpi">
                  <span>Coût total</span>
                  <strong>{{ formatMoney(scenario.totalCost) }}</strong>
                </article>
                <article class="summary-kpi">
                  <span>Intérêts estimés</span>
                  <strong>{{ formatMoney(scenario.totalInterest) }}</strong>
                </article>
                <article class="summary-kpi">
                  <span>Date de fin</span>
                  <strong>{{ formatLongDate(scenario.endDate) }}</strong>
                </article>
              </div>
            </article>
          }
        }

        <section class="results-zone">
          <div class="section-head compact">
            <span class="section-kicker">Comparaison</span>
            <h3>Scénarios simples</h3>
          </div>

          <div class="simple-scenarios">
            @for (scenario of projection().scenarios.slice(0, 3); track scenario.id; let index = $index) {
              <button
                type="button"
                class="scenario-card"
                [class.is-active]="renderedScenarioId() === scenario.id"
                (click)="applyScenarioSelection(scenario.id)"
              >
                <span>{{ creditScenarioLabel(scenario, index) }}</span>
                <strong>{{ formatMoney(scenario.monthlyPayment) }}</strong>
                <small>{{ formatDuration(scenario.form.durationMonths) }} · {{ formatLongDate(scenario.endDate) }}</small>
                <em>{{ creditScenarioStatus(scenario) }}</em>
              </button>
            }
          </div>
        </section>

      </section>
    </div>
  `,
  styleUrl: './credit-simulator.component.scss'
})
export class CreditSimulatorComponent {
  private readonly fb = inject(FormBuilder);
  private readonly destroyRef = inject(DestroyRef);
  private readonly simulationsService = inject(SimulationsService);
  private previousTimelineAudit: { formKey: string; timelineKey: string } | null = null;
  private readonly requestSource: SimulationRequestSource = {
    component: 'CreditSimulatorComponent',
    method: 'setupApiProjection'
  };

  readonly durationPresets = [60, 84, 120, 180];
  readonly selectedScenarioId = signal<string | null>(null);
  readonly hasSimulated = signal(false);
  readonly engineSource = signal<'api' | 'local'>('local');
  readonly engineError = signal<string | null>(null);
  readonly isSyncing = signal(false);
  readonly liveProjection = signal<CreditProjectionResult | null>(null);
  readonly liveScenario = signal<CreditScenarioResult | null>(null);
  readonly liveProjectionFormKey = signal<string | null>(null);
  readonly renderedScenario = signal<CreditScenarioResult | null>(null);
  readonly renderedTimelineItems = signal<TimelineItem[]>([]);

  readonly form = this.fb.group(
    {
      creditAmount: [DEFAULT_CREDIT_FORM.creditAmount, [Validators.required, Validators.min(10_000)]],
      downPayment: [DEFAULT_CREDIT_FORM.downPayment, [Validators.required, Validators.min(0)]],
      interestRate: [DEFAULT_CREDIT_FORM.interestRate, [Validators.required, Validators.min(0), Validators.max(30)]],
      durationMonths: [DEFAULT_CREDIT_FORM.durationMonths, [Validators.required, Validators.min(12), Validators.max(360)]],
      monthlyIncome: [DEFAULT_CREDIT_FORM.monthlyIncome, [Validators.min(0)]],
      earlyRepaymentAmount: [DEFAULT_CREDIT_FORM.earlyRepaymentAmount, [Validators.min(0)]],
      earlyRepaymentMonth: [DEFAULT_CREDIT_FORM.earlyRepaymentMonth, [Validators.required, Validators.min(1), Validators.max(360)]]
    },
    { validators: downPaymentValidator }
  );

  private readonly rawFormValue = toSignal(
    this.form.valueChanges.pipe(startWith(this.form.getRawValue())),
    { initialValue: this.form.getRawValue() }
  );

  readonly normalizedFormValue = computed<CreditFormValue>(() => {
    const raw = this.rawFormValue();

    return normalizeCreditFormValue({
      creditAmount: raw.creditAmount ?? undefined,
      downPayment: raw.downPayment ?? undefined,
      interestRate: raw.interestRate ?? undefined,
      durationMonths: raw.durationMonths ?? undefined,
      monthlyIncome: raw.monthlyIncome ?? undefined,
      earlyRepaymentAmount: raw.earlyRepaymentAmount ?? undefined,
      earlyRepaymentMonth: raw.earlyRepaymentMonth ?? undefined
    });
  });

  readonly currentDuration = computed(() => this.normalizedFormValue().durationMonths);
  readonly currentScenarioId = computed(() => `term-${this.currentDuration()}`);
  readonly currentFormKey = computed(() => this.buildFormKey(this.normalizedFormValue()));
  readonly financedAmount = computed(() => Math.max(0, this.normalizedFormValue().creditAmount - this.normalizedFormValue().downPayment));

  readonly localProjection = computed(() => projectCredit(this.normalizedFormValue()));

  readonly projection = computed(() => {
    const liveProjection = this.liveProjectionFormKey() === this.currentFormKey()
      ? this.liveProjection()
      : null;
    const liveScenario = this.liveProjectionFormKey() === this.currentFormKey()
      ? this.liveScenario()
      : null;

    return this.rebuildCreditProjection(
      this.localProjection(),
      liveProjection,
      liveScenario
    );
  });
  readonly renderedScenarioId = computed(() => this.renderedScenario()?.id ?? this.currentScenarioId());

  readonly selectedScenario = computed(() => {
    const scenarios = this.projection().scenarios;
    const fallback = this.findCurrentScenario(scenarios) ?? scenarios[0];

    if (!fallback) {
      throw new Error('Credit scenarios unavailable');
    }

    return scenarios.find((scenario) => scenario.id === this.selectedScenarioId()) ?? fallback;
  });
  readonly activeScenario = computed(() => this.renderedScenario() ?? this.selectedScenario());
  readonly scenarioBadges = computed(() => this.buildScenarioBadges(this.activeScenario()));

  readonly builderHighlights = computed<BuilderHighlight[]>(() => {
    const scenario = this.activeScenario();
    return [
      { label: 'Capital finance', value: formatMoney(scenario.principal), tone: 'strong' },
      { label: 'Mensualite estimee', value: formatMoney(scenario.monthlyPayment), tone: 'warning' },
      { label: 'Ratio d effort', value: formatPercent(scenario.debtRatio), tone: 'neutral' }
    ];
  });

  readonly compareCards = computed<CompareScenarioCard[]>(() => (
    this.projection().scenarios.map((scenario) => ({
      id: scenario.id,
      label: scenario.label,
      badge: `${scenario.form.durationMonths} mois`,
      headline: formatMoney(scenario.monthlyPayment),
      description: this.describeScenario(scenario),
      accent: this.mapAccent(scenario),
      metrics: [
        {
          label: 'Interets',
          value: formatMoney(scenario.totalInterest),
          tone: scenario.tone === 'growth' ? 'warning' : 'neutral'
        },
        {
          label: 'Cout global',
          value: formatMoney(scenario.totalCost),
          hint: 'Apport et remboursements cumules'
        },
        {
          label: 'Ratio d effort',
          value: formatPercent(scenario.debtRatio),
          tone: this.resolveDebtTone(scenario)
        }
      ],
      footer: this.footerCopy(scenario)
    }))
  ));

  readonly lineChartModel = computed(() => {
    const scenarios = this.projection().scenarios;
    const longestScenario = scenarios.reduce((longest, current) => (
      current.points.length > longest.points.length ? current : longest
    ));

    return {
      title: 'Capital restant du selon le scenario',
      subtitle: 'Visualisez le rythme d extinction du capital finance et l ecart de cadence entre les differentes durees.',
      labels: longestScenario.points.map((point) => formatMonthLabel(point.date)),
      series: scenarios.map((scenario) => ({
        label: `${scenario.label} - ${scenario.form.durationMonths} mois`,
        values: longestScenario.points.map((_, index) => scenario.points[index]?.remainingBalance ?? 0),
        color: this.resolveLineColor(scenario),
        fillColor: this.resolveFillColor(scenario),
        tension: 0.28
      }))
    };
  });

  readonly barChartModel = computed(() => ({
    title: 'Charge d interets par horizon',
    subtitle: 'La vue compare immediatement le trade off entre souplesse mensuelle et cout financier total.',
    items: this.projection().scenarios.map((scenario) => ({
      label: `${scenario.form.durationMonths} mois`,
      value: scenario.totalInterest,
      color: this.resolveLineColor(scenario),
      meta: formatMoney(scenario.monthlyPayment)
    }))
  }));

  readonly engineStatusLabel = computed(() => {
    if (this.isSyncing()) {
      return 'Calcul en cours';
    }

    return this.engineSource() === 'api'
      ? 'Calcul synchronisé'
      : 'Estimation disponible';
  });

  readonly engineStatusCaption = computed(() => {
    if (this.engineSource() === 'api') {
      return 'Calcul réalisé avec le moteur de simulation de l’application.';
    }

    return this.engineError()
      ? 'Le calcul distant est indisponible. Une estimation locale est utilisée.'
      : 'Une estimation rapide reste disponible pendant votre saisie.';
  });

  readonly statusMessage = computed(() => {
    const scenario = this.activeScenario();
    const earlyRepayment = scenario.earlyRepayment;

    if (!earlyRepayment) {
      return `Avec ce scenario, une mensualite estimee a ${formatMoney(scenario.monthlyPayment)} permet d amortir ${formatMoney(scenario.principal)} sur ${formatDuration(scenario.form.durationMonths)}.`;
    }

    if (earlyRepayment.termReductionMonths <= 0 && earlyRepayment.interestSaved <= 0) {
      return `Avec ce scenario, le remboursement anticipe de ${formatMoney(earlyRepayment.amount)} au mois ${earlyRepayment.month} modifie peu le calendrier de remboursement.`;
    }

    return `Avec ce scenario, un remboursement anticipe de ${formatMoney(earlyRepayment.amount)} au mois ${earlyRepayment.month} reduit la duree du credit de ${earlyRepayment.termReductionMonths} mois et diminue la charge d interets de ${formatMoney(earlyRepayment.interestSaved)}.`;
  });

  readonly formatMoney = formatMoney;
  readonly formatLongDate = formatLongDate;
  readonly formatDuration = formatDuration;
  readonly formatPercent = formatPercent;

  constructor() {
    this.setupSelectionSync();
    this.setupRenderedState();
    this.setupApiProjection();
    this.setupDebugLogging();
  }

  setNumericValue(controlName: CreditNumericControl, value: number | string): void {
    const parsed = Number(value);

    if (!Number.isFinite(parsed)) {
      return;
    }

    this.form.controls[controlName].setValue(parsed);
  }

  resetSimulation(): void {
    this.form.reset({ ...DEFAULT_CREDIT_FORM });
    this.selectedScenarioId.set(`term-${DEFAULT_CREDIT_FORM.durationMonths}`);
    this.hasSimulated.set(false);
  }

  runSimulation(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }

    this.hasSimulated.set(true);
  }

  clearEarlyRepayment(): void {
    this.form.patchValue({
      earlyRepaymentAmount: 0,
      earlyRepaymentMonth: Math.min(DEFAULT_CREDIT_FORM.earlyRepaymentMonth, this.normalizedFormValue().durationMonths)
    });
  }

  applyScenarioSelection(scenarioId: string): void {
    const selectedCard = this.projection().scenarios.find((scenario) => scenario.id === scenarioId);

    if (!selectedCard) {
      return;
    }

    this.selectedScenarioId.set(selectedCard.id);
    this.form.patchValue({
      durationMonths: selectedCard.form.durationMonths,
      earlyRepaymentMonth: Math.min(this.normalizedFormValue().earlyRepaymentMonth, selectedCard.form.durationMonths)
    });
  }

  simpleStatusMessage(): string {
    const scenario = this.activeScenario();
    const earlyRepayment = scenario.earlyRepayment;

    if (earlyRepayment?.termReductionMonths) {
      return `Un remboursement anticipé peut réduire la durée d’environ ${earlyRepayment.termReductionMonths} mois.`;
    }

    return `Votre crédit se termine autour de ${formatLongDate(scenario.endDate)} avec un coût total estimé à ${formatMoney(scenario.totalCost)}.`;
  }

  creditScenarioLabel(scenario: CreditScenarioResult, index = 0): string {
    if (index === 0) {
      return 'Plan actuel';
    }

    if (index === 1) {
      return 'Durée plus courte';
    }

    if (scenario.earlyRepayment?.amount || this.normalizedFormValue().earlyRepaymentAmount > 0) {
      return 'Remboursement anticipé';
    }

    return 'Mensualité réduite';
  }

  creditScenarioStatus(scenario: CreditScenarioResult): string {
    if (scenario.debtRatio !== null && scenario.debtRatio > 40) {
      return 'Difficile';
    }

    if (scenario.debtRatio !== null && scenario.debtRatio > 30) {
      return 'À surveiller';
    }

    return 'Réaliste';
  }

  private setupSelectionSync(): void {
    toObservable(this.normalizedFormValue)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe((formValue) => {
        this.selectedScenarioId.set(`term-${formValue.durationMonths}`);
      });
  }

  private setupRenderedState(): void {
    effect(() => {
      const formValue = this.normalizedFormValue();
      const scenario = this.selectedScenario();
      const renderedScenario = this.cloneCreditScenario(scenario);
      const recalculatedTimeline = this.buildTimelineItems(renderedScenario);

      this.validateTimelineConsistency(formValue, renderedScenario, recalculatedTimeline);

      if (!environment.production) {
        console.debug('[CreditSimulator] current scenario retained', {
          selectedScenarioId: this.selectedScenarioId(),
          currentDurationId: this.currentScenarioId(),
          retainedScenarioId: renderedScenario.id,
          retainedDurationMonths: renderedScenario.form.durationMonths,
          formDurationMonths: formValue.durationMonths
        });
        console.debug('[CreditSimulator] timeline recalculated before binding', recalculatedTimeline);
      }

      this.renderedScenario.set(renderedScenario);
      this.renderedTimelineItems.set([...recalculatedTimeline]);
    }, { allowSignalWrites: true });
  }

  private setupApiProjection(): void {
    toObservable(this.normalizedFormValue)
      .pipe(
        map((formValue) => ({
          formValue,
          formKey: this.buildFormKey(formValue)
        })),
        distinctUntilChanged((previous, current) => previous.formKey === current.formKey),
        debounceTime(240),
        switchMap(({ formValue, formKey }) => {
          const fallback = projectCredit(formValue);
          const fallbackCurrentScenario = this.findScenarioByDuration(fallback.scenarios, formValue.durationMonths) ?? fallback.scenarios[0];

          if (!this.canRequestCreditApi(formValue)) {
            if (!environment.production) {
              console.debug('[CreditSimulator] API skipped, form invalid', {
                values: this.form.getRawValue(),
                invalidControls: this.collectInvalidControls()
              });
            }

            this.isSyncing.set(false);
            this.engineError.set(null);
            this.liveProjection.set(null);
            this.liveScenario.set(null);
            this.liveProjectionFormKey.set(null);

            return of({
              source: 'local' as const,
              formKey,
              calculated: fallbackCurrentScenario,
              compared: fallback,
              error: null
            });
          }

          this.isSyncing.set(true);
          this.engineError.set(null);
          this.liveProjection.set(null);
          this.liveScenario.set(null);
          this.liveProjectionFormKey.set(null);

          return forkJoin({
            calculated: this.simulationsService.calculateCredit(formValue, this.requestSource),
            compared: this.simulationsService.compareCredit(formValue, this.requestSource)
          }).pipe(
            map((result) => ({
              source: 'api' as const,
              formKey,
              calculated: result.calculated,
              compared: result.compared,
              error: null
            })),
            catchError((error) => of({
              source: 'local' as const,
              formKey,
              calculated: fallbackCurrentScenario,
              compared: fallback,
              error
            }))
          );
        }),
        takeUntilDestroyed(this.destroyRef)
      )
      .subscribe((state) => {
        this.isSyncing.set(false);
        this.engineSource.set(state.source);
        this.liveScenario.set(state.calculated);
        this.liveProjection.set(state.compared);
        this.liveProjectionFormKey.set(state.formKey);
        this.engineError.set(
          state.error?.error?.message ||
          state.error?.message ||
          (state.source === 'local' ? 'Estimation locale en cours.' : null)
        );
      });
  }

  private rebuildCreditProjection(
    localProjection: CreditProjectionResult,
    liveProjection: CreditProjectionResult | null,
    liveScenario: CreditScenarioResult | null
  ): CreditProjectionResult {
    const liveScenarios = liveProjection?.scenarios ?? [];
    const scenarios = localProjection.scenarios.map((localScenario) => {
      const comparedScenario = this.findMatchingScenario(liveScenarios, localScenario);
      const mergedScenario = this.mergeCreditScenario(localScenario, comparedScenario);

      if (liveScenario && this.isSameScenario(liveScenario, localScenario)) {
        return this.mergeCreditScenario(mergedScenario, liveScenario);
      }

      return mergedScenario;
    });

    return {
      referenceDate: liveProjection?.referenceDate ?? localProjection.referenceDate,
      scenarios: [...scenarios]
    };
  }

  private buildFormKey(formValue: CreditFormValue): string {
    return [
      formValue.creditAmount,
      formValue.downPayment,
      formValue.interestRate,
      formValue.durationMonths,
      formValue.monthlyIncome,
      formValue.earlyRepaymentAmount,
      formValue.earlyRepaymentMonth
    ].join('|');
  }

  private canRequestCreditApi(formValue: CreditFormValue): boolean {
    const raw = this.form.getRawValue();

    return !this.form.invalid
      && raw.creditAmount !== null
      && raw.creditAmount !== undefined
      && raw.downPayment !== null
      && raw.downPayment !== undefined
      && raw.interestRate !== null
      && raw.interestRate !== undefined
      && raw.durationMonths !== null
      && raw.durationMonths !== undefined
      && raw.earlyRepaymentMonth !== null
      && raw.earlyRepaymentMonth !== undefined
      && Number.isFinite(formValue.creditAmount)
      && Number.isFinite(formValue.downPayment)
      && Number.isFinite(formValue.interestRate)
      && Number.isFinite(formValue.durationMonths);
  }

  private collectInvalidControls(): string[] {
    return Object.entries(this.form.controls)
      .filter(([, control]) => control.invalid)
      .map(([name]) => name);
  }

  private mergeCreditScenario(
    baseScenario: CreditScenarioResult,
    overrideScenario: CreditScenarioResult | null | undefined
  ): CreditScenarioResult {
    if (!overrideScenario) {
      return this.cloneCreditScenario(baseScenario);
    }

    const points = overrideScenario.points.length ? overrideScenario.points : baseScenario.points;
    const milestones = overrideScenario.milestones.length ? overrideScenario.milestones : baseScenario.milestones;

    return {
      ...baseScenario,
      ...overrideScenario,
      form: {
        ...baseScenario.form,
        ...overrideScenario.form
      },
      points: points.map((point) => ({ ...point })),
      milestones: milestones.map((milestone) => ({ ...milestone })),
      earlyRepayment: overrideScenario.earlyRepayment
        ? { ...overrideScenario.earlyRepayment }
        : baseScenario.earlyRepayment
          ? { ...baseScenario.earlyRepayment }
          : null
    };
  }

  private cloneCreditScenario(scenario: CreditScenarioResult): CreditScenarioResult {
    return {
      ...scenario,
      form: { ...scenario.form },
      points: scenario.points.map((point) => ({ ...point })),
      milestones: scenario.milestones.map((milestone) => ({ ...milestone })),
      earlyRepayment: scenario.earlyRepayment ? { ...scenario.earlyRepayment } : null
    };
  }

  private findCurrentScenario(scenarios: CreditScenarioResult[]): CreditScenarioResult | null {
    return (
      scenarios.find((scenario) => scenario.id === this.currentScenarioId()) ??
      this.findScenarioByDuration(scenarios, this.currentDuration()) ??
      null
    );
  }

  private findScenarioByDuration(
    scenarios: CreditScenarioResult[],
    durationMonths: number
  ): CreditScenarioResult | null {
    return scenarios.find((scenario) => scenario.form.durationMonths === durationMonths) ?? null;
  }

  private findMatchingScenario(
    scenarios: CreditScenarioResult[],
    targetScenario: CreditScenarioResult
  ): CreditScenarioResult | null {
    return (
      scenarios.find((scenario) => scenario.id === targetScenario.id) ??
      this.findScenarioByDuration(scenarios, targetScenario.form.durationMonths)
    );
  }

  private isSameScenario(
    left: CreditScenarioResult,
    right: CreditScenarioResult
  ): boolean {
    return left.id === right.id || left.form.durationMonths === right.form.durationMonths;
  }

  private buildTimelineItems(scenario: CreditScenarioResult): TimelineItem[] {
    return scenario.milestones.map((milestone) => this.mapTimelineItem(milestone, scenario));
  }

  private buildScenarioBadges(scenario: CreditScenarioResult): string[] {
    const badges = [this.buildDurationBadge(scenario), this.buildDebtBadge(scenario), this.buildInterestBadge(scenario)];
    return badges.filter((badge, index, source) => !!badge && source.indexOf(badge) === index).slice(0, 3);
  }

  private buildDurationBadge(scenario: CreditScenarioResult): string {
    if (scenario.form.durationMonths <= 72) {
      return 'Horizon agile';
    }
    if (scenario.form.durationMonths >= 144) {
      return 'Horizon long';
    }
    return 'Lecture equilibree';
  }

  private buildDebtBadge(scenario: CreditScenarioResult): string {
    if (scenario.debtRatio === null) {
      return 'Effort non renseigne';
    }
    if (scenario.debtRatio <= 30) {
      return 'Effort modere';
    }
    if (scenario.debtRatio <= 40) {
      return 'Effort suivi';
    }
    return 'Effort soutenu';
  }

  private buildInterestBadge(scenario: CreditScenarioResult): string {
    if (scenario.totalInterest <= scenario.principal * 0.22) {
      return 'Charge contenue';
    }
    if (scenario.totalInterest >= scenario.principal * 0.45) {
      return 'Charge etendue';
    }
    return 'Charge lissee';
  }

  private validateTimelineConsistency(
    formValue: CreditFormValue,
    scenario: CreditScenarioResult,
    timelineItems: TimelineItem[]
  ): void {
    if (environment.production) {
      return;
    }

    const milestoneTimes = scenario.milestones.map((milestone) => milestone.date.getTime());
    const isChronological = milestoneTimes.every((time, index) => index === 0 || milestoneTimes[index - 1] <= time);
    const matchesEndDate = scenario.milestones[scenario.milestones.length - 1]?.date.getTime() === scenario.endDate.getTime();

    if (!isChronological) {
      console.warn('[CreditSimulator] timeline chronology assertion failed', {
        scenarioId: scenario.id,
        milestones: scenario.milestones
      });
    }

    if (!matchesEndDate) {
      console.warn('[CreditSimulator] timeline endDate assertion failed', {
        scenarioId: scenario.id,
        endDate: scenario.endDate,
        milestone100: scenario.milestones[scenario.milestones.length - 1]
      });
    }

    const formKey = this.buildFormKey(formValue);
    const timelineKey = timelineItems.map((item) => `${item.label}:${item.dateLabel}:${item.valueLabel}`).join('|');

    if (
      this.previousTimelineAudit &&
      this.previousTimelineAudit.formKey !== formKey &&
      this.previousTimelineAudit.timelineKey === timelineKey
    ) {
      console.warn('[CreditSimulator] timeline remained unchanged after financial inputs changed', {
        previous: this.previousTimelineAudit,
        next: { formKey, timelineKey },
        scenarioId: scenario.id
      });
    }

    this.previousTimelineAudit = { formKey, timelineKey };
  }

  private setupDebugLogging(): void {
    if (environment.production) {
      return;
    }

    effect(() => {
      const formValue = this.normalizedFormValue();
      const projection = this.projection();
      const scenario = this.renderedScenario();
      const timeline = this.renderedTimelineItems();

      if (!scenario) {
        return;
      }

      console.debug('[CreditSimulator] form parameters', { ...formValue });
      console.debug('[CreditSimulator] scenario recalculated', {
        source: this.engineSource(),
        selectedScenarioId: scenario.id,
        currentDurationRetained: scenario.form.durationMonths,
        comparedDurations: projection.scenarios.map((item) => item.form.durationMonths),
        principal: scenario.principal,
        monthlyPayment: scenario.monthlyPayment,
        totalInterest: scenario.totalInterest,
        totalCost: scenario.totalCost,
        endDate: scenario.endDate,
        earlyRepayment: scenario.earlyRepayment
      });
      console.debug('[CreditSimulator] timeline bound in template', timeline.map((item) => ({
        label: item.label,
        progress: item.progress,
        dateLabel: item.dateLabel,
        valueLabel: item.valueLabel
      })));
    });
  }

  private mapTimelineItem(
    milestone: ProjectionMilestone,
    scenario: CreditScenarioResult
  ): TimelineItem {
    return {
      label: milestone.label,
      progress: milestone.progress,
      dateLabel: formatLongDate(milestone.date),
      valueLabel: formatMoney(milestone.amount),
      caption: `Sur un capital finance de ${formatMoney(scenario.principal)}.`
    };
  }

  private describeScenario(scenario: CreditScenarioResult): string {
    if (scenario.id === `term-${this.currentDuration()}`) {
      return 'Scenario cale sur la duree actuellement retenue dans le formulaire.';
    }
    if (scenario.form.durationMonths < this.currentDuration()) {
      return 'Lecture plus resserree avec une mensualite plus soutenue et une sortie anticipee.';
    }
    return 'Cadence plus souple au mois, avec une facture d interets plus etalee dans le temps.';
  }

  private footerCopy(scenario: CreditScenarioResult): string {
    if (scenario.earlyRepayment?.termReductionMonths) {
      return `Remboursement anticipe simule : sortie avancee de ${scenario.earlyRepayment.termReductionMonths} mois.`;
    }
    return `Date de fin estimee : ${formatLongDate(scenario.endDate)}.`;
  }

  private mapAccent(scenario: CreditScenarioResult): 'orange' | 'charcoal' | 'sand' {
    if (scenario.form.durationMonths === this.currentDuration()) {
      return 'charcoal';
    }
    return scenario.form.durationMonths < this.currentDuration() ? 'orange' : 'sand';
  }

  private resolveDebtTone(scenario: CreditScenarioResult): 'neutral' | 'positive' | 'warning' | 'danger' {
    if (scenario.debtRatio === null) {
      return 'neutral';
    }
    if (scenario.debtRatio <= 30) {
      return 'positive';
    }
    if (scenario.debtRatio <= 40) {
      return 'warning';
    }
    return 'danger';
  }

  private resolveLineColor(scenario: CreditScenarioResult): string {
    if (scenario.form.durationMonths === this.currentDuration()) {
      return '#1C1C1C';
    }
    return scenario.form.durationMonths < this.currentDuration() ? '#F58220' : '#B67E4B';
  }

  private resolveFillColor(scenario: CreditScenarioResult): string {
    if (scenario.form.durationMonths === this.currentDuration()) {
      return 'rgba(28, 28, 28, 0.08)';
    }
    return scenario.form.durationMonths < this.currentDuration()
      ? 'rgba(245, 130, 32, 0.18)'
      : 'rgba(182, 126, 75, 0.16)';
  }
}
