import { CommonModule } from '@angular/common';
import { ChangeDetectionStrategy, Component, DestroyRef, OnInit, computed, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { forkJoin } from 'rxjs';
import {
  TransactionCashBreakdownItem,
  TransactionCashBreakdownRequest,
  TransactionCashBreakdownResponse,
  TransactionCategory,
  TransactionResponse,
  normalizeTransactionCategory
} from '../../../core/models';
import { TransactionService } from '../../../core/services/api.services';
import { NotificationService } from '../../../core/services/notification.service';
import { TRANSACTION_CATEGORIES } from '../transaction-ui';

interface CashBreakdownRow {
  uid: string;
  category: TransactionCategory;
  amount: number | null;
  note: string;
}

@Component({
  selector: 'app-cash-breakdown',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink],
  templateUrl: './cash-breakdown.component.html',
  styleUrl: './cash-breakdown.component.css',
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class CashBreakdownComponent implements OnInit {
  private readonly route = inject(ActivatedRoute);
  private readonly transactionService = inject(TransactionService);
  private readonly notificationService = inject(NotificationService);
  private readonly destroyRef = inject(DestroyRef);
  private readonly moneyFormatter = new Intl.NumberFormat('fr-FR', {
    minimumFractionDigits: 0,
    maximumFractionDigits: 2
  });

  readonly categories = TRANSACTION_CATEGORIES;
  readonly loading = signal(true);
  readonly saving = signal(false);
  readonly errorMessage = signal<string | null>(null);
  readonly transaction = signal<TransactionResponse | null>(null);
  readonly breakdown = signal<TransactionCashBreakdownResponse | null>(null);
  readonly rows = signal<CashBreakdownRow[]>([]);

  readonly allocatedAmount = computed(() =>
    this.rows().reduce((total, row) => total + Math.max(row.amount ?? 0, 0), 0)
  );
  readonly transactionAmount = computed(() => this.transaction()?.amount ?? 0);
  readonly remainingAmount = computed(() =>
    Math.max((this.transactionAmount() - this.allocatedAmount()), 0)
  );
  readonly isCashExpense = computed(() => {
    const transaction = this.transaction();
    return transaction?.paymentMethod === 'CASH' && transaction.type === 'DEPENSE';
  });
  readonly isComplete = computed(() => Math.abs(this.remainingAmount()) < 0.01);

  ngOnInit(): void {
    const transactionId = Number(this.route.snapshot.paramMap.get('id'));

    if (!Number.isFinite(transactionId) || transactionId <= 0) {
      this.loading.set(false);
      this.errorMessage.set('Identifiant de transaction invalide.');
      return;
    }

    this.load(transactionId);
  }

  addRow(): void {
    this.rows.update((rows) => [
      ...rows,
      {
        uid: this.buildUid(),
        category: this.categories[0] ?? 'ALIMENTATION',
        amount: null,
        note: ''
      }
    ]);
  }

  removeRow(uid: string): void {
    this.rows.update((rows) => rows.filter((row) => row.uid !== uid));

    if (this.rows().length === 0) {
      this.addRow();
    }
  }

  updateCategory(uid: string, category: string): void {
    this.updateRow(uid, { category: this.normalizeCategory(category) });
  }

  updateAmount(uid: string, amount: string | number): void {
    const numericValue = typeof amount === 'number' ? amount : Number(amount);
    this.updateRow(uid, { amount: Number.isFinite(numericValue) ? numericValue : null });
  }

  updateNote(uid: string, note: string): void {
    this.updateRow(uid, { note });
  }

  save(): void {
    const transaction = this.transaction();

    if (!transaction || !this.isCashExpense()) {
      return;
    }

    const payloadItems = this.rows()
      .filter((row) => row.amount !== null && Number.isFinite(row.amount) && row.amount > 0)
      .map((row) => ({
        category: row.category,
        amount: Number(row.amount),
        note: row.note.trim() || undefined
      }));

    if (!payloadItems.length) {
      this.notificationService.error('Ajoutez au moins une ligne de decomposition.');
      return;
    }

    if (this.allocatedAmount() - this.transactionAmount() > 0.01) {
      this.notificationService.error('Le total detaille ne peut pas depasser le montant de la transaction.');
      return;
    }

    const payload: TransactionCashBreakdownRequest = { items: payloadItems };
    this.saving.set(true);

    this.transactionService.saveCashBreakdown(transaction.id, payload)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (response) => {
          this.saving.set(false);
          this.breakdown.set(response);
          this.rows.set(this.mapRows(response.items));
          this.notificationService.success('Cash breakdown enregistre avec succes.');
        },
        error: (error: unknown) => {
          this.saving.set(false);
          this.notificationService.error(this.extractErrorMessage(error));
        }
      });
  }

  formatMoney(value: number | null | undefined): string {
    return `${this.moneyFormatter.format(value ?? 0)} DT`;
  }

  private load(transactionId: number): void {
    this.loading.set(true);
    this.errorMessage.set(null);

    forkJoin({
      transaction: this.transactionService.getById(transactionId),
      breakdown: this.transactionService.getCashBreakdown(transactionId)
    })
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: ({ transaction, breakdown }) => {
          this.loading.set(false);
          this.transaction.set(transaction);
          this.breakdown.set(breakdown);
          this.rows.set(this.mapRows(breakdown.items));

          if (!breakdown.items.length) {
            this.addRow();
          }
        },
        error: (error: unknown) => {
          this.loading.set(false);
          this.errorMessage.set(this.extractErrorMessage(error));
        }
      });
  }

  private mapRows(items: TransactionCashBreakdownItem[]): CashBreakdownRow[] {
    return items.map((item) => ({
      uid: this.buildUid(),
      category: item.category,
      amount: item.amount,
      note: item.note ?? ''
    }));
  }

  private updateRow(uid: string, patch: Partial<CashBreakdownRow>): void {
    this.rows.update((rows) =>
      rows.map((row) => row.uid === uid ? { ...row, ...patch } : row)
    );
  }

  private normalizeCategory(value: string): TransactionCategory {
    return normalizeTransactionCategory(value);
  }

  private buildUid(): string {
    return `${Date.now()}-${Math.random().toString(16).slice(2)}`;
  }

  private extractErrorMessage(error: unknown): string {
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

    return 'Impossible de charger ou enregistrer la decomposition cash.';
  }
}
