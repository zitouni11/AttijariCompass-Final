import type { TransactionCategory } from './index';

export interface TransactionCashBreakdownItem {
  id?: number;
  category: TransactionCategory;
  categoryLabel?: string;
  amount: number;
  note?: string;
}

export interface TransactionCashBreakdownRequest {
  items: Array<{
    category: TransactionCategory;
    amount: number;
    note?: string;
  }>;
}

export interface TransactionCashBreakdownResponse {
  transactionId: number;
  transactionAmount: number;
  allocatedAmount: number;
  remainingAmount: number;
  complete: boolean;
  items: TransactionCashBreakdownItem[];
}
