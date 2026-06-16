import { CommonModule } from '@angular/common';
import { Component, DestroyRef, OnInit, computed, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { ActivatedRoute, Router } from '@angular/router';
import { catchError, finalize, forkJoin, map, of, switchMap } from 'rxjs';
import { EnrichedWithIncomeClassification, IncomeTransactionSnapshot } from '../../core/models';
import { IncomeClassificationService } from '../../core/services/income-classification.service';
import { NotificationService } from '../../core/services/notification.service';
import { CardSummaryHeaderComponent } from './components/card-summary-header.component';
import { CardTransactionsTableComponent } from './components/card-transactions-table.component';
import { CardsService } from './cards.service';
import {
  CardTransaction,
  UserCardDetails,
  formatCardExpiry,
  resolvePrimaryCardLabel,
  resolveCardDisplayName,
  resolveCardStatusLabel
} from './cards.models';

type CardDetailTransaction = EnrichedWithIncomeClassification<CardTransaction>;

@Component({
  selector: 'app-card-detail-page',
  standalone: true,
  imports: [
    CommonModule,
    CardSummaryHeaderComponent,
    CardTransactionsTableComponent
  ],
  templateUrl: './card-detail-page.component.html',
  styleUrl: './card-detail-page.component.scss'
})
export class CardDetailPageComponent implements OnInit {
  private readonly cardsService = inject(CardsService);
  private readonly incomeClassificationService = inject(IncomeClassificationService);
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly destroyRef = inject(DestroyRef);
  private readonly notificationService = inject(NotificationService);

  readonly card = signal<UserCardDetails | null>(null);
  readonly transactions = signal<CardDetailTransaction[]>([]);
  readonly loading = signal(true);
  readonly pageError = signal<string | null>(null);
  readonly transactionsError = signal<string | null>(null);
  readonly deleting = signal(false);
  readonly deleteDialogOpen = signal(false);

  readonly displayName = computed(() => {
    const current = this.card();
    return current ? resolveCardDisplayName(current) : 'Carte';
  });

  ngOnInit(): void {
    this.route.paramMap
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe((params) => {
        const id = Number(params.get('id'));

        if (!id || !Number.isFinite(id)) {
          this.pageError.set('La carte demandee est invalide.');
          this.loading.set(false);
          return;
        }

        this.loadCard(id);
      });
  }

  loadCard(id: number): void {
    this.loading.set(true);
    this.pageError.set(null);
    this.transactionsError.set(null);

    forkJoin({
      card: this.cardsService.getCardDetails(id),
      transactions: this.cardsService.getCardTransactions(id).pipe(
        catchError((error: unknown) => {
          this.transactionsError.set(
            this.extractErrorMessage(error, 'Impossible de charger les transactions de cette carte.')
          );
          return of([] as CardTransaction[]);
        })
      )
    }).pipe(
      switchMap(({ card, transactions }) => {
        const resolvedTransactions = transactions.length ? transactions : card.transactions ?? [];

        return this.incomeClassificationService.enrichCredits(resolvedTransactions, {
          isCredit: (transaction) => this.isCreditTransaction(transaction),
          toSnapshot: (transaction) => this.toIncomeSnapshot(transaction)
        }).pipe(
          map((enrichedTransactions) => ({
            card,
            transactions: enrichedTransactions
          }))
        );
      }),
      finalize(() => this.loading.set(false))
    )
      .subscribe({
        next: ({ card, transactions }) => {
          this.card.set(card);
          this.transactions.set(transactions);

          if (transactions.length) {
            this.transactionsError.set(null);
          }
        },
        error: (error: unknown) => {
          this.card.set(null);
          this.transactions.set([]);
          this.pageError.set(this.extractErrorMessage(error, 'Impossible de charger le detail de cette carte.'));
        }
      });
  }

  goBack(): void {
    void this.router.navigate(['/my-cards']);
  }

  openDeleteDialog(): void {
    if (!this.deleting()) {
      this.deleteDialogOpen.set(true);
    }
  }

  closeDeleteDialog(): void {
    if (!this.deleting()) {
      this.deleteDialogOpen.set(false);
    }
  }

  confirmDelete(): void {
    const current = this.card();

    if (!current || this.deleting()) {
      return;
    }

    this.deleting.set(true);

    this.cardsService.unlinkCard(current.id)
      .pipe(finalize(() => this.deleting.set(false)))
      .subscribe({
        next: () => {
          this.notificationService.success('La carte a ete dissociee avec succes.');
          void this.router.navigate(['/my-cards']);
        },
        error: (error: unknown) => {
          this.notificationService.error(
            this.extractErrorMessage(error, 'Impossible de dissocier cette carte pour le moment.')
          );
        }
      });
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

  formatCardExpiry = formatCardExpiry;
  resolvePrimaryCardLabel = resolvePrimaryCardLabel;
  resolveCardStatusLabel = resolveCardStatusLabel;

  private extractErrorMessage(error: unknown, fallback: string): string {
    if (typeof error === 'object' && error !== null) {
      const source = error as Record<string, unknown>;
      const nested = source['error'];

      if (nested && typeof nested === 'object' && !Array.isArray(nested)) {
        const nestedSource = nested as Record<string, unknown>;
        const message = nestedSource['message'] ?? nestedSource['detail'];
        if (typeof message === 'string' && message.trim()) {
          return message.trim();
        }
      }

      const message = source['message'];
      if (typeof message === 'string' && message.trim()) {
        return message.trim();
      }
    }

    return fallback;
  }

  private isCreditTransaction(transaction: CardTransaction): boolean {
    if (transaction.amount > 0) {
      return true;
    }

    return transaction.amount === 0 && transaction.direction === 'credit';
  }

  private toIncomeSnapshot(transaction: CardTransaction): IncomeTransactionSnapshot {
    const merchantName = transaction.merchantName || 'Credit carte';
    const description = [transaction.merchantName, transaction.category, transaction.channel]
      .filter((value): value is string => Boolean(value && value.trim()))
      .join(' - ');

    return {
      merchantName,
      description: description || merchantName,
      amount: Math.abs(transaction.amount),
      transactionDate: this.toDateOnly(transaction.transactionDate)
    };
  }

  private toDateOnly(value: string): string {
    const isoDate = value.match(/^\d{4}-\d{2}-\d{2}/)?.[0];

    if (isoDate) {
      return isoDate;
    }

    const parsed = new Date(value);

    if (Number.isNaN(parsed.getTime())) {
      return new Date().toISOString().slice(0, 10);
    }

    return parsed.toISOString().slice(0, 10);
  }
}
