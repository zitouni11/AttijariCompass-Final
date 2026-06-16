export type RecommendationPriority = 'LOW' | 'MEDIUM' | 'HIGH' | 'CRITICAL';
export type CurrentMonthSeverity = 'NORMAL' | 'CRITICAL';

export interface RecommendationDto {
  id?: number | string | null;
  title: string;
  category?: string | null;
  type?: string | null;
  source?: string | null;
  sourceType?: string | null;
  module?: string | null;
  engine?: string | null;
  engineType?: string | null;
  recommendationType?: string | null;
  priority: RecommendationPriority;
  message: string;
  suggestedAction: string;
  estimatedMonthlyGain?: number | null;
  targetedTransactionsTotal?: number | null;
  potentialPercent?: number | null;
  explanation: string;
  basedOn: string[];
  raw?: Record<string, unknown> | null;
}

export interface RecommendationSummaryDto {
  globalStatus: string;
  recommendationCount: number;
  potentialMonthlyGain: number;
  aiSummary: string;
  financialScore?: number | null;
  financialScoreLabel?: string | null;
  currentMonthSeverity?: CurrentMonthSeverity | null;
  currentMonthStatusLabel?: string | null;
}

export interface RecommendationResponseDto {
  summary: RecommendationSummaryDto;
  recommendations: RecommendationDto[];
  generatedAt?: string | null;
  hasActiveGoal?: boolean | null;
  priorityGoal?: RecommendationGoalContextDto | null;
  objectiveImpactMonths?: number | null;
  currentGoalDate?: string | null;
  simulatedGoalDate?: string | null;
}

export interface RecommendationGoalContextDto {
  id?: number | string | null;
  title?: string | null;
  targetAmount?: number | null;
  currentAmount?: number | null;
  targetDate?: string | null;
}

export interface RecommendationExplanationRequest {
  title: string;
  category?: string | null;
  type?: string | null;
  amount?: number | null;
  period?: string | null;
  targetedTransactionsTotal?: number | null;
  monthlyImpactEstimated?: number | null;
  potentialPercent?: number | null;
  goalTitle?: string | null;
  financialScore?: number | null;
  globalStatus?: string | null;
  savingsRate?: number | null;
  message?: string | null;
  suggestedAction?: string | null;
}

export interface RecommendationExplanationResponse {
  explanation: string;
  source: 'RAG' | 'FALLBACK' | string;
  fallbackUsed: boolean;
}
