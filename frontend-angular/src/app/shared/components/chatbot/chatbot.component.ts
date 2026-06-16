import { CommonModule } from '@angular/common';
import {
  ChangeDetectionStrategy,
  Component,
  DestroyRef,
  ElementRef,
  ViewChild,
  computed,
  inject,
  signal
} from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { FormsModule } from '@angular/forms';
import type {
  ChatAssistantReply,
  ChatConversationMessage,
  ChatMessageSection,
  ChatMessageState,
  ChatMessageTable
} from '../../../core/models';
import { ChatbotService } from '../../../core/services/chatbot.service';

@Component({
  selector: 'app-chatbot',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './chatbot.component.html',
  styleUrls: ['./chatbot.component.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class ChatbotComponent {
  private static readonly MIN_SEND_INTERVAL_MS = 650;
  private static readonly EMPTY_STATE_QUESTIONS = [
    'Combien sont mes revenus ce mois ?',
    'Quel est mon solde net du mois ?',
    'Quelles sont mes plus grosses depenses ?',
    'Quel budget est le plus critique ?',
    'Que dois-je faire ce mois pour ameliorer ma situation ?'
  ];

  private readonly chatbotService = inject(ChatbotService);
  private readonly destroyRef = inject(DestroyRef);
  private readonly timeFormatter = new Intl.DateTimeFormat('fr-FR', {
    hour: '2-digit',
    minute: '2-digit'
  });
  private readonly fullDateFormatter = new Intl.DateTimeFormat('fr-FR', {
    day: '2-digit',
    month: 'short',
    hour: '2-digit',
    minute: '2-digit'
  });

  private lastSubmissionAt = 0;

  @ViewChild('messagesViewport') private readonly messagesViewport?: ElementRef<HTMLDivElement>;
  @ViewChild('composerInput') private readonly composerInput?: ElementRef<HTMLTextAreaElement>;

  readonly isOpen = signal(false);
  readonly draft = signal('');
  readonly messages = signal<ChatConversationMessage[]>([]);
  readonly isLoading = this.chatbotService.isLoading;
  readonly starterQuestions = ChatbotComponent.EMPTY_STATE_QUESTIONS;

  readonly hasConversation = computed(() => this.messages().length > 0);
  readonly canSend = computed(() => this.draft().trim().length > 0 && !this.isLoading());

  constructor() {
    this.chatbotService.openPrompt$
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe((prompt) => {
        this.isOpen.set(true);
        this.draft.set(prompt);
        this.scheduleFocus();
        this.sendMessage(prompt);
      });
  }

  toggle(): void {
    const nextState = !this.isOpen();
    this.isOpen.set(nextState);

    if (nextState) {
      this.scheduleScrollToBottom('auto');
      this.scheduleFocus();
    }
  }

  close(): void {
    this.isOpen.set(false);
  }

  useStarterQuestion(question: string): void {
    if (this.isLoading()) {
      return;
    }

    this.isOpen.set(true);
    this.draft.set(question);
    this.scheduleFocus();
    this.sendMessage(question);
  }

  retryMessage(message: ChatConversationMessage): void {
    if (!message.retryMessage || this.isLoading()) {
      return;
    }

    this.sendMessage(message.retryMessage);
  }

  handleComposerKeydown(event: KeyboardEvent): void {
    if (event.key === 'Enter' && !event.shiftKey) {
      event.preventDefault();
      this.sendMessage();
    }
  }

  handleDraftChange(value: string, textarea: HTMLTextAreaElement): void {
    this.draft.set(value);
    this.resizeComposer(textarea);
  }

  sendMessage(prefilledMessage?: string): void {
    const message = (prefilledMessage ?? this.draft()).trim();
    if (!message || this.isLoading() || this.isSpamAttempt()) {
      return;
    }

    this.lastSubmissionAt = Date.now();
    this.isOpen.set(true);

    this.messages.update((current) => [
      ...current,
      this.createMessage({
        role: 'user',
        state: 'ready',
        text: message,
        timestamp: new Date().toISOString()
      })
    ]);

    const pendingMessage = this.createMessage({
      role: 'assistant',
      state: 'loading',
      text: 'Attijari Compass AI prepare sa reponse...',
      timestamp: new Date().toISOString()
    });

    this.messages.update((current) => [...current, pendingMessage]);
    this.draft.set('');
    this.resetComposerSize();
    this.scheduleScrollToBottom();
    this.scheduleFocus();

    this.chatbotService.sendMessage(message)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (response) => this.replacePendingMessage(pendingMessage.id, response),
        error: (error: unknown) => this.replacePendingWithError(
          pendingMessage.id,
          error instanceof Error ? error.message : null
        )
      });
  }

  formatTimestamp(value: string): string {
    const date = new Date(value);
    if (!Number.isFinite(date.getTime())) {
      return 'Maintenant';
    }

    const now = new Date();
    const sameDay = date.toDateString() === now.toDateString();
    return sameDay
      ? this.timeFormatter.format(date)
      : this.fullDateFormatter.format(date);
  }

  formatRichText(text: string): string {
    if (!text?.trim()) {
      return '';
    }

    let html = this.escapeHtml(this.normalizeDisplayText(text));
    html = html.replace(/\*\*(.+?)\*\*/g, '<strong>$1</strong>');
    html = html.replace(/`([^`]+)`/g, '<code>$1</code>');
    html = html.replace(/\n/g, '<br>');
    return html;
  }

  trackTableRow(index: number): number {
    return index;
  }

  private replacePendingMessage(pendingId: string, response: ChatAssistantReply): void {
    this.messages.update((current) => current.map((message) => {
      if (message.id !== pendingId) {
        return message;
      }

      return this.createMessage({
        id: pendingId,
        role: 'assistant',
        state: 'ready',
        text: response.answer,
        timestamp: response.timestamp,
        retryMessage: null
      });
    }));

    this.scheduleScrollToBottom();
  }

  private replacePendingWithError(pendingId: string, message?: string | null): void {
    const fallbackMessage = 'Le service IA est temporairement indisponible.';
    const errorMessage = message?.trim() || fallbackMessage;

    this.messages.update((current) => current.map((message) => {
      if (message.id !== pendingId) {
        return message;
      }

      return this.createMessage({
        id: pendingId,
        role: 'assistant',
        state: 'error',
        text: errorMessage,
        timestamp: new Date().toISOString(),
        retryMessage: this.findRetryMessage(pendingId)
      });
    }));

    this.scheduleScrollToBottom();
    this.scheduleFocus();
  }

  private createMessage(input: {
    id?: string;
    role: 'user' | 'assistant';
    state: ChatMessageState;
    text: string;
    timestamp: string;
    retryMessage?: string | null;
  }): ChatConversationMessage {
    return {
      id: input.id ?? this.generateId(),
      role: input.role,
      state: input.state,
      text: input.text,
      timestamp: this.normalizeTimestamp(input.timestamp),
      sections: this.buildSections(input.text),
      retryMessage: input.retryMessage ?? null
    };
  }

  private buildSections(text: string): ChatMessageSection[] {
    const normalized = this.normalizeDisplayText(text);
    if (!normalized) {
      return [{ title: null, paragraphs: [], bullets: [], tables: [] }];
    }

    const lines = normalized.split('\n');
    const sections: ChatMessageSection[] = [];
    let currentTitle: string | null = null;
    let currentParagraphLines: string[] = [];
    let currentParagraphs: string[] = [];
    let currentBullets: string[] = [];
    let currentTables: ChatMessageTable[] = [];

    const pushParagraph = (): void => {
      const parsedTableBlock = this.parseWholeTableBlock(currentParagraphLines);
      if (parsedTableBlock) {
        currentTables.push(parsedTableBlock);
        currentParagraphLines = [];
        return;
      }

      const paragraph = currentParagraphLines.join('\n').trim();
      if (paragraph) {
        currentParagraphs.push(paragraph);
      }
      currentParagraphLines = [];
    };

    const pushSection = (): void => {
      pushParagraph();
      if (!currentTitle && currentParagraphs.length === 0 && currentBullets.length === 0 && currentTables.length === 0) {
        return;
      }

      sections.push({
        title: currentTitle,
        paragraphs: [...currentParagraphs],
        bullets: [...currentBullets],
        tables: [...currentTables]
      });

      currentTitle = null;
      currentParagraphs = [];
      currentBullets = [];
      currentTables = [];
    };

    for (let index = 0; index < lines.length; index += 1) {
      const line = lines[index].trim();
      const semanticLine = this.stripMarkdownWrappers(line);

      if (!line) {
        pushParagraph();
        continue;
      }

      const heading = this.extractStructuredHeading(semanticLine);
      if (heading) {
        pushSection();
        currentTitle = heading.title;
        if (heading.remainder) {
          currentParagraphLines.push(heading.remainder);
        }
        continue;
      }

      const parsedTable = this.parseMarkdownTable(lines, index);
      if (parsedTable) {
        pushParagraph();
        currentTables.push(parsedTable.table);
        index = parsedTable.nextIndex;
        continue;
      }

      if (this.isBulletLine(semanticLine)) {
        pushParagraph();
        currentBullets.push(this.cleanBulletLine(semanticLine));
        continue;
      }

      currentParagraphLines.push(semanticLine);
    }

    pushSection();
    return sections.length > 0
      ? sections
      : [{ title: null, paragraphs: [normalized], bullets: [], tables: [] }];
  }

  private parseMarkdownTable(
    lines: string[],
    startIndex: number
  ): { table: ChatMessageTable; nextIndex: number } | null {
    if (startIndex + 1 >= lines.length) {
      return null;
    }

    const headerLine = lines[startIndex].trim();
    const separatorLine = lines[startIndex + 1].trim();
    if (!this.isTableRow(headerLine) || !this.isTableSeparator(separatorLine)) {
      return null;
    }

    const headers = this.parseTableCells(headerLine);
    if (headers.length === 0) {
      return null;
    }

    const rows: string[][] = [];
    let nextIndex = startIndex + 1;

    for (let index = startIndex + 2; index < lines.length; index += 1) {
      const candidate = lines[index].trim();
      if (!candidate) {
        nextIndex = index;
        break;
      }

      if (!this.isTableRow(candidate)) {
        nextIndex = index - 1;
        break;
      }

      rows.push(this.normalizeTableRow(this.parseTableCells(candidate), headers.length));
      nextIndex = index;
    }

    return {
      table: { headers, rows },
      nextIndex
    };
  }

  private extractStructuredHeading(line: string): { title: string; remainder: string } | null {
    const normalizedLine = line.replace(/^\d+\.\s*/, '').trim();
    const colonIndex = normalizedLine.indexOf(':');
    const label = (colonIndex >= 0 ? normalizedLine.slice(0, colonIndex) : normalizedLine).trim();
    const remainder = colonIndex >= 0 ? normalizedLine.slice(colonIndex + 1).trim() : '';

    const explicitTitle = this.formatHeadingTitle(label);
    if (explicitTitle) {
      return {
        title: explicitTitle,
        remainder
      };
    }

    if (colonIndex < 0) {
      return null;
    }

    const candidateTitle = label.trim();
    const wordCount = candidateTitle.split(/\s+/).filter(Boolean).length;
    if (wordCount > 4 || !/^[\p{L}\p{N}' -]{3,40}$/u.test(candidateTitle)) {
      return null;
    }

    return {
      title: this.capitalize(candidateTitle),
      remainder
    };
  }

  private formatHeadingTitle(value: string): string | null {
    const normalized = this.normalizeForMatch(this.stripMarkdownSyntax(value));

    if (normalized === 'constat') {
      return 'Constat';
    }

    if (normalized === 'explication') {
      return 'Explication';
    }

    if (normalized === 'analyse') {
      return 'Analyse';
    }

    if (normalized === 'synthese') {
      return 'Synthese';
    }

    if (/^action\s+recommandee\s+principale$/u.test(normalized) || normalized === 'action') {
      return 'Action recommandee';
    }

    if (/^actions?\s+recommandees?(?:\s+recommandees?)?$/u.test(normalized)) {
      return normalized.startsWith('actions') || normalized.includes('recommandees recommandees')
        ? 'Actions recommandees'
        : 'Action recommandee';
    }

    if (/^priorites?$/u.test(normalized)) {
      return normalized.endsWith('s') ? 'Priorites' : 'Priorite';
    }

    if (/^prochaines?\s+etapes?$/u.test(normalized)) {
      return normalized.startsWith('prochaines') ? 'Prochaines etapes' : 'Prochaine etape';
    }

    return null;
  }

  private isBulletLine(line: string): boolean {
    return /^(?:[-*]|\u2022)\s+/.test(line) || /^\d+\.\s+/.test(line);
  }

  private cleanBulletLine(line: string): string {
    return line
      .replace(/^(?:[-*]|\u2022)\s+/, '')
      .replace(/^\d+\.\s+/, '')
      .trim();
  }

  private isTableRow(line: string): boolean {
    return line.includes('|') && this.parseTableCells(line).length > 1;
  }

  private isTableSeparator(line: string): boolean {
    return /^\|?(\s*:?-{3,}:?\s*\|)+\s*:?-{3,}:?\s*\|?$/.test(line);
  }

  private parseTableCells(line: string): string[] {
    return line
      .split('|')
      .map((cell) => cell.trim())
      .filter((cell, index, array) => cell.length > 0 || (index > 0 && index < array.length - 1));
  }

  private normalizeTableRow(row: string[], columnCount: number): string[] {
    const normalizedRow = row.slice(0, columnCount);

    while (normalizedRow.length < columnCount) {
      normalizedRow.push('');
    }

    return normalizedRow;
  }

  private parseWholeTableBlock(lines: string[]): ChatMessageTable | null {
    if (lines.length < 2) {
      return null;
    }

    const parsed = this.parseMarkdownTable(lines, 0);
    if (!parsed || parsed.nextIndex !== lines.length - 1) {
      return null;
    }

    return parsed.table;
  }

  private stripMarkdownWrappers(value: string): string {
    return value
      .replace(/^\*\*(.+)\*\*$/u, '$1')
      .replace(/^__(.+)__$/u, '$1')
      .trim();
  }

  private stripMarkdownSyntax(value: string): string {
    return value
      .replace(/\*\*/g, '')
      .replace(/__/g, '')
      .replace(/`/g, '')
      .trim();
  }

  private normalizeDisplayText(value: string): string {
    return value
      .replace(/\r\n?/g, '\n')
      .split('\n')
      .map((line) => this.normalizeStructuralLine(line))
      .join('\n')
      .trim();
  }

  private normalizeStructuralLine(value: string): string {
    return value
      .replace(
        /\*\*Action recommand(?:ee|\u00e9e)\*\*s recommand(?:ee|ees|\u00e9e|\u00e9es)s?/giu,
        '**Actions recommandees**'
      )
      .replace(
        /\*\*Action recommand(?:ee|\u00e9e)\*\*\s+principale/giu,
        '**Action recommandee principale**'
      )
      .trim();
  }

  private isSpamAttempt(): boolean {
    return Date.now() - this.lastSubmissionAt < ChatbotComponent.MIN_SEND_INTERVAL_MS;
  }

  private resizeComposer(textarea: HTMLTextAreaElement): void {
    textarea.style.height = '0px';
    textarea.style.height = `${Math.min(textarea.scrollHeight, 192)}px`;
  }

  private resetComposerSize(): void {
    const textarea = this.composerInput?.nativeElement;
    if (!textarea) {
      return;
    }

    textarea.style.height = '0px';
  }

  private scheduleScrollToBottom(behavior: ScrollBehavior = 'smooth'): void {
    setTimeout(() => {
      const viewport = this.messagesViewport?.nativeElement;
      if (!viewport) {
        return;
      }

      viewport.scrollTo({
        top: viewport.scrollHeight,
        behavior
      });
    }, 40);
  }

  private scheduleFocus(): void {
    setTimeout(() => {
      this.composerInput?.nativeElement.focus();
    }, 50);
  }

  private findRetryMessage(pendingId: string): string | null {
    const currentMessages = this.messages();
    const pendingIndex = currentMessages.findIndex((message) => message.id === pendingId);
    if (pendingIndex <= 0) {
      return null;
    }

    for (let index = pendingIndex - 1; index >= 0; index -= 1) {
      const candidate = currentMessages[index];
      if (candidate.role === 'user' && candidate.text.trim()) {
        return candidate.text.trim();
      }
    }

    return null;
  }

  private normalizeTimestamp(value: string): string {
    const parsed = new Date(value);
    return Number.isFinite(parsed.getTime()) ? parsed.toISOString() : new Date().toISOString();
  }

  private normalizeForMatch(value: string): string {
    return value
      .normalize('NFD')
      .replace(/[\u0300-\u036f]/g, '')
      .trim()
      .toLowerCase();
  }

  private capitalize(value: string): string {
    const trimmed = value.trim();
    return trimmed ? `${trimmed.charAt(0).toUpperCase()}${trimmed.slice(1)}` : '';
  }

  private escapeHtml(value: string): string {
    return value
      .replace(/&/g, '&amp;')
      .replace(/</g, '&lt;')
      .replace(/>/g, '&gt;')
      .replace(/"/g, '&quot;')
      .replace(/'/g, '&#39;');
  }

  private generateId(): string {
    return `${Date.now()}-${Math.random().toString(36).slice(2, 9)}`;
  }
}
