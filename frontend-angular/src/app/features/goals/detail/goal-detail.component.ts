import { CommonModule } from '@angular/common';
import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { GoalService } from '../../../core/services/api.services';
import { NotificationService } from '../../../core/services/notification.service';
import {
  getBlockingCategoryLabel,
  getFeasibilityTone,
  getGoalDisplayName,
  getGoalIcon,
  getGoalProgressClass,
  getGoalStatusLabel,
  getGoalTypeLabel,
  getRiskLevelLabel,
  hasGoalRecommendationsContent,
  hasGoalStorytellingContent,
  normalizeGoalPrediction,
  normalizeGoalRecommendations,
  normalizeGoalResponse,
  normalizeGoalScenarios,
  normalizeGoalStorytelling,
  NormalizedGoal,
  NormalizedGoalRecommendation,
  NormalizedGoalScenario,
  NormalizedGoalStorytelling,
  resolveGoalPredictionCard
} from '../goal-ui';

@Component({
  selector: 'app-goal-detail',
  standalone: true,
  imports: [CommonModule, RouterLink],
  templateUrl: './goal-detail.component.html',
  styleUrl: './goal-detail.component.css'
})
export class GoalDetailComponent implements OnInit {
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly goalService = inject(GoalService);
  private readonly notifService = inject(NotificationService);

  readonly goalId = signal<number | null>(null);
  readonly goal = signal<NormalizedGoal | null>(null);
  readonly goalLoading = signal(true);
  readonly goalError = signal<string | null>(null);

  readonly scenarios = signal<NormalizedGoalScenario[]>([]);

  readonly recommendations = signal<NormalizedGoalRecommendation[]>([]);
  readonly recommendationsLoading = signal(true);
  readonly recommendationsError = signal<string | null>(null);

  readonly storytelling = signal<NormalizedGoalStorytelling | null>(null);
  readonly storytellingLoading = signal(true);
  readonly storytellingError = signal<string | null>(null);
  readonly storytellingPulse = signal(false);

  private readonly predictionData = signal<ReturnType<typeof normalizeGoalPrediction> | null>(null);
  readonly predictionLoading = signal(true);
  readonly predictionError = signal<string | null>(null);

  readonly contributionSlider = signal(0);

  readonly goalName = computed(() => getGoalDisplayName(this.goal()));
  readonly progressPercent = computed(() => Math.max(0, Math.min(100, this.goal()?.progressPercentage || 0)));
  readonly progressClass = computed(() => getGoalProgressClass(this.progressPercent()));
  readonly predictionCard = computed(() => resolveGoalPredictionCard(this.goal(), this.predictionData(), this.scenarios()));
  readonly feasibilityScore = computed(() => this.predictionCard().feasibilityScore);
  readonly successProbability = computed(() => this.predictionCard().successProbability);
  readonly riskLevel = computed(() => this.predictionCard().riskLevel);
  readonly predictedDate = computed(() => this.predictionCard().predictedDate);
  readonly requiredMonthlyContribution = computed(() => this.predictionCard().requiredMonthlyContribution);
  readonly feasibilityTone = computed(() => getFeasibilityTone(this.feasibilityScore()));
  readonly hasPredictionData = computed(() => (
    this.feasibilityScore() !== null
    || this.successProbability() !== null
    || !!this.riskLevel()
    || !!this.predictedDate()
    || this.requiredMonthlyContribution() !== null
  ));
  readonly hasRecommendationsData = computed(() => hasGoalRecommendationsContent(this.recommendations()));
  readonly hasStoryData = computed(() => hasGoalStorytellingContent(this.storytelling()) || !!this.storytellingSummary());

  readonly recommendedContribution = computed(() => {
    const baseline = this.requiredMonthlyContribution() ?? this.goal()?.monthlySavingsRequired ?? 0;
    return Math.max(0, Math.round(baseline));
  });

  readonly sliderMin = computed(() => {
    const remaining = this.goal()?.remainingAmount ?? 0;
    if (remaining <= 0) {
      return 0;
    }

    const baseline = this.recommendedContribution();
    return Math.max(50, Math.floor(Math.max(remaining / 24, baseline * 0.35) / 10) * 10);
  });

  readonly sliderMax = computed(() => {
    const remaining = this.goal()?.remainingAmount ?? 0;
    const baseline = this.recommendedContribution();
    return Math.max(300, Math.ceil(Math.max(remaining, baseline * 2.6, 1200) / 10) * 10);
  });

  readonly sliderStep = computed(() => this.sliderMax() <= 600 ? 10 : 50);

  readonly interactiveContribution = computed(() => {
    const current = this.contributionSlider();
    if (current > 0) {
      return current;
    }

    return this.recommendedContribution();
  });

