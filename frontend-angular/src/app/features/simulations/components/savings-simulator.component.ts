import { CommonModule } from '@angular/common';
import { Component, DestroyRef, computed, inject, signal } from '@angular/core';
import { takeUntilDestroyed, toObservable, toSignal } from '@angular/core/rxjs-interop';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { catchError, debounceTime, distinctUntilChanged, forkJoin, map, of, startWith, switchMap } from 'rxjs';
import { normalizeSavingsFormValue, projectSavings } from '../simulations.engine';
import { formatDuration, formatLongDate, formatMoney, formatMonthLabel } from '../simulations.formatters';
import {
  BuilderHighlight,
  CompareScenarioCard,
  ProjectionMilestone,
  SavingsFormValue,
  SavingsFrequency,
  SAVINGS_FREQUENCY_OPTIONS,
  SAVINGS_GOAL_OPTIONS,
  SavingsGoalType,
  SavingsProjectionResult,
  SavingsScenarioResult,
  TimelineItem
} from '../simulations.models';
import { SimulationRequestSource, SimulationsService } from '../simulations.service';
import { environment } from '../../../../environments/environment';

type SavingsNumericControl =
  | 'targetAmount'
  | 'initialContribution'
  | 'monthlyContribution'
  | 'oneTimeContribution';

