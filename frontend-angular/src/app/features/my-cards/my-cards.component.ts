import { CommonModule } from '@angular/common';
import { HttpErrorResponse } from '@angular/common/http';
import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { Router } from '@angular/router';
import { finalize } from 'rxjs';
import {
  ConnectTestCardResponse,
  CardSyncResponse,
  CardTransactionDto,
  GenerateTestCardRequest,
  GenerateTestCardResponse,
  GeneratedSandboxCardDto,
  UserCardDto
} from '../../core/models';
import { CardService } from '../../core/services/card.service';
import { NotificationService } from '../../core/services/notification.service';
import { AddTestCardComponent } from './components/add-test-card/add-test-card.component';
import { CardEntryOptionsModalComponent } from './components/card-entry-options-modal/card-entry-options-modal.component';
import {
  ConnectExistingCardModalComponent,
  PendingSandboxCardOption
} from './components/connect-existing-card-modal/connect-existing-card-modal.component';

type ActivityTone = 'success' | 'info';
type AlertTone = 'high' | 'medium';
type TimelineTone = 'accent' | 'dark' | 'soft';
const PENDING_SANDBOX_CARDS_STORAGE_KEY = 'attijari-compass.pending-sandbox-cards';
const SANDBOX_CARD_CATALOG_STORAGE_KEY = 'attijari-compass.sandbox-card-catalog';
const CARD_IMAGE_BY_CODE: Record<string, string> = {
  CARTE_FLEX: 'assets/cards/CARTE_FLEX.png',
  CARTE_PLATINUM: 'assets/cards/CARTE_PLATINUM.png',
  CARTE_GOLD_NATIONALE: 'assets/cards/CARTE_GOLD_NATIONALE.png',
  CARTE_GOLD_INTERNATIONALE: 'assets/cards/CARTE_GOLD_INTERNATIONALE.png',
  CARTE_VISA_NATIONALE: 'assets/cards/CARTE_VISA_NATIONALE.png',
  CARTE_VISA_INTERNATIONALE: 'assets/cards/CARTE_VISA_INTERNATIONALE.png',
  CARTE_CIB: 'assets/cards/CARTE_CIB.png',
  CARTE_TAWA_TAWA: 'assets/cards/CARTE_TAWA_TAWA.png',
  CARTE_IDDIKHAR: 'assets/cards/CARTE_IDDIKHAR.png',
  CARTE_VOYAGE: 'assets/cards/CARTE_VOYAGE.png',
  CARTE_OULIDHA: 'assets/cards/CARTE_OULIDHA.png',
  CARTE_TECHNOLOGIQUE: 'assets/cards/CARTE_TECHNOLOGIQUE.png',
  CARTE_AVENIR: 'assets/cards/CARTE_AVENIR.png'
};

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

interface PendingSandboxCard extends PendingSandboxCardOption {}

interface SandboxCardCatalogEntry {
  testCardId: number;
  expiryMonth: number | null;
  expiryYear: number | null;
}

@Component({
  selector: 'app-my-cards',
  standalone: true,
  imports: [
    CommonModule,
    AddTestCardComponent,
    CardEntryOptionsModalComponent,
    ConnectExistingCardModalComponent
  ],
  templateUrl: './my-cards.component.html',
  styleUrl: './my-cards.component.css'
})
export class MyCardsComponent implements OnInit {
  private readonly cardService = inject(CardService);
  private readonly notificationService = inject(NotificationService);
  private readonly router = inject(Router);

  readonly cards = signal<UserCardDto[]>([]);
  readonly loading = signal(true);
  readonly pageError = signal<string | null>(null);

  readonly selectedCardId = signal<number | null>(null);
  readonly transactions = signal<CardTransactionDto[]>([]);
  readonly transactionsLoading = signal(false);
  readonly transactionsError = signal<string | null>(null);

