export interface ConnectTestCardRequest {
  cardNumber: string;
  holderName: string;
  expiryMonth: number;
  expiryYear: number;
  cvv: string;
}

export type SandboxCardProfile = 'STUDENT' | 'SALARIED' | 'FAMILY' | 'PREMIUM';

export interface GenerateTestCardRequest {
  holderName: string;
  profile: SandboxCardProfile;
  transactionCount: number;
  connectToCurrentUser: boolean;
}

export interface UserCardDto {
  id: number;
  linkedTestCardId: number;
  holderName: string;
  maskedCardNumber: string;
  cardType: string;
  bankName: string;
  status: string;
  connectedAt: string;
  lastSyncAt: string;
  active: boolean;
}

export interface ConnectTestCardResponse {
  message: string;
  card: UserCardDto;
  importedTransactions: number;
  skippedTransactions: number;
  syncedAt: string;
}

export interface GeneratedSandboxCardDto {
  id: number;
  holderName: string;
  maskedCardNumber: string;
  cardType: string;
  bankName: string;
  expiryMonth: number;
  expiryYear: number;
  cvv: string;
  profile: SandboxCardProfile;
  transactionCount: number;
}

export interface GenerateTestCardResponse {
  message: string;
  generatedCard: GeneratedSandboxCardDto;
  connectToCurrentUser: boolean;
  card: UserCardDto | null;
  importedTransactions: number;
  skippedTransactions: number;
  syncedAt: string | null;
}

export interface CardSyncResponse {
  message: string;
  card: UserCardDto;
  importedTransactions: number;
  skippedTransactions: number;
  syncedAt: string;
}

export interface CardTransactionDto {
  id: number | string;
  description: string;
  amount: number;
  date: string;
  currency: string;
  merchantName?: string;
  category?: string;
  transactionType?: string;
  type?: string;
  status?: string;
  paymentMethod?: string;
  source?: string;
  cardLast4?: string;
}