  readonly projectedMonthsToGoal = computed(() => {
    const remaining = this.goal()?.remainingAmount ?? 0;
    const contribution = this.interactiveContribution();

    if (remaining <= 0) {
      return 0;
    }

    if (!contribution || contribution <= 0) {
      return null;
    }

    return Math.max(1, Math.ceil(remaining / contribution));
  });

  readonly projectedDateWithContribution = computed(() => {
    const months = this.projectedMonthsToGoal();
    if (months === null) {
      return null;
    }

    const baseDate = new Date();
    baseDate.setMonth(baseDate.getMonth() + months);
    return baseDate.toISOString();
  });

  readonly interactiveSuccessProbability = computed(() => {
    const goal = this.goal();
    const recommended = this.recommendedContribution();
    const chosen = this.interactiveContribution();
    const months = this.projectedMonthsToGoal();

    if (!goal || months === null) {
      return this.successProbability();
    }

    const targetDate = this.parseDate(goal.targetDate);
    const ratio = recommended > 0 ? chosen / recommended : 1;
    const scoreBase = (this.successProbability() ?? this.feasibilityScore() ?? 55);
    let adjusted = scoreBase + ((ratio - 1) * 18);

    if (targetDate) {
      const monthsUntilTarget = Math.max(1, Math.ceil((targetDate.getTime() - Date.now()) / (30 * 24 * 60 * 60 * 1000)));
      adjusted += months <= monthsUntilTarget ? 8 : -12;
    }

    return Math.max(5, Math.min(99, adjusted));
  });

  readonly interactiveFeasibilityScore = computed(() => {
    const recommended = this.recommendedContribution();
    const chosen = this.interactiveContribution();
    const current = this.feasibilityScore() ?? 55;
    const ratio = recommended > 0 ? chosen / recommended : 1;
    const adjustment = (ratio - 1) * 16;
    return Math.max(5, Math.min(99, current + adjustment));
  });

  readonly interactiveRiskLevel = computed(() => {
    const score = this.interactiveFeasibilityScore();
    if (score >= 75) {
      return 'LOW';
    }
    if (score >= 45) {
      return 'MEDIUM';
    }
    return 'HIGH';
  });

  readonly interactiveFeasibilityTone = computed(() => getFeasibilityTone(this.interactiveFeasibilityScore()));

  readonly storytellingSummary = computed(() => {
    const goal = this.goal();
    if (!goal) {
      return null;
    }

    const monthlyContribution = this.formatCurrency(this.interactiveContribution());
    const predictedDate = this.formatDate(this.projectedDateWithContribution());
    const riskLevel = this.riskLabel(this.interactiveRiskLevel());

    return `Il reste ${this.formatCurrency(goal.remainingAmount)} a epargner. Avec une contribution cible de ${monthlyContribution} par mois, la projection locale vous mene autour du ${predictedDate}, avec un niveau de risque ${riskLevel.toLowerCase()}.`;
  });

  ngOnInit(): void {
    const id = Number(this.route.snapshot.paramMap.get('id'));

    if (!id) {
      this.notifService.error('Objectif introuvable.');
      void this.router.navigate(['/goals']);
      return;
    }

    this.goalId.set(id);
    this.refreshAll();
  }

  refreshAll(): void {
    const id = this.goalId();
    if (!id) {
      return;
    }

    this.loadGoal(id);
    this.loadPrediction(id);
    this.loadSimulations(id);
    this.loadRecommendations(id);
    this.loadStorytelling(id);
  }

  reloadStorytelling(): void {
    const id = this.goalId();
    if (!id) {
      return;
    }

    this.storytellingPulse.set(true);
    this.loadStorytelling(id);

    if (typeof window !== 'undefined') {
      window.setTimeout(() => this.storytellingPulse.set(false), 900);
    }
  }

  recommendationImpactValue(recommendation: NormalizedGoalRecommendation): string {
    const amount = recommendation.blockingCategories.reduce((sum, category) => {
      const currentAmount = category.amount ?? category.monthlyAmount ?? 0;
      return sum + currentAmount;
    }, 0);

    if (amount > 0) {
      return `${this.formatCurrency(amount)} a arbitrer`;
    }

    return recommendation.priority ? `Priorite ${recommendation.priority}` : 'Impact progressif';
  }

  recommendationImpactTone(recommendation: NormalizedGoalRecommendation): string {
    const priority = (recommendation.priority || '').toUpperCase();

    if (priority.includes('HIGH') || priority.includes('HAUTE')) {
      return 'impact-strong';
    }

    if (recommendation.blockingCategories.length > 0) {
      return 'impact-medium';
    }

    return 'impact-soft';
  }

