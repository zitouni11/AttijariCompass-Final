export interface DashboardCategorySummary {
  category: string;
  amount: number;
  share: number;
}

export interface DashboardFinancialHealth {
  score: number | null;
  status: string | null;
  label: string | null;
  message: string | null;
}

export interface DashboardSummary {
  month: string;
  monthLabel: string;
  income: number;
  expenses: number;
  netBalance: number;
  savingsRate: number;
  trackedTransactions: number;
  expenseByCategory: DashboardCategorySummary[];
  financialHealth: DashboardFinancialHealth;
  currency: 'DT';
  hasData: boolean;
}

export interface DashboardCategoryApiItem {
  category?: string;
  name?: string;
  label?: string;
  amount?: number;
  total?: number;
  value?: number;
}

export interface DashboardFinancialHealthApi {
  score?: number;
  status?: string;
  label?: string;
  message?: string;
  insight?: string;
  summary?: string;
}

export interface DashboardSummaryApiResponse {
  month?: string;
  monthLabel?: string;
  currentMonth?: string;
  monthKey?: string;
  period?: string;
  moisCourant?: string;
  income?: number;
  totalIncome?: number;
  totalRevenu?: number;
  revenue?: number;
  expenses?: number;
  totalExpenses?: number;
  totalDepenses?: number;
  spending?: number;
  netBalance?: number;
  balance?: number;
  net?: number;
  soldeActuel?: number;
  savingsRate?: number;
  savingsRatePercent?: number;
  tauxEpargne?: number;
  trackedTransactions?: number;
  transactionCount?: number;
  totalTransactions?: number;
  nombreTransactions?: number;
  expenseByCategory?: Record<string, number> | DashboardCategoryApiItem[];
  depensesParCategorie?: Record<string, number>;
  financialHealth?: DashboardFinancialHealthApi;
  health?: DashboardFinancialHealthApi;
  financialHealthScore?: number;
  currency?: string;
  hasData?: boolean;
}
