import { CommonModule } from '@angular/common';
import { ChangeDetectionStrategy, Component, DestroyRef, OnInit, computed, inject, signal } from '@angular/core';
import { takeUntilDestroyed, toSignal } from '@angular/core/rxjs-interop';
import { Router } from '@angular/router';
import { Chart, ChartData, ChartOptions, registerables } from 'chart.js';
import { BaseChartDirective } from 'ng2-charts';
import { finalize } from 'rxjs';
import {
  BudgetTargetResponse,
  RecommendationDto,
  RecommendationExplanationRequest,
  RecommendationPriority,
  RecommendationResponseDto,
  TransactionCategory,
  normalizeTransactionCategoryOrNull
} from '../../core/models';
import { DEFAULT_PUBLIC_APP_SETTINGS } from '../../core/models/app-settings.models';
import { AppSettingsService } from '../../core/services/app-settings.service';
import { BudgetTargetService } from '../../core/services/budget-target.service';
import { ChatbotService } from '../../core/services/chatbot.service';
import { NotificationService } from '../../core/services/notification.service';
import { RecommendationService } from '../../core/services/recommendation.service';
import {
  BudgetRecommendationActionData,
  RecommendationDecisionPlan,
  RecommendationUiModel,
  SecurityRecommendationActionData,
  SecurityRecommendationPreparedPlan,
  buildBudgetRecommendationActionData,
  buildRecommendationDecisionPlan,
  buildSecurityRecommendationActionData,
  normalizeRecommendationUi
} from './recommendation-ui';
import { RecommendationBudgetActionModalComponent } from './components/recommendation-budget-action-modal.component';
import { RecommendationSecurityActionModalComponent } from './components/recommendation-security-action-modal.component';

Chart.register(...registerables);

type SimulationScenario = 'conservateur' | 'equilibre' | 'agressif';

interface ScenarioOption {
  key: SimulationScenario;
  label: string;
  description: string;
}

interface HeroSummaryView {
  statusLabel: string;
  statusClass: string;
  impactLabel: string;
  summaryText: string;
}

interface RecommendationFocusView {
  recommendation: RecommendationUiModel;
  summary: string;
  monthlyImpactLabel: string;
  goalImpactLabel: string;
  actionLine: string;
  detailNarrative: string;
  visualPercent: number;
  budgetStatusLabel: string | null;
}

interface RecommendationExplanationState {
  loading: boolean;
  text: string | null;
  source: string | null;
  fallbackUsed: boolean;
}

interface FinancialKpiView {
  icon: string;
  label: string;
  value: string;
  toneClass: string;
  tooltip?: string;
}

