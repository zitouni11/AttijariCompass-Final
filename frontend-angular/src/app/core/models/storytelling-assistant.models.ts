export type StorytellingAssistantState = 'idle' | 'listening' | 'thinking' | 'speaking';

export type StorytellingMessageRole = 'assistant' | 'user';

export type StorytellingLocale = 'fr-FR' | 'en-US' | 'ar-SA';

export type StorytellingTextDirection = 'ltr' | 'rtl';

export interface StorytellingAssistantMessage {
  id: string;
  role: StorytellingMessageRole;
  text: string;
  createdAt: Date;
  language: StorytellingLocale;
  direction: StorytellingTextDirection;
  emotion?: string | null;
  action?: string | null;
  intent?: string | null;
}

export interface ConversationMessage {
  role: StorytellingMessageRole;
  text: string;
}

export interface StorytellingFinancialContext {
  salary?: number;
  accountBalance?: number;
  savingsBalance?: number;
  currency?: string;
  balance?: number;
  monthlyIncome?: number;
  monthlyExpenses?: number;
  savingsGoal?: number;
  topCategories?: string[];
  metadata?: Record<string, unknown>;
  [key: string]: unknown;
}

export interface StorytellingRequest {
  message: string;
  userObjective?: string | null;
  conversationHistory: ConversationMessage[];
  financialContext?: StorytellingFinancialContext | null;
}

export interface StorytellingResponse {
  reply: string;
  emotion?: string | null;
  action?: string | null;
  intent?: string | null;
}

export type StorytellingConversationHistoryItem = ConversationMessage;
export type StorytellingChatRequest = StorytellingRequest;
export type StorytellingChatResponse = StorytellingResponse;
