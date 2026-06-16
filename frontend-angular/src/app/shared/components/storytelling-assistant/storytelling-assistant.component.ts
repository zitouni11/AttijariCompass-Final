import { CommonModule, DOCUMENT } from '@angular/common';
import { HttpErrorResponse } from '@angular/common/http';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import {
  Component,
  DestroyRef,
  ElementRef,
  EventEmitter,
  HostListener,
  Input,
  OnDestroy,
  Output,
  ViewChild,
  computed,
  effect,
  inject,
  signal
} from '@angular/core';
import { FormsModule } from '@angular/forms';
import { finalize } from 'rxjs';
import { NotificationService } from '../../../core/services/notification.service';
import { StorytellingService } from '../../../core/services/storytelling.service';
import {
  ConversationMessage,
  StorytellingAssistantMessage,
  StorytellingAssistantState,
  StorytellingFinancialContext,
  StorytellingLocale,
  StorytellingRequest,
  StorytellingResponse,
  StorytellingTextDirection
} from '../../../core/models/storytelling-assistant.models';

interface SpeechRecognitionResultLike {
  transcript: string;
}

interface SpeechRecognitionAlternativeLike {
  0: SpeechRecognitionResultLike;
  length: number;
}

interface SpeechRecognitionEventLike {
  results: ArrayLike<SpeechRecognitionAlternativeLike>;
}

interface SpeechRecognitionLike {
  continuous: boolean;
  interimResults: boolean;
  lang: string;
  maxAlternatives: number;
  onend: (() => void) | null;
  onerror: ((event: { error?: string }) => void) | null;
  onresult: ((event: SpeechRecognitionEventLike) => void) | null;
  onstart: (() => void) | null;
  abort(): void;
  start(): void;
  stop(): void;
}

interface SpeechRecognitionConstructor {
  new (): SpeechRecognitionLike;
}

interface ContextPreviewItem {
  key: string;
  value: string;
}

@Component({
  selector: 'app-storytelling-assistant',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './storytelling-assistant.component.html',
  styleUrls: ['./storytelling-assistant.component.css']
})
export class StorytellingAssistantComponent implements OnDestroy {
  private readonly document = inject(DOCUMENT);
  private readonly destroyRef = inject(DestroyRef);
  private readonly notificationService = inject(NotificationService);
  private readonly storytellingService = inject(StorytellingService);

  @ViewChild('messageInput') private readonly messageInput?: ElementRef<HTMLTextAreaElement>;
  @ViewChild('messagesViewport') private readonly messagesViewport?: ElementRef<HTMLDivElement>;

  @Output() closed = new EventEmitter<void>();

  @Input() set initialUserObjective(value: string | null | undefined) {
    this.userObjective.set(this.sanitizeUserObjective(value) ?? '');
  }

  @Input() set assistantFinancialContext(value: StorytellingFinancialContext | null | undefined) {
    this.financialContext.set(value ?? null);
  }

  @Input() set open(value: boolean) {
    if (value === this.isOpen()) {
      return;
    }

    this.isOpen.set(value);

    if (value) {
      this.handleOpen();
      return;
    }

    this.handleCloseState();
  }

  readonly draft = signal('');
  readonly userObjective = signal('');
  readonly financialContext = signal<StorytellingFinancialContext | null>(null);
  readonly messages = signal<StorytellingAssistantMessage[]>([]);
  readonly assistantState = signal<StorytellingAssistantState>('idle');
  readonly eyesClosed = signal(false);
  readonly isOpen = signal(false);
  readonly openImageSrc = signal('assets/agent_open.png');
  readonly closedImageSrc = signal('assets/agent_closed.png');
  readonly activeLocale = signal<StorytellingLocale>('fr-FR');
  readonly lastEmotion = signal<string | null>(null);
  readonly lastAction = signal<string | null>(null);
  readonly lastIntent = signal<string | null>(null);