@Component({
  selector: 'app-savings-simulator',
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
            <h3>Simulateur Épargne</h3>
            <p>Estimez la date d’atteinte de votre objectif selon votre apport et votre effort mensuel.</p>
          </div>

          <form [formGroup]="form" class="simple-form">
            <label class="field">
              <span>Montant cible</span>
              <input type="number" formControlName="targetAmount" min="100" step="100" />
            </label>

            <label class="field">
              <span>Apport initial</span>
              <input type="number" formControlName="initialContribution" min="0" step="100" />
            </label>

            <label class="field">
              <span>Épargne mensuelle</span>
              <input type="number" formControlName="monthlyContribution" min="0" step="10" />
            </label>

            <label class="field">
              <span>Date cible optionnelle</span>
              <input type="date" formControlName="targetDate" />
            </label>
          </form>

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
            <span class="material-symbols-rounded">query_stats</span>
            <h3>Remplissez les champs puis lancez la simulation.</h3>
            <p>Le résultat principal apparaîtra ici avec la date estimée, la durée et le montant restant.</p>
          </article>
        } @else {
          @if (selectedScenario(); as scenario) {
            <article class="result-card">
              <div class="section-head compact">
                <span class="section-kicker">Résultat principal</span>
                <h3>À ce rythme, votre objectif sera atteint en {{ formatLongDate(scenario.completionDate) }}.</h3>
                <p>{{ userResultMessage() }}</p>
              </div>

              <div class="result-grid">
                <article class="result-kpi strong">
                  <span>Date estimée</span>
                  <strong>{{ formatLongDate(scenario.completionDate) }}</strong>
                </article>
                <article class="result-kpi">
                  <span>Durée estimée</span>
                  <strong>{{ formatDuration(scenario.durationMonths) }}</strong>
                </article>
                <article class="result-kpi">
                  <span>Montant restant</span>
                  <strong>{{ formatMoney(scenario.remainingAmount) }}</strong>
                </article>
                <article class="result-kpi">
                  <span>Total versé</span>
                  <strong>{{ formatMoney(scenario.totalContributed) }}</strong>
                </article>
              </div>

              <div class="progress-track" aria-label="Progression vers l'objectif">
                <span [style.width.%]="progressPercent()"></span>
              </div>
            </article>
          }
        }

        <section class="scenario-strip">
          <div class="section-head compact">
            <span class="section-kicker">Comparaison</span>
            <h3>Scénarios simples</h3>
          </div>

          <div class="simple-scenarios">
            @for (scenario of projection().scenarios.slice(0, 3); track scenario.id) {
              <button
                type="button"
                class="scenario-card"
                [class.is-active]="selectedScenario().id === scenario.id"
                (click)="selectedScenarioId.set(scenario.id)"
              >
                <span>{{ scenarioLabel(scenario.id) }}</span>
                <strong>{{ formatLongDate(scenario.completionDate) }}</strong>
                <small>{{ formatDuration(scenario.durationMonths) }} · {{ formatMoney(scenario.monthlyEquivalent) }}/mois</small>
                <em>{{ scenarioStatus(scenario) }}</em>
              </button>
            }
          </div>
        </section>

      </section>
    </div>
  `,
  styles: [`
    :host {
      display: block;
    }

    .simple-simulator {
      display: grid;
      grid-template-columns: minmax(20rem, 28rem) minmax(0, 1fr);
      gap: 1.15rem;
      align-items: start;
      padding-bottom: 7rem;
    }

    .input-column {
      display: grid;
      gap: 1rem;
      min-width: 0;
    }

    .form-card,
    .help-card,
    .result-card,
    .empty-card,
    .scenario-strip,
    .scenario-strip {
      border-radius: 28px;
      border: 1px solid rgba(17, 17, 17, 0.08);
      background: linear-gradient(180deg, rgba(255, 255, 255, 0.98), rgba(249, 246, 242, 0.98));
      box-shadow: 0 18px 38px rgba(17, 17, 17, 0.06);
    }

    .form-card,
    .help-card,
    .result-card,
    .empty-card,
    .scenario-strip {
      padding: 1.25rem;
    }

    .help-card {
      display: grid;
      grid-template-columns: auto 1fr;
      gap: 0.85rem;
      align-items: start;
    }

    .help-card .material-symbols-rounded {
      width: 2.7rem;
      height: 2.7rem;
      display: grid;
      place-items: center;
      border-radius: 16px;
      background: rgba(245, 130, 32, 0.12);
      color: var(--attijari-orange-dark);
    }

    .help-card h3 {
      margin: 0 0 0.35rem;
      color: var(--attijari-black);
      font-size: 1rem;
    }

    .help-card p,
    .help-card p {
      margin: 0;
      color: var(--attijari-muted);
      font-size: 0.86rem;
      line-height: 1.62;
    }

    .section-head {
      display: grid;
      gap: 0.4rem;
      margin-bottom: 1rem;
    }

    .section-head.compact {
      margin-bottom: 0.85rem;
    }

    .section-kicker {
      color: var(--attijari-orange-dark);
      font-size: 0.72rem;
      font-weight: 800;
      letter-spacing: 0.12em;
      text-transform: uppercase;
    }

    .section-head h3,
    .empty-card h3 {
      margin: 0;
      color: var(--attijari-black);
      font-size: clamp(1.2rem, 1.8vw, 1.7rem);
      letter-spacing: -0.04em;
    }

    .section-head p,
    .empty-card p {
      margin: 0;
      color: var(--attijari-muted);
      line-height: 1.65;
      font-size: 0.92rem;
    }

    .simple-form {
      display: grid;
      grid-template-columns: repeat(2, minmax(0, 1fr));
      gap: 0.85rem;
    }

    .field input {
      width: 100%;
      box-sizing: border-box;
      padding: 0.82rem 0.9rem;
      border-radius: 16px;
      border: 1.5px solid rgba(17, 17, 17, 0.1);
      background: #fff;
      color: var(--attijari-black);
      font: inherit;
      outline: none;
    }

    .field input:focus {
      border-color: var(--attijari-orange);
      box-shadow: 0 0 0 4px rgba(245, 130, 32, 0.12);
    }

    .form-actions {
      display: flex;
      gap: 0.75rem;
      flex-wrap: wrap;
      margin-top: 1rem;
    }

    .primary-action,
    .secondary-action {
      min-height: 2.8rem;
      border-radius: 999px;
      padding: 0.75rem 1.05rem;
      border: 1px solid transparent;
      font: inherit;
      font-size: 0.84rem;
      font-weight: 800;
      cursor: pointer;
    }

    .primary-action {
      color: white;
      background: linear-gradient(135deg, var(--attijari-orange), var(--attijari-orange-dark));
      box-shadow: 0 12px 24px rgba(245, 130, 32, 0.2);
    }

    .secondary-action {
      color: var(--attijari-black);
      background: #fff;
      border-color: rgba(17, 17, 17, 0.08);
    }

    .result-column {
      display: grid;
      gap: 1rem;
      min-width: 0;
    }

    .empty-card {
      min-height: 16rem;
      display: grid;
      align-content: center;
      justify-items: start;
      gap: 0.55rem;
    }

    .empty-card .material-symbols-rounded {
      width: 3rem;
      height: 3rem;
      display: grid;
      place-items: center;
      border-radius: 18px;
      background: rgba(245, 130, 32, 0.12);
      color: var(--attijari-orange-dark);
    }

    .result-grid {
      display: grid;
      grid-template-columns: repeat(4, minmax(0, 1fr));
      gap: 0.8rem;
    }

    .result-kpi,
    .result-kpi {
      display: grid;
      gap: 0.32rem;
      padding: 0.95rem;
      border-radius: 20px;
      background: rgba(255, 255, 255, 0.86);
      border: 1px solid rgba(17, 17, 17, 0.07);
    }

    .result-kpi.strong {
      background: linear-gradient(135deg, rgba(245, 130, 32, 0.14), rgba(255, 241, 230, 0.94));
      border-color: rgba(245, 130, 32, 0.18);
    }

    .result-kpi span,
    .result-kpi span {
      color: var(--attijari-muted);
      font-size: 0.74rem;
      font-weight: 800;
      text-transform: uppercase;
      letter-spacing: 0.06em;
    }

    .result-kpi strong,
    .result-kpi strong {
      color: var(--attijari-black);
      font-size: 1rem;
    }

    .progress-track {
      height: 0.7rem;
      margin-top: 1rem;
      border-radius: 999px;
      background: rgba(17, 17, 17, 0.08);
      overflow: hidden;
    }

    .progress-track span {
      display: block;
      height: 100%;
      border-radius: inherit;
      background: linear-gradient(135deg, var(--attijari-orange), var(--attijari-orange-dark));
    }

    .simple-scenarios,
    .simple-scenarios {
      display: grid;
      grid-template-columns: repeat(3, minmax(0, 1fr));
      gap: 0.8rem;
    }

    .scenario-card {
      display: grid;
      gap: 0.35rem;
      text-align: left;
      padding: 1rem;
      border-radius: 22px;
      border: 1px solid rgba(17, 17, 17, 0.08);
      background: #fff;
      cursor: pointer;
      font: inherit;
    }

    .scenario-card.is-active {
      border-color: rgba(245, 130, 32, 0.3);
      box-shadow: 0 14px 28px rgba(245, 130, 32, 0.12);
    }

    .scenario-card span,
    .scenario-card em {
      color: var(--attijari-orange-dark);
      font-size: 0.76rem;
      font-weight: 800;
      font-style: normal;
    }

    .scenario-card strong {
      color: var(--attijari-black);
    }

    .scenario-card small {
      color: var(--attijari-muted);
      line-height: 1.45;
    }


    .simulator-layout {
      display: grid;
      grid-template-columns: minmax(21rem, 32rem) minmax(0, 1fr);
      gap: 1.2rem;
      align-items: start;
    }

    .results-stack {
      display: grid;
      gap: 1rem;
    }

    .builder-form {
      display: grid;
      grid-template-columns: repeat(2, minmax(0, 1fr));
      gap: 0.95rem;
    }

    .field {
      display: grid;
      gap: 0.55rem;
    }

    .field-wide {
      grid-column: 1 / -1;
    }

    .field span {
      font-size: 0.82rem;
      font-weight: 700;
      color: var(--attijari-text);
    }

    .field small {
      color: var(--attijari-muted);
      font-size: 0.76rem;
      line-height: 1.5;
    }

    .field-head {
      display: flex;
      align-items: center;
      justify-content: space-between;
      gap: 0.75rem;
    }

    .field-head strong {
      color: var(--attijari-black);
      font-size: 0.9rem;
      letter-spacing: -0.02em;
    }

    .choice-grid {
      display: grid;
      grid-template-columns: repeat(2, minmax(0, 1fr));
      gap: 0.8rem;
    }

    .choice-chip {
      display: grid;
      gap: 0.35rem;
      text-align: left;
      padding: 0.95rem 1rem;
      border-radius: 20px;
      border: 1px solid rgba(17, 17, 17, 0.08);
      background: rgba(255, 255, 255, 0.84);
      cursor: pointer;
      transition: transform 0.2s ease, box-shadow 0.2s ease, border-color 0.2s ease;
    }

    .choice-chip:hover,
    .choice-chip-active {
      transform: translateY(-1px);
      border-color: rgba(245, 130, 32, 0.28);
      box-shadow: 0 16px 26px rgba(245, 130, 32, 0.12);
    }

    .choice-chip strong {
      color: var(--attijari-black);
      font-size: 0.88rem;
    }

    .choice-chip small {
      color: var(--attijari-muted);
      font-size: 0.74rem;
      line-height: 1.45;
    }

    .slider-field {
      padding: 1rem;
      border-radius: 22px;
      border: 1px solid rgba(17, 17, 17, 0.06);
      background: rgba(255, 255, 255, 0.72);
    }

    .pill-row {
      display: flex;
      flex-wrap: wrap;
      gap: 0.65rem;
    }

    .segmented-pill {
      padding: 0.7rem 0.95rem;
      border-radius: 999px;
      border: 1px solid rgba(17, 17, 17, 0.08);
      background: rgba(255, 255, 255, 0.84);
      color: var(--attijari-text);
      font-size: 0.8rem;
      font-weight: 700;
      cursor: pointer;
      transition: all 0.2s ease;
    }

    .segmented-pill-active {
      border-color: rgba(245, 130, 32, 0.28);
      background: linear-gradient(135deg, rgba(245, 130, 32, 0.14), rgba(255, 241, 230, 0.94));
      color: var(--attijari-orange-dark);
      box-shadow: 0 12px 22px rgba(245, 130, 32, 0.12);
    }

    .range-input {
      width: 100%;
      accent-color: var(--attijari-orange);
    }

    .engine-note {
      display: grid;
      grid-template-columns: auto 1fr;
      gap: 0.8rem;
      align-items: start;
      margin-top: 1rem;
      padding: 0.95rem 1rem;
      border-radius: 22px;
      background: rgba(17, 17, 17, 0.04);
      border: 1px solid rgba(17, 17, 17, 0.06);
    }

    .engine-note .material-symbols-rounded {
      color: var(--attijari-orange-dark);
    }

    .engine-note strong {
      display: block;
      color: var(--attijari-black);
      font-size: 0.86rem;
    }

    .engine-note p {
      margin: 0.18rem 0 0;
      color: var(--attijari-muted);
      font-size: 0.78rem;
      line-height: 1.5;
    }

    .engine-note-warning {
      background: rgba(255, 241, 230, 0.88);
      border-color: rgba(245, 130, 32, 0.18);
    }

    .hero-card {
      display: grid;
      grid-template-columns: minmax(0, 1.15fr) minmax(0, 1fr);
      gap: 1rem;
      padding: 1.4rem;
      border-radius: 32px;
      background:
        radial-gradient(circle at top right, rgba(245, 130, 32, 0.24), transparent 24%),
        linear-gradient(135deg, #14110f 0%, #231f1a 52%, #2a241f 100%);
      border: 1px solid rgba(245, 130, 32, 0.2);
      box-shadow: 0 26px 56px rgba(17, 17, 17, 0.16);
      color: var(--attijari-white);
    }

    .hero-copy {
      display: grid;
      gap: 0.75rem;
      align-content: start;
    }

    .hero-topline {
      display: flex;
      align-items: center;
      justify-content: space-between;
      gap: 0.75rem;
    }

    .hero-kicker {
      font-size: 0.72rem;
      font-weight: 700;
      letter-spacing: 0.16em;
      text-transform: uppercase;
      color: #ffbd87;
    }

    .hero-status-pill {
      padding: 0.45rem 0.7rem;
      border-radius: 999px;
      background: rgba(245, 130, 32, 0.16);
      border: 1px solid rgba(245, 130, 32, 0.18);
      color: #ffd0a4;
      font-size: 0.74rem;
      font-weight: 700;
    }

    .hero-status-pill-local {
      background: rgba(255, 255, 255, 0.08);
      border-color: rgba(255, 255, 255, 0.12);
      color: rgba(255, 255, 255, 0.84);
    }

    .hero-copy h3 {
      margin: 0;
      font-size: 1.55rem;
      letter-spacing: -0.04em;
      color: var(--attijari-white);
    }

    .hero-copy p {
      margin: 0;
      color: rgba(255, 255, 255, 0.76);
      line-height: 1.65;
      font-size: 0.92rem;
    }

    .milestone-strip {
      display: grid;
      grid-template-columns: repeat(4, minmax(0, 1fr));
      gap: 0.7rem;
    }

    .milestone-chip {
      display: grid;
      gap: 0.22rem;
      padding: 0.8rem;
      border-radius: 18px;
      background: rgba(255, 255, 255, 0.06);
      border: 1px solid rgba(255, 255, 255, 0.08);
    }

    .milestone-chip span {
      font-size: 0.72rem;
      font-weight: 700;
      color: rgba(255, 255, 255, 0.62);
      text-transform: uppercase;
      letter-spacing: 0.08em;
    }

    .milestone-chip strong {
      font-size: 0.84rem;
      color: var(--attijari-white);
    }

    .hero-metrics {
      display: grid;
      grid-template-columns: repeat(2, minmax(0, 1fr));
      gap: 0.75rem;
    }

    .hero-metric {
      display: grid;
      gap: 0.3rem;
      padding: 1rem;
      border-radius: 22px;
      background: rgba(255, 255, 255, 0.06);
      border: 1px solid rgba(255, 255, 255, 0.08);
    }

    .hero-metric-strong {
      background: linear-gradient(135deg, rgba(245, 130, 32, 0.18), rgba(245, 130, 32, 0.08));
      border-color: rgba(245, 130, 32, 0.18);
    }

    .hero-metric span,
    .hero-metric small {
      color: rgba(255, 255, 255, 0.68);
    }

    .hero-metric span {
      font-size: 0.72rem;
      font-weight: 700;
      text-transform: uppercase;
      letter-spacing: 0.08em;
    }

    .hero-metric strong {
      font-size: 1.02rem;
      color: var(--attijari-white);
    }

    .hero-metric small {
      font-size: 0.75rem;
      line-height: 1.45;
    }

    .snapshot-grid {
      display: grid;
      grid-template-columns: repeat(3, minmax(0, 1fr));
      gap: 0.9rem;
    }

    .snapshot-card {
      display: grid;
      gap: 0.35rem;
      padding: 1rem 1.05rem;
      border-radius: 24px;
      background: rgba(255, 255, 255, 0.84);
      border: 1px solid rgba(17, 17, 17, 0.08);
      box-shadow: 0 14px 34px rgba(17, 17, 17, 0.05);
    }

    .snapshot-label {
      color: var(--attijari-muted);
      font-size: 0.76rem;
      font-weight: 700;
      text-transform: uppercase;
      letter-spacing: 0.08em;
    }

    .snapshot-card strong {
      font-size: 1.06rem;
      color: var(--attijari-black);
    }

    .snapshot-card small {
      color: var(--attijari-muted);
      font-size: 0.77rem;
      line-height: 1.45;
    }

    @media (max-width: 1320px) {
      .simulator-layout,
      .simple-simulator {
        grid-template-columns: 1fr;
      }
    }

    @media (max-width: 960px) {
      .simple-form,
      .result-grid,
      .simple-scenarios,
      .builder-form,
      .choice-grid,
      .hero-card,
      .snapshot-grid,
      .hero-metrics,
      .milestone-strip {
        grid-template-columns: 1fr;
      }
    }
  `]
})
export class SavingsSimulatorComponent {
  private readonly fb = inject(FormBuilder);
  private readonly destroyRef = inject(DestroyRef);
  private readonly simulationsService = inject(SimulationsService);
  private readonly requestSource: SimulationRequestSource = {
    component: 'SavingsSimulatorComponent',
    method: 'setupApiProjection'
  };

  readonly goalOptions = SAVINGS_GOAL_OPTIONS;
  readonly frequencyOptions = SAVINGS_FREQUENCY_OPTIONS;
  readonly selectedScenarioId = signal<string | null>(null);
  readonly hasSimulated = signal(false);
  readonly engineSource = signal<'api' | 'local'>('local');
  readonly engineError = signal<string | null>(null);
  readonly isSyncing = signal(false);
  readonly liveProjection = signal<SavingsProjectionResult | null>(null);
  readonly liveScenario = signal<SavingsScenarioResult | null>(null);

  readonly form = this.fb.group({
    goalType: ['HOME' as SavingsGoalType, Validators.required],
    targetAmount: [25000, [Validators.required, Validators.min(100)]],
    initialContribution: [4000, [Validators.required, Validators.min(0)]],
    monthlyContribution: [900, [Validators.required, Validators.min(0)]],
    oneTimeContribution: [1500, [Validators.required, Validators.min(0)]],
    contributionFrequency: ['MONTHLY' as SavingsFrequency, Validators.required],
    targetDate: ['']
  });

  private readonly rawFormValue = toSignal(
    this.form.valueChanges.pipe(startWith(this.form.getRawValue())),
    { initialValue: this.form.getRawValue() }
  );

  readonly normalizedFormValue = computed<SavingsFormValue>(() => {
    const raw = this.rawFormValue();

    return normalizeSavingsFormValue({
      goalType: raw.goalType ?? undefined,
      targetAmount: raw.targetAmount ?? undefined,
      initialContribution: raw.initialContribution ?? undefined,
      recurringContribution: raw.monthlyContribution ?? undefined,
      exceptionalContribution: raw.oneTimeContribution ?? undefined,
      frequency: raw.contributionFrequency ?? undefined,
      targetDate: raw.targetDate ?? null
    });
  });

  readonly localProjection = computed(() => projectSavings(this.normalizedFormValue()));

  readonly projection = computed(() => this.liveProjection() ?? this.localProjection());

  readonly selectedScenario = computed(() => {
    const scenarios = this.projection().scenarios;
    const fallback = scenarios[0];

    if (!fallback) {
      throw new Error('Savings scenarios unavailable');
    }

    return scenarios.find((scenario) => scenario.id === this.selectedScenarioId()) ?? fallback;
  });

  readonly goalLabel = computed(() => (
    this.goalOptions.find((option) => option.value === this.normalizedFormValue().goalType)?.label ?? this.goalOptions[0].label
  ));

  readonly frequencyLabel = computed(() => (
    this.frequencyOptions.find((option) => option.value === this.normalizedFormValue().frequency)?.label ?? this.frequencyOptions[2].label
  ));

  readonly frequencyHint = computed(() => (
    this.frequencyOptions.find((option) => option.value === this.normalizedFormValue().frequency)?.hint ?? this.frequencyOptions[2].hint
  ));

  readonly builderHighlights = computed<BuilderHighlight[]>(() => {
    const scenario = this.selectedScenario();
    return [
      { label: 'Objectif', value: formatMoney(scenario.form.targetAmount), tone: 'strong' },
      { label: 'Cadence mensuelle', value: formatMoney(scenario.monthlyEquivalent), tone: 'warning' },
      { label: 'Capital restant', value: formatMoney(scenario.remainingAmount), tone: 'neutral' }
    ];
  });

  readonly compareCards = computed<CompareScenarioCard[]>(() => (
    this.projection().scenarios.map((scenario) => ({
      id: scenario.id,
      label: scenario.label,
      badge: this.resolveScenarioBadge(scenario),
      headline: formatLongDate(scenario.completionDate),
      description: this.describeScenario(scenario),
      accent: this.mapAccent(scenario),
      metrics: [
        {
          label: 'Duree',
          value: formatDuration(scenario.durationMonths),
          tone: scenario.id === 'boost' ? 'positive' : 'neutral'
        },
        {
          label: 'Equivalent mensuel',
          value: formatMoney(scenario.monthlyEquivalent),
          tone: scenario.id === 'target' ? 'warning' : 'neutral'
        },
        {
          label: 'Total verse',
          value: formatMoney(scenario.totalContributed),
          hint: 'Capital cumule a l arrivee'
        }
      ],
      footer: this.footerCopy(scenario)
    }))
  ));

  readonly timelineItems = computed<TimelineItem[]>(() => (
    this.selectedScenario().milestones.map((milestone) => this.mapTimelineItem(milestone))
  ));

  readonly lineChartModel = computed(() => {
    const scenarios = this.projection().scenarios;
    const longestScenario = scenarios.reduce((longest, current) => (
      current.points.length > longest.points.length ? current : longest
    ));

    return {
      title: 'Capital cumule dans le temps',
      subtitle: 'La courbe visualise le capital projete selon le rythme choisi et les scenarios compares.',
      labels: longestScenario.points.map((point) => formatMonthLabel(point.date)),
      series: scenarios.map((scenario) => {
        const lastValue = scenario.points[scenario.points.length - 1]?.amount ?? 0;
        const values = longestScenario.points.map((_, index) => scenario.points[index]?.amount ?? lastValue);

        return {
          label: scenario.label,
          values,
          color: this.resolveLineColor(scenario),
          fillColor: this.resolveFillColor(scenario),
          tension: 0.34
        };
      })
    };
  });

  readonly barChartModel = computed(() => ({
    title: 'Duree selon le scenario',
    subtitle: 'Comparez rapidement le temps necessaire pour atteindre l objectif.',
    items: this.projection().scenarios.map((scenario) => ({
      label: scenario.label,
      value: scenario.durationMonths,
      color: this.resolveLineColor(scenario),
      meta: formatDuration(scenario.durationMonths)
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
      ? `Le calcul distant est indisponible. Une estimation locale est utilisée.`
      : 'Une estimation rapide reste disponible pendant votre saisie.';
  });

  readonly statusMessage = computed(() => {
    const scenario = this.selectedScenario();

    if (scenario.hitTargetDate === true) {
      return `Ce plan atteint la cible avant la date renseignee, avec ${Math.abs(scenario.deltaMonthsToTarget ?? 0)} mois d avance.`;
    }

    if (scenario.hitTargetDate === false) {
      return `Ce plan depasse la date cible d environ ${scenario.deltaMonthsToTarget ?? 0} mois. Renforcez la cadence ou l apport pour resserrer la trajectoire.`;
    }

    return 'Chaque hypothese modifie immediatement la courbe, les comparatifs et la timeline de progression.';
  });

  readonly formatMoney = formatMoney;
  readonly formatLongDate = formatLongDate;
  readonly formatDuration = formatDuration;

  constructor() {
    this.setupApiProjection();
  }

  applyGoalType(value: SavingsGoalType): void {
    this.form.controls.goalType.setValue(value);
  }

  applyFrequency(value: SavingsFrequency): void {
    this.form.controls.contributionFrequency.setValue(value);
  }

  runSimulation(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }

    this.hasSimulated.set(true);
  }

  resetSimulation(): void {
    this.form.reset({
      goalType: 'HOME',
      targetAmount: 25000,
      initialContribution: 4000,
      monthlyContribution: 900,
      oneTimeContribution: 1500,
      contributionFrequency: 'MONTHLY',
      targetDate: ''
    });
    this.selectedScenarioId.set(null);
    this.hasSimulated.set(false);
  }

  progressPercent(): number {
    const scenario = this.selectedScenario();
    const target = Math.max(1, scenario.form.targetAmount);
    return Math.max(0, Math.min(100, Math.round((scenario.startAmount / target) * 100)));
  }

  userResultMessage(): string {
    const scenario = this.selectedScenario();

    if (scenario.hitTargetDate === true) {
      return 'Votre rythme actuel semble compatible avec la date cible indiquée.';
    }

    if (scenario.hitTargetDate === false) {
      return 'Pour accélérer, augmentez votre épargne mensuelle ou ajoutez un apport initial.';
    }

    return 'Pour accélérer, augmentez votre épargne mensuelle ou ajoutez un apport initial.';
  }

  scenarioLabel(id: string): string {
    if (id === 'current') {
      return 'Plan actuel';
    }

    if (id === 'boost') {
      return 'Cadence renforcée';
    }

    return 'Objectif à date cible';
  }

  scenarioStatus(scenario: SavingsScenarioResult): string {
    if (scenario.hitTargetDate === false) {
      return 'À renforcer';
    }

    if (scenario.durationMonths > 84) {
      return 'Difficile';
    }

    return 'Réaliste';
  }

  setNumericValue(controlName: SavingsNumericControl, value: number | string): void {
    const parsed = Number(value);

    if (!Number.isFinite(parsed)) {
      return;
    }

    this.form.controls[controlName].setValue(parsed);
  }

  private setupApiProjection(): void {
    toObservable(this.normalizedFormValue)
      .pipe(
        map((formValue) => ({
          formValue,
          formKey: this.buildSavingsFormKey(formValue)
        })),
        distinctUntilChanged((previous, current) => previous.formKey === current.formKey),
        debounceTime(240),
        switchMap(({ formValue }) => {
          const fallback = projectSavings(formValue);

          if (!this.canRequestSavingsApi(formValue, fallback)) {
            if (!environment.production) {
              console.debug('[SavingsSimulator] API skipped, form invalid', {
                values: this.form.getRawValue(),
                invalidControls: this.collectInvalidControls(),
                fallbackScenarioCount: fallback.scenarios.length
              });
            }

            this.isSyncing.set(false);
            this.engineError.set(null);
            this.liveProjection.set(null);
            this.liveScenario.set(null);

            return of({
              source: 'local' as const,
              calculated: fallback.scenarios[0],
              compared: fallback,
              error: null
            });
          }

          this.isSyncing.set(true);
          this.engineError.set(null);
          this.liveProjection.set(null);
          this.liveScenario.set(null);

          return forkJoin({
            calculated: this.simulationsService.calculateSavings(formValue, this.requestSource),
            compared: this.simulationsService.compareSavings(formValue, this.requestSource)
          }).pipe(
            map((result) => ({
              source: 'api' as const,
              calculated: result.calculated,
              compared: result.compared,
              error: null
            })),
            catchError((error) => of({
              source: 'local' as const,
              calculated: fallback.scenarios[0],
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
        this.engineError.set(
          state.error?.error?.message ||
          state.error?.message ||
          (state.source === 'local' ? 'Estimation locale en cours.' : null)
        );
      });
  }

  private mapTimelineItem(milestone: ProjectionMilestone): TimelineItem {
    return {
      label: milestone.label,
      progress: milestone.progress,
      dateLabel: formatLongDate(milestone.date),
      valueLabel: formatMoney(milestone.amount),
      caption: milestone.caption
    };
  }

  private resolveScenarioBadge(scenario: SavingsScenarioResult): string {
    if (scenario.id === 'current') {
      return 'Reference';
    }
    if (scenario.id === 'boost') {
      return 'Renforce';
    }
    return scenario.form.targetDate ? 'Date cible' : 'Accentu';
  }

  private describeScenario(scenario: SavingsScenarioResult): string {
    if (scenario.id === 'current') {
      return 'Votre rythme de depart, utile comme base de comparaison.';
    }
    if (scenario.id === 'boost') {
      return 'Une cadence plus intense pour raccourcir la duree globale.';
    }
    return scenario.form.targetDate
      ? 'Un plan calibre pour se rapprocher de la date souhaitée.'
      : 'Une trajectoire acceleree pour renforcer la progression.';
  }

  private footerCopy(scenario: SavingsScenarioResult): string {
    if (scenario.hitTargetDate === true) {
      return 'Compatible avec la date cible renseignee.';
    }
    if (scenario.hitTargetDate === false) {
      return 'Atteint l objectif apres l echeance visee.';
    }
    return 'Scenario libre sans contrainte de date.';
  }

  private mapAccent(scenario: SavingsScenarioResult): 'orange' | 'charcoal' | 'sand' {
    if (scenario.id === 'current') {
      return 'charcoal';
    }
    if (scenario.id === 'boost') {
      return 'orange';
    }
    return 'sand';
  }

  private resolveLineColor(scenario: SavingsScenarioResult): string {
    if (scenario.id === 'current') {
      return '#1C1C1C';
    }
    if (scenario.id === 'boost') {
      return '#F58220';
    }
    return '#B67E4B';
  }

  private resolveFillColor(scenario: SavingsScenarioResult): string {
    if (scenario.id === 'current') {
      return 'rgba(28, 28, 28, 0.08)';
    }
    if (scenario.id === 'boost') {
      return 'rgba(245, 130, 32, 0.18)';
    }
    return 'rgba(182, 126, 75, 0.16)';
  }

  private buildSavingsFormKey(formValue: SavingsFormValue): string {
    return [
      formValue.goalType,
      formValue.targetAmount,
      formValue.initialContribution,
      formValue.recurringContribution,
      formValue.exceptionalContribution,
      formValue.frequency,
      formValue.targetDate ?? ''
    ].join('|');
  }

  private canRequestSavingsApi(
    formValue: SavingsFormValue,
    fallback: SavingsProjectionResult
  ): boolean {
    const raw = this.form.getRawValue();

    return !this.form.invalid
      && raw.monthlyContribution !== null
      && raw.monthlyContribution !== undefined
      && raw.contributionFrequency !== null
      && raw.contributionFrequency !== undefined
      && Number.isFinite(formValue.recurringContribution)
      && !!formValue.frequency
      && fallback.scenarios.length > 0;
  }

  private collectInvalidControls(): string[] {
    return Object.entries(this.form.controls)
      .filter(([, control]) => control.invalid)
      .map(([name]) => name);
  }
}
