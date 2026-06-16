import { CommonModule } from '@angular/common';
import { Component, Input } from '@angular/core';
import { EnrichedWithIncomeClassification } from '../../../core/models';
import { IncomeClassificationDisplayComponent } from '../../../shared/components/income-classification-display/income-classification-display.component';
import { CardTransaction } from '../cards.models';

type CardTransactionListItem = EnrichedWithIncomeClassification<CardTransaction>;

@Component({
  selector: 'app-card-transactions-table',
  standalone: true,
  imports: [CommonModule, IncomeClassificationDisplayComponent],
  templateUrl: './card-transactions-table.component.html',
  styleUrl: './card-transactions-table.component.scss'
})
export class CardTransactionsTableComponent {
  @Input() transactions: CardTransactionListItem[] = [];
  @Input() loading = false;
  @Input() errorMessage: string | null = null;

  readonly skeletonRows = Array.from({ length: 5 });

  formatMoney(amount: number): string {
    const abs = Math.abs(amount);
    const [integerPart, decimalPart] = abs.toFixed(3).split('.');
    const groupedInteger = Number(integerPart).toLocaleString('fr-FR');

    return `${groupedInteger}.${decimalPart} DT`;
  }

  formatDate(value: string): string {
    return new Intl.DateTimeFormat('fr-FR', {
      day: '2-digit',
      month: 'short',
      year: 'numeric'
    }).format(new Date(value));
  }

  amountPrefix(transaction: CardTransaction): string {
    if (this.isCredit(transaction)) {
      return '+';
    }

    if (this.isDebit(transaction)) {
      return '-';
    }

    return '';
  }

  amountDisplay(transaction: CardTransaction): string {
    return `${this.amountPrefix(transaction)}${this.formatMoney(transaction.amount)}`;
  }

  amountBadgeLabel(transaction: CardTransaction): string {
    if (this.isCredit(transaction)) {
      return 'Credit';
    }

    if (this.isDebit(transaction)) {
      return 'Debit';
    }

    return 'Flux';
  }

  merchantLabel(transaction: CardTransaction): string {
    return transaction.merchantName || 'Operation carte';
  }

  isCredit(transaction: CardTransaction): boolean {
    if (transaction.amount > 0) {
      return true;
    }

    return transaction.amount === 0 && transaction.direction === 'credit';
  }

  isDebit(transaction: CardTransaction): boolean {
    if (transaction.amount < 0) {
      return true;
    }

    return transaction.amount === 0 && transaction.direction === 'debit';
  }
}
