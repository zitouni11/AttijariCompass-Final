import type { TransactionCategory } from './transaction-category';

export type BudgetTargetCategory = TransactionCategory;

export type BudgetTargetLevel = 'PRUDENT' | 'EQUILIBRE' | 'RENFORCE';
export type BudgetTargetSource = 'RECOMMENDATION_AI' | 'MANUAL';
export type BudgetTargetStatus = 'ACTIVE' | 'INACTIVE' | 'ARCHIVED';
export type BudgetTargetMonitoringStatus = 'SOUS_CONTROLE' | 'A_SURVEILLER' | 'DEPASSE';
export type BudgetAlertType =
  | 'BUDGET_DEPASSE'
  | 'BUDGET_QUASI_ATTEINT'
  | 'BUDGET_RESTE_FAIBLE'
  | 'BUDGET_SOUS_CONTROLE'
  | 'BUDGET_CRITIQUE_PRIORITAIRE'
  | 'BUDGET_MAITRISE_GLOBALE';
export type BudgetAlertSeverity = 'CRITICAL' | 'WARNING' | 'INFO';

export interface BudgetTargetCreateRequest {
  category: BudgetTargetCategory;
  categoryLabel: string;
  selectedLevel: BudgetTargetLevel;
  suggestedMonthlyAmount: number;
  source: BudgetTargetSource;
  recommendationId: string;
  recommendationTitle: string;
  summary: string;
}

export interface BudgetTargetStatusUpdateRequest {
  status: BudgetTargetStatus;
}

export interface BudgetTargetResponse {
  id: number;
  category: BudgetTargetCategory;
  categoryLabel: string;
  selectedLevel: BudgetTargetLevel;
  suggestedMonthlyAmount: number;
  targetAmount: number | null;
  spentThisMonth: number | null;
  remainingAmount: number | null;
  usagePercent: number | null;
  monitoringStatus: BudgetTargetMonitoringStatus | null;
  monitoringStatusLabel: string | null;
  source: BudgetTargetSource;
  recommendationId: string | null;
  recommendationTitle: string | null;
  summary: string | null;
  status: BudgetTargetStatus;
  createdAt: string;
  updatedAt: string;
  sourceLabel: string | null;
  selectedLevelLabel: string | null;
  selectedLevelSummary: string | null;
  statusLabel: string | null;
  aiGenerated: boolean;
}

export interface BudgetAlertResponse {
  alertType: BudgetAlertType;
  severity: BudgetAlertSeverity;
  budgetTargetId: number | null;
  category: BudgetTargetCategory | null;
  categoryLabel: string | null;
  title: string;
  message: string;
  usagePercent: number | null;
  targetAmount: number | null;
  spentThisMonth: number | null;
  remainingAmount: number | null;
  generatedAt: string | null;
  priorityRank: number;
}