  readonly canUseSpeechRecognition = signal(false);
  readonly currentCharacterImage = computed(() => this.eyesClosed() ? this.closedImageSrc() : this.openImageSrc());
  readonly isListening = computed(() => this.assistantState() === 'listening');
  readonly isThinking = computed(() => this.assistantState() === 'thinking');
  readonly isSpeaking = computed(() => this.assistantState() === 'speaking');
  readonly composerDirection = computed<StorytellingTextDirection>(() => {
    const sample = this.draft().trim() || this.userObjective().trim();
    return this.getTextDirection(this.detectLocale(sample || this.activeLocale()));
  });
  readonly stateLabel = computed(() => {
    switch (this.assistantState()) {
      case 'listening':
        return 'A l\'ecoute';
      case 'thinking':
        return 'Analyse en cours';
      case 'speaking':
        return 'Reponse vocale';
      default:
        return 'Disponible';
    }
  });
  readonly financialContextPreview = computed<ContextPreviewItem[]>(() => {
    const context = this.financialContext();
    if (!context) {
      return [];
    }

    return Object.entries(context)
      .filter(([, value]) => value !== null && value !== undefined && value !== '')
      .slice(0, 6)
      .map(([key, value]) => ({
        key: this.humanizeKey(key),
        value: this.stringifyContextValue(value)
      }));
  });

  private blinkTimeoutId: number | null = null;
  private bodyOverflowBeforeOpen = '';
  private recognition: SpeechRecognitionLike | null = null;
  private speechSynthesisRef: SpeechSynthesis | null = null;
  private availableVoices: SpeechSynthesisVoice[] = [];

  constructor() {
    this.initializeSpeechSynthesis();
    this.initializeSpeechRecognition();

    effect(() => {
      if (!this.isOpen()) {
        return;
      }

      this.messages();
      this.scheduleScrollAfterRender();
    });
  }

  ngOnDestroy(): void {
    this.clearBlinkTimer();
    this.stopListening(true);
    this.stopSpeaking();
    this.document.body.style.overflow = this.bodyOverflowBeforeOpen;
  }

  @HostListener('document:keydown.escape')
  handleEscape(): void {
    if (this.isOpen()) {
      this.requestClose();
    }
  }

  closeFromBackdrop(event: MouseEvent): void {
    if (event.target === event.currentTarget) {
      this.requestClose();
    }
  }

  requestClose(): void {
    this.stopListening();
    this.stopSpeaking();
    this.assistantState.set('idle');
    this.closed.emit();
  }

  sendMessage(): void {
    const text = this.draft().trim();
    if (!text || this.isThinking()) {
      return;
    }

    const language = this.detectLocale(text);
    this.activeLocale.set(language);
    this.pushMessage({
      role: 'user',
      text,
      language,
      direction: this.getTextDirection(language)
    });

    this.draft.set('');
    this.stopListening();
    this.stopSpeaking();
    this.assistantState.set('thinking');

    const payload = this.buildStorytellingPayload(text);
    console.log('Storytelling payload sent:', JSON.stringify(payload, null, 2));

    this.storytellingService.chat(payload)
      .pipe(
        takeUntilDestroyed(this.destroyRef),
        finalize(() => {
          if (this.assistantState() === 'thinking') {
            this.assistantState.set('idle');
          }
        })
      )
      .subscribe({
        next: (response) => this.handleAssistantResponse(response),
        error: (err: HttpErrorResponse) => {
          const fallback =
            'Le service storytelling est temporairement indisponible. Vous pouvez reessayer dans quelques instants.';
          this.pushMessage({
            role: 'assistant',
            text: fallback,
            language: this.activeLocale(),
            direction: this.getTextDirection(this.activeLocale()),
            emotion: 'neutral',
            action: 'wait',
            intent: 'fallback'
          });
          this.assistantState.set('idle');
          this.notificationService.warning(`Backend storytelling indisponible (${err.status || 'n/a'}).`);
        }
      });
  }

  handleComposerKeydown(event: KeyboardEvent): void {
    if (event.key === 'Enter' && !event.shiftKey) {
      event.preventDefault();
      this.sendMessage();
    }
  }

  toggleListening(): void {
    if (!this.canUseSpeechRecognition()) {
      this.notificationService.info('La reconnaissance vocale n\'est pas disponible sur ce navigateur.');
      return;
    }

    if (this.isThinking()) {
      return;
    }

    if (this.isListening()) {
      this.stopListening();
      return;
    }

    this.stopSpeaking();

    if (this.recognition) {
      this.recognition.lang = this.resolveRecognitionLocale();
    }

    try {
      this.recognition?.start();
    } catch {
      this.assistantState.set('idle');
    }
  }

