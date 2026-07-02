import { CommonModule } from '@angular/common';
import { Component, DestroyRef, computed, effect, inject, signal } from '@angular/core';
import { takeUntilDestroyed, toObservable, toSignal } from '@angular/core/rxjs-interop';
import { AbstractControl, FormBuilder, ReactiveFormsModule, ValidationErrors, ValidatorFn, Validators } from '@angular/forms';
import { catchError, debounceTime, distinctUntilChanged, map, of, startWith, switchMap } from 'rxjs';
import { normalizeCreditFormValue, projectCredit } from '../simulations.engine';
import { formatDuration, formatLongDate, formatMoney, formatPercent } from '../simulations.formatters';
import {
  CreditFormValue,
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
  | 'existingMonthlyCharges'
  | 'earlyRepaymentAmount'
  | 'earlyRepaymentMonth';

const DEFAULT_CREDIT_FORM: CreditFormValue = {
  creditAmount: 220000,
  downPayment: 0,
  interestRate: 7.1,
  durationMonths: 84,
  monthlyIncome: 6200,
  existingMonthlyCharges: 0,
  earlyRepaymentAmount: 0,
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
            <span class="section-kicker">Simulation bancaire</span>
            <h3>Paramètres du crédit</h3>
            <p>Renseignez votre projet et votre situation financière pour obtenir une estimation claire.</p>
          </div>

          <form [formGroup]="form" class="credit-form">
            <fieldset class="form-section-card">
              <legend><span>1</span> Crédit demandé</legend>
              <p>Définissez le financement souhaité et ses conditions.</p>
              <div class="section-fields">
                <label class="field">
                  <span>Montant du crédit demandé</span>
                  <small>Capital que vous souhaitez emprunter.</small>
                  <div class="input-shell">
                    <input type="number" formControlName="creditAmount" min="10000" step="1000" placeholder="Ex. 220000" />
                    <em>DT</em>
                  </div>
                </label>

                <label class="field">
                  <span>Durée en mois</span>
                  <small>Durée contractuelle en mois.</small>
                  <div class="input-shell">
                    <input type="number" formControlName="durationMonths" min="12" max="360" step="12" placeholder="Ex. 84" />
                    <em>mois</em>
                  </div>
                </label>

                <label class="field field-wide">
                  <span>Taux d’intérêt annuel</span>
                  <small>Taux nominal utilisé pour estimer la mensualité.</small>
                  <div class="input-shell">
                    <input type="number" formControlName="interestRate" min="0" max="30" step="0.1" placeholder="Ex. 7,1" />
                    <em>%</em>
                  </div>
                </label>
              </div>
            </fieldset>

            <fieldset class="form-section-card">
              <legend><span>2</span> Situation financière</legend>
              <p>Ces informations déterminent votre capacité réelle de remboursement.</p>
              <div class="section-fields">
                <label class="field">
                  <span>Revenu mensuel net</span>
                  <small>Revenu disponible après impôts.</small>
                  <div class="input-shell">
                    <input type="number" formControlName="monthlyIncome" min="1" step="100" placeholder="Ex. 6200" />
                    <em>DT</em>
                  </div>
                </label>

                <label class="field">
                  <span>Charges mensuelles existantes</span>
                  <small>Crédits, loyers et engagements récurrents.</small>
                  <div class="input-shell">
                    <input type="number" formControlName="existingMonthlyCharges" min="0" step="100" placeholder="Ex. 800" />
                    <em>DT</em>
                  </div>
                </label>

              </div>
            </fieldset>

            <fieldset class="form-section-card">
              <legend><span>3</span> Options</legend>
              <p>Mesurez l’effet d’un remboursement anticipé éventuel.</p>
              <div class="section-fields">
                <label class="field field-wide">
                  <span>Remboursement anticipé optionnel</span>
                  <small>Laissez 0 si vous ne prévoyez aucun versement exceptionnel.</small>
                  <div class="input-shell">
                    <input type="number" formControlName="earlyRepaymentAmount" min="0" step="1000" placeholder="Ex. 15000" />
                    <em>DT</em>
                  </div>
                </label>

                @if ((form.controls.earlyRepaymentAmount.value ?? 0) > 0) {
                  <label class="field field-wide">
                    <span>Mois prévu du remboursement anticipé</span>
                    <small>La nouvelle durée estimée apparaîtra dans le résultat.</small>
                    <div class="input-shell">
                      <input type="number" formControlName="earlyRepaymentMonth" min="1" [max]="currentDuration()" step="1" placeholder="Ex. 24" />
                      <em>mois</em>
                    </div>
                  </label>
                }
              </div>
            </fieldset>
          </form>

          @if (form.hasError('downPaymentExceeds')) {
            <div class="inline-alert">L’apport ne peut pas dépasser le montant total du projet.</div>
          }

          <div class="form-actions">
            <button type="button" class="primary-action" (click)="runSimulation()">Simuler</button>
            <button type="button" class="secondary-action" (click)="resetSimulation()">Réinitialiser</button>
          </div>
        </section>

      </aside>

      <section class="result-column">
        @if (!hasSimulated()) {
          <article class="empty-card">
            <span class="material-symbols-rounded">account_balance</span>
            <h3>Lancez une simulation pour obtenir votre estimation.</h3>
            <p>Vous verrez ici la décision estimée, les indicateurs essentiels et une recommandation adaptée.</p>
          </article>
        } @else {
          @if (renderedScenario(); as scenario) {
            <article
              class="eligibility-card decision-card"
              [class.eligibility-card-eligible]="scenario.eligibility.status === 'ELIGIBLE'"
              [class.eligibility-card-watch]="scenario.eligibility.status === 'WATCH'"
              [class.eligibility-card-rejected]="scenario.eligibility.status === 'NOT_ELIGIBLE'"
            >
              @if (scenario.eligibility.status !== 'ELIGIBLE') {
                <div class="decision-alert">
                  <span class="material-symbols-rounded">warning</span>
                  <strong>{{ simulationAlertLabel(scenario) }}</strong>
                </div>
              }

              <div class="eligibility-head">
                <div>
                  <span class="section-kicker">Analyse bancaire</span>
                  <h3>Estimation bancaire indicative</h3>
                </div>
                <span class="eligibility-status">{{ eligibilityStatusLabel(scenario.eligibility.status) }}</span>
              </div>

              <p class="decision-message">{{ scenario.eligibility.message }}</p>

              <div class="decision-facts">
                <div>
                  <span>Statut</span>
                  <strong>{{ eligibilityStatusLabel(scenario.eligibility.status) }}</strong>
                </div>
                <div>
                  <span>Limite recommandée</span>
                  <strong>40 %</strong>
                </div>
                <div>
                  <span>Capacité réelle</span>
                  <strong>{{ formatMoney(scenario.eligibility.realRepaymentCapacity) }}</strong>
                </div>
                <div>
                  <span>Montant maximum conseillé</span>
                  <strong>{{ formatMoney(scenario.eligibility.maximumRecommendedAmount) }}</strong>
                </div>
              </div>

              <div class="debt-gauge">
                <div class="debt-gauge-head">
                  <div>
                    <span>Taux d’endettement</span>
                    <strong>{{ formatPercent(scenario.eligibility.debtRatio) }}</strong>
                  </div>
                  <small>Zone recommandée : jusqu’à 40 %</small>
                </div>
                <div class="gauge-track">
                  <span class="gauge-safe"></span>
                  <span class="gauge-watch"></span>
                  <span class="gauge-critical"></span>
                  <i [style.left.%]="debtGaugePosition(scenario)"></i>
                </div>
                <div class="gauge-labels">
                  <span>0 %</span>
                  <span>40 %</span>
                  <span>50 %</span>
                  <span>60 %+</span>
                </div>
              </div>

              @if (scenario.eligibility.status !== 'ELIGIBLE') {
                <section class="recommendation-panel">
                  <div class="recommendation-title">
                    <span class="material-symbols-rounded">lightbulb</span>
                    <div>
                      <strong>Recommandation automatique</strong>
                      <small>Une trajectoire plus prudente selon les données saisies.</small>
                    </div>
                  </div>
                  <div class="recommendation-grid">
                    <div>
                      <span>Montant demandé</span>
                      <strong>{{ formatMoney(scenario.form.creditAmount) }}</strong>
                    </div>
                    <div>
                      <span>Montant conseillé</span>
                      <strong>{{ formatMoney(scenario.eligibility.maximumRecommendedAmount) }}</strong>
                    </div>
                    <div>
                      <span>Réduction recommandée</span>
                      <strong>{{ formatMoney(recommendedReduction(scenario)) }}</strong>
                    </div>
                    <div>
                      <span>Mensualité cible</span>
                      <strong>{{ formatMoney(scenario.eligibility.realRepaymentCapacity) }}</strong>
                    </div>
                  </div>
                  <p>{{ recommendationAction(scenario) }}</p>
                </section>
              } @else {
                <section class="positive-recommendation">
                  <span class="material-symbols-rounded">verified</span>
                  <p>Vous pouvez poursuivre votre demande auprès de la banque, sous réserve de validation officielle.</p>
                </section>
              }

              @if (scenario.earlyRepayment; as earlyRepayment) {
                <div class="early-result">
                  <span>Durée estimée après remboursement anticipé</span>
                  <strong>{{ formatDuration(earlyRepayment.newDurationMonths) }}</strong>
                </div>
              }

            </article>

            <article class="summary-card">
              <div class="section-head compact">
                <span class="section-kicker">Projection financière</span>
                <h3>Indicateurs essentiels</h3>
              </div>

              <div class="essential-metrics">
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
                <article class="summary-kpi">
                  <span>Taux d’endettement</span>
                  <strong>{{ formatPercent(scenario.eligibility.debtRatio) }}</strong>
                </article>
                <article class="summary-kpi">
                  <span>Capacité réelle</span>
                  <strong>{{ formatMoney(scenario.eligibility.realRepaymentCapacity) }}</strong>
                </article>
                <article class="summary-kpi">
                  <span>Montant maximum conseillé</span>
                  <strong>{{ formatMoney(scenario.eligibility.maximumRecommendedAmount) }}</strong>
                </article>
              </div>

              <p class="eligibility-note">Cette estimation est indicative et ne remplace pas l’étude officielle de la banque.</p>
            </article>
          }
        }

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

  readonly hasSimulated = signal(false);
  readonly engineSource = signal<'api' | 'local'>('local');
  readonly engineError = signal<string | null>(null);
  readonly isSyncing = signal(false);
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
      monthlyIncome: [DEFAULT_CREDIT_FORM.monthlyIncome, [Validators.required, Validators.min(1)]],
      existingMonthlyCharges: [DEFAULT_CREDIT_FORM.existingMonthlyCharges, [Validators.required, Validators.min(0)]],
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
      existingMonthlyCharges: raw.existingMonthlyCharges ?? undefined,
      earlyRepaymentAmount: raw.earlyRepaymentAmount ?? undefined,
      earlyRepaymentMonth: raw.earlyRepaymentMonth ?? undefined
    });
  });

  readonly currentDuration = computed(() => this.normalizedFormValue().durationMonths);
  readonly currentScenarioId = computed(() => `term-${this.currentDuration()}`);
  readonly currentFormKey = computed(() => this.buildFormKey(this.normalizedFormValue()));
  readonly financedAmount = computed(() => Math.max(0, this.normalizedFormValue().creditAmount - this.normalizedFormValue().downPayment));

  readonly localProjection = computed(() => projectCredit(this.normalizedFormValue()));

  readonly localScenario = computed(() => (
    this.findScenarioByDuration(this.localProjection().scenarios, this.currentDuration())
    ?? this.localProjection().scenarios[0]
  ));
  readonly sourceScenario = computed(() => {
    const local = this.localScenario();
    const live = this.liveProjectionFormKey() === this.currentFormKey() ? this.liveScenario() : null;
    return this.mergeCreditScenario(local, live);
  });
  readonly activeScenario = computed(() => this.renderedScenario() ?? this.sourceScenario());

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

  simpleStatusMessage(): string {
    const scenario = this.activeScenario();
    const earlyRepayment = scenario.earlyRepayment;

    if (earlyRepayment?.termReductionMonths) {
      return `Un remboursement anticipé peut réduire la durée d’environ ${earlyRepayment.termReductionMonths} mois.`;
    }

    return `Votre crédit se termine autour de ${formatLongDate(scenario.endDate)} avec un coût total estimé à ${formatMoney(scenario.totalCost)}.`;
  }

  eligibilityStatusLabel(status: CreditScenarioResult['eligibility']['status']): string {
    if (status === 'ELIGIBLE') {
      return 'Éligible';
    }
    if (status === 'WATCH') {
      return 'À surveiller';
    }
    return 'Non recommandé';
  }

  eligibilityDecisionMessage(scenario: CreditScenarioResult): string {
    if (scenario.eligibility.status === 'ELIGIBLE') {
      return 'Votre mensualité reste dans la limite recommandée par rapport à vos revenus.';
    }
    if (scenario.eligibility.status === 'WATCH') {
      return 'Votre demande est proche de la limite recommandée. Une réduction du montant est conseillée.';
    }
    return 'Votre mensualité dépasse largement la limite recommandée. Il est préférable de réduire le montant demandé avant toute demande bancaire.';
  }

  simulationAlertLabel(scenario: CreditScenarioResult): string {
    return scenario.eligibility.status === 'WATCH'
      ? 'Simulation à surveiller'
      : 'Simulation non recommandée';
  }

  recommendedReduction(scenario: CreditScenarioResult): number {
    return Math.max(0, scenario.form.creditAmount - scenario.eligibility.maximumRecommendedAmount);
  }

  recommendationAction(scenario: CreditScenarioResult): string {
    if (scenario.eligibility.status === 'WATCH') {
      return 'Réduisez légèrement le montant demandé ou augmentez la durée afin de ramener la mensualité sous la zone recommandée.';
    }
    return 'Réduisez le montant demandé, allongez la durée si votre projet le permet et diminuez vos charges existantes avant une demande bancaire.';
  }

  debtGaugePosition(scenario: CreditScenarioResult): number {
    return Math.min(100, Math.max(0, scenario.eligibility.debtRatio / 60 * 100));
  }

  private setupRenderedState(): void {
    effect(() => {
      const formValue = this.normalizedFormValue();
      const scenario = this.sourceScenario();
      const renderedScenario = this.cloneCreditScenario(scenario);
      const recalculatedTimeline = this.buildTimelineItems(renderedScenario);

      this.validateTimelineConsistency(formValue, renderedScenario, recalculatedTimeline);

      if (!environment.production) {
        console.debug('[CreditSimulator] current scenario retained', {
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
            this.liveScenario.set(null);
            this.liveProjectionFormKey.set(null);

            return of({
              source: 'local' as const,
              formKey,
              calculated: fallbackCurrentScenario,
              error: null
            });
          }

          this.isSyncing.set(true);
          this.engineError.set(null);
          this.liveScenario.set(null);
          this.liveProjectionFormKey.set(null);

          return this.simulationsService.calculateCredit(formValue, this.requestSource).pipe(
            map((calculated) => ({
              source: 'api' as const,
              formKey,
              calculated,
              error: null
            })),
            catchError((error) => of({
              source: 'local' as const,
              formKey,
              calculated: fallbackCurrentScenario,
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
        this.liveProjectionFormKey.set(state.formKey);
        this.engineError.set(
          state.error?.error?.message ||
          state.error?.message ||
          (state.source === 'local' ? 'Estimation locale en cours.' : null)
        );
      });
  }

  private buildFormKey(formValue: CreditFormValue): string {
    return [
      formValue.creditAmount,
      formValue.downPayment,
      formValue.interestRate,
      formValue.durationMonths,
      formValue.monthlyIncome,
      formValue.existingMonthlyCharges,
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
      && raw.monthlyIncome !== null
      && raw.monthlyIncome !== undefined
      && raw.existingMonthlyCharges !== null
      && raw.existingMonthlyCharges !== undefined
      && raw.earlyRepaymentMonth !== null
      && raw.earlyRepaymentMonth !== undefined
      && Number.isFinite(formValue.creditAmount)
      && Number.isFinite(formValue.downPayment)
      && Number.isFinite(formValue.interestRate)
      && Number.isFinite(formValue.durationMonths)
      && Number.isFinite(formValue.monthlyIncome)
      && Number.isFinite(formValue.existingMonthlyCharges);
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
      eligibility: {
        ...baseScenario.eligibility,
        ...overrideScenario.eligibility
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
      eligibility: { ...scenario.eligibility },
      points: scenario.points.map((point) => ({ ...point })),
      milestones: scenario.milestones.map((milestone) => ({ ...milestone })),
      earlyRepayment: scenario.earlyRepayment ? { ...scenario.earlyRepayment } : null
    };
  }

  private findScenarioByDuration(
    scenarios: CreditScenarioResult[],
    durationMonths: number
  ): CreditScenarioResult | null {
    return scenarios.find((scenario) => scenario.form.durationMonths === durationMonths) ?? null;
  }

  private buildTimelineItems(scenario: CreditScenarioResult): TimelineItem[] {
    return scenario.milestones.map((milestone) => this.mapTimelineItem(milestone, scenario));
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
      const scenario = this.renderedScenario();
      const timeline = this.renderedTimelineItems();

      if (!scenario) {
        return;
      }

      console.debug('[CreditSimulator] form parameters', { ...formValue });
      console.debug('[CreditSimulator] scenario recalculated', {
        source: this.engineSource(),
        currentDurationRetained: scenario.form.durationMonths,
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

}
