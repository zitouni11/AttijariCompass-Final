export type GoalScenarioProfile = 'PRUDENT' | 'EQUILIBRE' | 'AGRESSIF' | string;

export interface GoalBlockingCategoryResponse {
  category?: string;
  name?: string;
  amount?: number;
  monthlyAmount?: number;
  percentage?: number;
  share?: number;
  severity?: string;
  impact?: string;
  message?: string;
}

export interface GoalAnalysisResponse {
  averageMonthlyIncome?: number;
  monthlyIncome?: number;
  averageIncome?: number;
  averageMonthlyExpenses?: number;
  monthlyExpenses?: number;
  averageExpenses?: number;
  estimatedFixedCharges?: number;
  fixedCharges?: number;
  compressibleExpenses?: number;
  discretionaryExpenses?: number;
  prudentSavingsCapacity?: number;
  conservativeSavingsCapacity?: number;
  balancedSavingsCapacity?: number;
  equilibriumSavingsCapacity?: number;
  aggressiveSavingsCapacity?: number;
  blockingCategories?: GoalBlockingCategoryResponse[];
  blockingCategoryResponses?: GoalBlockingCategoryResponse[];
  [key: string]: unknown;
}

export interface GoalPredictionResponse {
  feasibilityScore?: number;
  goalFeasibilityScore?: number;
  successProbability?: number;
  probabilityOfSuccess?: number;
  successRate?: number;
  riskLevel?: string;
  risk?: string;
  predictedDate?: string;
  predictedCompletionDate?: string;
  predictedAchievementDate?: string;
  expectedCompletionDate?: string;
  targetDate?: string;
  requiredMonthlyContribution?: number;
  monthlyContributionRequired?: number;
  recommendedMonthlyContribution?: number;
  [key: string]: unknown;
}

export interface GoalScenarioResponse {
  profile?: GoalScenarioProfile;
  scenario?: GoalScenarioProfile;
  type?: GoalScenarioProfile;
  scenarioName?: GoalScenarioProfile;
  monthlyContribution?: number;
  contribution?: number;
  recommendedMonthlyContribution?: number;
  suggestedMonthlySaving?: number;
  estimatedDate?: string;
  estimatedCompletionDate?: string;
  predictedDate?: string;
  predictedAchievementDate?: string;
  monthsToReachGoal?: number;
  achievableByTargetDate?: boolean;
  shortfallAtTargetDate?: number;
  completionPercentageAtTargetDate?: number;
  successProbability?: number;
  probability?: number;
  score?: number;
  feasibilityScore?: number;
  explanation?: string;
  summary?: string;
  message?: string;
  [key: string]: unknown;
}

export interface GoalSimulationsResponse {
  scenarios?: GoalScenarioResponse[];
  prudent?: GoalScenarioResponse;
  balanced?: GoalScenarioResponse;
  equilibre?: GoalScenarioResponse;
  aggressive?: GoalScenarioResponse;
  agressif?: GoalScenarioResponse;
  [key: string]: unknown;
}

export interface GoalRecommendationResponse {
  title?: string;
  category?: string;
  categoriesToReduce?: GoalBlockingCategoryResponse[];
  blockingCategories?: GoalBlockingCategoryResponse[];
  message?: string;
  advice?: string;
  suggestion?: string;
  explanation?: string;
  impact?: string;
  priority?: string;
  [key: string]: unknown;
}

export interface GoalRecommendationsResponse {
  recommendations?: GoalRecommendationResponse[];
  items?: GoalRecommendationResponse[];
  [key: string]: unknown;
}

export interface GoalStorytellingRequest {
  objective?: string;
  tone?: string;
  [key: string]: unknown;
}

export interface GoalStorytellingResponse {
  title?: string;
  summary?: string;
  narrative?: string;
  story?: string;
  message?: string;
  fallback?: boolean;
  source?: string;
  [key: string]: unknown;
}