  onCharacterImageError(): void {
    if (this.eyesClosed() && this.closedImageSrc() !== this.openImageSrc()) {
      this.closedImageSrc.set(this.openImageSrc());
      return;
    }

    this.openImageSrc.set('assets/agent_open.png');
  }

  trackMessage(_: number, message: StorytellingAssistantMessage): string {
    return message.id;
  }

  private handleOpen(): void {
    this.bodyOverflowBeforeOpen = this.document.body.style.overflow;
    this.document.body.style.overflow = 'hidden';
    this.ensureWelcomeMessage();
    this.scheduleBlink();
    this.scheduleFocus();
  }

  private handleCloseState(): void {
    this.stopListening();
    this.stopSpeaking();
    this.clearBlinkTimer();
    this.eyesClosed.set(false);
    this.assistantState.set('idle');
    this.document.body.style.overflow = this.bodyOverflowBeforeOpen;
  }

  private ensureWelcomeMessage(): void {
    if (this.messages().length > 0) {
      return;
    }

    const locale = this.detectInitialLocale();
    this.activeLocale.set(locale);
    const welcomeMessage = this.getWelcomeMessage(locale);

    this.pushMessage({
      role: 'assistant',
      text: welcomeMessage,
      language: locale,
      direction: this.getTextDirection(locale),
      emotion: 'warm',
      action: 'welcome',
      intent: 'greeting'
    });

    this.speakReply(welcomeMessage, locale);
  }

  private handleAssistantResponse(response: StorytellingResponse): void {
    const reply = response.reply?.trim();
    if (!reply) {
      this.assistantState.set('idle');
      return;
    }

    const language = this.detectLocale(reply);
    const direction = this.getTextDirection(language);

    this.lastEmotion.set(response.emotion ?? null);
    this.lastAction.set(response.action ?? null);
    this.lastIntent.set(response.intent ?? null);
    this.activeLocale.set(language);

    this.pushMessage({
      role: 'assistant',
      text: reply,
      language,
      direction,
      emotion: response.emotion ?? null,
      action: response.action ?? null,
      intent: response.intent ?? null
    });

    if (!this.speakReply(reply, language)) {
      this.assistantState.set('idle');
    }
  }

  private pushMessage(message: Omit<StorytellingAssistantMessage, 'id' | 'createdAt'>): void {
    const nextMessage: StorytellingAssistantMessage = {
      id: this.generateMessageId(),
      createdAt: new Date(),
      ...message
    };

    this.messages.update((current) => [...current, nextMessage]);
  }

  private buildConversationHistory(): ConversationMessage[] {
    return this.messages().map((message) => ({
      role: message.role,
      text: message.text
    }));
  }

  private buildStorytellingPayload(message: string): StorytellingRequest {
    return {
      message: String(message),
      userObjective: this.sanitizeUserObjective(this.userObjective()),
      conversationHistory: this.buildConversationHistory(),
      financialContext: this.buildFinancialContextPayload()
    };
  }

  private sanitizeUserObjective(value: string | null | undefined): string | null {
    const normalized = value?.replace(/\s+/g, ' ').trim() ?? '';

    if (!normalized) {
      return null;
    }

    if (normalized.length < 3) {
      return null;
    }

    if (/^\d+(?:[.,]\d+)?$/.test(normalized)) {
      return null;
    }

    if (!/[\p{L}]/u.test(normalized)) {
      return null;
    }

    return normalized;
  }

  private buildFinancialContextPayload(): StorytellingFinancialContext | null {
    const context = this.financialContext();
    if (!context) {
      return null;
    }

    return Object.fromEntries(
      Object.entries(context).filter(([, value]) => value !== null && value !== undefined && value !== '')
    ) as StorytellingFinancialContext;
  }

  private detectInitialLocale(): StorytellingLocale {
    const objective = this.userObjective().trim();
    if (objective) {
      return this.detectLocale(objective);
    }

    if (typeof navigator !== 'undefined' && navigator.language) {
      return this.normalizeLocale(navigator.language);
    }

    return 'fr-FR';
  }

