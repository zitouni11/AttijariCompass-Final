export type KnownIncomeFinalType =
  | 'salaire'
  | 'transfer'
  | 'cash_deposit'
  | 'freelance'
  | 'loyer'
  | 'unknown';

export interface IncomeTransactionSnapshot {
  merchantName: string;
  description: string;
  amount: number;
  transactionDate: string;
}

export interface IncomeClassificationRequest {
  currentTransaction: IncomeTransactionSnapshot;
  historicalCredits: IncomeTransactionSnapshot[];
}

export interface IncomeClassificationResult {
  finalType: KnownIncomeFinalType | string;
  finalConfidence: number;
  source: string | null;
  confidence: number;
  reason: string | null;
  explanation: string | null;
  mlPredictedType: string | null;
  mlConfidence: number;
  patternDetected: boolean;
  patternType: string | null;
  patternConfidence: number;
}

export type EnrichedWithIncomeClassification<T> = T & {
  incomeClassification: IncomeClassificationResult | null;
};
