export type CategorizationSource =
  | 'RULE_ENGINE'
  | 'ML_MODEL'
  | 'USER_FEEDBACK'
  | 'ML_LOW_CONFIDENCE'
  | 'FALLBACK';

export interface CategorizationRequest {
  merchantName: string;
  description: string;
}

export interface CategorizationResult {
  category: string;
  confidence: number;
  source: CategorizationSource;
  normalizedText: string;
}
