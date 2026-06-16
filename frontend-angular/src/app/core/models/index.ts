import type { CategorizationSource } from './categorization.models';
import type { TransactionCategory } from './transaction-category';

// ==================== AUTH MODELS ====================
export interface LoginRequest {
  email: string;
  password: string;
}

export interface RegisterRequest {
  email: string;
  password: string;
  role: 'USER';
}

export interface AdminRegistrationRequest {
  fullName: string;
  email: string;
  password: string;
  confirmPassword: string;
}

export interface AdminRegistrationVerifyRequest {
  email: string;
  code: string;
}

export interface RefreshTokenRequest {
  refreshToken: string;
}

export interface AuthResponse {
  accessToken?: string;
  token: string;
  refreshToken: string;
  email: string;
  role: string;
  fullName?: string;
}

export interface AuthMessageResponse {
  message: string;
}

export interface VerifyEmailRequest {
  email: string;
  code: string;
}

export interface ResendVerificationCodeRequest {
  email: string;
}

export interface AccountRestoreRequest {
  email: string;
}

export interface AccountRestoreVerifyRequest {
  email: string;
  code: string;
}

// ==================== USER MODELS ====================
export interface UserResponse {
  id: number;
  fullName?: string;
  email: string;
  role: string;
  active?: boolean;
  createdAt: string;
  lastLoginAt?: string;
  profilePictureUrl?: string | null;
}

export interface UserRequest {
  email: string;
  password?: string | null;
}

// ==================== TRANSACTION MODELS ====================
export type TransactionType = 'DEPENSE' | 'REVENU';
export type PaymentMethod = 'CARD' | 'BANK_TRANSFER' | 'CASH' | 'DIGITAL_WALLET';
export type TransactionSource =
  | 'BANK_API'
  | 'CARD_SYNC'
  | 'CARD_SANDBOX'
  | 'MANUAL_CARD'
  | 'MANUAL_ENTRY'
  | 'IMPORTED_FILE'
  | 'TEST_CARD';

export interface TransactionResponse {
  id: number;
  description: string;
  amount: number;
  date: string;
  category: TransactionCategory;
  type: TransactionType;
  userId: number;
  merchantName?: string;
  paymentMethod: PaymentMethod;
  userCardId?: number;
  source: TransactionSource;
  cardLast4?: string;
  categorizationSource?: CategorizationSource;
  categorizationConfidence?: number | null;
  normalizedText?: string;
  createdAt: string;
}

export interface TransactionRequest {
  description: string;
  amount: number;
  date: string;
  category: TransactionCategory;
  predictedCategory?: TransactionCategory;
  type: TransactionType;
  merchantName?: string;
  paymentMethod?: PaymentMethod;
  userCardId?: number;
  source?: TransactionSource;
  categorizationSource?: CategorizationSource;
  categorizationConfidence?: number | null;
  normalizedText?: string;
}

export interface CardPaymentRequest {
  merchantName: string;
  amount: number;
  date: string;
  description?: string;
  cardLast4: string;
  mcc?: string;
}

export interface UpdateCategoryRequest {
  category: TransactionCategory;
}

export interface PaginatedResponse<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  pageNumber?: number;
  number?: number;
  size?: number;
  numberOfElements?: number;
}

export interface ImportTransactionsSummary {
  categorizedCount: number;
  expenseCount: number;
  incomeCount: number;
  totalExpenses: number;
  totalIncome: number;
  netAmount: number;
}

export interface ImportTransactionError {
  rowNumber: number;
  message: string;
}

export interface ImportTransactionsResponse {
  totalProcessed: number;
  successCount: number;
  importedCount: number;
  errorCount: number;
  errors: ImportTransactionError[];
  message: string;
  transactions: TransactionResponse[];
  summary?: ImportTransactionsSummary;
}

// ==================== GOAL MODELS ====================
export type GoalStatus = 'EN_COURS' | 'ATTEINT' | 'ABANDONNE';

export interface GoalResponse {
  id: number;
  name?: string;
  title?: string;
  description?: string;
  targetAmount: number;
  currentAmount: number;
  targetDate: string;
  type?: string;
  status: GoalStatus;
  createdAt: string;
  progressPercentage: number;
  remainingAmount: number;
  monthlySavingsRequired: number;
  feasibilityScore?: number | null;
  successProbability?: number | null;
  riskLevel?: string;
  predictedDate?: string;
}

export interface GoalRequest {
  name: string;
  title?: string;
  description?: string;
  targetAmount: number;
  currentAmount?: number;
  targetDate: string;
  type?: string;
}

// ==================== RECOMMENDATION MODELS ====================
export * from './recommendation.models';
export * from './card.models';

// ==================== SIMULATION MODELS ====================
export interface SavingsSimulationRequest {
  montantEpargne: number;
  objectifMontant: number;
  revenuMensuel?: number;
}

export interface SavingsSimulationResponse {
  montantEpargne: number;
  objectifMontant: number;
  nombreMois: number;
  totalEpargne: number;
  message: string;
}

export interface CreditSimulationRequest {
  montantCredit: number;
  tauxInteret: number;
  dureeEnMois: number;
  revenuMensuel: number;
}

export interface CreditSimulationResponse {
  montantCredit: number;
  mensualite: number;
  coutTotal: number;
  tauxEndettement: number;
  resteAVivre: number;
  scoreRisque: string;
  message: string;
}

// ==================== STORYTELLING MODELS ====================
export interface MonthlyStoryResponse {
  mois: string;
  resume: string;
  categoriesPrincipales: string;
  totalDepenses: number;
  totalRevenus: number;
  epargneRealisee: number;
  alertes: string[];
  missions: string[];
}

// ==================== SHARED ====================
export interface ApiError {
  message: string;
  status: number;
  timestamp?: string;
}

export interface Notification {
  id: string;
  type: 'success' | 'error' | 'warning' | 'info';
  message: string;
  duration?: number;
}

export * from './categorization.models';
export * from './goal-ai.models';
export * from './income-classification.models';
export * from './budget-target.models';
export * from './report.models';
export * from './cash-breakdown.models';
export * from './transaction-category';
export * from './chat.models';
export * from './app-settings.models';
