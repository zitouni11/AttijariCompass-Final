import { CommonModule } from '@angular/common';
import { Component, computed, inject, OnInit, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { GoalService, UserService } from '../../../core/services/api.services';
import { NotificationService } from '../../../core/services/notification.service';
import {
  getGoalDisplayName,
  getGoalIcon,
  getGoalProgressClass,
  getGoalStatusLabel,
  getGoalTypeLabel,
  NormalizedGoal,
  normalizeGoalCollection,
  normalizeGoalResponse
} from '../goal-ui';

interface GoalInspirationCard {
  icon: string;
  title: string;
  copy: string;
}

type GoalHealthTone = 'success' | 'warning' | 'danger' | 'neutral';

@Component({
  selector: 'app-goals-list',
  standalone: true,
  imports: [CommonModule, RouterLink, FormsModule],
  templateUrl: './goals-list.component.html',
  styleUrl: './goals-list.component.css'
})
export class GoalsListComponent implements OnInit {
  private readonly goalService = inject(GoalService);
  private readonly userService = inject(UserService);
  private readonly notifService = inject(NotificationService);

  readonly goals = signal<NormalizedGoal[]>([]);
  readonly loading = signal(true);
  readonly progressAmounts = signal<Record<number, number>>({});

  readonly inspirations: GoalInspirationCard[] = [
    {
      icon: 'health_and_safety',
      title: "Fonds d'urgence",
      copy: 'Constituez un coussin financier pour absorber les imprévus sans stress.'
    },
    {
      icon: 'flight_takeoff',
      title: 'Voyage',
      copy: 'Planifiez une escapade sereine avec une trajectoire d epargne claire.'
    },
    {
      icon: 'workspace_premium',
      title: 'Achat important',
      copy: 'Anticipez un projet majeur avec un cap, une echeance et un rythme adapte.'
    }
  ];

  readonly activeGoalsCount = computed(() => this.goals().filter((goal) => goal.status === 'EN_COURS').length);
  readonly totalTargetAmount = computed(() => this.goals().reduce((sum, goal) => sum + goal.targetAmount, 0));
  readonly totalCurrentAmount = computed(() => this.goals().reduce((sum, goal) => sum + goal.currentAmount, 0));
  readonly averageProgress = computed(() => {
    const goals = this.goals();
    if (!goals.length) {
      return 0;
    }

    return goals.reduce((sum, goal) => sum + Math.min(100, Math.max(0, goal.progressPercentage || 0)), 0) / goals.length;
  });

  ngOnInit(): void {
    this.loadGoals();
  }

  addProgress(goal: NormalizedGoal): void {
    const amount = Number(this.progressAmounts()[goal.id] || 0);

    if (!amount || amount <= 0) {
      this.notifService.warning('Entrez un montant valide a ajouter.');
      return;
    }

    this.goalService.addProgress(goal.id, amount).subscribe({
      next: (updatedGoal) => {
        const normalizedGoal = normalizeGoalResponse(updatedGoal);
        this.goals.update((currentGoals) => this.sortGoals(
          currentGoals.map((item) => item.id === normalizedGoal.id ? normalizedGoal : item)
        ));
        this.progressAmounts.update((current) => ({ ...current, [goal.id]: 0 }));
        this.notifService.success(`Progression ajoutee a "${this.goalTitle(goal)}".`);
      },
      error: () => this.notifService.error('Erreur lors de la mise a jour de la progression.')
    });
  }

  delete(goal: NormalizedGoal): void {
    if (!confirm(`Supprimer l'objectif "${this.goalTitle(goal)}" ?`)) {
      return;
    }

    this.goalService.delete(goal.id).subscribe({
      next: () => {
        this.goals.update((currentGoals) => currentGoals.filter((item) => item.id !== goal.id));
        this.notifService.success('Objectif supprime.');
      },
      error: () => this.notifService.error('Erreur lors de la suppression de l objectif.')
    });
  }

  setProgressAmount(goalId: number, rawValue: string): void {
    const parsed = Number(rawValue);
    this.progressAmounts.update((current) => ({
      ...current,
      [goalId]: Number.isFinite(parsed) ? parsed : 0
    }));
  }

  formatCurrency(value: number | null | undefined): string {
    if (value === null || value === undefined || Number.isNaN(value)) {
      return '0 DT';
    }

    return `${value.toLocaleString('fr-FR', {
      minimumFractionDigits: 0,
      maximumFractionDigits: 0
    })} DT`;
  }

  formatDate(date: string | null | undefined): string {
    const parsed = this.parseDate(date);
    if (!parsed) {
      return 'Non renseignee';
    }

    return parsed.toLocaleDateString('fr-FR', {
      day: '2-digit',
      month: 'short',
      year: 'numeric'
    });
  }

  daysRemaining(goal: NormalizedGoal): string {
    const targetDate = this.parseDate(goal.targetDate);
    if (!targetDate) {
      return 'Date manquante';
    }

    const now = new Date();
    const difference = Math.ceil((targetDate.getTime() - now.getTime()) / 86400000);

    if (difference < 0) {
      return `${Math.abs(difference)} jour(s) de retard`;
    }

    if (difference === 0) {
      return 'Echeance aujourd hui';
    }

    return `${difference} jour(s) restants`;
  }

  goalTitle(goal: NormalizedGoal): string {
    return getGoalDisplayName(goal);
  }

  goalType(type?: string | null): string {
    return getGoalTypeLabel(type);
  }

  goalIcon(goal: NormalizedGoal): string {
    return getGoalIcon(goal);
  }

  progressClass(progress: number): string {
    return getGoalProgressClass(progress);
  }

  goalStatusLabel(status: string): string {
    return getGoalStatusLabel(status);
  }

  goalHealthLabel(goal: NormalizedGoal): string {
    if (goal.status === 'ATTEINT' || goal.progressPercentage >= 100) {
      return 'Atteint';
    }

    if (goal.status === 'ABANDONNE') {
      return 'En pause';
    }

    const targetDate = this.parseDate(goal.targetDate);
    if (targetDate && targetDate.getTime() < Date.now()) {
      return 'En retard';
    }

    const expectedProgress = this.expectedProgress(goal);
    const actualProgress = Math.max(0, Math.min(100, goal.progressPercentage || 0));

    if (actualProgress >= Math.max(25, expectedProgress - 10)) {
      return 'En bonne voie';
    }

    if (actualProgress >= Math.max(10, expectedProgress - 25)) {
      return 'A surveiller';
    }

    return 'En retard';
  }

  goalHealthTone(goal: NormalizedGoal): GoalHealthTone {
    const label = this.goalHealthLabel(goal);

    switch (label) {
      case 'Atteint':
      case 'En bonne voie':
        return 'success';
      case 'A surveiller':
        return 'warning';
      case 'En retard':
        return 'danger';
      default:
        return 'neutral';
    }
  }

  goalProgressHint(goal: NormalizedGoal): string {
    const label = this.goalHealthLabel(goal);

    if (label === 'Atteint') {
      return 'Votre objectif a deja atteint son niveau cible.';
    }

    if (label === 'En bonne voie') {
      return 'La progression actuelle est coherente avec la date cible.';
    }

    if (label === 'A surveiller') {
      return 'Un petit effort supplementaire peut remettre la trajectoire en confiance.';
    }

    if (label === 'En retard') {
      return 'Le rythme d epargne doit etre renforce pour rester dans les temps.';
    }

    return 'Objectif en cours de suivi.';
  }

  hasAiInsights(goal: NormalizedGoal): boolean {
    return goal.feasibilityScore !== null
      || goal.successProbability !== null
      || !!goal.riskLevel
      || !!goal.predictedDate;
  }

  canAddProgress(goal: NormalizedGoal): boolean {
    return goal.status === 'EN_COURS' && goal.progressPercentage < 100;
  }

  private loadGoals(): void {
    this.loading.set(true);

    this.userService.getMe().subscribe({
      next: (user) => {
        this.goalService.getGoalsByUser(user.id).subscribe({
          next: (response) => {
            this.goals.set(this.sortGoals(normalizeGoalCollection(response)));
            this.loading.set(false);
          },
          error: () => this.loadGoalsFallback()
        });
      },
      error: () => this.loadGoalsFallback()
    });
  }

  private loadGoalsFallback(): void {
    this.goalService.getAll().subscribe({
      next: (response) => {
        this.goals.set(this.sortGoals(normalizeGoalCollection(response)));
        this.loading.set(false);
      },
      error: () => {
        this.loading.set(false);
        this.notifService.error('Impossible de charger les objectifs.');
      }
    });
  }

  private sortGoals(goals: NormalizedGoal[]): NormalizedGoal[] {
    return [...goals].sort((left, right) => {
      const priorityGap = this.goalPriority(left) - this.goalPriority(right);
      if (priorityGap !== 0) {
        return priorityGap;
      }

      const leftDate = this.parseDate(left.targetDate)?.getTime() ?? Number.MAX_SAFE_INTEGER;
      const rightDate = this.parseDate(right.targetDate)?.getTime() ?? Number.MAX_SAFE_INTEGER;
      if (leftDate !== rightDate) {
        return leftDate - rightDate;
      }

      return right.id - left.id;
    });
  }

  private goalPriority(goal: NormalizedGoal): number {
    if (goal.status === 'EN_COURS') {
      return 0;
    }

    if (goal.status === 'ATTEINT') {
      return 1;
    }

    return 2;
  }

  private expectedProgress(goal: NormalizedGoal): number {
    const createdAt = this.parseDate(goal.createdAt);
    const targetDate = this.parseDate(goal.targetDate);

    if (!createdAt || !targetDate) {
      return Math.min(100, Math.max(0, goal.progressPercentage || 0));
    }

    const totalDuration = targetDate.getTime() - createdAt.getTime();
    if (totalDuration <= 0) {
      return 100;
    }

    const elapsedDuration = Date.now() - createdAt.getTime();
    return Math.max(0, Math.min(100, (elapsedDuration / totalDuration) * 100));
  }

  private parseDate(value: string | null | undefined): Date | null {
    if (!value) {
      return null;
    }

    const parsed = new Date(value);
    return Number.isNaN(parsed.getTime()) ? null : parsed;
  }
}
