import { CommonModule } from '@angular/common';
import { Component, Input } from '@angular/core';
import { CardTransactionDto, UserCardDto } from '../../../../core/models';

@Component({
  selector: 'app-card-transactions',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './card-transactions.component.html',
  styleUrl: './card-transactions.component.css'
})
export class CardTransactionsComponent {
  @Input() card: UserCardDto | null = null;
  @Input() transactions: CardTransactionDto[] = [];
  @Input() loading = false;
  @Input() error: string | null = null;

  get totalAmount(): number {
    return this.transactions.reduce((sum, transaction) => sum + Math.abs(transaction.amount), 0);
  }

  formatMoney(amount: number): string {
    return `${new Intl.NumberFormat('fr-FR', {
      minimumFractionDigits: 0,
      maximumFractionDigits: 0
    }).format(Math.abs(amount))} DT`;
  }

  formatDate(value: string): string {
    return new Intl.DateTimeFormat('fr-FR', {
      day: '2-digit',
      month: 'short',
      year: 'numeric'
    }).format(new Date(value));
  }

  getTransactionTone(transaction: CardTransactionDto): 'credit' | 'debit' {
    const direction = this.getTransactionDirection(transaction);

    if (direction === 'credit') {
      return 'credit';
    }

    if (direction === 'debit') {
      return 'debit';
    }

    return transaction.amount < 0 ? 'debit' : 'credit';
  }

  getAmountPrefix(transaction: CardTransactionDto): string {
    return this.getTransactionTone(transaction) === 'credit' ? '+' : '-';
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
}