  predictionSummary(): string {
    const goal = this.goal();
    if (!goal) {
      return '';
    }

    return `Projection basee sur votre objectif ${this.goalName().toLowerCase()}, votre reste a epargner et les scenarios IA disponibles.`;
  }

  statusLabel(status?: string | null): string {
    return getGoalStatusLabel(status);
  }

  typeLabel(type?: string | null): string {
    return getGoalTypeLabel(type);
  }

  goalIcon(goal: NormalizedGoal | null): string {
    return goal ? getGoalIcon(goal) : 'flag';
  }

  formatCurrency(value: number | null | undefined): string {
    if (value === null || value === undefined || Number.isNaN(value)) {
      return 'Non disponible';
    }

    return `${value.toLocaleString('fr-FR', { minimumFractionDigits: 0, maximumFractionDigits: 0 })} DT`;
  }

  formatPercent(value: number | null | undefined): string {
    if (value === null || value === undefined || Number.isNaN(value)) {
      return 'Non disponible';
    }

    return `${value.toFixed(0)}%`;
  }

  formatDate(value: string | null | undefined): string {
    const parsed = this.parseDate(value);
    if (!parsed) {
      return 'Non disponible';
    }

    return parsed.toLocaleDateString('fr-FR', {
      day: '2-digit',
      month: 'short',
      year: 'numeric'
    });
  }

  hasStorytellingContent(): boolean {
    return hasGoalStorytellingContent(this.storytelling());
  }

  riskLabel(value: string | null | undefined): string {
    return getRiskLevelLabel(value);
  }

  blockingCategoryLabel(category: { category?: string; name?: string } | null | undefined): string {
    return getBlockingCategoryLabel(category);
  }

  private loadGoal(id: number): void {
    this.goalLoading.set(true);
    this.goalError.set(null);
    this.goal.set(null);

    this.goalService.getGoalById(id).subscribe({
      next: (goal) => {
        this.goal.set(normalizeGoalResponse(goal));
        this.goalLoading.set(false);
        this.syncContributionSlider();
      },
      error: (error) => {
        this.goalLoading.set(false);
        const message = error?.error?.message || 'Impossible de charger le detail de l objectif.';
        this.goalError.set(message);
      }
    });
  }

  private loadPrediction(id: number): void {
    this.predictionLoading.set(true);
    this.predictionError.set(null);
    this.predictionData.set(null);

    this.goalService.getGoalPrediction(id).subscribe({
      next: (response) => {
        this.predictionData.set(normalizeGoalPrediction(response));
        this.predictionLoading.set(false);
        this.syncContributionSlider();
      },
      error: (error) => {
        this.predictionLoading.set(false);
        this.predictionError.set(error?.error?.message || 'Prediction indisponible pour cet objectif.');
      }
    });
  }

  private loadSimulations(id: number): void {
    this.scenarios.set([]);

    this.goalService.getGoalSimulations(id).subscribe({
      next: (response) => {
        this.scenarios.set(normalizeGoalScenarios(response));
        this.syncContributionSlider();
      },
      error: () => this.scenarios.set([])
    });
  }

  private loadRecommendations(id: number): void {
    this.recommendationsLoading.set(true);
    this.recommendationsError.set(null);
    this.recommendations.set([]);

    this.goalService.getGoalRecommendations(id).subscribe({
      next: (response) => {
        this.recommendations.set(normalizeGoalRecommendations(response));
        this.recommendationsLoading.set(false);
      },
      error: (error) => {
        this.recommendationsLoading.set(false);
        this.recommendationsError.set(error?.error?.message || 'Recommandations indisponibles pour cet objectif.');
      }
    });
  }

  private loadStorytelling(id: number): void {
    this.storytellingLoading.set(true);
    this.storytellingError.set(null);
    this.storytelling.set(null);

    this.goalService.getGoalStorytelling(id).subscribe({
      next: (response) => {
        this.storytelling.set(normalizeGoalStorytelling(response));
        this.storytellingLoading.set(false);
      },
      error: (error) => {
        this.storytellingLoading.set(false);
        this.storytellingError.set(error?.error?.message || 'Storytelling indisponible pour cet objectif.');
      }
    });
  }

  private syncContributionSlider(): void {
    const recommended = this.requiredMonthlyContribution() ?? this.goal()?.monthlySavingsRequired ?? 0;
    if (recommended > 0 && this.contributionSlider() === 0) {
      this.contributionSlider.set(Math.round(recommended));
    }
  }

  private parseDate(value: string | null | undefined): Date | null {
    if (!value) {
      return null;
    }

    const parsed = new Date(value);
    return Number.isNaN(parsed.getTime()) ? null : parsed;
  }

}