  readonly addModalOpen = signal(false);
  readonly addCardOptionsOpen = signal(false);
  readonly connectExistingModalOpen = signal(false);
  readonly generating = signal(false);
  readonly generatedCardResult = signal<GenerateTestCardResponse | null>(null);
  readonly connectingGeneratedCard = signal(false);
  readonly generatedCardConnectError = signal<string | null>(null);
  readonly connectingExistingCard = signal(false);
  readonly connectExistingCardError = signal<string | null>(null);
  readonly pendingSandboxCards = signal<PendingSandboxCard[]>([]);
  readonly sandboxCardCatalog = signal<Record<number, SandboxCardCatalogEntry>>({});
  readonly syncingCardId = signal<number | null>(null);
  readonly disconnectingCardId = signal<number | null>(null);
  readonly pendingDisconnectCardId = signal<number | null>(null);
  readonly activityBanner = signal<ActivityBanner | null>(null);

  readonly selectedCard = computed(
    () => this.cards().find((card) => card.id === this.selectedCardId()) ?? null
  );

  readonly pendingDisconnectCard = computed(
    () => this.cards().find((card) => card.id === this.pendingDisconnectCardId()) ?? null
  );

  readonly activeCardsCount = computed(() => this.cards().filter((card) => card.active).length);
  readonly pendingSandboxCardsCount = computed(() => this.pendingSandboxCards().length);

  readonly latestSyncDate = computed(() => {
    const values = this.cards()
      .map((card) => card.lastSyncAt)
      .filter((value): value is string => Boolean(value));

    if (!values.length) {
      return null;
    }

    return values.sort((left, right) => new Date(right).getTime() - new Date(left).getTime())[0];
  });

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
    const selectedCard = this.selectedCard();
    const recommendations: AiRecommendation[] = [];

    if (!analysis.transactionCount) {
      return [{
        icon: 'tips_and_updates',
        title: 'Analyse IA en attente',
        description: 'Synchronisez une carte ou chargez des transactions pour activer les recommandations.',
        emphasis: 'Les conseils apparaissent automatiquement des que les flux sont disponibles.'
      }];
    }