  private resolveRecognitionLocale(): StorytellingLocale {
    const sample = this.draft().trim() || this.userObjective().trim();
    if (sample) {
      return this.detectLocale(sample);
    }

    return this.activeLocale();
  }

  private detectLocale(text: string): StorytellingLocale {
    if (!text) {
      return this.activeLocale();
    }

    if (this.isArabicText(text)) {
      return 'ar-SA';
    }

    const normalized = text.toLowerCase();
    const hasFrenchAccents = /[\u00e0\u00e2\u00e7\u00e9\u00e8\u00ea\u00eb\u00ee\u00ef\u00f4\u00fb\u00f9\u00fc\u00ff\u0153]/i.test(text);
    const frenchHints = /\b(bonjour|merci|budget|depense|depenses|revenu|revenus|objectif|epargne|aujourd'hui|comment|aide|financier|conseil)\b/i.test(normalized);
    if (hasFrenchAccents || frenchHints) {
      return 'fr-FR';
    }

    const englishHints = /\b(hello|thanks|budget|expense|expenses|income|goal|today|help|financial|savings|plan|money)\b/i.test(normalized);
    if (englishHints) {
      return 'en-US';
    }

    return this.activeLocale() === 'ar-SA' ? 'ar-SA' : 'fr-FR';
  }

  private normalizeLocale(locale: string): StorytellingLocale {
    const lowered = locale.toLowerCase();
    if (lowered.startsWith('ar')) {
      return 'ar-SA';
    }
    if (lowered.startsWith('en')) {
      return 'en-US';
    }
    return 'fr-FR';
  }

  private isArabicText(text: string): boolean {
    return /[\u0600-\u06FF\u0750-\u077F\u08A0-\u08FF]/.test(text);
  }

  private getTextDirection(language: StorytellingLocale): StorytellingTextDirection {
    return language === 'ar-SA' ? 'rtl' : 'ltr';
  }

  private getWelcomeMessage(locale: StorytellingLocale): string {
    switch (locale) {
      case 'ar-SA':
        return '\u0645\u0631\u062d\u0628\u0627\u060c \u0623\u0646\u0627 \u0645\u0633\u0627\u0639\u062f\u0643 \u0627\u0644\u0645\u0627\u0644\u064a. \u0643\u064a\u0641 \u064a\u0645\u0643\u0646\u0646\u064a \u0645\u0633\u0627\u0639\u062f\u062a\u0643 \u0627\u0644\u064a\u0648\u0645\u061f';
      case 'en-US':
        return 'Hello, I am your financial assistant. How can I help you today?';
      default:
        return 'Bonjour, je suis votre assistant financier. Comment puis-je vous aider aujourd\'hui ?';
    }
  }

  private generateMessageId(): string {
    return `${Date.now()}-${Math.random().toString(36).slice(2, 9)}`;
  }

  private scheduleFocus(): void {
    setTimeout(() => this.messageInput?.nativeElement.focus(), 60);
  }

  private scheduleScrollAfterRender(): void {
    setTimeout(() => {
      const viewport = this.messagesViewport?.nativeElement;
      if (!viewport) {
        return;
      }

      viewport.scrollTo({
        top: viewport.scrollHeight,
        behavior: 'smooth'
      });
    }, 30);
  }

  private scheduleBlink(): void {
    this.clearBlinkTimer();

    const nextDelay = 2800 + Math.round(Math.random() * 2600);
    this.blinkTimeoutId = window.setTimeout(() => {
      this.eyesClosed.set(true);

      window.setTimeout(() => {
        this.eyesClosed.set(false);
        if (this.isOpen()) {
          this.scheduleBlink();
        }
      }, 140);
    }, nextDelay);
  }

  private clearBlinkTimer(): void {
    if (this.blinkTimeoutId !== null) {
      window.clearTimeout(this.blinkTimeoutId);
      this.blinkTimeoutId = null;
    }
  }

  private initializeSpeechSynthesis(): void {
    if (typeof window === 'undefined' || !('speechSynthesis' in window)) {
      return;
    }

    this.speechSynthesisRef = window.speechSynthesis;
    this.availableVoices = this.speechSynthesisRef.getVoices();
    this.speechSynthesisRef.onvoiceschanged = () => {
      this.availableVoices = this.speechSynthesisRef?.getVoices() ?? [];
    };
  }

  private speakReply(reply: string, language: StorytellingLocale): boolean {
    if (!this.speechSynthesisRef) {
      return false;
    }

    this.stopSpeaking();

    const utterance = new SpeechSynthesisUtterance(reply);
    utterance.lang = language;
    utterance.rate = language === 'ar-SA' ? 0.92 : 0.97;
    utterance.pitch = 1;
    utterance.volume = 1;

    const preferredVoice = this.selectVoiceForLocale(language);
    if (preferredVoice) {
      utterance.voice = preferredVoice;
    }

    utterance.onstart = () => this.assistantState.set('speaking');
    utterance.onend = () => this.assistantState.set('idle');
    utterance.onerror = () => this.assistantState.set('idle');

    this.assistantState.set('speaking');
    this.speechSynthesisRef.cancel();
    this.speechSynthesisRef.speak(utterance);

    return true;
  }

  private selectVoiceForLocale(locale: StorytellingLocale): SpeechSynthesisVoice | null {
    const primaryLanguage = locale.split('-')[0];

    return (
      this.availableVoices.find((voice) => voice.lang.toLowerCase() === locale.toLowerCase()) ??
      this.availableVoices.find((voice) => voice.lang.toLowerCase().startsWith(primaryLanguage.toLowerCase())) ??
      this.availableVoices.find((voice) => voice.default) ??
      null
    );
  }

  private stopSpeaking(): void {
    if (this.speechSynthesisRef?.speaking || this.speechSynthesisRef?.pending) {
      this.speechSynthesisRef.cancel();
    }

    if (this.assistantState() === 'speaking') {
      this.assistantState.set('idle');
    }
  }

  private initializeSpeechRecognition(): void {
    if (typeof window === 'undefined') {
      return;
    }

    const recognitionConstructor = (
      (window as Window & {
        SpeechRecognition?: SpeechRecognitionConstructor;
        webkitSpeechRecognition?: SpeechRecognitionConstructor;
      }).SpeechRecognition ??
      (window as Window & {
        SpeechRecognition?: SpeechRecognitionConstructor;
        webkitSpeechRecognition?: SpeechRecognitionConstructor;
      }).webkitSpeechRecognition
    );

    if (!recognitionConstructor) {
      return;
    }

    this.recognition = new recognitionConstructor();
    this.recognition.continuous = false;
    this.recognition.interimResults = false;
    this.recognition.lang = this.activeLocale();
    this.recognition.maxAlternatives = 1;

    this.recognition.onstart = () => {
      this.assistantState.set('listening');
    };

    this.recognition.onresult = (event) => {
      const transcript = Array.from(event.results)
        .map((result) => result[0]?.transcript ?? '')
        .join(' ')
        .trim();

      if (!transcript) {
        this.assistantState.set('idle');
        return;
      }

      this.draft.set(transcript);
      this.sendMessage();
    };

    this.recognition.onerror = () => {
      this.assistantState.set('idle');
      this.notificationService.info('La reconnaissance vocale a ete interrompue. Vous pouvez reessayer.');
    };

    this.recognition.onend = () => {
      if (this.assistantState() === 'listening') {
        this.assistantState.set('idle');
      }
    };

    this.canUseSpeechRecognition.set(true);
  }

  private stopListening(forceAbort = false): void {
    if (forceAbort) {
      this.recognition?.abort();
      return;
    }

    if (this.isListening()) {
      this.recognition?.stop();
    }
  }

  private humanizeKey(key: string): string {
    return key
      .replace(/([a-z])([A-Z])/g, '$1 $2')
      .replace(/[_-]+/g, ' ')
      .replace(/^\w/, (char) => char.toUpperCase());
  }

  private stringifyContextValue(value: unknown): string {
    if (Array.isArray(value)) {
      return value.join(', ');
    }

    if (typeof value === 'object' && value !== null) {
      return Object.entries(value as Record<string, unknown>)
        .slice(0, 3)
        .map(([key, itemValue]) => `${this.humanizeKey(key)}: ${String(itemValue)}`)
        .join(' | ');
    }

    return String(value);
  }
}
