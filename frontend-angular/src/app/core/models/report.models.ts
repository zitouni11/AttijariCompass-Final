export interface ReportCategory {
  category: string;
  categoryLabel: string;
  icon: string;
  budget: number;
  spent: number;
  usagePercent: number | null;
  remainingAmount: number | null;
  advice: string;
  status: string;
}

export interface ReportCashCategory {
  category: string;
  categoryLabel: string;
  amount: number;
  share: number;
  transactionCount: number;
}

export interface ReportCashBreakdown {
  totalCashExpenses: number;
  shareOfExpenses: number;
  transactionCount: number;
  completedBreakdowns: number;
  pendingBreakdowns: number;
  averageTransactionAmount: number;
  categories: ReportCashCategory[];
}

export interface ReportSummary {
  month: string;
  monthLabel: string;
  income: number;
  expenses: number;
  netBalance: number;
  savingsRate: number;
  trackedTransactions: number;
  alertCount: number;
  categories: ReportCategory[];
  cashBreakdown: ReportCashBreakdown;
}