    if (analysis.dominantCategory) {
      const potentialSaving = Math.max(0, Math.round(analysis.dominantCategory.total * 0.1));

      recommendations.push({
        icon: 'category',
        title: `Surveiller ${analysis.dominantCategory.category}`,
        description: 'Cette categorie concentre la part la plus importante des depenses recentes sur la carte.',
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
        emphasis: selectedCard?.lastSyncAt
          ? `Derniere mise a jour le ${this.formatDateTime(selectedCard.lastSyncAt)}.`
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
    const selectedCard = this.selectedCard();
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

    if (selectedCard?.lastSyncAt) {
      events.push({
        icon: 'sync',
        tone: 'dark',
        title: 'Derniere synchronisation',
        detail: this.formatDateTime(selectedCard.lastSyncAt),
        meta: 'Mise a jour la plus recente de la carte'
      });
    }

    return events.slice(0, 5);
  });

  ngOnInit(): void {
    this.restorePendingSandboxCards();
    this.restoreSandboxCardCatalog();
    this.loadCards();
  }

  loadCards(preferredCardId?: number): void {
    this.loading.set(true);
    this.pageError.set(null);

    this.cardService.getMyCards()
      .pipe(finalize(() => this.loading.set(false)))
      .subscribe({
        next: (cards) => {
          const orderedCards = [...cards].sort(
            (left, right) => new Date(right.connectedAt).getTime() - new Date(left.connectedAt).getTime()
          );

          this.cards.set(orderedCards);
          this.selectedCardId.set(preferredCardId ?? null);
          this.transactions.set([]);
          this.transactionsError.set(null);
        },
        error: (error) => {
          this.cards.set([]);
          this.selectedCardId.set(null);
          this.transactions.set([]);
          this.transactionsError.set(null);
          this.pageError.set(
            this.extractErrorMessage(error, 'Impossible de charger vos cartes pour le moment.')
          );
        }
      });
  }

  openCardDetails(card: UserCardDto): void {
    this.selectedCardId.set(card.id);
    void this.router.navigate(['/my-cards', card.id]);
  }

  openAddCardOptions(): void {
    this.addCardOptionsOpen.set(true);
  }

  closeAddCardOptions(): void {
    this.addCardOptionsOpen.set(false);
  }

  openGenerateCardFlow(): void {
    this.closeAddCardOptions();
    this.openAddCardModal();
  }

  openConnectExistingCardFlow(): void {
    this.closeAddCardOptions();
    this.connectExistingCardError.set(null);
    this.connectExistingModalOpen.set(true);
  }

  openAddCardModal(): void {
    this.connectExistingModalOpen.set(false);
    this.generatedCardResult.set(null);
    this.generatedCardConnectError.set(null);
    this.addModalOpen.set(true);
  }

  closeAddCardModal(): void {
    if (!this.generating() && !this.connectingGeneratedCard()) {
      this.clearAddCardModalState();
    }
  }

  resetGeneratedCardResult(): void {
    if (!this.generating() && !this.connectingGeneratedCard()) {
      this.generatedCardResult.set(null);
      this.generatedCardConnectError.set(null);
    }
  }

  closeConnectExistingCardModal(): void {
    if (!this.connectingExistingCard()) {
      this.connectExistingCardError.set(null);
      this.connectExistingModalOpen.set(false);
    }
  }

  generateTestCard(request: GenerateTestCardRequest): void {
    if (this.generating() || this.connectingGeneratedCard()) {
      return;
    }

    this.generatedCardConnectError.set(null);
    this.generating.set(true);

    this.cardService.generateTestCard(request)
      .pipe(finalize(() => this.generating.set(false)))
      .subscribe({
        next: (response) => {
          this.notificationService.success(response.message);
          this.rememberSandboxCardCatalog(response.generatedCard);

          if (response.connectToCurrentUser) {
            this.removePendingSandboxCard(response.generatedCard.id);
            this.activityBanner.set({
              tone: 'success',
              title: 'Carte test generee',
              description:
                response.importedTransactions > 0
                  ? `${response.importedTransactions} transactions ont ete preparees et la carte a ete connectee a votre compte.`
                  : 'La carte generee a ete connectee automatiquement a votre compte.'
            });
            this.clearAddCardModalState();
            this.redirectToCardDetails(response.card?.id);
            return;
          }

          this.rememberGeneratedSandboxCard(response.generatedCard);
          this.generatedCardResult.set(response);
          this.generatedCardConnectError.set(null);
          this.activityBanner.set({
            tone: 'success',
            title: 'Carte sandbox generee',
            description: `${response.generatedCard.transactionCount} transactions ont ete generees pour cette nouvelle carte de test.`
          });
        },
        error: (error) => {
          this.notificationService.error(
            this.extractErrorMessage(error, 'La generation de la carte test a echoue.')
          );
        }
      });
  }

  connectGeneratedCardToAccount(testCardId: number): void {
    if (this.connectingGeneratedCard() || this.generating()) {
      return;
    }

    this.generatedCardConnectError.set(null);
    this.connectingGeneratedCard.set(true);

    this.cardService.connectGeneratedTestCard(testCardId)
      .pipe(finalize(() => this.connectingGeneratedCard.set(false)))
      .subscribe({
        next: (response) => {
          this.handleSandboxCardConnected(testCardId, response, () => this.clearAddCardModalState());
        },
        error: (error) => {
          this.generatedCardConnectError.set(
            this.extractErrorMessage(
              error,
              'Impossible d ajouter cette carte a votre compte pour le moment.'
            )
          );
        }
      });
  }

  connectExistingSandboxCard(testCardId: number): void {
    if (this.connectingExistingCard()) {
      return;
    }

    this.connectExistingCardError.set(null);
    this.connectingExistingCard.set(true);

    this.cardService.connectGeneratedTestCard(testCardId)
      .pipe(finalize(() => this.connectingExistingCard.set(false)))
      .subscribe({
        next: (response) => {
          this.handleSandboxCardConnected(testCardId, response, () => this.closeConnectExistingCardModal());
        },
        error: (error) => {
          this.connectExistingCardError.set(
            this.extractErrorMessage(
              error,
              'Impossible de rattacher cette carte sandbox pour le moment.'
            )
          );
        }
      });
  }

  syncCard(card: UserCardDto): void {
    if (this.isSyncing(card.id)) {
      return;
    }

    this.syncingCardId.set(card.id);

    this.cardService.syncCard(card.id)
      .pipe(finalize(() => this.syncingCardId.set(null)))
      .subscribe({
        next: (response) => this.handleSyncSuccess(response, card),
        error: (error) => {
          this.notificationService.error(
            this.extractErrorMessage(error, 'La synchronisation de la carte a echoue.')
          );
        }
      });
  }

  requestDisconnectCard(card: UserCardDto): void {
    if (!this.isDisconnecting(card.id)) {
      this.pendingDisconnectCardId.set(card.id);
    }
  }

  closeDisconnectModal(): void {
    if (!this.disconnectingCardId()) {
      this.pendingDisconnectCardId.set(null);
    }
  }

  confirmDisconnectCard(): void {
    const card = this.pendingDisconnectCard();

    if (!card || this.isDisconnecting(card.id)) {
      return;
    }

    this.disconnectingCardId.set(card.id);

    this.cardService.disconnectCard(card.id)
      .pipe(finalize(() => this.disconnectingCardId.set(null)))
      .subscribe({
        next: () => {
          const remainingCards = this.cards().filter((item) => item.id !== card.id);

          this.rememberDisconnectedSandboxCard(card);
          this.cards.set(remainingCards);
          this.pendingDisconnectCardId.set(null);
          this.notificationService.success('La carte a ete dissociee de votre espace.');
          this.activityBanner.set({
            tone: 'info',
            title: 'Carte retiree',
            description: 'La carte sandbox a bien ete retiree de votre interface.'
          });

          if (this.selectedCardId() === card.id) {
            const nextCardId = remainingCards[0]?.id ?? null;
            this.selectedCardId.set(nextCardId);

            if (nextCardId) {
              this.loadTransactions(nextCardId);
            } else {
              this.transactions.set([]);
              this.transactionsError.set(null);
            }
          }
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

  getCardImage(card: UserCardDto): string {
    const code = this.resolveCardImageCode(card);
    return CARD_IMAGE_BY_CODE[code] ?? CARD_IMAGE_BY_CODE['CARTE_FLEX'];
  }

  getCardDisplayName(card: UserCardDto): string {
    const source = card as unknown as Record<string, unknown>;
    const name = this.pickText(source, [
      'cardCatalogName',
      'catalogName',
      'cardName',
      'name',
      'label',
      'productName'
    ]);

    if (name) {
      return name;
    }

    const type = `${card.cardType ?? ''}`.trim();
    return type ? this.humanizeCardText(type) : 'Carte bancaire';
  }

  getDisplayCardCode(card: UserCardDto): string {
    return this.resolveCardImageCode(card);
  }

  getMaskedCardNumber(card: UserCardDto): string {
    const digits = `${card.maskedCardNumber ?? ''}`.replace(/\D+/g, '');
    const last4 = digits.length >= 4 ? digits.slice(-4) : '****';
    return `**** **** **** ${last4}`;
  }

  getCardSourceLabel(card: UserCardDto): string {
    const source = card as unknown as Record<string, unknown>;
    return this.pickText(source, ['sourceType', 'source', 'sourceLabel']) ?? card.bankName ?? 'Attijari Bank';
  }

  getCardExpiryLabel(card: UserCardDto): string {
    if (!card.linkedTestCardId) {
      return 'Non disponible';
    }

    const entry = this.sandboxCardCatalog()[card.linkedTestCardId];
    return entry?.expiryMonth && entry.expiryYear
      ? `${String(entry.expiryMonth).padStart(2, '0')} / ${entry.expiryYear}`
      : 'Non disponible';
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

  isSyncing(cardId: number): boolean {
    return this.syncingCardId() === cardId;
  }

  isDisconnecting(cardId: number): boolean {
    return this.disconnectingCardId() === cardId;
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
          this.transactions.set([]);
          this.transactionsError.set(
            this.extractErrorMessage(error, 'Impossible de charger les transactions de cette carte.')
          );
        }
      });
  }

  private clearAddCardModalState(): void {
    this.generatedCardResult.set(null);
    this.generatedCardConnectError.set(null);
    this.addModalOpen.set(false);
  }

  private handleSandboxCardConnected(
    testCardId: number,
    response: ConnectTestCardResponse,
    closeCurrentModal: () => void
  ): void {
    this.removePendingSandboxCard(testCardId);
    closeCurrentModal();
    this.notificationService.success(response.message);
    this.activityBanner.set({
      tone: 'success',
      title: 'Carte ajoutee a votre compte',
      description:
        response.importedTransactions > 0
          ? `${response.importedTransactions} transactions ont ete importees et la carte est prete a etre analysee.`
          : 'La carte generee a ete rattachee a votre espace avec succes.'
    });
    this.redirectToCardDetails(response.card?.id);
  }

  private redirectToCardDetails(cardId?: number | null): void {
    if (cardId) {
      void this.router.navigate(['/my-cards', cardId]);
      return;
    }

    this.loadCards();
  }

  private rememberGeneratedSandboxCard(card: GeneratedSandboxCardDto): void {
    if (!card.id) {
      return;
    }

    this.upsertPendingSandboxCard({
      testCardId: card.id,
      holderName: card.holderName,
      maskedCardNumber: card.maskedCardNumber,
      cardType: card.cardType,
      bankName: card.bankName,
      profile: card.profile,
      transactionCount: card.transactionCount,
      createdAt: new Date().toISOString()
    });
  }

  private rememberDisconnectedSandboxCard(card: UserCardDto): void {
    if (!card.linkedTestCardId) {
      return;
    }

    this.upsertPendingSandboxCard({
      testCardId: card.linkedTestCardId,
      holderName: card.holderName,
      maskedCardNumber: card.maskedCardNumber,
      cardType: card.cardType,
      bankName: card.bankName,
      profile: null,
      transactionCount: null,
      createdAt: new Date().toISOString()
    });
  }

  private upsertPendingSandboxCard(card: PendingSandboxCard): void {
    const next = [
      card,
      ...this.pendingSandboxCards().filter((item) => item.testCardId !== card.testCardId)
    ].slice(0, 12);

    this.pendingSandboxCards.set(next);
    this.persistPendingSandboxCards(next);
  }

  private removePendingSandboxCard(testCardId: number): void {
    if (!testCardId) {
      return;
    }

    const next = this.pendingSandboxCards().filter((item) => item.testCardId !== testCardId);
    this.pendingSandboxCards.set(next);
    this.persistPendingSandboxCards(next);
  }

  private rememberSandboxCardCatalog(card: GeneratedSandboxCardDto): void {
    if (!card.id) {
      return;
    }

    const next = {
      ...this.sandboxCardCatalog(),
      [card.id]: {
        testCardId: card.id,
        expiryMonth: card.expiryMonth ?? null,
        expiryYear: card.expiryYear ?? null
      }
    };

    this.sandboxCardCatalog.set(next);
    this.persistSandboxCardCatalog(next);
  }

  private restorePendingSandboxCards(): void {
    if (typeof localStorage === 'undefined') {
      return;
    }

    try {
      const raw = localStorage.getItem(PENDING_SANDBOX_CARDS_STORAGE_KEY);
      if (!raw) {
        return;
      }

      const parsed = JSON.parse(raw);
      if (!Array.isArray(parsed)) {
        return;
      }

      const cards = parsed
        .map((item) => this.normalizePendingSandboxCard(item))
        .filter((item): item is PendingSandboxCard => item !== null)
        .slice(0, 12);

      this.pendingSandboxCards.set(cards);
    } catch {
      this.pendingSandboxCards.set([]);
    }
  }

  private persistPendingSandboxCards(cards: PendingSandboxCard[]): void {
    if (typeof localStorage === 'undefined') {
      return;
    }

    try {
      localStorage.setItem(PENDING_SANDBOX_CARDS_STORAGE_KEY, JSON.stringify(cards));
    } catch {
      // Ignore persistence issues and keep the in-memory state.
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

  private persistSandboxCardCatalog(catalog: Record<number, SandboxCardCatalogEntry>): void {
    if (typeof localStorage === 'undefined') {
      return;
    }

    try {
      localStorage.setItem(SANDBOX_CARD_CATALOG_STORAGE_KEY, JSON.stringify(catalog));
    } catch {
      // Ignore persistence issues and keep the in-memory state.
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

  private resolveCardImageCode(card: UserCardDto): string {
    const source = card as unknown as Record<string, unknown>;
    const cardCatalog = this.asRecord(source['cardCatalog']);
    const catalog = this.asRecord(source['catalog']);
    const catalogue = this.asRecord(source['catalogue']);
    const candidates = [
      source['cardCatalogCode'],
      source['catalogCode'],
      source['cardTypeCode'],
      source['catalogueCode'],
      cardCatalog['code'],
      cardCatalog['cardCatalogCode'],
      cardCatalog['catalogCode'],
      catalog['code'],
      catalog['cardCatalogCode'],
      catalog['catalogCode'],
      catalogue['code'],
      catalogue['cardCatalogCode'],
      catalogue['catalogCode'],
      source['cardCatalogName'],
      source['catalogName'],
      source['cardName'],
      source['name'],
      source['productName']
    ];

    for (const candidate of candidates) {
      const normalized = this.normalizeCardCode(candidate);

      if (normalized && CARD_IMAGE_BY_CODE[normalized]) {
        return normalized;
      }
    }

    const cardCode = `${source['cardCode'] ?? ''}`.trim().toUpperCase();
    if (cardCode && CARD_IMAGE_BY_CODE[cardCode]) {
      return cardCode;
    }

    return 'CARTE_FLEX';
  }

  private normalizeCardCode(value: unknown): string | null {
    const normalized = `${value ?? ''}`
      .normalize('NFD')
      .replace(/[\u0300-\u036f]/g, '')
      .trim()
      .toUpperCase()
      .replace(/[^A-Z0-9]+/g, '_')
      .replace(/^_+|_+$/g, '');

    if (!normalized) {
      return null;
    }

    if (CARD_IMAGE_BY_CODE[normalized]) {
      return normalized;
    }

    if (normalized.includes('FLEX')) {
      return 'CARTE_FLEX';
    }

    if (normalized.includes('PLATINUM')) {
      return 'CARTE_PLATINUM';
    }

    if (normalized.includes('GOLD') && normalized.includes('INTERNATIONALE')) {
      return 'CARTE_GOLD_INTERNATIONALE';
    }

    if (normalized.includes('GOLD')) {
      return 'CARTE_GOLD_NATIONALE';
    }

    if (normalized.includes('VISA') && normalized.includes('INTERNATIONALE')) {
      return 'CARTE_VISA_INTERNATIONALE';
    }

    if (normalized.includes('VISA')) {
      return 'CARTE_VISA_NATIONALE';
    }

    if (normalized.includes('TAWA')) {
      return 'CARTE_TAWA_TAWA';
    }

    if (normalized.includes('IDDIKHAR')) {
      return 'CARTE_IDDIKHAR';
    }

    if (normalized.includes('VOYAGE')) {
      return 'CARTE_VOYAGE';
    }

    if (normalized.includes('OULIDHA')) {
      return 'CARTE_OULIDHA';
    }

    if (normalized.includes('TECHNOLOGIQUE')) {
      return 'CARTE_TECHNOLOGIQUE';
    }

    if (normalized.includes('AVENIR')) {
      return 'CARTE_AVENIR';
    }

    if (normalized.includes('CIB')) {
      return 'CARTE_CIB';
    }

    return null;
  }

  private pickText(source: Record<string, unknown>, keys: string[]): string | null {
    for (const key of keys) {
      const value = source[key];

      if (typeof value === 'string' && value.trim()) {
        return value.trim();
      }
    }

    return null;
  }

  private asRecord(value: unknown): Record<string, unknown> {
    return value && typeof value === 'object' && !Array.isArray(value)
      ? value as Record<string, unknown>
      : {};
  }

  private humanizeCardText(value: string): string {
    return value
      .replace(/[_-]+/g, ' ')
      .toLowerCase()
      .replace(/\b\w/g, (match) => match.toUpperCase());
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

    this.loadCards(card.id);
  }

  private resolveSelectedCardId(cards: UserCardDto[], preferredCardId?: number): number | null {
    if (!cards.length) {
      return null;
    }

    if (preferredCardId && cards.some((card) => card.id === preferredCardId)) {
      return preferredCardId;
    }

    const currentSelectedId = this.selectedCardId();
    if (currentSelectedId && cards.some((card) => card.id === currentSelectedId)) {
      return currentSelectedId;
    }

    return cards[0].id;
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
}
