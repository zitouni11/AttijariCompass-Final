import { CommonModule } from '@angular/common';
import { HttpErrorResponse } from '@angular/common/http';
import { Component, DestroyRef, OnInit, computed, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { finalize } from 'rxjs';
import {
  CardSyncResponse,
  CardTransactionDto,
  UserCardDto
} from '../../core/models';
import { CardService } from '../../core/services/card.service';
import { NotificationService } from '../../core/services/notification.service';
import { CardTransactionsComponent } from './components/card-transactions/card-transactions.component';
import { DisconnectCardModalComponent } from './components/disconnect-card-modal/disconnect-card-modal.component';

type ActivityTone = 'success' | 'info';
type AlertTone = 'high' | 'medium';
type TimelineTone = 'accent' | 'dark' | 'soft';
const PENDING_SANDBOX_CARDS_STORAGE_KEY = 'attijari-compass.pending-sandbox-cards';
const SANDBOX_CARD_CATALOG_STORAGE_KEY = 'attijari-compass.sandbox-card-catalog';

interface ActivityBanner {
  tone: ActivityTone;
  title: string;
  description: string;
}

interface RecurringMerchantInsight {
  merchant: string;
  count: number;
}

interface MerchantAggregate {
  merchant: string;
  count: number;
  total: number;
  latestDate: string;
}

interface CategoryAggregate {
  category: string;
  count: number;
  total: number;
  recentTotal: number;
  previousTotal: number;
}

interface SpendingSpike {
  date: string;
  total: number;
  count: number;
}

interface TransactionAnalysis {
  totalSpent: number;
  totalCredited: number;
  transactionCount: number;
  dominantCategory: CategoryAggregate | null;
  topMerchant: MerchantAggregate | null;
  recurringMerchants: RecurringMerchantInsight[];
  averageDebit: number;
  highThreshold: number;
  significantPayment: CardTransactionDto | null;
  uniqueHighMerchantPayment: CardTransactionDto | null;
  spendingSpike: SpendingSpike | null;
  categoryAcceleration: CategoryAggregate | null;
  recentSpent: number;
  recentWindowLabel: string;
}

interface CardInsights {
  totalSpent: number;
  totalCredited: number;
  dominantCategory: string;
  topMerchant: string;
  transactionCount: number;
  recurringMerchants: RecurringMerchantInsight[];
  recurringSummary: string;
}

interface AiRecommendation {
  icon: string;
  title: string;
  description: string;
  emphasis: string;
}

interface SmartAlert {
  icon: string;
  tone: AlertTone;
  title: string;
  description: string;
  meta: string;
}

interface TimelineEvent {
  icon: string;
  tone: TimelineTone;
  title: string;
  detail: string;
  meta: string;
}

interface PendingSandboxCard {
  testCardId: number;
  holderName: string;
  maskedCardNumber: string;
  cardType: string;
  bankName: string;
  profile: string | null;
  transactionCount: number | null;
  createdAt: string;
}

interface SandboxCardCatalogEntry {
  testCardId: number;
  expiryMonth: number | null;
  expiryYear: number | null;
}

@Component({
  selector: 'app-card-details',
  standalone: true,
  imports: [
    CommonModule,
    RouterLink,
    CardTransactionsComponent,
    DisconnectCardModalComponent
  ],
  templateUrl: './card-details.component.html',
  styleUrl: './card-details.component.css'
})
export class CardDetailsComponent implements OnInit {
  private readonly cardService = inject(CardService);
  private readonly notificationService = inject(NotificationService);
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly destroyRef = inject(DestroyRef);

  readonly cardId = signal<number | null>(null);
  readonly loading = signal(true);
  readonly pageError = signal<string | null>(null);
  readonly card = signal<UserCardDto | null>(null);

  readonly transactions = signal<CardTransactionDto[]>([]);
  readonly transactionsLoading = signal(false);
  readonly transactionsError = signal<string | null>(null);

  readonly syncing = signal(false);
  readonly disconnecting = signal(false);
  readonly disconnectModalOpen = signal(false);
  readonly activityBanner = signal<ActivityBanner | null>(null);
  readonly sandboxCardCatalog = signal<Record<number, SandboxCardCatalogEntry>>({});

  readonly transactionAnalysis = computed<TransactionAnalysis>(() =>
    this.buildTransactionAnalysis(this.transactions())
  );

  readonly cardInsights = computed<CardInsights>(() => {
    const analysis = this.transactionAnalysis();

    return {
      totalSpent: analysis.totalSpent,
      totalCredited: analysis.totalCredited,
      dominantCategory: analysis.dominantCategory?.category ?? 'Categorie non detectee',
      topMerchant: analysis.topMerchant?.merchant ?? 'Marchand non detecte',
      transactionCount: analysis.transactionCount,
      recurringMerchants: analysis.recurringMerchants,
      recurringSummary: analysis.recurringMerchants.length
        ? `${analysis.recurringMerchants.length} marchand(s) semblent revenir regulierement.`
        : 'Aucune recurrence evidente n est detectee sur les flux affiches.'
    };
  });

  readonly aiRecommendations = computed<AiRecommendation[]>(() => {
    const analysis = this.transactionAnalysis();
    const currentCard = this.card();
    const recommendations: AiRecommendation[] = [];

    if (!analysis.transactionCount) {
      return [{
        icon: 'tips_and_updates',
        title: 'Analyse IA en attente',
        description: 'Synchronisez cette carte ou attendez les premiers flux pour activer les recommandations.',
        emphasis: 'Les conseils apparaissent automatiquement des que les mouvements sont disponibles.'
      }];
    }

    if (analysis.dominantCategory) {
      const potentialSaving = Math.max(0, Math.round(analysis.dominantCategory.total * 0.1));

      recommendations.push({
        icon: 'category',
        title: `Surveiller ${analysis.dominantCategory.category}`,
        description: 'Cette categorie concentre la part la plus importante des depenses recentes de la carte.',
        emphasis: potentialSaving > 0
          ? `Une reduction prudente pourrait liberer environ ${this.formatMoney(potentialSaving)}.`
          : `${analysis.dominantCategory.count} operation(s) sont a suivre dans cette categorie.`
      });
    }

    if (analysis.recurringMerchants.length) {
      const topRecurring = analysis.recurringMerchants[0];

      recommendations.push({
        icon: 'autorenew',
        title: 'Encadrer les paiements recurrents',
        description: `${topRecurring.merchant} revient ${topRecurring.count} fois dans l historique charge.`,
        emphasis: 'Verifier ce flux peut clarifier vos depenses fixes et faciliter les arbitrages.'
      });
    } else {
      recommendations.push({
        icon: 'rule',
        title: 'Maintenir une lecture simple des flux',
        description: 'Aucune recurrence forte ne ressort pour le moment sur les transactions chargees.',
        emphasis: 'Une synchronisation reguliere aidera a detecter les futurs automatismes.'
      });
    }

    if (analysis.totalCredited > 0 && analysis.totalSpent > analysis.totalCredited) {
      recommendations.push({
        icon: 'balance',
        title: 'Reequilibrer le flux de la carte',
        description: 'Les depenses depassent les credits observes sur la periode actuellement chargee.',
        emphasis: 'Prioriser les postes les moins essentiels peut redonner une marge plus confortable.'
      });
    } else if (analysis.spendingSpike) {
      recommendations.push({
        icon: 'calendar_month',
        title: 'Anticiper les jours de pic',
        description: `Un volume eleve apparait le ${this.formatDate(analysis.spendingSpike.date)} sur une courte periode.`,
        emphasis: 'Etaler certaines sorties peut lisser la pression sur votre budget carte.'
      });
    } else {
      recommendations.push({
        icon: 'verified',
        title: 'Conserver le rythme actuel',
        description: 'Les flux de cette carte restent lisibles et relativement stables sur la periode observee.',
        emphasis: currentCard?.lastSyncAt
          ? `Derniere mise a jour le ${this.formatDateTime(currentCard.lastSyncAt)}.`
          : 'Une synchronisation reguliere gardera cette lecture fiable.'
      });
    }

    return recommendations.slice(0, 3);
  });

  readonly smartAlerts = computed<SmartAlert[]>(() => {
    const analysis = this.transactionAnalysis();
    const alerts: SmartAlert[] = [];

    if (analysis.significantPayment) {
      alerts.push({
        icon: 'priority_high',
        tone: 'high',
        title: 'Montant inhabituellemment eleve',
        description: `Un paiement de ${this.formatMoney(Math.abs(analysis.significantPayment.amount))} ressort nettement au-dessus du niveau moyen observe.`,
        meta: `${this.resolveTransactionLabel(analysis.significantPayment)} - ${this.formatDate(analysis.significantPayment.date)}`
      });
    }

    if (analysis.uniqueHighMerchantPayment) {
      alerts.push({
        icon: 'new_releases',
        tone: 'medium',
        title: 'Nouveau marchand a verifier',
        description: 'Un marchand apparait une seule fois avec un montant significatif dans la periode chargee.',
        meta: `${this.resolveTransactionLabel(analysis.uniqueHighMerchantPayment)} - ${this.formatMoney(Math.abs(analysis.uniqueHighMerchantPayment.amount))}`
      });
    }

    if (analysis.spendingSpike) {
      alerts.push({
        icon: 'stacked_line_chart',
        tone: 'medium',
        title: 'Pic de depenses concentre',
        description: `${analysis.spendingSpike.count} operations ont ete regroupees sur une meme journee pour un volume eleve.`,
        meta: `${this.formatDate(analysis.spendingSpike.date)} - ${this.formatMoney(analysis.spendingSpike.total)}`
      });
    }

    if (analysis.categoryAcceleration) {
      alerts.push({
        icon: 'trending_up',
        tone: 'medium',
        title: `Hausse visible sur ${analysis.categoryAcceleration.category}`,
        description: 'La partie recente de l historique semble plus intense que la precedente sur cette categorie.',
        meta: `Recent: ${this.formatMoney(analysis.categoryAcceleration.recentTotal)} - Avant: ${this.formatMoney(analysis.categoryAcceleration.previousTotal)}`
      });
    }

    return alerts.slice(0, 4);
  });

  readonly activityTimeline = computed<TimelineEvent[]>(() => {
    const analysis = this.transactionAnalysis();
    const currentCard = this.card();
    const events: TimelineEvent[] = [];

    if (analysis.significantPayment) {
      events.push({
        icon: 'payments',
        tone: 'accent',
        title: 'Dernier paiement important',
        detail: this.resolveTransactionLabel(analysis.significantPayment),
        meta: `${this.formatMoney(Math.abs(analysis.significantPayment.amount))} - ${this.formatDate(analysis.significantPayment.date)}`
      });
    }

    if (analysis.topMerchant) {
      events.push({
        icon: 'storefront',
        tone: 'dark',
        title: 'Marchand le plus frequent',
        detail: analysis.topMerchant.merchant,
        meta: `${analysis.topMerchant.count} passage(s) sur la periode chargee`
      });
    }

    if (analysis.recurringMerchants.length) {
      const recurring = analysis.recurringMerchants[0];

      events.push({
        icon: 'autorenew',
        tone: 'soft',
        title: 'Paiement recurrent detecte',
        detail: recurring.merchant,
        meta: `${recurring.count} occurrences relevees`
      });
    }

    if (analysis.recentSpent > 0) {
      events.push({
        icon: 'schedule',
        tone: 'accent',
        title: 'Depenses recentes',
        detail: this.formatMoney(analysis.recentSpent),
        meta: analysis.recentWindowLabel
      });
    }

    if (currentCard?.lastSyncAt) {
      events.push({
        icon: 'sync',
        tone: 'dark',
        title: 'Derniere synchronisation',
        detail: this.formatDateTime(currentCard.lastSyncAt),
        meta: 'Mise a jour la plus recente de la carte'
      });
    }

    return events.slice(0, 5);
  });

  ngOnInit(): void {
    this.restoreSandboxCardCatalog();

    this.route.paramMap
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe((params) => {
        const cardId = Number(params.get('cardId'));

        if (!cardId || !Number.isFinite(cardId)) {
          this.cardId.set(null);
          this.card.set(null);
          this.transactions.set([]);
          this.transactionsError.set(null);
          this.pageError.set('La carte demandee est invalide.');
          this.loading.set(false);
          return;
        }

        this.cardId.set(cardId);
        this.loadCardDetails(cardId);
      });
  }

  reloadCard(): void {
    const cardId = this.cardId();
    if (cardId) {
      this.loadCardDetails(cardId);
    }
  }

  goBack(): void {
    void this.router.navigate(['/my-cards']);
  }

  syncCurrentCard(): void {
    const currentCard = this.card();

    if (!currentCard || this.syncing()) {
      return;
    }

    this.syncing.set(true);

    this.cardService.syncCard(currentCard.id)
      .pipe(finalize(() => this.syncing.set(false)))
      .subscribe({
        next: (response) => this.handleSyncSuccess(response, currentCard),
        error: (error) => {
          this.notificationService.error(
            this.extractErrorMessage(error, 'La synchronisation de la carte a echoue.')
          );
        }
      });
  }

  requestDisconnectCard(): void {
    if (!this.disconnecting()) {
      this.disconnectModalOpen.set(true);
    }
  }

  closeDisconnectModal(): void {
    if (!this.disconnecting()) {
      this.disconnectModalOpen.set(false);
    }
  }

  confirmDisconnectCard(): void {
    const currentCard = this.card();

    if (!currentCard || this.disconnecting()) {
      return;
    }

    this.disconnecting.set(true);

    this.cardService.disconnectCard(currentCard.id)
      .pipe(finalize(() => this.disconnecting.set(false)))
      .subscribe({
        next: () => {
          this.rememberDisconnectedSandboxCard(currentCard);
          this.disconnectModalOpen.set(false);
          this.notificationService.success('La carte a ete dissociee de votre espace.');
          void this.router.navigate(['/my-cards']);
        },
        error: (error) => {
          this.notificationService.error(
            this.extractErrorMessage(error, 'Impossible de dissocier cette carte pour le moment.')
          );
        }
      });
  }

  getStatusLabel(card: UserCardDto): string {
    switch ((card.status || '').toUpperCase()) {
      case 'ACTIVE':
        return 'Active';
      case 'PENDING':
        return 'En attente';
      case 'BLOCKED':
        return 'Bloquee';
      case 'INACTIVE':
        return 'Inactive';
      default:
        return card.active ? 'Operationnelle' : 'A verifier';
    }
  }

  getStatusTone(card: UserCardDto): string {
    switch ((card.status || '').toUpperCase()) {
      case 'ACTIVE':
        return 'status-active';
      case 'PENDING':
        return 'status-pending';
      case 'BLOCKED':
      case 'INACTIVE':
        return 'status-muted';
      default:
        return card.active ? 'status-active' : 'status-muted';
    }
  }

  formatDate(value: string | null | undefined): string {
    if (!value) {
      return 'Non disponible';
    }

    return new Intl.DateTimeFormat('fr-FR', {
      day: '2-digit',
      month: 'short',
      year: 'numeric'
    }).format(new Date(value));
  }

  formatDateTime(value: string | null | undefined): string {
    if (!value) {
      return 'En attente';
    }

    return new Intl.DateTimeFormat('fr-FR', {
      day: '2-digit',
      month: 'short',
      year: 'numeric',
      hour: '2-digit',
      minute: '2-digit'
    }).format(new Date(value));
  }

  formatMoney(amount: number): string {
    return `${new Intl.NumberFormat('fr-FR', {
      minimumFractionDigits: 0,
      maximumFractionDigits: 0
    }).format(amount)} DT`;
  }

  getSelectedCardExpiryLabel(card: UserCardDto | null): string {
    if (!card?.linkedTestCardId) {
      return 'Non disponible';
    }

    const entry = this.sandboxCardCatalog()[card.linkedTestCardId];

    if (!entry?.expiryMonth || !entry?.expiryYear) {
      return 'Non disponible';
    }

    return `${String(entry.expiryMonth).padStart(2, '0')} / ${entry.expiryYear}`;
  }

  private loadCardDetails(cardId: number): void {
    this.loading.set(true);
    this.pageError.set(null);
    this.card.set(null);
    this.transactions.set([]);
    this.transactionsError.set(null);

    this.cardService.getMyCards()
      .pipe(finalize(() => this.loading.set(false)))
      .subscribe({
        next: (cards) => {
          const targetCard = cards.find((item) => item.id === cardId) ?? null;

          if (!targetCard) {
            this.pageError.set('Cette carte est introuvable ou n est plus connectee a votre espace.');
            return;
          }

          this.card.set(targetCard);
          this.loadTransactions(targetCard.id);
        },
        error: (error) => {
          this.pageError.set(
            this.extractErrorMessage(error, 'Impossible de charger le detail de cette carte.')
          );
        }
      });
  }

  private loadTransactions(cardId: number): void {
    this.transactionsLoading.set(true);
    this.transactionsError.set(null);
    this.transactions.set([]);

    this.cardService.getCardTransactions(cardId)
      .pipe(finalize(() => this.transactionsLoading.set(false)))
      .subscribe({
        next: (transactions) => {
          this.transactions.set(transactions);
        },
        error: (error) => {
          this.transactionsError.set(
            this.extractErrorMessage(error, 'Impossible de charger les transactions de cette carte.')
          );
        }
      });
  }

  private handleSyncSuccess(response: CardSyncResponse, card: UserCardDto): void {
    const importedTransactions = response.importedTransactions ?? 0;

    this.activityBanner.set({
      tone: 'info',
      title: 'Synchronisation terminee',
      description:
        importedTransactions > 0
          ? `${importedTransactions} nouvelles transactions ont ete importees.`
          : 'La carte est deja a jour. Aucune nouvelle transaction n a ete ajoutee.'
    });

    this.notificationService.info(
      importedTransactions > 0
        ? `${importedTransactions} nouvelles transactions ont ete synchronisees.`
        : 'La carte est deja synchronisee.'
    );

    this.loadCardDetails(card.id);
  }

  private buildTransactionAnalysis(transactions: CardTransactionDto[]): TransactionAnalysis {
    const sortedTransactions = [...transactions].sort(
      (left, right) => new Date(right.date).getTime() - new Date(left.date).getTime()
    );
    const debitTransactions = sortedTransactions.filter((transaction) => !this.isCreditTransaction(transaction));
    const totalCredited = sortedTransactions
      .filter((transaction) => this.isCreditTransaction(transaction))
      .reduce((sum, transaction) => sum + Math.abs(transaction.amount), 0);
    const totalSpent = debitTransactions.reduce((sum, transaction) => sum + Math.abs(transaction.amount), 0);

    const latestTransactionDate = sortedTransactions[0]?.date ?? null;
    const recentCutoff = latestTransactionDate ? new Date(latestTransactionDate) : null;
    if (recentCutoff) {
      recentCutoff.setDate(recentCutoff.getDate() - 6);
    }

    const categoryStats = new Map<string, { total: number; count: number; recentTotal: number; previousTotal: number }>();
    const merchantStats = new Map<string, MerchantAggregate>();
    const dailySpendStats = new Map<string, SpendingSpike>();
    const recentSplitIndex = Math.ceil(debitTransactions.length / 2);

    debitTransactions.forEach((transaction, index) => {
      const amount = Math.abs(transaction.amount);
      const category = this.normalizeLabel(transaction.category) ?? 'Autres depenses';
      const merchant = this.resolveMerchantLabel(transaction);
      const dateKey = transaction.date.slice(0, 10);

      const existingCategory = categoryStats.get(category) ?? {
        total: 0,
        count: 0,
        recentTotal: 0,
        previousTotal: 0
      };

      existingCategory.total += amount;
      existingCategory.count += 1;

      if (index < recentSplitIndex) {
        existingCategory.recentTotal += amount;
      } else {
        existingCategory.previousTotal += amount;
      }

      categoryStats.set(category, existingCategory);

      if (merchant) {
        const existingMerchant = merchantStats.get(merchant) ?? {
          merchant,
          count: 0,
          total: 0,
          latestDate: transaction.date
        };

        existingMerchant.count += 1;
        existingMerchant.total += amount;

        if (new Date(transaction.date).getTime() > new Date(existingMerchant.latestDate).getTime()) {
          existingMerchant.latestDate = transaction.date;
        }

        merchantStats.set(merchant, existingMerchant);
      }

      const existingDaily = dailySpendStats.get(dateKey) ?? {
        date: transaction.date,
        total: 0,
        count: 0
      };

      existingDaily.total += amount;
      existingDaily.count += 1;
      dailySpendStats.set(dateKey, existingDaily);
    });

    const categoryAggregates: CategoryAggregate[] = [...categoryStats.entries()]
      .map(([category, values]) => ({
        category,
        count: values.count,
        total: values.total,
        recentTotal: values.recentTotal,
        previousTotal: values.previousTotal
      }))
      .sort((left, right) => right.total - left.total);

    const merchantAggregates = [...merchantStats.values()]
      .sort((left, right) => right.count - left.count || right.total - left.total);

    const recurringMerchants = merchantAggregates
      .filter((merchant) => merchant.count > 1)
      .slice(0, 3)
      .map((merchant) => ({
        merchant: merchant.merchant,
        count: merchant.count
      }));

    const averageDebit = debitTransactions.length ? totalSpent / debitTransactions.length : 0;
    const debitVariance = debitTransactions.length
      ? debitTransactions.reduce((sum, transaction) => {
          const diff = Math.abs(transaction.amount) - averageDebit;
          return sum + diff * diff;
        }, 0) / debitTransactions.length
      : 0;
    const debitStdDev = Math.sqrt(debitVariance);
    const highThreshold = debitTransactions.length
      ? Math.max(250, averageDebit * 2.1, averageDebit + debitStdDev * 1.2)
      : 250;

    const significantPayment = debitTransactions.find(
      (transaction) => Math.abs(transaction.amount) >= highThreshold
    ) ?? null;

    const uniqueHighMerchantPayment = debitTransactions.find((transaction) => {
      const merchant = this.resolveMerchantLabel(transaction);
      if (!merchant) {
        return false;
      }

      const stats = merchantStats.get(merchant);
      return Boolean(
        stats &&
        stats.count === 1 &&
        Math.abs(transaction.amount) >= Math.max(highThreshold, averageDebit * 1.6, 220)
      );
    }) ?? null;

    const dailySpendAggregates = [...dailySpendStats.values()]
      .sort((left, right) => right.total - left.total);
    const averageDailySpent = dailySpendAggregates.length
      ? totalSpent / dailySpendAggregates.length
      : 0;
    const spendingSpike = dailySpendAggregates.find((day) =>
      day.count >= 2 && day.total >= Math.max(300, averageDailySpent * 1.8)
    ) ?? null;

    const categoryAcceleration = categoryAggregates.find((category) => {
      if (!category.recentTotal) {
        return false;
      }

      if (category.previousTotal > 0) {
        return category.recentTotal >= category.previousTotal * 1.35
          && (category.recentTotal - category.previousTotal) >= Math.max(80, averageDebit * 0.8);
      }

      return category.count >= 2 && category.recentTotal >= Math.max(180, averageDebit * 1.4);
    }) ?? null;

    const recentSpent = recentCutoff
      ? debitTransactions.reduce((sum, transaction) => {
          const transactionDate = new Date(transaction.date);
          return transactionDate >= recentCutoff ? sum + Math.abs(transaction.amount) : sum;
        }, 0)
      : 0;

    return {
      totalSpent,
      totalCredited,
      transactionCount: sortedTransactions.length,
      dominantCategory: categoryAggregates[0] ?? null,
      topMerchant: merchantAggregates[0] ?? null,
      recurringMerchants,
      averageDebit,
      highThreshold,
      significantPayment,
      uniqueHighMerchantPayment,
      spendingSpike,
      categoryAcceleration,
      recentSpent,
      recentWindowLabel: recentCutoff ? '7 derniers jours observes' : 'Periode recente'
    };
  }

  private isCreditTransaction(transaction: CardTransactionDto): boolean {
    const direction = this.getTransactionDirection(transaction);

    if (direction === 'credit') {
      return true;
    }

    if (direction === 'debit') {
      return false;
    }

    return transaction.amount > 0;
  }

  private getTransactionDirection(transaction: CardTransactionDto): 'credit' | 'debit' | null {
    const rawDirection = (transaction.transactionType ?? transaction.type ?? '').trim().toLowerCase();

    if (!rawDirection) {
      return null;
    }

    if (
      rawDirection.includes('credit') ||
      rawDirection.includes('refund') ||
      rawDirection.includes('incoming')
    ) {
      return 'credit';
    }

    if (
      rawDirection.includes('debit') ||
      rawDirection.includes('payment') ||
      rawDirection.includes('purchase') ||
      rawDirection.includes('withdraw') ||
      rawDirection.includes('expense')
    ) {
      return 'debit';
    }

    return null;
  }

  private normalizeLabel(value: string | undefined | null): string | null {
    if (!value?.trim()) {
      return null;
    }

    return value
      .trim()
      .replace(/[_-]+/g, ' ')
      .replace(/\s+/g, ' ')
      .split(' ')
      .filter(Boolean)
      .map((part) => part.charAt(0).toUpperCase() + part.slice(1).toLowerCase())
      .join(' ');
  }

  private resolveMerchantLabel(transaction: CardTransactionDto): string | null {
    const merchant = this.normalizeLabel(transaction.merchantName);

    if (merchant) {
      return merchant;
    }

    const fallback = this.normalizeLabel(transaction.description);

    if (!fallback || fallback.toLowerCase().startsWith('transaction ')) {
      return null;
    }

    return fallback;
  }

  private resolveTransactionLabel(transaction: CardTransactionDto): string {
    return this.resolveMerchantLabel(transaction) ?? this.normalizeLabel(transaction.description) ?? 'Operation';
  }

  private extractErrorMessage(error: unknown, fallback: string): string {
    if (error instanceof HttpErrorResponse) {
      if (error.status === 401) {
        return 'Votre session a expire. Reconnectez-vous pour acceder a vos cartes.';
      }

      if (error.status === 403) {
        return 'Vous n etes pas autorise a acceder a cette fonctionnalite.';
      }

      if (error.status >= 500) {
        return 'Le service cartes est momentanement indisponible. Reessayez dans un instant.';
      }

      const payloadMessage =
        typeof error.error?.message === 'string'
          ? error.error.message
          : typeof error.error?.detail === 'string'
            ? error.error.detail
            : null;

      return payloadMessage ?? fallback;
    }

    return fallback;
  }

  private rememberDisconnectedSandboxCard(card: UserCardDto): void {
    if (!card.linkedTestCardId) {
      return;
    }

    const pendingCards = this.restorePendingSandboxCards();
    const next = [
      {
        testCardId: card.linkedTestCardId,
        holderName: card.holderName,
        maskedCardNumber: card.maskedCardNumber,
        cardType: card.cardType,
        bankName: card.bankName,
        profile: null,
        transactionCount: null,
        createdAt: new Date().toISOString()
      },
      ...pendingCards.filter((item) => item.testCardId !== card.linkedTestCardId)
    ].slice(0, 12);

    this.persistPendingSandboxCards(next);
  }

  private restorePendingSandboxCards(): PendingSandboxCard[] {
    if (typeof localStorage === 'undefined') {
      return [];
    }

    try {
      const raw = localStorage.getItem(PENDING_SANDBOX_CARDS_STORAGE_KEY);
      if (!raw) {
        return [];
      }

      const parsed = JSON.parse(raw);
      if (!Array.isArray(parsed)) {
        return [];
      }

      return parsed
        .map((item) => this.normalizePendingSandboxCard(item))
        .filter((item): item is PendingSandboxCard => item !== null)
        .slice(0, 12);
    } catch {
      return [];
    }
  }

  private persistPendingSandboxCards(cards: PendingSandboxCard[]): void {
    if (typeof localStorage === 'undefined') {
      return;
    }

    try {
      localStorage.setItem(PENDING_SANDBOX_CARDS_STORAGE_KEY, JSON.stringify(cards));
    } catch {
      // Ignore persistence issues and keep the flow available.
    }
  }

  private restoreSandboxCardCatalog(): void {
    if (typeof localStorage === 'undefined') {
      return;
    }

    try {
      const raw = localStorage.getItem(SANDBOX_CARD_CATALOG_STORAGE_KEY);
      if (!raw) {
        return;
      }

      const parsed = JSON.parse(raw);
      if (!parsed || typeof parsed !== 'object' || Array.isArray(parsed)) {
        return;
      }

      const next: Record<number, SandboxCardCatalogEntry> = {};

      for (const [key, value] of Object.entries(parsed)) {
        const normalized = this.normalizeSandboxCardCatalogEntry(key, value);
        if (normalized) {
          next[normalized.testCardId] = normalized;
        }
      }

      this.sandboxCardCatalog.set(next);
    } catch {
      this.sandboxCardCatalog.set({});
    }
  }

  private normalizePendingSandboxCard(value: unknown): PendingSandboxCard | null {
    if (!value || typeof value !== 'object' || Array.isArray(value)) {
      return null;
    }

    const source = value as Record<string, unknown>;
    const testCardId = typeof source['testCardId'] === 'number'
      ? source['testCardId']
      : typeof source['testCardId'] === 'string'
        ? Number(source['testCardId'])
        : null;

    if (!testCardId || !Number.isFinite(testCardId)) {
      return null;
    }

    return {
      testCardId,
      holderName: typeof source['holderName'] === 'string' ? source['holderName'] : 'Titulaire inconnu',
      maskedCardNumber:
        typeof source['maskedCardNumber'] === 'string' ? source['maskedCardNumber'] : '**** **** **** ****',
      cardType: typeof source['cardType'] === 'string' ? source['cardType'] : 'CARTE',
      bankName: typeof source['bankName'] === 'string' ? source['bankName'] : 'Attijari Bank Tunisie',
      profile: typeof source['profile'] === 'string' ? source['profile'] : null,
      transactionCount:
        typeof source['transactionCount'] === 'number'
          ? source['transactionCount']
          : typeof source['transactionCount'] === 'string'
            ? Number(source['transactionCount'])
            : null,
      createdAt:
        typeof source['createdAt'] === 'string' && source['createdAt']
          ? source['createdAt']
          : new Date().toISOString()
    };
  }

  private normalizeSandboxCardCatalogEntry(
    key: string,
    value: unknown
  ): SandboxCardCatalogEntry | null {
    if (!value || typeof value !== 'object' || Array.isArray(value)) {
      return null;
    }

    const source = value as Record<string, unknown>;
    const testCardId = Number(source['testCardId'] ?? key);

    if (!testCardId || !Number.isFinite(testCardId)) {
      return null;
    }

    const expiryMonth =
      typeof source['expiryMonth'] === 'number'
        ? source['expiryMonth']
        : typeof source['expiryMonth'] === 'string'
          ? Number(source['expiryMonth'])
          : null;

    const expiryYear =
      typeof source['expiryYear'] === 'number'
        ? source['expiryYear']
        : typeof source['expiryYear'] === 'string'
          ? Number(source['expiryYear'])
          : null;

    return {
      testCardId,
      expiryMonth: expiryMonth && Number.isFinite(expiryMonth) ? expiryMonth : null,
      expiryYear: expiryYear && Number.isFinite(expiryYear) ? expiryYear : null
    };
  }
}
