export interface ChatRequestDto {
  message: string;
}

export interface ChatResponseDto {
  answer?: string | null;
  ragContextPreview?: string | null;
  usedModel?: string | null;
  timestamp?: string | null;
}

export type ChatMessageRole = 'user' | 'assistant';
export type ChatMessageState = 'ready' | 'loading' | 'error';

export interface ChatMessageTable {
  headers: string[];
  rows: string[][];
}

export interface ChatMessageSection {
  title?: string | null;
  paragraphs: string[];
  bullets: string[];
  tables: ChatMessageTable[];
}

export interface ChatConversationMessage {
  id: string;
  role: ChatMessageRole;
  state: ChatMessageState;
  text: string;
  timestamp: string;
  sections: ChatMessageSection[];
  usedModel?: string | null;
  retryMessage?: string | null;
}

export interface ChatAssistantReply {
  answer: string;
  ragContextPreview: string | null;
  usedModel: string | null;
  timestamp: string;
}