@Component({
  selector: 'app-recommendations',
  standalone: true,
  imports: [
    CommonModule,
    BaseChartDirective,
    RecommendationBudgetActionModalComponent,
    RecommendationSecurityActionModalComponent
  ],
  templateUrl: './recommendations.component.html',
  styleUrls: ['./recommendations.component.css', './recommendations.theme.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class RecommendationsComponent implements OnInit {
  private readonly budgetTargetService = inject(BudgetTargetService);
  private readonly appSettingsService = inject(AppSettingsService);
  private readonly recommendationService = inject(RecommendationService);
  private readonly chatbotService = inject(ChatbotService);
  private readonly notificationService = inject(NotificationService);
  private readonly router = inject(Router);
  private readonly destroyRef = inject(DestroyRef);

  private readonly numberFormatter = new Intl.NumberFormat('fr-FR', {
    minimumFractionDigits: 0,
    maximumFractionDigits: 0
  });
  private readonly monthFormatter = new Intl.DateTimeFormat('fr-FR', {
    month: 'long',
    year: 'numeric'
  });
  private readonly brandPalette = {
    deepNavy: '#111111',
    accentOrange: '#F58220',
    accentGold: '#F7A356',
    muted: '#6F6F6F',
    mutedLine: 'rgba(17, 17, 17, 0.10)',
    tooltipBody: '#F5EEE8',
    currentLine: '#8D857E',
    currentFill: 'rgba(17, 17, 17, 0.08)',
    optimizedFill: 'rgba(245, 130, 32, 0.18)',
    low: '#5B8E63',
    medium: '#D88B2E',
    high: '#F58220',
    critical: '#C85A4F'
  };

  readonly response = signal<RecommendationResponseDto | null>(null);
  readonly loading = signal(true);
  readonly refreshing = signal(false);
  readonly error = signal<string | null>(null);
  readonly expandedRecommendations = signal<Record<string, boolean>>({});
  readonly detailedAnalysisExpanded = signal(false);
  readonly otherRecommendationsExpanded = signal(false);
  readonly advancedDetailsExpanded = signal(false);
  readonly budgetTargets = signal<BudgetTargetResponse[]>([]);
  readonly budgetRecommendationModalData = signal<BudgetRecommendationActionData | null>(null);
  readonly securityRecommendationModalData = signal<SecurityRecommendationActionData | null>(null);
  readonly explanationStates = signal<Record<string, RecommendationExplanationState>>({});
  readonly simulationEffort = signal(0);
  readonly selectedScenario = signal<SimulationScenario>('equilibre');
  readonly publicSettings = toSignal(this.appSettingsService.publicSettings$, {
    initialValue: DEFAULT_PUBLIC_APP_SETTINGS
  });
  readonly recommendationsDisabled = computed(() =>
    this.publicSettings().maintenanceMode || !this.publicSettings().recommendationsEnabled
  );
  readonly recommendationsDisabledMessage = computed(() =>
    this.publicSettings().maintenanceMode
      ? 'L’application est temporairement en maintenance.'
      : 'Le module de recommandations est temporairement désactivé par l’administrateur.'
  );

  readonly scenarioOptions: ScenarioOption[] = [
    {
      key: 'conservateur',
      label: 'Conservateur',
      description: 'Effort faible, progression prudente.'
    },
    {
      key: 'equilibre',
      label: 'Équilibré',
      description: 'Effort moyen, compromis confortable.'
    },
    {
      key: 'agressif',
      label: 'Agressif',
      description: 'Effort fort, accélération plus rapide.'
    }
  ];

  readonly summary = computed(() => this.response()?.summary ?? null);
  readonly hasActiveGoal = computed(() => Boolean(this.response()?.hasActiveGoal && this.response()?.priorityGoal));
  readonly priorityGoal = computed(() => this.hasActiveGoal() ? this.response()?.priorityGoal ?? null : null);
  readonly recommendations = computed(() =>
    [...(this.response()?.recommendations ?? [])].sort((left, right) => this.compareRecommendations(left, right))
  );
  readonly recommendationUiModels = computed<RecommendationUiModel[]>(() =>
    this.recommendations().map((recommendation, index) =>
      this.withFinalMonthlyImpact(normalizeRecommendationUi(recommendation, index))
    )
  );
  readonly decisionPlan = computed<RecommendationDecisionPlan>(() =>
    buildRecommendationDecisionPlan(this.filteredRecommendationUiModels(), 3)
  );
  readonly primaryRecommendation = computed(() => this.decisionPlan().primary);
  readonly secondaryRecommendations = computed(() => this.decisionPlan().secondary);
  readonly filteredRecommendationUiModels = computed(() => this.filterActionableRecommendations(this.recommendationUiModels()));
  readonly hiddenDetailedLevers = computed(() => {
    const visibleIds = new Set([
      this.primaryRecommendation()?.id,
      ...this.secondaryRecommendations().map((recommendation) => recommendation.id)
    ].filter((id): id is string | number => id !== undefined && id !== null).map((id) => String(id)));

    return this.filteredRecommendationUiModels().filter((recommendation) => !visibleIds.has(String(recommendation.id)));
  });
  readonly advancedDetailRecommendations = computed(() => {
    const primaryId = this.primaryRecommendation()?.id;
    return this.filteredRecommendationUiModels()
      .filter((recommendation) => primaryId === null || primaryId === undefined || String(recommendation.id) !== String(primaryId))
      .slice(0, 3);
  });
  readonly financialKpis = computed<FinancialKpiView[]>(() => this.buildFinancialKpis());
  readonly hasRecommendations = computed(() => this.recommendationUiModels().length > 0);
  readonly potentialMonthlyGain = computed(() => {
    const fromSummary = this.summary()?.potentialMonthlyGain;

    if (typeof fromSummary === 'number' && Number.isFinite(fromSummary)) {
      return Math.max(fromSummary, 0);
    }

    return this.recommendations().reduce(
      (total, recommendation) => total + Math.max(recommendation.estimatedMonthlyGain ?? 0, 0),
      0
    );
  });
  readonly currentSavingsCapacity = computed(() =>
    this.deriveCurrentSavingsCapacity(
      this.summary()?.globalStatus ?? null,
      this.potentialMonthlyGain(),
      this.recommendationUiModels().length
    )
  );
  readonly currentGoalTimelineMonths = computed(() =>
    this.hasActiveGoal()
      ? this.monthsUntilGoalDate(this.response()?.currentGoalDate ?? this.priorityGoal()?.targetDate ?? null)
      : 0
  );
  readonly baseGoalImpactMonths = computed(() =>
    this.hasActiveGoal()
      ? Math.max(0, Number(this.response()?.objectiveImpactMonths ?? 0))
      : 0
  );
  readonly projectedSavingsCapacity = computed(() =>
    this.simulateSavings(
      this.currentSavingsCapacity(),
      this.potentialMonthlyGain(),
      this.simulationEffort(),
      this.selectedScenario()
    )
  );
  readonly optimizedGoalTimelineMonths = computed(() =>
    this.hasActiveGoal()
      ? this.calculateNewGoalDate(
          this.currentGoalTimelineMonths(),
          this.currentSavingsCapacity(),
          this.potentialMonthlyGain(),
          this.simulationEffort(),
          this.selectedScenario()
        )
      : 0
  );
  readonly simulationMonthsGain = computed(() =>
    Math.max(this.currentGoalTimelineMonths() - this.optimizedGoalTimelineMonths(), 0)
  );
  readonly dynamicInsight = computed(() =>
    this.generateDynamicInsight(
      this.projectedSavingsCapacity(),
      this.optimizedGoalTimelineMonths(),
      this.simulationEffort(),
      this.selectedScenario()
    )
  );
  readonly heroSummary = computed<HeroSummaryView>(() => {
    const summary = this.summary();

    return {
      statusLabel: this.getStatusLabel(summary?.globalStatus),
      statusClass: this.getStatusClass(summary?.globalStatus),
      impactLabel: this.getEstimatedGoalImpactLabel(),
      summaryText: this.getHeroSummaryText()
    };
  });
  readonly trajectoryChartData = computed<ChartData<'line'>>(() => this.buildTrajectoryChartData());
  readonly recommendationImpactChartData = computed<ChartData<'bar'>>(() => this.buildRecommendationImpactChartData());

  readonly trajectoryChartOptions: ChartOptions<'line'> = {
    responsive: true,
    maintainAspectRatio: false,
    interaction: {
      mode: 'index',
      intersect: false
    },
    plugins: {
      legend: {
        display: false
      },
      tooltip: {
        displayColors: false,
        backgroundColor: this.brandPalette.deepNavy,
        titleColor: '#ffffff',
        bodyColor: this.brandPalette.tooltipBody,
        padding: 12
      }
    },
    elements: {
      line: {
        tension: 0.36,
        borderWidth: 3
      },
      point: {
        radius: 0,
        hoverRadius: 5
      }
    },
    scales: {
      x: {
        grid: {
          display: false
        },
        ticks: {
          color: this.brandPalette.muted,
          font: {
            family: 'Sora',
            size: 11
          }
        },
        border: {
          display: false
        }
      },
      y: {
        grid: {
          color: this.brandPalette.mutedLine
        },
        ticks: {
          display: false
        },
        border: {
          display: false
        }
      }
    }
  };

  readonly impactChartOptions: ChartOptions<'bar'> = {
    responsive: true,
    maintainAspectRatio: false,
    plugins: {
      legend: {
        display: false
      },
      tooltip: {
        displayColors: false,
        backgroundColor: this.brandPalette.deepNavy,
        titleColor: '#ffffff',
        bodyColor: this.brandPalette.tooltipBody,
        padding: 12
      }
    },
    scales: {
      x: {
        grid: {
          display: false
        },
        ticks: {
          color: this.brandPalette.muted,
          font: {
            family: 'Sora',
            size: 11
          }
        },
        border: {
          display: false
        }
      },
      y: {
        beginAtZero: true,
        grid: {
          color: this.brandPalette.mutedLine
        },
        ticks: {
          display: false
        },
        border: {
          display: false
        }
      }
    }
  };

  ngOnInit(): void {
    this.appSettingsService.refreshSettings()
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe(() => this.loadRecommendations());
    this.loadBudgetTargets();
  }

  updateSimulationEffort(value: number | string): void {
    const parsed = typeof value === 'string' ? Number(value) : value;
    const safeValue = Number.isFinite(parsed) ? Math.max(0, Math.min(1000, Math.round(parsed))) : 0;
    this.simulationEffort.set(safeValue);
  }

  setScenario(scenario: SimulationScenario): void {
    this.selectedScenario.set(scenario);
    this.simulationEffort.set(this.getScenarioDefaultEffort(scenario));
  }

  isScenarioSelected(scenario: SimulationScenario): boolean {
    return this.selectedScenario() === scenario;
  }

  toggleRecommendationDetails(recommendation: RecommendationUiModel): void {
    const key = String(recommendation.id);

    this.expandedRecommendations.update((current) => ({
      ...current,
      [key]: !current[key]
    }));
  }

  isRecommendationDetailsExpanded(recommendation: RecommendationUiModel): boolean {
    return !!this.expandedRecommendations()[String(recommendation.id)];
  }

  toggleDetailedAnalysis(): void {
    this.detailedAnalysisExpanded.update((current) => !current);
  }

  toggleOtherRecommendations(): void {
    this.otherRecommendationsExpanded.update((current) => !current);
  }

  toggleAdvancedDetails(): void {
    this.advancedDetailsExpanded.update((current) => !current);
  }

  getDetailedAnalysisButtonLabel(): string {
    return this.detailedAnalysisExpanded()
      ? 'Masquer l’analyse détaillée'
      : 'Voir l’analyse détaillée';
  }

  loadRecommendations(refresh = false): void {
    if (this.recommendationsDisabled()) {
      this.loading.set(false);
      this.refreshing.set(false);
      this.response.set(null);
      this.error.set(this.recommendationsDisabledMessage());
      return;
    }

    const hasExistingData = !!this.response();

    if (refresh && hasExistingData) {
      this.refreshing.set(true);
    } else {
      this.loading.set(true);
    }

    this.error.set(null);

    this.recommendationService.getMyRecommendations()
      .pipe(
        takeUntilDestroyed(this.destroyRef),
        finalize(() => {
          this.loading.set(false);
          this.refreshing.set(false);
        })
      )
      .subscribe({
        next: (response) => {
          this.response.set(response);
          this.loadRecommendationExplanations();
        },
        error: () => {
          if (!hasExistingData) {
            this.response.set(null);
          }

          this.error.set('Impossible de charger vos recommandations pour le moment.');
        }
      });
  }

  onRecommendationAction(recommendation: RecommendationUiModel, origin: 'primary' | 'secondary' = 'secondary'): void {
    const context = recommendation.actionContext;

    this.logRecommendationAction(recommendation, origin);

    if (this.isSavingsOrGoalRecommendation(recommendation)) {
      void this.router.navigate(this.resolveGoalNavigationCommands(recommendation));
      return;
    }

    const analysisQueryParams = this.buildRecommendationAnalysisQueryParams(recommendation);

    if (analysisQueryParams) {
      console.log('[Recommendations] navigate to transactions analysis', {
        recommendationId: recommendation.id,
        title: recommendation.title,
        intent: recommendation.actionContext.intent,
        queryParams: analysisQueryParams
      });
      void this.router.navigate(['/transactions'], { queryParams: analysisQueryParams });
      return;
    }

    switch (context.intent) {
      case 'budget':
        void this.router.navigate(['/budgets']);
        return;
      case 'security':
        this.openSecurityRecommendationModal(recommendation);
        return;
      default:
        break;
    }

    if (context.route) {
      this.notificationService.info(
        `Ouverture de ${context.routeLabel ?? 'la section associée'} pour poursuivre cette action.`
      );
      void this.router.navigate([context.route]);
      return;
    }

    this.notificationService.info(
      `Cette action sera bientôt automatisée. ${context.fallbackLabel} reste disponible depuis le parcours recommandé.`
    );
  }

  getRecommendationExplanationState(recommendation: RecommendationUiModel | null): RecommendationExplanationState | null {
    if (!recommendation) {
      return null;
    }

    return this.explanationStates()[this.getRecommendationStateKey(recommendation)] ?? null;
  }

  isRecommendationExplanationLoading(recommendation: RecommendationUiModel | null): boolean {
    return !!this.getRecommendationExplanationState(recommendation)?.loading;
  }

  getRecommendationExplanationText(recommendation: RecommendationUiModel | null): string {
    if (!recommendation) {
      return '';
    }

    const state = this.getRecommendationExplanationState(recommendation);
    return state?.text?.trim() || this.getWhyNarrative(recommendation.raw);
  }

  getRecommendationIcon(recommendation: RecommendationUiModel | null): string {
    const normalized = recommendation ? this.normalizeRecommendationAnalysisText([
      recommendation.title,
      recommendation.category,
      recommendation.raw.category,
      recommendation.raw.type,
      recommendation.raw.recommendationType,
      recommendation.action
    ].filter((value): value is string => typeof value === 'string' && !!value.trim()).join(' ')) : '';

    if (normalized.includes('shopping') || normalized.includes('achat')) {
      return 'shopping_bag';
    }

    if (normalized.includes('epargne') || normalized.includes('saving')) {
      return 'savings';
    }

    if (normalized.includes('cafe') || normalized.includes('coffee')) {
      return 'local_cafe';
    }

    if (normalized.includes('restaurant') || normalized.includes('sortie')) {
      return 'restaurant';
    }

    if (normalized.includes('budget')) {
      return 'account_balance_wallet';
    }

    if (normalized.includes('objectif') || normalized.includes('goal')) {
      return 'track_changes';
    }

    if (normalized.includes('alerte') || normalized.includes('critique') || normalized.includes('risque')) {
      return 'warning';
    }

    return 'auto_awesome';
  }

  getVisualPotentialTooltip(recommendation: RecommendationUiModel | null = null): string {
    const normalized = recommendation ? this.normalizeRecommendationAnalysisText([
      recommendation.raw.type,
      recommendation.raw.recommendationType,
      recommendation.raw.category,
      recommendation.category,
      recommendation.title,
      recommendation.source
    ].filter((value): value is string => typeof value === 'string' && !!value.trim()).join(' ')) : '';

    if (normalized.includes('saving') || normalized.includes('epargne')) {
      return 'Effort d’épargne supplémentaire estimé pour améliorer votre marge de sécurité.';
    }

    if (normalized.includes('goal') || normalized.includes('objectif')) {
      return 'Progression estimée vers l’objectif si l’action recommandée est appliquée.';
    }

    if (normalized.includes('budget')) {
      return 'Marge d’optimisation estimée sur votre budget mensuel.';
    }

    if (normalized.includes('expense') || normalized.includes('depense') || normalized.includes('shopping') || normalized.includes('cafe') || normalized.includes('restaurant')) {
      return 'Pourcentage de réduction estimée sur cette catégorie par rapport à votre rythme actuel.';
    }

    return 'Potentiel estimé de l’action recommandée.';
  }

  openAssistantForRecommendation(recommendation: RecommendationUiModel): void {
    this.chatbotService.openWithPrompt(this.buildAssistantPrompt(recommendation));
  }

  openBudgetRecommendationModal(recommendation: RecommendationUiModel): void {
    this.securityRecommendationModalData.set(null);
    this.budgetRecommendationModalData.set(buildBudgetRecommendationActionData(recommendation));
  }

  closeBudgetRecommendationModal(): void {
    this.budgetRecommendationModalData.set(null);
  }

  openSecurityRecommendationModal(recommendation: RecommendationUiModel): void {
    this.budgetRecommendationModalData.set(null);
    this.securityRecommendationModalData.set(buildSecurityRecommendationActionData(recommendation));
  }

  closeSecurityRecommendationModal(): void {
    this.securityRecommendationModalData.set(null);
  }

  prepareSecurityRecommendationPlan(event: SecurityRecommendationPreparedPlan): void {
    this.closeSecurityRecommendationModal();
    this.notificationService.success('Plan de sécurité préparé. Le branchement métier sera ajouté ensuite.');

    console.info('[Recommendations] security plan prepared', event);
  }

  onBudgetTargetSaved(savedBudgetTarget: BudgetTargetResponse): void {
    this.closeBudgetRecommendationModal();
    this.upsertBudgetTarget(savedBudgetTarget);
  }

  isBudgetRecommendationPrepared(recommendation: RecommendationUiModel): boolean {
    if (recommendation.actionContext.intent !== 'budget') {
      return false;
    }

    const recommendationId = String(recommendation.id);

    return this.budgetTargets().some((target) =>
      target.status === 'ACTIVE'
      && target.recommendationId === recommendationId
    );
  }

  getBudgetRecommendationStatusLabel(recommendation: RecommendationUiModel): string | null {
    if (!this.isBudgetRecommendationPrepared(recommendation)) {
      return null;
    }

    return 'Budget déjà défini';
  }

  getLeverProgressPercent(recommendation: RecommendationUiModel): number {
    return Math.max(8, Math.min(100, this.getRecommendationVisualPercent(recommendation)));
  }

  getAdvancedSignals(recommendation: RecommendationUiModel): string[] {
    return this.getDetailedSignals(recommendation.raw).slice(0, 6);
  }

  getOtherRecommendationsButtonLabel(): string {
    return this.otherRecommendationsExpanded()
      ? 'Masquer les autres recommandations'
      : 'Voir les autres recommandations';
  }

  getAdvancedDetailsButtonLabel(): string {
    return this.advancedDetailsExpanded()
      ? 'Masquer les détails avancés'
      : 'Voir les détails avancés';
  }

  private buildFinancialKpis(): FinancialKpiView[] {
    const income = this.resolveSummaryNumber(['estimatedIncome', 'incomeTotal', 'totalIncome', 'monthlyIncome', 'revenusEstimes']);
    const expenses = this.resolveSummaryNumber(['estimatedExpenses', 'expenseTotal', 'totalExpenses', 'monthlyExpenses', 'depensesEstimees']);
    const netBalance = this.resolveSummaryNumber(['netBalance', 'soldeNet', 'balance'])
      ?? (income !== null && expenses !== null ? income - expenses : null);
    const unavailableTooltip = 'Donnée indisponible pour cette période.';

    return [
      {
        icon: 'payments',
        label: 'Revenus estimés',
        value: income === null ? '—' : this.formatMoney(income),
        toneClass: 'analysis-kpi-positive',
        tooltip: income === null ? unavailableTooltip : undefined
      },
      {
        icon: 'receipt_long',
        label: 'Dépenses estimées',
        value: expenses === null ? '—' : this.formatMoney(Math.abs(expenses)),
        toneClass: 'analysis-kpi-warning',
        tooltip: expenses === null ? unavailableTooltip : undefined
      },
      {
        icon: 'account_balance_wallet',
        label: 'Solde net',
        value: netBalance === null ? '—' : this.formatMoney(netBalance),
        toneClass: netBalance !== null && netBalance < 0 ? 'analysis-kpi-danger' : 'analysis-kpi-positive',
        tooltip: netBalance === null ? unavailableTooltip : undefined
      },
      {
        icon: 'flag',
        label: 'Statut global',
        value: this.getStatusLabel(this.summary()?.globalStatus),
        toneClass: this.getStatusClass(this.summary()?.globalStatus)
      },
      {
        icon: 'track_changes',
        label: 'Objectif prioritaire',
        value: this.resolvePriorityGoalLabel(),
        toneClass: 'analysis-kpi-neutral',
        tooltip: this.hasActiveGoal() ? undefined : 'Créez un objectif pour afficher une priorité.'
      }
    ];
  }

  private resolvePriorityGoalLabel(): string {
    if (!this.hasActiveGoal()) {
      return 'Aucun objectif actif';
    }

    const goalTitle = this.priorityGoal()?.title;
    return typeof goalTitle === 'string' && goalTitle.trim()
      ? this.humanizeCopy(goalTitle, 'visible').replace(/\.$/, '')
      : 'Aucun objectif actif';
  }

  private resolveSummaryNumber(keys: string[]): number | null {
    const summary = this.summary() as unknown as Record<string, unknown> | null;

    if (!summary) {
      return null;
    }

    for (const key of keys) {
      const value = summary[key];
      const parsed = typeof value === 'number' ? value : Number(value);

      if (Number.isFinite(parsed)) {
        return parsed;
      }
    }

    return null;
  }

  getScenarioLabel(scenario: SimulationScenario | null = null): string {
    const target = scenario ?? this.selectedScenario();
    return this.scenarioOptions.find((option) => option.key === target)?.label ?? 'Équilibré';
  }

  getScenarioDescription(): string {
    return this.scenarioOptions.find((option) => option.key === this.selectedScenario())?.description
      ?? 'Projection standard du plan.';
  }

  private getScenarioDefaultEffort(scenario: SimulationScenario): number {
    switch (scenario) {
      case 'conservateur':
        return 150;
      case 'agressif':
        return 650;
      case 'equilibre':
      default:
        return 350;
    }
  }

  getSimulationEffortLabel(): string {
    return this.formatMoney(this.simulationEffort());
  }

  getSimulationMonthsLabel(): string {
    if (!this.hasActiveGoal()) {
      return '0 mois';
    }

    const months = this.simulationMonthsGain();

    if (months <= 0) {
      return '0 mois';
    }

    return months === 1 ? '+ 1 mois' : `+ ${months} mois`;
  }

  getCurrentGoalDateLabel(): string {
    if (!this.hasActiveGoal()) {
      return 'Aucun objectif actif';
    }

    const explicitDate = this.response()?.currentGoalDate ?? this.priorityGoal()?.targetDate ?? null;
    if (explicitDate) {
      return this.formatGoalDate(explicitDate);
    }

    return this.formatProjectedDate(this.currentGoalTimelineMonths());
  }

  getOptimizedGoalDateLabel(): string {
    if (!this.hasActiveGoal()) {
      return 'Aucun objectif actif';
    }

    if (this.simulationEffort() <= 0) {
      return this.getCurrentGoalDateLabel();
    }

    const explicitDate = this.response()?.simulatedGoalDate ?? null;
    if (explicitDate) {
      return this.formatGoalDate(explicitDate);
    }

    return this.formatProjectedDate(this.optimizedGoalTimelineMonths());
  }

  goToGoals(): void {
    void this.router.navigate(['/goals']);
  }

  getStatusLabel(status: string | null | undefined): string {
    const normalized = status?.trim().toUpperCase();

    switch (normalized) {
      case 'HEALTHY':
        return 'Bonne situation';
      case 'OPPORTUNITY':
        return 'Bonne dynamique';
      case 'STABLE':
        return 'Sous controle';
      case 'HIGH':
      case 'HIGH_ATTENTION':
        return 'A surveiller';
      case 'CRITICAL':
        return 'Action urgente';
      default:
        return status ? status.replace(/_/g, ' ') : 'En analyse';
    }
  }

  getDisplayedFinancialScoreLabel(): string {
    const label = this.summary()?.financialScoreLabel?.trim();

    if (this.isCriticalCurrentMonth() && label?.toUpperCase() === 'EXCELLENT') {
      return 'A surveiller';
    }

    return label ?? 'Indisponible';
  }

  getCurrentMonthStatusLabel(): string {
    const explicitLabel = this.summary()?.currentMonthStatusLabel?.trim();

    if (explicitLabel) {
      return explicitLabel;
    }

    return this.isCriticalCurrentMonth() ? 'Critique' : 'Normale';
  }

  getCurrentMonthStatusClass(): string {
    return this.isCriticalCurrentMonth() ? 'status-critical' : 'status-balanced';
  }

  getCurrentMonthInsight(): string {
    return this.isCriticalCurrentMonth()
      ? 'Le mois courant demande un retour rapide à l’équilibre.'
      : 'Lecture specifique du mois en cours.';
  }

  getStatusClass(status: string | null | undefined): string {
    const normalized = status?.trim().toUpperCase();

    switch (normalized) {
      case 'HEALTHY':
      case 'OPPORTUNITY':
        return 'status-positive';
      case 'STABLE':
        return 'status-balanced';
      case 'HIGH':
      case 'HIGH_ATTENTION':
        return 'status-alert';
      case 'CRITICAL':
        return 'status-critical';
      default:
        return 'status-neutral';
    }
  }

  getStatusInsight(status: string | null | undefined): string {
    const normalized = status?.trim().toUpperCase();

    switch (normalized) {
      case 'HEALTHY':
        return 'Votre situation est saine et peut etre optimisee en douceur.';
      case 'OPPORTUNITY':
        return 'Votre trajectoire est bonne et quelques leviers peuvent l accelerer.';
      case 'STABLE':
        return 'Votre budget tient la route, avec une marge de progression claire.';
      case 'HIGH':
      case 'HIGH_ATTENTION':
        return 'Une action ciblee ce mois-ci peut rapidement soulager votre budget.';
      case 'CRITICAL':
        return 'La priorité est de recréer de la marge et de retrouver un équilibre plus serein.';
      default:
        return 'Votre situation est en cours de qualification par le moteur IA.';
    }
  }

  getDetailedExplanation(recommendation: RecommendationDto): string {
    return this.humanizeCopy(recommendation.explanation, 'detailed');
  }

  getDetailedSignals(recommendation: RecommendationDto): string[] {
    return recommendation.basedOn
      .map((signal) => this.formatTechnicalSignal(signal))
      .filter(Boolean);
  }

  getWhyNarrative(recommendation: RecommendationDto): string {
    const savingsSignal = this.findSignal(recommendation, ['taux d epargne', 'capacite d epargne', 'marge d epargne']);
    const expenseSignal = this.findSignal(recommendation, ['depense', 'charges']);
    const contributionSignal = this.findSignal(recommendation, ['contribution', 'versement']);
    const requiredSignal = this.findSignal(recommendation, ['effort requis', 'effort necessaire', 'effort cible']);
    const horizonSignal = this.findSignal(recommendation, ['echeance', 'delai', 'horizon', 'objectif']);

    if ((savingsSignal && this.hasNegativePercentage(savingsSignal)) || (expenseSignal && this.hasNegativePercentage(expenseSignal))) {
      return 'Cette recommandation passe en premier car vos dépenses dépassent aujourd’hui votre capacité d’épargne.';
    }

    if (contributionSignal && requiredSignal) {
      const currentContribution = this.extractFirstNumber(contributionSignal);
      const requiredContribution = this.extractFirstNumber(requiredSignal);

      if (currentContribution !== null && requiredContribution !== null && currentContribution < requiredContribution) {
        return 'Cette recommandation est prioritaire car votre contribution actuelle reste inferieure a l effort requis pour respecter l echeance.';
      }
    }

    if (horizonSignal) {
      return `Cette recommandation merite d etre activee car ${this.describeSignalForNarrative(horizonSignal)}`;
    }

    if (savingsSignal) {
      return `Cette recommandation devient importante car ${this.describeSignalForNarrative(savingsSignal)}`;
    }

    if (recommendation.priority === 'CRITICAL' || recommendation.priority === 'HIGH') {
      return 'Cette recommandation est prioritaire car elle peut corriger rapidement le principal frein a votre trajectoire.';
    }

    return 'Cette recommandation a été retenue car elle peut améliorer votre confort budgétaire sans alourdir inutilement vos efforts.';
  }

  simulateSavings(
    currentSavings: number,
    recommendedGain: number,
    additionalEffort: number,
    scenario: SimulationScenario
  ): number {
    const profile = this.getScenarioProfile(scenario);
    const recommendationBoost = recommendedGain * profile.recommendationWeight;
    const manualBoost = additionalEffort * profile.savingsMultiplier;

    return Math.max(0, Math.round(currentSavings + recommendationBoost + manualBoost));
  }

  calculateNewGoalDate(
    currentGoalTimelineMonths: number,
    currentSavings: number,
    recommendedGain: number,
    additionalEffort: number,
    scenario: SimulationScenario
  ): number {
    if (additionalEffort <= 0) {
      return currentGoalTimelineMonths;
    }

    const profile = this.getScenarioProfile(scenario);
    const simulatedSavings = this.simulateSavings(currentSavings, recommendedGain, additionalEffort, scenario);
    const featuredPriority = this.primaryRecommendation()?.priority ?? 'MEDIUM';
    const baselineImpact = this.deriveGoalImpactMonths(recommendedGain, featuredPriority, this.recommendationUiModels().length);

    if (simulatedSavings <= 0) {
      return currentGoalTimelineMonths;
    }

    const additionalMonths = Math.min(6, Math.round((additionalEffort * profile.goalAcceleration) / 220));
    const recoveryBonus = currentSavings <= 0 && simulatedSavings > 0 ? 1 : 0;
    const stabilityBonus = simulatedSavings >= 350 ? 1 : 0;
    const totalImpact = baselineImpact + additionalMonths + recoveryBonus + stabilityBonus;

    return Math.max(1, currentGoalTimelineMonths - totalImpact);
  }

  getRecommendationSummary(recommendation: RecommendationUiModel | null): string {
    return recommendation ? this.humanizeCopy(recommendation.message, 'visible') : '';
  }

  getPriorityMonthSentence(recommendation: RecommendationUiModel | null): string {
    if (!recommendation) {
      return '';
    }

    const normalized = this.normalizeRecommendationAnalysisText([
      recommendation.title,
      recommendation.category,
      recommendation.raw.category,
      recommendation.raw.type,
      recommendation.action
    ].filter((value): value is string => typeof value === 'string' && !!value.trim()).join(' '));

    if (normalized.includes('shopping') || normalized.includes('achat')) {
      return 'Priorité du mois : réduire les dépenses shopping, principal levier d’amélioration ce mois-ci.';
    }

    if (normalized.includes('cafe') || normalized.includes('coffee')) {
      return 'Priorité du mois : maîtriser les dépenses café pour libérer rapidement de la marge.';
    }

    if (normalized.includes('epargne') || normalized.includes('saving') || normalized.includes('objectif') || normalized.includes('goal')) {
      return 'Priorité du mois : renforcer votre épargne pour sécuriser votre objectif.';
    }

    if (normalized.includes('budget')) {
      return 'Priorité du mois : ajuster votre budget pour reprendre la main sur votre trajectoire.';
    }

    return `Priorité du mois : ${recommendation.title}.`;
  }

  getRecommendationMonthlyImpactDisplayPublic(recommendation: RecommendationUiModel | null): string {
    return recommendation ? this.getRecommendationMonthlyImpactDisplay(recommendation) : '';
  }

  getRecommendationGoalImpactDisplayPublic(recommendation: RecommendationUiModel | null): string {
    return recommendation ? this.getRecommendationGoalImpactDisplay(recommendation) : '';
  }

  getRecommendationActionLine(recommendation: RecommendationUiModel | null): string {
    return recommendation ? this.formatActionLine(recommendation.action) : '';
  }

  getRecommendationDetailNarrative(recommendation: RecommendationUiModel | null): string {
    return this.getRecommendationExplanationText(recommendation);
  }

  private filterActionableRecommendations(items: RecommendationUiModel[]): RecommendationUiModel[] {
    const concrete = items.filter((recommendation) => !this.isGenericRecommendation(recommendation));
    const source = concrete.length > 0 ? concrete : items;
    const byCategory = new Map<string, RecommendationUiModel>();

    for (const recommendation of source) {
      const key = this.getRecommendationCategoryKey(recommendation);
      const existing = byCategory.get(key);

      if (!existing || this.compareRecommendationForDisplay(recommendation, existing) < 0) {
        byCategory.set(key, recommendation);
      }
    }

    return Array.from(byCategory.values()).sort((left, right) => this.compareRecommendationForDisplay(left, right));
  }

  private withFinalMonthlyImpact(recommendation: RecommendationUiModel): RecommendationUiModel {
    const targetedTransactionsTotal = this.resolveTargetedTransactionsTotal(recommendation);
    const potentialPercent = this.getRecommendationVisualPercent(recommendation);

    if (
      this.isExpenseRecommendation(recommendation)
      && targetedTransactionsTotal !== null
      && targetedTransactionsTotal > 0
      && potentialPercent > 0
    ) {
      return {
        ...recommendation,
        monthlyImpactValue: Math.round((targetedTransactionsTotal * potentialPercent) / 100)
      };
    }

    return recommendation;
  }

  private compareRecommendationForDisplay(left: RecommendationUiModel, right: RecommendationUiModel): number {
    const priorityGap = this.priorityWeight(right.priority) - this.priorityWeight(left.priority);

    if (priorityGap !== 0) {
      return priorityGap;
    }

    const impactGap = this.resolveFinalMonthlyImpactEstimated(right) - this.resolveFinalMonthlyImpactEstimated(left);

    if (impactGap !== 0) {
      return impactGap;
    }

    const actionableGap = Number(this.hasReliableAction(right)) - Number(this.hasReliableAction(left));

    if (actionableGap !== 0) {
      return actionableGap;
    }

    return left.title.localeCompare(right.title, 'fr');
  }

  private getRecommendationCategoryKey(recommendation: RecommendationUiModel): string {
    const category = this.normalizeRecommendationAnalysisText(
      recommendation.raw.category ?? recommendation.category ?? recommendation.source ?? recommendation.title
    );

    if (category.includes('cafe')) return 'CAFES';
    if (category.includes('shopping') || category.includes('achat')) return 'SHOPPING';
    if (category.includes('restaurant') || category.includes('sortie')) return 'RESTAURANT';
    if (category.includes('aliment')) return 'ALIMENTATION';
    if (category.includes('epargne') || category.includes('saving')) return 'EPARGNE';
    if (category.includes('objectif') || category.includes('goal')) return 'OBJECTIF';
    if (category.includes('budget')) return 'BUDGET';

    return category || String(recommendation.id);
  }

  private isGenericRecommendation(recommendation: RecommendationUiModel): boolean {
    const normalized = this.normalizeRecommendationAnalysisText([
      recommendation.title,
      recommendation.message,
      recommendation.action,
      recommendation.raw.type,
      recommendation.raw.recommendationType
    ].filter((value): value is string => typeof value === 'string' && !!value.trim()).join(' '));

    return normalized.includes('gestion prudente')
      || normalized.includes('fonds de securite')
      || normalized.includes('fond de securite')
      || normalized.includes('matelas de securite')
      || normalized.includes('recommandation generale');
  }

  private hasReliableAction(recommendation: RecommendationUiModel): boolean {
    return !!recommendation.primaryActionLabel.trim()
      && (recommendation.actionContext.intent !== 'general' || !!recommendation.actionContext.route);
  }

  getRecommendationVisualPercentPublic(recommendation: RecommendationUiModel | null): number {
    return recommendation ? this.getRecommendationVisualPercent(recommendation) : 0;
  }

  private buildRecommendationFocusView(recommendation: RecommendationUiModel | null): RecommendationFocusView | null {
    if (!recommendation) {
      return null;
    }

    return {
      recommendation,
      summary: this.humanizeCopy(recommendation.message, 'visible'),
      monthlyImpactLabel: this.getRecommendationMonthlyImpactDisplay(recommendation),
      goalImpactLabel: this.getRecommendationGoalImpactDisplay(recommendation),
      actionLine: this.formatActionLine(recommendation.action),
      detailNarrative: this.getRecommendationExplanationText(recommendation),
      visualPercent: this.getRecommendationVisualPercent(recommendation),
      budgetStatusLabel: this.getBudgetRecommendationStatusLabel(recommendation)
    };
  }

  private getRecommendationMonthlyImpactDisplay(recommendation: RecommendationUiModel): string {
    const monthlyImpact = this.resolveFinalMonthlyImpactEstimated(recommendation);

    if (monthlyImpact > 0) {
      return `+ ${this.formatMonthlyMoney(monthlyImpact)}`;
    }

    if (recommendation.primaryImpact.domain === 'stabilite') {
      return 'Risque reduit';
    }

    if (this.currentSavingsCapacity() <= 0) {
      return 'Retour à l’équilibre';
    }

    return 'Marge stabilisee';
  }

  private getRecommendationGoalImpactDisplay(recommendation: RecommendationUiModel): string {
    if (!this.hasActiveGoal()) {
      return '';
    }

    if (recommendation.goalImpactMonths > 0) {
      return recommendation.goalImpactMonths === 1
        ? '+ 1 mois'
        : `+ ${recommendation.goalImpactMonths} mois`;
    }

    if (recommendation.primaryImpact.domain === 'stabilite') {
      return 'Risque reduit';
    }

    return this.currentSavingsCapacity() <= 0 ? 'Équilibre d’abord' : 'Trajectoire stabilisée';
  }

  private getEstimatedGoalImpactLabel(): string {
    if (!this.hasActiveGoal()) {
      return '';
    }

    const months = this.baseGoalImpactMonths();

    if (months <= 0) {
      return this.currentSavingsCapacity() <= 0 ? 'Retour à l’équilibre' : 'Trajectoire stabilisée';
    }

    return months === 1 ? '+ 1 mois d avance' : `+ ${months} mois d avance`;
  }

  private getHeroSummaryText(): string {
    const source = this.humanizeCopy(this.summary()?.aiSummary || this.buildCompactSummary(), 'visible')
      .replace(/Votre score financier est de\s*\d+\s*\/\s*100(?:\s*\([^)]+\))?/gi, 'Votre situation financière demande une attention adaptée')
      .replace(/\bscore financier\b/gi, 'situation financière');
    const sentences = source.match(/[^.!?]+[.!?]?/g)?.map((sentence) => sentence.trim()).filter(Boolean) ?? [];
    const compact = sentences.slice(0, 2).join(' ');

    if (compact) {
      return compact.length > 220 ? `${compact.slice(0, 217).trim()}...` : compact;
    }

    return this.getStatusInsight(this.summary()?.globalStatus);
  }

  private buildCompactSummary(): string {
    const primary = this.primaryRecommendation();
    const statusLead = this.getStatusInsight(this.summary()?.globalStatus);

    if (!primary) {
      return statusLead;
    }

    return `${statusLead} Priorité du mois : ${primary.title}.`;
  }

  private generateDynamicInsight(
    simulatedSavings: number,
    optimizedGoalTimelineMonths: number,
    additionalEffort: number,
    scenario: SimulationScenario
  ): { tone: 'positive' | 'neutral' | 'warning'; message: string } {
    if (!this.hasActiveGoal()) {
      return {
        tone: 'neutral',
        message: 'Créez un objectif financier pour visualiser l’effet de vos économies sur une date cible.'
      };
    }

    const monthsGain = Math.max(this.currentGoalTimelineMonths() - optimizedGoalTimelineMonths, 0);
    const scenarioLabel = this.getScenarioLabel(scenario).toLowerCase();

    if (additionalEffort === 0) {
      return {
        tone: 'neutral',
        message: 'Ajoutez un effort mensuel pour visualiser l’impact sur votre objectif.'
      };
    }

    if (monthsGain >= 3 || simulatedSavings >= Math.max(this.currentSavingsCapacity(), 0) + 250) {
      return {
        tone: 'positive',
        message: `Ce scénario ${scenarioLabel} crée un gain visible sur votre marge et rapproche clairement votre objectif.`
      };
    }

    if (simulatedSavings <= 0 || (scenario === 'agressif' && additionalEffort >= 700 && monthsGain < 2)) {
      return {
        tone: 'warning',
        message: 'Ce niveau d’effort reste encore tendu. Une trajectoire plus progressive protégerait mieux votre équilibre.'
      };
    }

    return {
      tone: 'neutral',
      message: `L’amélioration est réelle mais progressive : le scénario ${scenarioLabel} renforce surtout la régularité du budget.`
    };
  }

  private getScenarioProfile(scenario: SimulationScenario): {
    savingsMultiplier: number;
    recommendationWeight: number;
    goalAcceleration: number;
    curve: number[];
  } {
    switch (scenario) {
      case 'conservateur':
        return {
          savingsMultiplier: 0.72,
          recommendationWeight: 0.92,
          goalAcceleration: 0.82,
          curve: [0, 0.12, 0.3, 0.58, 0.88]
        };
      case 'agressif':
        return {
          savingsMultiplier: 1.12,
          recommendationWeight: 1.1,
          goalAcceleration: 1.18,
          curve: [0, 0.28, 0.58, 0.9, 1.14]
        };
      case 'equilibre':
      default:
        return {
          savingsMultiplier: 0.92,
          recommendationWeight: 1,
          goalAcceleration: 1,
          curve: [0, 0.18, 0.42, 0.74, 1]
        };
    }
  }

  private buildTrajectoryChartData(): ChartData<'line'> {
    const points = this.getTrajectoryTimelinePoints();
    const currentSavings = Math.max(this.currentSavingsCapacity(), 0);
    const projectedSavings = Math.max(this.projectedSavingsCapacity(), 0);
    const curve = this.getScenarioProfile(this.selectedScenario()).curve;
    const horizon = Math.max(points[points.length - 1] ?? 1, 1);
    const currentCurve = points.map((point) => point / horizon);

    return {
      labels: points.map((point) => this.formatTimelineMarker(point)),
      datasets: [
        {
          label: 'Trajectoire actuelle',
          data: currentCurve.map((factor) => Math.round(currentSavings * horizon * factor)),
          borderColor: this.brandPalette.currentLine,
          backgroundColor: this.brandPalette.currentFill,
          fill: false
        },
        {
          label: 'Trajectoire simulée',
          data: points.map((_, index) => Math.round(projectedSavings * horizon * (curve[index] ?? curve[curve.length - 1] ?? 1))),
          borderColor: this.brandPalette.accentOrange,
          backgroundColor: this.brandPalette.optimizedFill,
          fill: true
        }
      ]
    };
  }

  private buildRecommendationImpactChartData(): ChartData<'bar'> {
    const items = this.recommendationUiModels().slice(0, 4);
    const profile = this.getScenarioProfile(this.selectedScenario());
    const manualEffort = Math.round(this.simulationEffort() * profile.savingsMultiplier);
    const labels = items.map((recommendation) => this.shortenChartLabel(recommendation.title));
    const values = items.map((recommendation) => Math.round(this.deriveVisualMonthlyGain(recommendation) * profile.recommendationWeight));
    const colors = items.map((recommendation) => this.getChartBarColor(recommendation.priority));

    labels.push('Effort manuel');
    values.push(manualEffort);
    colors.push(this.brandPalette.accentGold);

    return {
      labels,
      datasets: [
        {
          label: 'Impact mensuel',
          data: values,
          backgroundColor: colors,
          borderRadius: 12,
          borderSkipped: false,
          maxBarThickness: 34
        }
      ]
    };
  }

  private buildNarrative(): string {
    const primary = this.primaryRecommendation();
    const summaryText = this.humanizeCopy(this.summary()?.aiSummary, 'visible');
    const statusLead = this.getStatusInsight(this.summary()?.globalStatus);
    const primaryLead = primary
      ? `La priorité du mois est "${primary.title}", car son effet peut devenir visible rapidement sur votre budget.`
      : 'Aucune recommandation prioritaire n a ete identifiee pour le moment.';
    const opportunityLead = this.potentialMonthlyGain() > 0
      ? `Les leviers retenus peuvent liberer jusqu a ${this.formatMonthlyMoney(this.potentialMonthlyGain())} supplementaires.`
      : 'Les actions proposées servent surtout à consolider votre équilibre avant toute accélération nette.';

    return [summaryText || statusLead, primaryLead, opportunityLead].filter(Boolean).join(' ');
  }

  private getTrajectoryTimelinePoints(): number[] {
    const horizon = Math.min(12, Math.max(6, this.currentGoalTimelineMonths()));
    const points = [
      0,
      Math.max(1, Math.round(horizon / 4)),
      Math.max(2, Math.round(horizon / 2)),
      Math.max(3, Math.round((horizon * 3) / 4)),
      horizon
    ];

    return Array.from(new Set(points)).sort((left, right) => left - right);
  }

  private formatTimelineMarker(value: number): string {
    if (value === 0) {
      return 'Ajd';
    }

    return `M+${value}`;
  }

  private shortenChartLabel(value: string): string {
    const compact = value.trim();
    return compact.length > 18 ? `${compact.slice(0, 16).trim()}...` : compact;
  }

  private getRecommendationVisualPercent(recommendation: RecommendationUiModel): number {
    const explicitPotentialPercent = this.resolveExplicitPotentialPercent(recommendation);

    if (explicitPotentialPercent !== null) {
      return explicitPotentialPercent;
    }

    const normalized = this.normalizeRecommendationAnalysisText([
      recommendation.source,
      recommendation.title,
      recommendation.category,
      recommendation.raw.category,
      recommendation.raw.type,
      recommendation.raw.recommendationType,
      recommendation.message,
      recommendation.action
    ].filter((value): value is string => typeof value === 'string' && !!value.trim()).join(' '));

    if (normalized.includes('shopping') || normalized.includes('achat')) {
      return 30;
    }

    if (normalized.includes('cafe') || normalized.includes('coffee')) {
      return 22;
    }

    if (normalized.includes('restaurant') || normalized.includes('sortie')) {
      return 22;
    }

    if (normalized.includes('epargne') || normalized.includes('saving')) {
      return 22;
    }

    if (normalized.includes('objectif') || normalized.includes('goal')) {
      return 25;
    }

    if (normalized.includes('budget')) {
      return 20;
    }

    switch (recommendation.priority) {
      case 'CRITICAL':
        return 28;
      case 'HIGH':
        return 24;
      case 'MEDIUM':
        return 18;
      case 'LOW':
      default:
        return 15;
    }
  }

  private resolveExplicitPotentialPercent(recommendation: RecommendationUiModel): number | null {
    const raw = recommendation.raw.raw ?? {};
    const candidates = [
      recommendation.raw.potentialPercent,
      raw['potentialPercent'],
      raw['visualPotentialPercent'],
      raw['reductionPercent']
    ];

    for (const candidate of candidates) {
      const value = typeof candidate === 'number' ? candidate : Number(candidate);

      if (Number.isFinite(value) && value > 0) {
        return Math.max(1, Math.min(100, Math.round(value)));
      }
    }

    return null;
  }

  private deriveVisualMonthlyGain(recommendation: RecommendationUiModel): number {
    if (recommendation.monthlyImpactValue > 0) {
      return Math.round(recommendation.monthlyImpactValue);
    }

    const averageGain = this.recommendationUiModels().length > 0
      ? this.potentialMonthlyGain() / this.recommendationUiModels().length
      : 0;

    if (averageGain <= 0) {
      return 0;
    }

    const multiplier = (() => {
      switch (recommendation.priority) {
        case 'CRITICAL':
          return 0.95;
        case 'HIGH':
          return 0.8;
        case 'MEDIUM':
          return 0.55;
        case 'LOW':
        default:
          return 0.35;
      }
    })();

    return Math.max(0, Math.round(averageGain * multiplier));
  }

  private getChartBarColor(priority: RecommendationPriority): string {
    switch (priority) {
      case 'CRITICAL':
        return this.brandPalette.critical;
      case 'HIGH':
        return this.brandPalette.high;
      case 'MEDIUM':
        return this.brandPalette.medium;
      case 'LOW':
      default:
        return this.brandPalette.low;
    }
  }

  private compareRecommendations(left: RecommendationDto, right: RecommendationDto): number {
    const priorityGap = this.priorityWeight(right.priority) - this.priorityWeight(left.priority);

    if (priorityGap !== 0) {
      return priorityGap;
    }

    return (right.estimatedMonthlyGain ?? 0) - (left.estimatedMonthlyGain ?? 0);
  }

  private priorityWeight(priority: RecommendationPriority): number {
    switch (priority) {
      case 'CRITICAL':
        return 4;
      case 'HIGH':
        return 3;
      case 'MEDIUM':
        return 2;
      case 'LOW':
      default:
        return 1;
    }
  }

  private deriveCurrentSavingsCapacity(status: string | null, projectedGain: number, recommendationCount: number): number {
    const normalized = status?.trim().toUpperCase();
    const floor = Math.max(90, recommendationCount * 35);

    switch (normalized) {
      case 'CRITICAL':
        return -Math.max(floor, Math.round(Math.max(projectedGain, 120) * 0.35));
      case 'HIGH':
      case 'HIGH_ATTENTION':
        return Math.max(0, Math.round(projectedGain * 0.2));
      case 'STABLE':
        return Math.max(120, Math.round(projectedGain * 0.55));
      case 'OPPORTUNITY':
        return Math.max(180, Math.round(projectedGain * 0.75));
      case 'HEALTHY':
        return Math.max(240, Math.round(projectedGain * 0.95));
      default:
        return projectedGain > 0 ? Math.round(projectedGain * 0.45) : 0;
    }
  }

  private deriveCurrentGoalTimelineMonths(status: string | null, recommendationCount: number, projectedGain: number): number {
    const normalized = status?.trim().toUpperCase();
    const base = projectedGain >= 350 ? 8 : projectedGain >= 180 ? 10 : 12;

    switch (normalized) {
      case 'HEALTHY':
        return Math.max(5, base - 2 + recommendationCount);
      case 'OPPORTUNITY':
        return Math.max(6, base - 1 + recommendationCount);
      case 'STABLE':
        return base + recommendationCount;
      case 'HIGH':
      case 'HIGH_ATTENTION':
        return base + recommendationCount + 3;
      case 'CRITICAL':
        return base + recommendationCount + 5;
      default:
        return base + recommendationCount + 1;
    }
  }

  private deriveGoalImpactMonths(
    monthlyGain: number,
    priority: RecommendationPriority,
    recommendationCount: number
  ): number {
    const gainScore = monthlyGain > 0 ? Math.max(1, Math.round(monthlyGain / 170)) : 0;
    const portfolioBonus = recommendationCount >= 4 ? 1 : 0;

    switch (priority) {
      case 'CRITICAL':
        return Math.min(6, gainScore + 2 + portfolioBonus);
      case 'HIGH':
        return Math.min(5, gainScore + 1 + portfolioBonus);
      case 'MEDIUM':
        return Math.min(4, gainScore + portfolioBonus);
      case 'LOW':
      default:
        return Math.min(3, gainScore);
    }
  }

  private loadRecommendationExplanations(): void {
    const items = this.recommendationUiModels();

    if (items.length === 0) {
      this.explanationStates.set({});
      return;
    }

    const initialStates = items.reduce<Record<string, RecommendationExplanationState>>((states, recommendation) => {
      states[this.getRecommendationStateKey(recommendation)] = {
        loading: true,
        text: null,
        source: null,
        fallbackUsed: false
      };
      return states;
    }, {});

    this.explanationStates.set(initialStates);

    items.forEach((recommendation) => {
      const key = this.getRecommendationStateKey(recommendation);
      const request = this.buildExplanationRequest(recommendation);

      this.recommendationService.getRecommendationExplanation(request)
        .pipe(takeUntilDestroyed(this.destroyRef))
        .subscribe({
          next: (response) => {
            this.explanationStates.update((current) => ({
              ...current,
              [key]: {
                loading: false,
                text: response.explanation?.trim() || this.buildLocalExplanationFallback(recommendation),
                source: response.source ?? null,
                fallbackUsed: !!response.fallbackUsed
              }
            }));
          },
          error: () => {
            this.explanationStates.update((current) => ({
              ...current,
              [key]: {
                loading: false,
                text: this.buildLocalExplanationFallback(recommendation),
                source: 'LOCAL_FALLBACK',
                fallbackUsed: true
              }
            }));
          }
        });
    });
  }

  private buildExplanationRequest(recommendation: RecommendationUiModel): RecommendationExplanationRequest {
    const targetedTransactionsTotal = this.resolveTargetedTransactionsTotal(recommendation);
    const monthlyImpactEstimated = this.resolveFinalMonthlyImpactEstimated(recommendation);

    return {
      title: recommendation.title,
      category: recommendation.raw.category ?? recommendation.category,
      type: recommendation.raw.type ?? recommendation.raw.recommendationType ?? recommendation.source,
      amount: monthlyImpactEstimated > 0 ? monthlyImpactEstimated : null,
      period: '30d',
      targetedTransactionsTotal,
      monthlyImpactEstimated: monthlyImpactEstimated > 0 ? monthlyImpactEstimated : null,
      potentialPercent: this.getRecommendationVisualPercent(recommendation),
      goalTitle: recommendation.source === 'goal' ? recommendation.title : null,
      financialScore: this.summary()?.financialScore ?? null,
      globalStatus: this.summary()?.globalStatus ?? null,
      savingsRate: this.resolveSavingsRate(recommendation),
      message: recommendation.message,
      suggestedAction: recommendation.action
    };
  }

  private getRecommendationStateKey(recommendation: RecommendationUiModel): string {
    return String(recommendation.id);
  }

  private buildLocalExplanationFallback(recommendation: RecommendationUiModel): string {
    const normalized = this.normalizeRecommendationAnalysisText([
      recommendation.title,
      recommendation.category,
      recommendation.raw.category,
      recommendation.raw.type,
      recommendation.action
    ].filter((value): value is string => typeof value === 'string' && !!value.trim()).join(' '));
    const monthlyImpact = this.resolveFinalMonthlyImpactEstimated(recommendation);
    const amount = monthlyImpact > 0
      ? ` avec un impact mensuel estimé autour de ${this.formatMoney(monthlyImpact)}`
      : '';

    if (normalized.includes('cafe') || normalized.includes('coffee')) {
      return `Cette recommandation est prioritaire car vos achats café semblent récurrents${amount} et peuvent être ajustés avec une limite hebdomadaire simple.`;
    }

    if (normalized.includes('shopping') || normalized.includes('achat')) {
      return `Cette recommandation est prioritaire car vos achats shopping peuvent contenir des dépenses reportables${amount} qui libèrent rapidement de la marge.`;
    }

    if (normalized.includes('epargne') || normalized.includes('saving')) {
      return `Cette recommandation est prioritaire car une hausse progressive de votre effort d’épargne${amount} peut accélérer votre objectif sans déséquilibrer le mois.`;
    }

    if (normalized.includes('restaurant') || normalized.includes('sortie')) {
      return `Cette recommandation est prioritaire car les sorties non planifiées${amount} peuvent être pilotées avec un suivi hebdomadaire du budget repas.`;
    }

    return this.getWhyNarrative(recommendation.raw);
  }

  private buildAssistantPrompt(recommendation: RecommendationUiModel): string {
    const normalized = this.normalizeRecommendationAnalysisText([
      recommendation.title,
      recommendation.category,
      recommendation.raw.category,
      recommendation.raw.type,
      recommendation.action
    ].filter((value): value is string => typeof value === 'string' && !!value.trim()).join(' '));

    if (normalized.includes('cafe') || normalized.includes('coffee')) {
      return 'Explique-moi en détail pourquoi je devrais maîtriser mes dépenses café et donne-moi un plan d’action concret.';
    }

    if (normalized.includes('shopping') || normalized.includes('achat')) {
      return 'Explique-moi comment réduire mes dépenses shopping sans impacter mon confort et propose-moi un plan simple.';
    }

    if (normalized.includes('epargne') || normalized.includes('saving')) {
      return 'Explique-moi comment améliorer mon épargne pour atteindre mon objectif plus rapidement.';
    }

    if (normalized.includes('restaurant') || normalized.includes('sortie')) {
      return 'Explique-moi comment limiter mes sorties non planifiées et suivre mon budget repas chaque semaine.';
    }

    if (normalized.includes('budget')) {
      return `Explique-moi comment ajuster mon budget pour appliquer cette recommandation: ${recommendation.title}.`;
    }

    if (normalized.includes('objectif') || normalized.includes('goal')) {
      return `Explique-moi comment cette recommandation peut m’aider à atteindre mon objectif: ${recommendation.title}.`;
    }

    return `Explique-moi en détail cette recommandation et propose-moi un plan d’action concret: ${recommendation.title}.`;
  }

  private buildRecommendationAnalysisQueryParams(
    recommendation: RecommendationUiModel
  ): Record<string, string> | null {
    if (!this.shouldOpenRecommendationAnalysis(recommendation)) {
      return null;
    }

    const category = this.resolveRecommendationAnalysisCategory(recommendation);

    if (category) {
      return {
        analysisMode: 'category',
        category,
        type: 'DEPENSE',
        period: '30d',
        source: 'recommendation'
      };
    }

    return {
      analysisMode: 'global_expense',
      type: 'DEPENSE',
      period: '30d',
      source: 'recommendation'
    };
  }

  private shouldOpenRecommendationAnalysis(recommendation: RecommendationUiModel): boolean {
    if (recommendation.actionContext.intent !== 'expense') {
      return false;
    }

    if (this.isSavingsOrGoalRecommendation(recommendation)) {
      return false;
    }

    if (recommendation.actionContext.intent === 'expense') {
      return true;
    }

    return this.normalizeRecommendationAnalysisText(recommendation.primaryActionLabel).includes('analyser mes depenses');
  }

  private resolveRecommendationAnalysisCategory(recommendation: RecommendationUiModel): TransactionCategory | null {
    const directCategory = this.normalizeTransactionCategoryCandidate(
      recommendation.raw.category ?? recommendation.category ?? null
    );

    if (directCategory && directCategory !== 'AUTRES') {
      return directCategory;
    }

    const corpus = this.normalizeRecommendationAnalysisText([
      recommendation.title,
      recommendation.message,
      recommendation.action,
      recommendation.category,
      recommendation.raw.category,
      recommendation.raw.type,
      recommendation.raw.sourceType,
      recommendation.raw.recommendationType
    ].filter((value): value is string => typeof value === 'string' && !!value.trim()).join(' '));

    if (corpus.includes('shopping') || corpus.includes('achat')) {
      return 'SHOPPING';
    }

    if (corpus.includes('alimentation') || corpus.includes('alimentaire') || corpus.includes('course') || corpus.includes('grocery') || corpus.includes('food')) {
      return 'ALIMENTATION';
    }

    if (corpus.includes('livraison') || corpus.includes('delivery') || corpus.includes('glovo') || corpus.includes('talabat')) {
      return 'LIVRAISON';
    }

    if (corpus.includes('restaurant') || corpus.includes('restauration')) {
      return 'RESTAURANT';
    }

    if (corpus.includes('cafe') || corpus.includes('coffee')) {
      return 'CAFES';
    }

    return null;
  }

  private resolveTargetedTransactionsTotal(recommendation: RecommendationUiModel): number | null {
    const raw = recommendation.raw.raw ?? {};
    const candidates = [
      recommendation.raw.targetedTransactionsTotal,
      raw['targetedTransactionsTotal'],
      raw['targetedTransactionTotal'],
      raw['transactionsTotal'],
      raw['periodTotal'],
      raw['totalExpenses']
    ];

    for (const candidate of candidates) {
      const value = typeof candidate === 'number' ? candidate : Number(candidate);

      if (Number.isFinite(value) && value !== 0) {
        return Math.abs(value);
      }
    }

    return null;
  }

  private resolveFinalMonthlyImpactEstimated(recommendation: RecommendationUiModel): number {
    const targetedTransactionsTotal = this.resolveTargetedTransactionsTotal(recommendation);
    const potentialPercent = this.getRecommendationVisualPercent(recommendation);

    if (
      this.isExpenseRecommendation(recommendation)
      && targetedTransactionsTotal !== null
      && targetedTransactionsTotal > 0
      && potentialPercent > 0
    ) {
      return Math.round((targetedTransactionsTotal * potentialPercent) / 100);
    }

    return recommendation.monthlyImpactValue > 0
      ? recommendation.monthlyImpactValue
      : Math.max(recommendation.raw.estimatedMonthlyGain ?? 0, 0);
  }

  private isExpenseRecommendation(recommendation: RecommendationUiModel): boolean {
    const normalized = this.normalizeRecommendationAnalysisText([
      recommendation.source,
      recommendation.raw.sourceType,
      recommendation.raw.type,
      recommendation.raw.recommendationType,
      recommendation.raw.category,
      recommendation.category,
      recommendation.title
    ].filter((value): value is string => typeof value === 'string' && !!value.trim()).join(' '));

    return !this.isSavingsOrGoalRecommendation(recommendation)
      && (
        normalized.includes('expense')
        || normalized.includes('depense')
        || normalized.includes('shopping')
        || normalized.includes('cafe')
        || normalized.includes('restaurant')
      );
  }

  private resolveSavingsRate(recommendation: RecommendationUiModel): number | null {
    const raw = recommendation.raw.raw ?? {};
    const candidates = [
      raw['savingsRate'],
      raw['currentSavingsRate'],
      raw['tauxEpargne'],
      ...recommendation.raw.basedOn
    ];

    for (const candidate of candidates) {
      if (typeof candidate === 'number' && Number.isFinite(candidate)) {
        return candidate;
      }

      if (typeof candidate === 'string') {
        const normalized = this.normalizeRecommendationAnalysisText(candidate);

        if (normalized.includes('epargne')) {
          const value = this.extractFirstNumber(candidate);

          if (value !== null) {
            return value;
          }
        }
      }
    }

    return null;
  }

  private isSavingsOrGoalRecommendation(recommendation: RecommendationUiModel): boolean {
    const normalized = this.normalizeRecommendationAnalysisText([
      recommendation.source,
      recommendation.title,
      recommendation.category,
      recommendation.raw.category,
      recommendation.raw.type,
      recommendation.raw.recommendationType,
      recommendation.message,
      recommendation.action
    ].filter((value): value is string => typeof value === 'string' && !!value.trim()).join(' '));

    return normalized.includes('epargne')
      || normalized.includes('saving')
      || normalized.includes('objectif')
      || normalized.includes('goal');
  }

  private resolveGoalNavigationCommands(recommendation: RecommendationUiModel): any[] {
    const raw = recommendation.raw.raw ?? {};
    const candidates = [
      raw['goalId'],
      raw['objectifId'],
      raw['targetGoalId'],
      raw['linkedGoalId'],
      raw['goal_id']
    ];

    for (const candidate of candidates) {
      if (typeof candidate === 'number' && Number.isFinite(candidate) && candidate > 0) {
        return ['/goals', candidate];
      }

      if (typeof candidate === 'string' && candidate.trim()) {
        return ['/goals', candidate.trim()];
      }
    }

    return ['/goals'];
  }

  private isGlobalExpenseRecommendation(recommendation: RecommendationUiModel): boolean {
    const corpus = this.normalizeRecommendationAnalysisText([
      recommendation.title,
      recommendation.message,
      recommendation.action,
      recommendation.category,
      recommendation.raw.category,
      recommendation.raw.type,
      recommendation.raw.sourceType
    ].filter((value): value is string => typeof value === 'string' && !!value.trim()).join(' '));

    return corpus.includes('hausse globale')
      || corpus.includes('depenses globales')
      || corpus.includes('hausse des depenses')
      || corpus.includes('surveiller la hausse')
      || corpus.includes('global expense');
  }

  private normalizeTransactionCategoryCandidate(value: string | null | undefined): TransactionCategory | null {
    return normalizeTransactionCategoryOrNull(value);
  }

  private normalizeRecommendationAnalysisText(value: string): string {
    return value
      .normalize('NFD')
      .replace(/[\u0300-\u036f]/g, '')
      .toLowerCase();
  }

  private logRecommendationAction(
    recommendation: RecommendationUiModel,
    origin: 'primary' | 'secondary'
  ): void {
    console.info('[Recommendations] action triggered', {
      origin,
      recommendationId: recommendation.id,
      source: recommendation.source,
      priority: recommendation.priority,
      intent: recommendation.actionContext.intent,
      route: recommendation.actionContext.route
    });
  }

  private loadBudgetTargets(): void {
    this.budgetTargetService.getMyBudgetTargets()
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (targets) => {
          this.budgetTargets.set(targets);
        },
        error: () => {
          if (!this.response()) {
            return;
          }

          console.warn('[Recommendations] unable to refresh budget targets');
        }
      });
  }

  private upsertBudgetTarget(savedBudgetTarget: BudgetTargetResponse): void {
    this.budgetTargets.update((current) => {
      const existingIndex = current.findIndex((target) => target.id === savedBudgetTarget.id);

      if (existingIndex === -1) {
        return [savedBudgetTarget, ...current];
      }

      const next = [...current];
      next[existingIndex] = savedBudgetTarget;
      return next;
    });
  }

  private cleanSentence(value: string | null | undefined): string {
    if (!value?.trim()) {
      return '';
    }

    return value.trim().endsWith('.') ? value.trim() : `${value.trim()}.`;
  }

  private formatMoney(amount: number): string {
    const rounded = Math.round(amount);
    return `${this.numberFormatter.format(rounded)} DT`;
  }

  private formatPlainNumber(amount: number): string {
    return this.numberFormatter.format(Math.round(amount));
  }

  private formatProjectedDate(monthOffset: number): string {
    const projectedDate = new Date();
    projectedDate.setMonth(projectedDate.getMonth() + monthOffset);
    const formattedDate = this.monthFormatter.format(projectedDate);
    return `${formattedDate.charAt(0).toUpperCase()}${formattedDate.slice(1)}`;
  }

  private formatGoalDate(value: string): string {
    const parsed = new Date(value);

    if (Number.isNaN(parsed.getTime())) {
      return 'Date indisponible';
    }

    const formattedDate = this.monthFormatter.format(parsed);
    return `${formattedDate.charAt(0).toUpperCase()}${formattedDate.slice(1)}`;
  }

  private monthsUntilGoalDate(value: string | null): number {
    if (!value) {
      return 0;
    }

    const parsed = new Date(value);

    if (Number.isNaN(parsed.getTime())) {
      return 0;
    }

    const now = new Date();
    const monthDelta = (parsed.getFullYear() - now.getFullYear()) * 12 + (parsed.getMonth() - now.getMonth());
    return Math.max(0, monthDelta);
  }

  private formatMonthCount(months: number): string {
    return `${Math.round(months)} mois`;
  }

  private formatMonthlyMoney(amount: number): string {
    return `${this.formatMoney(amount)} / mois`;
  }

  private formatActionLine(value: string | null | undefined): string {
    const cleaned = this.humanizeCopy(value, 'visible').replace(/\.$/, '').trim();

    if (!cleaned) {
      return '';
    }

    return `${cleaned.charAt(0).toUpperCase()}${cleaned.slice(1)}`;
  }

  private humanizeCopy(value: string | null | undefined, mode: 'visible' | 'detailed'): string {
    if (!value?.trim()) {
      return '';
    }

    let text = value.trim().replace(/\s+/g, ' ');
    text = this.applyFrenchWordingFixes(text);
    text = this.replaceNegativeSavingsText(text);
    text = text.replace(/\bavec\s+(-?\d+(?:[.,]\d+)?)\s+(?:de\s+)?revenus?\b/gi, (_, rawValue: string) =>
      `Avec ${this.formatMoney(this.parseNumber(rawValue))} de revenus mensuels`
    );
    text = text.replace(/(-?\d+(?:[.,]\d+)?)\s*(dt|tnd|dinars?\s*tunisiens?)/gi, (_, rawValue: string) =>
      this.formatMoney(this.parseNumber(rawValue))
    );
    text = text.replace(/(-?\d+(?:[.,]\d+)?)\s*mois/gi, (_, rawValue: string) =>
      this.formatMonthCount(this.parseNumber(rawValue))
    );
    text = text.replace(/(-?\d+(?:[.,]\d+)?)\s*%/gi, (_, rawValue: string) =>
      this.formatPercentText(this.parseNumber(rawValue), text)
    );
    text = text.replace(/-?\d+(?:[.,]\d+)/g, (match: string, offset: number, source: string) =>
      this.formatBareDecimalMatch(match, offset, source)
    );
    text = text.replace(/\s+([,.;:!?])/g, '$1');

    if (mode === 'visible') {
      text = text.replace(/\bL IA relie cette recommandation a\b/i, 'Cette recommandation devient importante car');
    }

    return this.cleanSentence(text);
  }

  private replaceNegativeSavingsText(text: string): string {
    const normalized = this.normalizeForMatch(text);

    if (normalized.includes('taux d epargne negatif')) {
      return 'Votre capacité d’épargne est actuellement insuffisante.';
    }

    if (this.hasNegativePercentage(text) && normalized.includes('depense')) {
      return 'Vos dépenses dépassent actuellement votre capacité d’épargne.';
    }

    if (this.hasNegativePercentage(text) && (normalized.includes('epargne') || normalized.includes('capacite'))) {
      return 'Votre capacité d’épargne est actuellement insuffisante.';
    }

    return text;
  }

  private applyFrenchWordingFixes(text: string): string {
    return text
      .replace(
        /Automatisez une mise de cote des reception du revenu/gi,
        'Automatisez votre mise de cote des la reception de votre revenu'
      )
      .replace(
        /Automatisez une mise de cote des la reception du revenu/gi,
        'Automatisez votre mise de cote des la reception de votre revenu'
      )
      .replace(
        /\bavec\s+(-?\d+(?:[.,]\d+)?)\s*(DT)?\s+de revenus\b/gi,
        (_, rawValue: string) => `Avec ${this.formatMoney(this.parseNumber(rawValue))} de revenus mensuels`
      )
      .replace(/\bbonne gestion financiere\b/gi, 'situation financiere saine')
      .replace(/\bdes reception du revenu\b/gi, 'des la reception de votre revenu');
  }

  private formatPercentText(value: number, fullText: string): string {
    const normalized = this.normalizeForMatch(fullText);

    if (value < 0 && normalized.includes('depense')) {
      return 'dépenses supérieures à la capacité d’épargne';
    }

    if (value < 0 && (normalized.includes('epargne') || normalized.includes('capacite'))) {
      return 'capacité d’épargne insuffisante';
    }

    return `${this.formatPlainNumber(value)} %`;
  }

  private formatBareDecimalMatch(match: string, offset: number, source: string): string {
    const value = this.parseNumber(match);
    const context = this.normalizeForMatch(source.slice(Math.max(0, offset - 28), Math.min(source.length, offset + 28)));

    if (
      context.includes('gain')
      || context.includes('budget')
      || context.includes('epargne')
      || context.includes('depense')
      || context.includes('marge')
      || context.includes('contribution')
      || context.includes('mensuel')
      || context.includes('effort')
      || context.includes('reste a vivre')
      || context.includes('solde')
      || context.includes('revenu')
      || context.includes('salaire')
      || context.includes('versement')
      || context.includes('surplus')
    ) {
      return this.formatMoney(value);
    }

    if (
      context.includes('echeance')
      || context.includes('delai')
      || context.includes('horizon')
      || context.includes('avance')
      || context.includes('objectif')
      || context.includes('duree')
    ) {
      return this.formatMonthCount(value);
    }

    return this.formatPlainNumber(value);
  }

  private formatTechnicalSignal(signal: string): string {
    const normalized = this.normalizeForMatch(signal);
    const value = this.extractFirstNumber(signal);

    if ((normalized.includes('taux d epargne') || normalized.includes('capacite d epargne')) && this.hasNegativePercentage(signal)) {
      return 'Capacité d’épargne : insuffisante';
    }

    if (normalized.includes('depense') && this.hasNegativePercentage(signal)) {
      return 'Depenses : au-dessus de la marge disponible';
    }

    if (normalized.includes('contribution') && normalized.includes('actuelle') && value !== null) {
      return `Contribution actuelle : ${this.formatMonthlyMoney(value)}`;
    }

    if ((normalized.includes('effort requis') || normalized.includes('effort necessaire') || normalized.includes('effort cible')) && value !== null) {
      return `Contribution requise : ${this.formatMonthlyMoney(value)}`;
    }

    if ((normalized.includes('echeance') || normalized.includes('horizon') || normalized.includes('delai') || normalized.includes('objectif')) && value !== null) {
      return `Echeance cible : ${this.formatMonthCount(value)}`;
    }

    if ((normalized.includes('gain') || normalized.includes('economie')) && value !== null) {
      return `Gain estimé : ${this.formatMonthlyMoney(value)}`;
    }

    if (normalized.includes('epargne') && value !== null) {
      return value <= 0
        ? 'Épargne estimée : insuffisante'
        : `Épargne estimée : ${this.formatMonthlyMoney(value)}`;
    }

    return this.humanizeCopy(signal, 'detailed').replace(/\.$/, '');
  }

  private describeSignalForNarrative(signal: string): string {
    const normalized = this.normalizeForMatch(signal);
    const value = this.extractFirstNumber(signal);

    if ((normalized.includes('taux d epargne') || normalized.includes('capacite d epargne')) && this.hasNegativePercentage(signal)) {
      return 'votre capacité d’épargne reste actuellement insuffisante.';
    }

    if (normalized.includes('depense') && this.hasNegativePercentage(signal)) {
      return 'vos dépenses dépassent aujourd’hui votre capacité d’épargne.';
    }

    if (normalized.includes('contribution') && normalized.includes('actuelle') && value !== null) {
      return `votre contribution mensuelle actuelle reste autour de ${this.formatMoney(value)}.`;
    }

    if ((normalized.includes('effort requis') || normalized.includes('effort necessaire') || normalized.includes('effort cible')) && value !== null) {
      return `l effort mensuel a fournir se situe autour de ${this.formatMoney(value)}.`;
    }

    if ((normalized.includes('echeance') || normalized.includes('horizon') || normalized.includes('delai') || normalized.includes('objectif')) && value !== null) {
      return `l echeance cible se situe a environ ${this.formatMonthCount(value)}.`;
    }

    if ((normalized.includes('gain') || normalized.includes('economie')) && value !== null) {
      return `le gain potentiel peut atteindre environ ${this.formatMoney(value)} par mois.`;
    }

    return this.humanizeCopy(signal, 'visible').replace(/\.$/, '.').replace(/^[A-Z]/, (letter) => letter.toLowerCase());
  }

  private findSignal(recommendation: RecommendationDto, tokens: string[]): string | null {
    return recommendation.basedOn.find((signal) => {
      const normalized = this.normalizeForMatch(signal);
      return tokens.some((token) => normalized.includes(token));
    }) ?? null;
  }

  private parseNumber(rawValue: string): number {
    return Number(rawValue.replace(/\s/g, '').replace(',', '.'));
  }

  private extractFirstNumber(value: string): number | null {
    const match = value.match(/-?\d+(?:[.,]\d+)?/);
    return match ? this.parseNumber(match[0]) : null;
  }

  private hasNegativePercentage(value: string): boolean {
    return /-\d+(?:[.,]\d+)?\s*%/.test(value);
  }

  private normalizeForMatch(value: string): string {
    return value
      .normalize('NFD')
      .replace(/[\u0300-\u036f]/g, '')
      .toLowerCase();
  }

  private isCriticalCurrentMonth(): boolean {
    return this.summary()?.currentMonthSeverity?.trim().toUpperCase() === 'CRITICAL';
  }
}
