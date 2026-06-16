import { CommonModule, DOCUMENT } from '@angular/common';
import { ChangeDetectionStrategy, Component, DestroyRef, OnInit, computed, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { finalize } from 'rxjs';
import {
  BudgetAlertResponse,
  BudgetAlertSeverity,
  BudgetAlertType,
  BudgetTargetCategory,
  BudgetTargetLevel,
  BudgetTargetMonitoringStatus,
  BudgetTargetResponse,
  BudgetTargetSource,
  BudgetTargetStatus,
  TRANSACTION_CATEGORIES,
  getTransactionCategoryLabel,
  getTransactionCategoryMaterialIcon
} from '../../core/models';
import { BudgetTargetService } from '../../core/services/budget-target.service';
import { NotificationService } from '../../core/services/notification.service';
import {
  BudgetRecommendationActionData,
  BudgetRecommendationFrameOption
} from '../recommendations/recommendation-ui';
import { BudgetDefinitionModalComponent } from './components/budget-definition-modal.component';

type BudgetSourceFilter = 'all' | 'ai' | 'manual';
type BudgetSortKey = 'recent' | 'amount' | 'category';
type BudgetLevelTone = 'prudent' | 'equilibre' | 'renforce';
type BudgetStatusTone = 'active' | 'inactive' | 'archived';
type BudgetMonitoringTone = 'sous-controle' | 'a-surveiller' | 'depasse' | 'neutral';
type BudgetAlertTone = 'critical' | 'warning' | 'info';

interface BudgetFilterOption {
  key: BudgetSourceFilter;
  label: string;
  count: number;
}

interface BudgetSortOption {
  key: BudgetSortKey;
  label: string;
}

interface BudgetAlertUiModel {
  id: string;
  alertType: BudgetAlertType;
  severity: BudgetAlertSeverity;
  severityLabel: string;
  severityTone: BudgetAlertTone;
  title: string;
  message: string;
  categoryLabel: string | null;
  metricLabel: string | null;
  contextLabel: string | null;
  budgetTargetId: number | null;
  accentTone: BudgetAlertTone;
  iconName: string;
  isCritical: boolean;
  isWarning: boolean;
  isInfo: boolean;
  priorityRank: number;
}

interface BudgetTargetUiModel {
  id: number;
  category: string;
  categoryLabel: string;
  categoryIcon: string;
  amount: number;
  amountLabel: string;
  targetAmount: number | null;
  targetAmountLabel: string;
  selectedLevel: BudgetTargetLevel;
  levelLabel: string;
  levelSummary: string;
  source: BudgetTargetSource;
  sourceLabel: string;
  status: BudgetTargetStatus;
  statusLabel: string;
  createdAt: string;
  createdAtLabel: string;
  recommendationTitle: string | null;
  summary: string | null;
  isAiGenerated: boolean;
  isActive: boolean;
  levelTone: BudgetLevelTone;
  statusTone: BudgetStatusTone;
  contextualLine: string;
  sourceInsight: string;
  hasMonitoring: boolean;
  monitoringStatus: BudgetTargetMonitoringStatus | null;
  monitoringLabel: string;
  monitoringTone: BudgetMonitoringTone;
  monitoringMessage: string;
  monitoringRank: number;
  spentThisMonth: number | null;
  spentThisMonthLabel: string;
  remainingAmount: number | null;
  remainingAmountLabel: string;
  usagePercent: number | null;
  usagePercentLabel: string;
  progressWidth: number;
  progressAriaValue: number;
  isExceeded: boolean;
  isNearLimit: boolean;
}

@Component({
  selector: 'app-budgets-page',
  standalone: true,
  imports: [CommonModule, BudgetDefinitionModalComponent],
  templateUrl: './budgets-page.component.html',
  styleUrls: ['./budgets-page.component.scss', './budgets.theme.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class BudgetsPageComponent implements OnInit {
  private readonly budgetTargetService = inject(BudgetTargetService);
  private readonly notificationService = inject(NotificationService);
  private readonly destroyRef = inject(DestroyRef);
  private readonly document = inject(DOCUMENT);
  private readonly amountFormatter = new Intl.NumberFormat('fr-FR', {
    minimumFractionDigits: 0,
    maximumFractionDigits: 0
  });
  private readonly percentFormatter = new Intl.NumberFormat('fr-FR', {
    minimumFractionDigits: 0,
    maximumFractionDigits: 0
  });
  private readonly dateFormatter = new Intl.DateTimeFormat('fr-FR', {
    day: 'numeric',
    month: 'short',
    year: 'numeric'
  });
  private readonly preferredBudgetCreationCategories: readonly BudgetTargetCategory[] = [
    'SHOPPING',
    'ALIMENTATION',
    'SUPERMARCHE',
    'RESTAURANT',
    'CAFES',
    'FACTURES',
    'LOGEMENT',
    'TRANSPORT',
    'TECHNOLOGIE',
    'LIVRAISON',
    'DIVERTISSEMENT'
  ];
  private highlightResetTimer: ReturnType<typeof globalThis.setTimeout> | null = null;

  readonly budgets = signal<BudgetTargetResponse[]>([]);
  readonly budgetAlerts = signal<BudgetAlertResponse[]>([]);
  readonly loading = signal(true);
  readonly alertsLoading = signal(true);
  readonly errorMessage = signal<string | null>(null);
  readonly alertsErrorMessage = signal<string | null>(null);
  readonly disableErrorMessage = signal<string | null>(null);
  readonly selectedFilter = signal<BudgetSourceFilter>('all');
  readonly selectedSort = signal<BudgetSortKey>('recent');
  readonly highlightedBudgetId = signal<number | null>(null);
  readonly pendingDisableBudget = signal<BudgetTargetUiModel | null>(null);
  readonly updatingBudgetId = signal<number | null>(null);

  readonly sortOptions: BudgetSortOption[] = [
    { key: 'recent', label: 'Plus recents' },
    { key: 'amount', label: 'Montant le plus eleve' },
    { key: 'category', label: 'Categorie A-Z' }
  ];

  readonly budgetUiModels = computed<BudgetTargetUiModel[]>(() =>
    this.budgets().map((budget) => this.mapBudgetToUiModel(budget))
  );
  readonly alertUiModels = computed<BudgetAlertUiModel[]>(() =>
    [...this.budgetAlerts()]
      .sort((left, right) => this.compareBudgetAlerts(left, right))
      .map((alert) => this.mapBudgetAlertToUiModel(alert))
  );
  readonly topBudgetAlerts = computed<BudgetAlertUiModel[]>(() => this.alertUiModels().slice(0, 3));
  readonly hiddenBudgetAlertCount = computed(() =>
    Math.max(this.alertUiModels().length - this.topBudgetAlerts().length, 0)
  );
  readonly criticalAlertCount = computed(() =>
    this.alertUiModels().filter((alert) => alert.isCritical).length
  );
  readonly warningAlertCount = computed(() =>
    this.alertUiModels().filter((alert) => alert.isWarning).length
  );
  readonly infoAlertCount = computed(() =>
    this.alertUiModels().filter((alert) => alert.isInfo).length
  );
  readonly activeBudgets = computed<BudgetTargetUiModel[]>(() =>
    this.budgetUiModels().filter((budget) => budget.isActive)
  );
  readonly visibleBudgets = computed<BudgetTargetUiModel[]>(() => {
    const filter = this.selectedFilter();
    const filtered = this.activeBudgets().filter((budget) => {
      if (filter === 'ai') {
        return budget.isAiGenerated || budget.source === 'RECOMMENDATION_AI';
      }

      if (filter === 'manual') {
        return budget.source === 'MANUAL';
      }

      return true;
    });

    return [...filtered].sort((left, right) => this.compareBudgets(left, right, this.selectedSort()));
  });
  readonly filterOptions = computed<BudgetFilterOption[]>(() => {
    const activeBudgets = this.activeBudgets();

    return [
      { key: 'all', label: 'Tous', count: activeBudgets.length }
    ];
  });
  readonly hasBudgets = computed(() => this.budgets().length > 0);
  readonly hasActiveBudgets = computed(() => this.activeBudgets().length > 0);
  readonly historicalBudgetCount = computed(() =>
    this.budgets().filter((budget) => budget.status !== 'ACTIVE').length
  );
  readonly inactiveBudgetCount = computed(() =>
    this.budgets().filter((budget) => budget.status === 'INACTIVE').length
  );
  readonly archivedBudgetCount = computed(() =>
    this.budgets().filter((budget) => budget.status === 'ARCHIVED').length
  );
  readonly activeBudgetCount = computed(() => this.activeBudgets().length);
  readonly totalBudgetedAmount = computed(() =>
    this.activeBudgets().reduce((total, budget) => total + Math.max(budget.amount, 0), 0)
  );
  readonly totalBudgetedAmountLabel = computed(() =>
    this.totalBudgetedAmount() > 0 ? this.formatMonthlyMoney(this.totalBudgetedAmount()) : 'Montant a definir'
  );
  readonly coveredCategoriesCount = computed(() =>
    new Set(this.activeBudgets().map((budget) => budget.category)).size
  );
  readonly aiBudgetCount = computed(() =>
    this.activeBudgets().filter((budget) => budget.isAiGenerated || budget.source === 'RECOMMENDATION_AI').length
  );
  readonly aiBudgetShare = computed(() => {
    const total = this.activeBudgetCount();

    if (!total) {
      return 0;
    }

    return Math.round((this.aiBudgetCount() / total) * 100);
  });
  readonly aiBudgetShareLabel = computed(() => `${this.aiBudgetShare()}%`);
  readonly underControlBudgetCount = computed(() =>
    this.activeBudgets().filter((budget) => budget.monitoringStatus === 'SOUS_CONTROLE').length
  );
  readonly watchBudgetCount = computed(() =>
    this.activeBudgets().filter((budget) => budget.monitoringStatus === 'A_SURVEILLER').length
  );
  readonly exceededBudgetCount = computed(() =>
    this.activeBudgets().filter((budget) => budget.isExceeded).length
  );
  readonly hasMonitoringInsight = computed(() =>
    this.activeBudgets().some((budget) => budget.hasMonitoring)
  );
  readonly monitoringSummaryLabel = computed(() => {
    if (!this.hasMonitoringInsight()) {
      return null;
    }

    const parts: string[] = [];

    if (this.underControlBudgetCount() > 0) {
      parts.push(`${this.underControlBudgetCount()} ${this.pluralizeBudget(this.underControlBudgetCount())} sous controle`);
    }

    if (this.watchBudgetCount() > 0) {
      parts.push(`${this.watchBudgetCount()} ${this.pluralizeBudget(this.watchBudgetCount())} a surveiller`);
    }

    if (this.exceededBudgetCount() > 0) {
      parts.push(`${this.exceededBudgetCount()} ${this.pluralizeBudget(this.exceededBudgetCount())} depasse`);
    }

    return parts.length ? `${parts.join(', ')}.` : null;
  });
  readonly hasBudgetAlerts = computed(() => this.alertUiModels().length > 0);
  readonly showAlertLoader = computed(() => this.alertsLoading() && !this.hasBudgetAlerts());
  readonly showAlertEmptyState = computed(() =>
    !this.showAlertLoader() && !this.alertsErrorMessage() && !this.hasBudgetAlerts()
  );
  readonly showInitialLoader = computed(() => this.loading() && !this.hasBudgets());
  readonly showInlineRefreshState = computed(() => this.loading() && this.hasBudgets());
  readonly showErrorBanner = computed(() => !!this.errorMessage() && this.hasBudgets());
  readonly budgetCreationData = computed<BudgetRecommendationActionData | null>(() => {
    const category = this.resolveAvailableBudgetCreationCategory();
    return category ? this.buildBudgetCreationData(category) : null;
  });
  readonly budgetCreationHint = computed(() =>
    'Definissez ou modifiez vos cadres budgetaires directement depuis cette page, categorie par categorie.'
  );

  ngOnInit(): void {
    this.loadBudgets();
    this.loadBudgetAlerts();
  }

  loadBudgets(): void {
    this.loading.set(true);
    this.errorMessage.set(null);

    this.budgetTargetService.getMyBudgetTargets()
      .pipe(
        takeUntilDestroyed(this.destroyRef),
        finalize(() => this.loading.set(false))
      )
      .subscribe({
        next: (budgets) => {
          this.budgets.set(this.sortBudgetsByDate(budgets));
        },
        error: (error: unknown) => {
          if (!this.hasBudgets()) {
            this.budgets.set([]);
          }

          this.errorMessage.set(
            this.extractErrorMessage(error, 'Impossible de charger vos budgets pour le moment.')
          );
        }
      });
  }

  loadBudgetAlerts(): void {
    this.alertsLoading.set(true);
    this.alertsErrorMessage.set(null);

    this.budgetTargetService.getMyBudgetAlerts()
      .pipe(
        takeUntilDestroyed(this.destroyRef),
        finalize(() => this.alertsLoading.set(false))
      )
      .subscribe({
        next: (alerts) => {
          this.budgetAlerts.set(alerts);
        },
        error: (error: unknown) => {
          this.budgetAlerts.set([]);
          this.alertsErrorMessage.set(
            this.extractErrorMessage(error, 'Impossible de charger les alertes budgetaires pour le moment.')
          );
        }
      });
  }

  trackBudget(_: number, budget: BudgetTargetUiModel): number {
    return budget.id;
  }

  trackBudgetAlert(_: number, alert: BudgetAlertUiModel): string {
    return alert.id;
  }

  setFilter(filter: BudgetSourceFilter): void {
    this.selectedFilter.set(filter);
  }

  setSort(sort: string): void {
    if (sort === 'amount' || sort === 'category' || sort === 'recent') {
      this.selectedSort.set(sort);
    }
  }

  isFilterSelected(filter: BudgetSourceFilter): boolean {
    return this.selectedFilter() === filter;
  }

  isBudgetHighlighted(budgetId: number): boolean {
    return this.highlightedBudgetId() === budgetId;
  }

  onBudgetDefinitionMutated(): void {
    this.loadBudgets();
    this.loadBudgetAlerts();
  }

  onBudgetAlertClick(alert: BudgetAlertUiModel): void {
    console.info('[Budgets] alert clicked', {
      alertType: alert.alertType,
      severity: alert.severity,
      budgetTargetId: alert.budgetTargetId,
      priorityRank: alert.priorityRank
    });

    if (alert.budgetTargetId === null) {
      return;
    }

    this.selectedFilter.set('all');
    this.highlightBudget(alert.budgetTargetId);
  }

  requestDisableBudget(budget: BudgetTargetUiModel): void {
    this.disableErrorMessage.set(null);
    this.pendingDisableBudget.set(budget);
  }

  closeDisableDialog(): void {
    if (this.updatingBudgetId()) {
      return;
    }

    this.disableErrorMessage.set(null);
    this.pendingDisableBudget.set(null);
  }

  confirmDisableBudget(): void {
    const budget = this.pendingDisableBudget();

    if (!budget || this.updatingBudgetId()) {
      return;
    }

    this.disableErrorMessage.set(null);
    this.updatingBudgetId.set(budget.id);

    this.budgetTargetService.updateBudgetTargetStatus(budget.id, { status: 'INACTIVE' })
      .pipe(
        takeUntilDestroyed(this.destroyRef),
        finalize(() => this.updatingBudgetId.set(null))
      )
      .subscribe({
        next: (updatedBudget) => {
          this.mergeBudget(updatedBudget);
          this.pendingDisableBudget.set(null);
          this.disableErrorMessage.set(null);

          if (this.highlightedBudgetId() === updatedBudget.id) {
            this.highlightedBudgetId.set(null);
          }

          this.notificationService.success('Budget desactive avec succes.');
          this.loadBudgetAlerts();
        },
        error: (error: unknown) => {
          const message = this.resolveStatusUpdateErrorMessage(error);
          this.disableErrorMessage.set(message);

          if (!this.isAuthError(error)) {
            this.notificationService.error(message);
          }
        }
      });
  }

  resetFilters(): void {
    this.selectedFilter.set('all');
    this.selectedSort.set('recent');
  }

  private mergeBudget(updatedBudget: BudgetTargetResponse): void {
    this.budgets.update((current) => {
      const next = [...current];
      const index = next.findIndex((budget) => budget.id === updatedBudget.id);

      if (index === -1) {
        return this.sortBudgetsByDate([updatedBudget, ...next]);
      }

      next[index] = updatedBudget;
      return this.sortBudgetsByDate(next);
    });
  }

  private highlightBudget(budgetId: number): void {
    this.highlightedBudgetId.set(budgetId);

    if (this.highlightResetTimer !== null) {
      globalThis.clearTimeout(this.highlightResetTimer);
    }

    globalThis.setTimeout(() => {
      const target = this.document.getElementById(`budget-card-${budgetId}`);
      target?.scrollIntoView({ behavior: 'smooth', block: 'center' });
    }, 80);

    this.highlightResetTimer = globalThis.setTimeout(() => {
      this.highlightedBudgetId.set(null);
      this.highlightResetTimer = null;
    }, 2600);
  }

  private resolveAvailableBudgetCreationCategory(): BudgetTargetCategory | null {
    const usedCategories = new Set(this.budgets().map((budget) => budget.category));
    const preferredCategory = this.preferredBudgetCreationCategories.find((category) => !usedCategories.has(category));

    if (preferredCategory) {
      return preferredCategory;
    }

    return TRANSACTION_CATEGORIES.find(
      (category) => !['AUTRES', 'SALAIRE', 'EPARGNE'].includes(category) && !usedCategories.has(category)
    ) ?? null;
  }

  private buildBudgetCreationData(category: BudgetTargetCategory): BudgetRecommendationActionData {
    const categoryLabel = getTransactionCategoryLabel(category);
    const frames = this.buildBudgetCreationFrames(category);
    const recommendedFrame = frames.find((frame) => frame.recommended) ?? frames[1] ?? frames[0];
    const impactValue = Math.max(40, Math.round(recommendedFrame.suggestedMonthlyAmount * 0.14));

    return {
      recommendationId: `ui-budget-${category.toLowerCase()}`,
      title: 'Definir un budget cible',
      subtitle: 'Creer un cadre premium depuis votre page Budgets, sans changer vos routes existantes.',
      categoryCode: category,
      category: categoryLabel,
      sourceLabel: 'Attijari Compass',
      priorityLabel: 'Pilotage',
      priorityToneClass: 'tone-medium',
      recommendationTitle: `Creation de budget ${categoryLabel}`,
      mainImpactLabel: `Cadre cible pour ${categoryLabel}`,
      impactMonthlyLabel: `${this.formatMoneyValue(impactValue)} de marge potentielle`,
      impactGoalLabel: 'Pilotage mensuel direct',
      suggestedAction: `Creer un budget cible pour ${categoryLabel} et l ajuster selon votre rythme de depense.`,
      confidenceLabel: 'Cadre preconfigure',
      effortLabel: 'Moyen',
      summary: 'Cadre budgetaire prepare depuis la page Budgets.',
      suggestedBudgetLabel: recommendedFrame.budgetLabel,
      frames
    };
  }

  private buildBudgetCreationFrames(category: BudgetTargetCategory): BudgetRecommendationFrameOption[] {
    const baseAmount = this.resolveBudgetCreationBaseAmount(category);

    return [
      {
        key: 'prudent',
        label: 'Prudent',
        badge: 'Souple',
        budgetLabel: `${this.formatMoneyValue(this.roundBudgetAmount(baseAmount * 1.08))} / mois`,
        guidanceLabel: `Marge de confort : ${this.formatMoneyValue(this.roundBudgetAmount(baseAmount * 0.08))} / mois`,
        description: 'Un premier cadre souple pour structurer la categorie sans casser vos habitudes.',
        suggestedMonthlyAmount: this.roundBudgetAmount(baseAmount * 1.08),
        recommended: false
      },
      {
        key: 'equilibre',
        label: 'Equilibre',
        badge: 'Recommande',
        budgetLabel: `${this.formatMoneyValue(baseAmount)} / mois`,
        guidanceLabel: 'Le meilleur compromis pour demarrer avec un cadre tenable.',
        description: 'Le niveau le plus polyvalent pour cadrer vos depenses et garder une marge respirable.',
        suggestedMonthlyAmount: baseAmount,
        recommended: true
      },
      {
        key: 'renforce',
        label: 'Renforce',
        badge: 'Tonique',
        budgetLabel: `${this.formatMoneyValue(this.roundBudgetAmount(baseAmount * 0.86))} / mois`,
        guidanceLabel: `Effort supplementaire : ${this.formatMoneyValue(this.roundBudgetAmount(baseAmount * 0.14))} / mois`,
        description: 'Une option plus stricte pour reprendre rapidement la main sur cette categorie.',
        suggestedMonthlyAmount: this.roundBudgetAmount(baseAmount * 0.86),
        recommended: false
      }
    ];
  }

  private resolveBudgetCreationBaseAmount(category: BudgetTargetCategory): number {
    switch (category) {
      case 'SHOPPING':
      case 'ALIMENTATION':
      case 'SUPERMARCHE':
        return 420;
      case 'CAFES':
      case 'LIVRAISON':
        return 260;
      case 'RESTAURANT':
        return 320;
      case 'FACTURES':
        return 240;
      case 'TRANSPORT':
      case 'STATION_SERVICES':
        return 320;
      case 'LOGEMENT':
        return 780;
      case 'TECHNOLOGIE':
      case 'DIVERTISSEMENT':
        return 280;
      case 'HOTEL':
      case 'VOYAGE':
        return 520;
      case 'SALAIRE':
        return 2200;
      case 'EPARGNE':
        return 500;
      default:
        return 300;
    }
  }

  private roundBudgetAmount(value: number): number {
    return Math.max(50, Math.round(value / 10) * 10);
  }

  private mapBudgetToUiModel(budget: BudgetTargetResponse): BudgetTargetUiModel {
    const targetAmount = this.resolvePrimaryTargetAmount(budget);
    const amount = targetAmount ?? 0;
    const levelTone = this.resolveLevelTone(budget.selectedLevel);
    const isAiGenerated = budget.aiGenerated || budget.source === 'RECOMMENDATION_AI';
    const levelLabel = this.resolveLevelLabel(budget.selectedLevel, budget.selectedLevelLabel);
    const statusLabel = this.resolveStatusLabel(budget.status, budget.statusLabel);
    const spentThisMonth = this.toNullableNumber(budget.spentThisMonth);
    const remainingAmount = this.toNullableNumber(budget.remainingAmount);
    const usagePercent = this.toNullableNumber(budget.usagePercent);
    const monitoringStatus = budget.monitoringStatus;
    const hasMonitoring = this.hasMonitoringData(budget, targetAmount, spentThisMonth, remainingAmount, usagePercent);
    const monitoringTone = this.resolveMonitoringTone(monitoringStatus);
    const isExceeded =
      monitoringStatus === 'DEPASSE'
      || (usagePercent !== null && usagePercent > 100)
      || (remainingAmount !== null && remainingAmount < 0);

    return {
      id: budget.id,
      category: budget.category,
      categoryLabel: budget.categoryLabel?.trim() || this.humanizeEnum(budget.category),
      categoryIcon: this.resolveCategoryIcon(budget.category),
      amount,
      amountLabel: targetAmount !== null ? this.formatMonthlyMoney(targetAmount) : 'Montant a definir',
      targetAmount,
      targetAmountLabel: targetAmount !== null ? this.formatMonthlyMoney(targetAmount) : 'Montant a definir',
      selectedLevel: budget.selectedLevel,
      levelLabel,
      levelSummary: budget.selectedLevelSummary?.trim() || this.resolveLevelSummary(budget.selectedLevel),
      source: budget.source,
      sourceLabel: isAiGenerated ? 'IA' : 'Manuel',
      status: budget.status,
      statusLabel,
      createdAt: budget.createdAt,
      createdAtLabel: this.formatDateLabel(budget.createdAt),
      recommendationTitle: budget.recommendationTitle?.trim() || null,
      summary: budget.summary?.trim() || null,
      isAiGenerated,
      isActive: budget.status === 'ACTIVE',
      levelTone,
      statusTone: this.resolveStatusTone(budget.status),
      contextualLine: this.buildContextualLine(budget.categoryLabel, isAiGenerated),
      sourceInsight: isAiGenerated
        ? 'Cadre prepare depuis une recommandation IA.'
        : 'Cadre defini manuellement depuis votre espace budget.',
      hasMonitoring,
      monitoringStatus,
      monitoringLabel: this.resolveMonitoringLabel(monitoringStatus, budget.monitoringStatusLabel, hasMonitoring),
      monitoringTone,
      monitoringMessage: this.resolveMonitoringMessage(monitoringStatus, hasMonitoring),
      monitoringRank: this.resolveMonitoringRank(monitoringStatus),
      spentThisMonth,
      spentThisMonthLabel: this.formatOptionalMoney(spentThisMonth),
      remainingAmount,
      remainingAmountLabel: this.formatOptionalMoney(remainingAmount),
      usagePercent,
      usagePercentLabel: this.formatUsagePercent(usagePercent),
      progressWidth: this.resolveProgressWidth(usagePercent),
      progressAriaValue: this.resolveProgressAriaValue(usagePercent),
      isExceeded,
      isNearLimit: monitoringStatus === 'A_SURVEILLER'
    };
  }

  private mapBudgetAlertToUiModel(alert: BudgetAlertResponse): BudgetAlertUiModel {
    const severityTone = this.resolveAlertSeverityTone(alert.severity);
    const categoryLabel = alert.categoryLabel?.trim() || (alert.category ? this.humanizeEnum(alert.category) : null);

    return {
      id: `${alert.alertType}-${alert.budgetTargetId ?? 'global'}-${alert.priorityRank}-${alert.generatedAt ?? 'na'}`,
      alertType: alert.alertType,
      severity: alert.severity,
      severityLabel: this.getAlertSeverityLabel(alert.severity),
      severityTone,
      title: alert.title,
      message: alert.message,
      categoryLabel,
      metricLabel: this.getAlertMetricLabel(alert),
      contextLabel: this.getAlertContextLabel(categoryLabel),
      budgetTargetId: alert.budgetTargetId,
      accentTone: severityTone,
      iconName: this.getAlertIconName(alert.alertType, alert.severity),
      isCritical: alert.severity === 'CRITICAL',
      isWarning: alert.severity === 'WARNING',
      isInfo: alert.severity === 'INFO',
      priorityRank: alert.priorityRank
    };
  }

  private compareBudgetAlerts(left: BudgetAlertResponse, right: BudgetAlertResponse): number {
    const severityGap = this.alertSeverityWeight(right.severity) - this.alertSeverityWeight(left.severity);

    if (severityGap !== 0) {
      return severityGap;
    }

    const priorityGap = left.priorityRank - right.priorityRank;

    if (priorityGap !== 0) {
      return priorityGap;
    }

    return this.toTimestamp(right.generatedAt ?? '') - this.toTimestamp(left.generatedAt ?? '');
  }

  private compareBudgets(
    left: BudgetTargetUiModel,
    right: BudgetTargetUiModel,
    sort: BudgetSortKey
  ): number {
    const monitoringGap = right.monitoringRank - left.monitoringRank;

    if (monitoringGap !== 0) {
      return monitoringGap;
    }

    switch (sort) {
      case 'amount':
        return this.sortByAmount(left, right) || this.sortByRecent(left, right);
      case 'category':
        return left.categoryLabel.localeCompare(right.categoryLabel, 'fr', { sensitivity: 'base' }) || this.sortByRecent(left, right);
      case 'recent':
      default:
        return this.sortByRecent(left, right);
    }
  }

  private sortByRecent(
    left: Pick<BudgetTargetUiModel, 'createdAt'>,
    right: Pick<BudgetTargetUiModel, 'createdAt'>
  ): number {
    return this.toTimestamp(right.createdAt) - this.toTimestamp(left.createdAt);
  }

  private sortByAmount(
    left: Pick<BudgetTargetUiModel, 'amount'>,
    right: Pick<BudgetTargetUiModel, 'amount'>
  ): number {
    return Math.max(right.amount, 0) - Math.max(left.amount, 0);
  }

  private sortBudgetsByDate(budgets: BudgetTargetResponse[]): BudgetTargetResponse[] {
    return [...budgets].sort((left, right) => this.toTimestamp(right.createdAt) - this.toTimestamp(left.createdAt));
  }

  private toTimestamp(value: string): number {
    const timestamp = new Date(value).getTime();
    return Number.isFinite(timestamp) ? timestamp : 0;
  }

  private resolveCategoryIcon(category: string): string {
    return getTransactionCategoryMaterialIcon(category);
  }

  private resolveLevelTone(level: BudgetTargetLevel): BudgetLevelTone {
    switch (level) {
      case 'PRUDENT':
        return 'prudent';
      case 'RENFORCE':
        return 'renforce';
      case 'EQUILIBRE':
      default:
        return 'equilibre';
    }
  }

  private resolveStatusTone(status: BudgetTargetStatus): BudgetStatusTone {
    switch (status) {
      case 'INACTIVE':
        return 'inactive';
      case 'ARCHIVED':
        return 'archived';
      case 'ACTIVE':
      default:
        return 'active';
    }
  }

  private resolveMonitoringTone(status: BudgetTargetMonitoringStatus | null): BudgetMonitoringTone {
    switch (status) {
      case 'SOUS_CONTROLE':
        return 'sous-controle';
      case 'A_SURVEILLER':
        return 'a-surveiller';
      case 'DEPASSE':
        return 'depasse';
      default:
        return 'neutral';
    }
  }

  private resolveAlertSeverityTone(severity: BudgetAlertSeverity): BudgetAlertTone {
    switch (severity) {
      case 'CRITICAL':
        return 'critical';
      case 'WARNING':
        return 'warning';
      case 'INFO':
      default:
        return 'info';
    }
  }

  private resolveMonitoringRank(status: BudgetTargetMonitoringStatus | null): number {
    switch (status) {
      case 'DEPASSE':
        return 3;
      case 'A_SURVEILLER':
        return 2;
      case 'SOUS_CONTROLE':
        return 1;
      default:
        return 0;
    }
  }

  private alertSeverityWeight(severity: BudgetAlertSeverity): number {
    switch (severity) {
      case 'CRITICAL':
        return 3;
      case 'WARNING':
        return 2;
      case 'INFO':
      default:
        return 1;
    }
  }

  private resolveLevelLabel(level: BudgetTargetLevel, fallback: string | null): string {
    if (fallback?.trim()) {
      return fallback.trim();
    }

    switch (level) {
      case 'PRUDENT':
        return 'Prudent';
      case 'RENFORCE':
        return 'Renforce';
      case 'EQUILIBRE':
      default:
        return 'Equilibre';
    }
  }

  private resolveStatusLabel(status: BudgetTargetStatus, fallback: string | null): string {
    if (fallback?.trim()) {
      return fallback.trim();
    }

    switch (status) {
      case 'INACTIVE':
        return 'Inactif';
      case 'ARCHIVED':
        return 'Archive';
      case 'ACTIVE':
      default:
        return 'Actif';
    }
  }

  private getAlertSeverityLabel(severity: BudgetAlertSeverity): string {
    switch (severity) {
      case 'CRITICAL':
        return 'Critique';
      case 'WARNING':
        return 'A surveiller';
      case 'INFO':
      default:
        return 'Info';
    }
  }

  private resolveMonitoringLabel(
    status: BudgetTargetMonitoringStatus | null,
    fallback: string | null,
    hasMonitoring: boolean
  ): string {
    if (fallback?.trim()) {
      return fallback.trim();
    }

    switch (status) {
      case 'SOUS_CONTROLE':
        return 'Sous controle';
      case 'A_SURVEILLER':
        return 'A surveiller';
      case 'DEPASSE':
        return 'Depasse';
      default:
        return hasMonitoring ? 'Suivi du mois' : 'Suivi indisponible';
    }
  }

  private resolveLevelSummary(level: BudgetTargetLevel): string {
    switch (level) {
      case 'PRUDENT':
        return 'Cadre progressif pour remettre la categorie sous controle sans tension excessive.';
      case 'RENFORCE':
        return 'Cadre plus ferme pour accelerer la reprise de marge mensuelle.';
      case 'EQUILIBRE':
      default:
        return 'Cadre regulier pour tenir la categorie dans une trajectoire durable.';
    }
  }

  private resolveMonitoringMessage(
    status: BudgetTargetMonitoringStatus | null,
    hasMonitoring: boolean
  ): string {
    if (!hasMonitoring) {
      return 'Le suivi du mois sera visible des que les donnees seront consolidees.';
    }

    switch (status) {
      case 'SOUS_CONTROLE':
        return 'Votre cadre reste sous controle ce mois-ci.';
      case 'A_SURVEILLER':
        return 'Ce budget approche de sa limite mensuelle.';
      case 'DEPASSE':
        return 'Ce budget a depasse son seuil prevu.';
      default:
        return 'Le suivi du mois reste en cours de consolidation.';
    }
  }

  private getAlertMetricLabel(alert: BudgetAlertResponse): string | null {
    if (alert.usagePercent !== null && Number.isFinite(alert.usagePercent)) {
      return `${this.percentFormatter.format(Math.round(alert.usagePercent))}% consomme`;
    }

    if (alert.remainingAmount !== null && Number.isFinite(alert.remainingAmount)) {
      if (alert.remainingAmount > 0) {
        return `Reste disponible : ${this.formatMoneyValue(alert.remainingAmount)}`;
      }

      if (alert.remainingAmount < 0) {
        return `Depassement estime : ${this.formatMoneyValue(Math.abs(alert.remainingAmount))}`;
      }

      return 'Reste disponible : 0 DT';
    }

    if (
      alert.spentThisMonth !== null
      && Number.isFinite(alert.spentThisMonth)
      && alert.targetAmount !== null
      && Number.isFinite(alert.targetAmount)
    ) {
      return `${this.formatMoneyValue(alert.spentThisMonth)} / ${this.formatMoneyValue(alert.targetAmount)}`;
    }

    if (alert.spentThisMonth !== null && Number.isFinite(alert.spentThisMonth)) {
      return `Depense ce mois : ${this.formatMoneyValue(alert.spentThisMonth)}`;
    }

    return null;
  }

  private getAlertContextLabel(categoryLabel: string | null): string | null {
    if (!categoryLabel) {
      return null;
    }

    return `Categorie ${categoryLabel}`;
  }

  private getAlertIconName(alertType: BudgetAlertType, severity: BudgetAlertSeverity): string {
    switch (alertType) {
      case 'BUDGET_DEPASSE':
      case 'BUDGET_CRITIQUE_PRIORITAIRE':
        return 'warning';
      case 'BUDGET_QUASI_ATTEINT':
        return 'schedule';
      case 'BUDGET_RESTE_FAIBLE':
        return 'speed';
      case 'BUDGET_SOUS_CONTROLE':
        return 'verified';
      case 'BUDGET_MAITRISE_GLOBALE':
        return 'check_circle';
      default:
        return severity === 'CRITICAL' ? 'warning' : 'insights';
    }
  }

  private resolvePrimaryTargetAmount(budget: BudgetTargetResponse): number | null {
    if (budget.targetAmount !== null && Number.isFinite(budget.targetAmount)) {
      return budget.targetAmount;
    }

    if (Number.isFinite(budget.suggestedMonthlyAmount) && budget.suggestedMonthlyAmount > 0) {
      return budget.suggestedMonthlyAmount;
    }

    return null;
  }

  private hasMonitoringData(
    budget: BudgetTargetResponse,
    targetAmount: number | null,
    spentThisMonth: number | null,
    remainingAmount: number | null,
    usagePercent: number | null
  ): boolean {
    return targetAmount !== null
      || spentThisMonth !== null
      || remainingAmount !== null
      || usagePercent !== null
      || budget.monitoringStatus !== null
      || !!budget.monitoringStatusLabel?.trim();
  }

  private resolveProgressWidth(usagePercent: number | null): number {
    if (usagePercent === null) {
      return 0;
    }

    return Math.max(0, Math.min(100, Math.round(usagePercent)));
  }

  private resolveProgressAriaValue(usagePercent: number | null): number {
    if (usagePercent === null) {
      return 0;
    }

    return Math.max(0, Math.min(100, Math.round(usagePercent)));
  }

  private buildContextualLine(categoryLabel: string, isAiGenerated: boolean): string {
    const label = categoryLabel?.trim() || 'cette categorie';

    if (isAiGenerated) {
      return `Ce cadre aide a mieux controler la categorie ${label} depuis un signal IA.`;
    }

    return `Ce cadre aide a mieux controler la categorie ${label}.`;
  }

  private formatMonthlyMoney(amount: number): string {
    return `${this.amountFormatter.format(Math.round(amount))} DT / mois`;
  }

  private formatMoneyValue(amount: number): string {
    return `${this.amountFormatter.format(Math.round(amount))} DT`;
  }

  private formatOptionalMoney(amount: number | null): string {
    if (amount === null) {
      return 'n/d';
    }

    const prefix = amount < 0 ? '-' : '';
    return `${prefix}${this.amountFormatter.format(Math.round(Math.abs(amount)))} DT`;
  }

  private formatUsagePercent(value: number | null): string {
    if (value === null) {
      return 'n/d';
    }

    return `${this.percentFormatter.format(Math.round(value))}%`;
  }

  private formatDateLabel(value: string): string {
    const date = new Date(value);

    if (Number.isNaN(date.getTime())) {
      return 'Date recente';
    }

    const formatted = this.dateFormatter.format(date);
    return `${formatted.charAt(0).toUpperCase()}${formatted.slice(1)}`;
  }

  private pluralizeBudget(count: number): string {
    return count > 1 ? 'budgets' : 'budget';
  }

  private humanizeEnum(value: string): string {
    return value
      .trim()
      .replace(/[_-]+/g, ' ')
      .toLowerCase()
      .split(' ')
      .filter(Boolean)
      .map((segment) => `${segment.charAt(0).toUpperCase()}${segment.slice(1)}`)
      .join(' ');
  }

  private toNullableNumber(value: number | null | undefined): number | null {
    return typeof value === 'number' && Number.isFinite(value) ? value : null;
  }

  private resolveStatusUpdateErrorMessage(error: unknown): string {
    const status = this.extractStatusCode(error);

    switch (status) {
      case 400:
        return 'Impossible de mettre a jour ce budget. Verifiez son statut actuel.';
      case 401:
      case 403:
        return 'Votre session doit etre revalidee.';
      default:
        return 'Impossible de mettre a jour ce budget.';
    }
  }

  private isAuthError(error: unknown): boolean {
    const status = this.extractStatusCode(error);
    return status === 401 || status === 403;
  }

  private extractStatusCode(error: unknown): number | null {
    if (typeof error !== 'object' || error === null) {
      return null;
    }

    const source = error as Record<string, unknown>;
    return typeof source['status'] === 'number' ? source['status'] : null;
  }

  private extractErrorMessage(error: unknown, fallback: string): string {
    if (typeof error === 'object' && error !== null) {
      const source = error as Record<string, unknown>;
      const nested = source['error'];

      if (nested && typeof nested === 'object' && !Array.isArray(nested)) {
        const nestedSource = nested as Record<string, unknown>;
        const nestedMessage = nestedSource['message'] ?? nestedSource['detail'];

        if (typeof nestedMessage === 'string' && nestedMessage.trim()) {
          return nestedMessage.trim();
        }
      }

      const message = source['message'];

      if (typeof message === 'string' && message.trim()) {
        return message.trim();
      }
    }

    return fallback;
  }
}
