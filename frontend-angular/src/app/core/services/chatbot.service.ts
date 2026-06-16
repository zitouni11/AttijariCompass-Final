import { HttpClient, HttpErrorResponse } from '@angular/common/http';
import { Injectable, inject, signal } from '@angular/core';
import { Observable, Subject, catchError, finalize, map, throwError } from 'rxjs';
import { environment } from '../../../environments/environment';
import type { ChatAssistantReply, ChatRequestDto, ChatResponseDto } from '../models';

@Injectable({ providedIn: 'root' })
export class ChatbotService {
  private readonly http = inject(HttpClient);
  private readonly apiUrl = `${environment.apiUrl}/chat`;
  private readonly openPromptSubject = new Subject<string>();

  readonly isLoading = signal(false);
  readonly openPrompt$ = this.openPromptSubject.asObservable();

  openWithPrompt(prompt: string): void {
    const cleanedPrompt = prompt.trim();

    if (cleanedPrompt) {
      this.openPromptSubject.next(cleanedPrompt);
    }
  }

  sendMessage(message: string): Observable<ChatAssistantReply> {
    const payload = this.buildPayload(message);

    this.isLoading.set(true);

    return this.http.post<ChatResponseDto>(this.apiUrl, payload).pipe(
      map((response) => this.normalizeResponse(response)),
      catchError((error: HttpErrorResponse) => {
        const friendlyMessage = this.mapError(error);
        return throwError(() => new Error(friendlyMessage));
      }),
      finalize(() => this.isLoading.set(false))
    );
  }

  private buildPayload(message: string): ChatRequestDto {
    return {
      message: message.trim()
    };
  }

  private normalizeResponse(response: ChatResponseDto | null | undefined): ChatAssistantReply {
    const answer = typeof response?.answer === 'string' ? response.answer.trim() : '';
    const ragContextPreview = typeof response?.ragContextPreview === 'string'
      ? response.ragContextPreview.trim()
      : '';
    const usedModel = typeof response?.usedModel === 'string' ? response.usedModel.trim() : '';
    const timestamp = this.normalizeTimestamp(response?.timestamp);

    return {
      answer: answer || "Je n'ai pas pu formuler une reponse exploitable pour le moment. Reessayez avec une question plus precise.",
      ragContextPreview: ragContextPreview || null,
      usedModel: usedModel || null,
      timestamp
    };
  }

  private normalizeTimestamp(value: string | null | undefined): string {
    if (!value) {
      return new Date().toISOString();
    }

    const parsed = new Date(value);
    return Number.isFinite(parsed.getTime()) ? parsed.toISOString() : new Date().toISOString();
  }

  private mapError(error: HttpErrorResponse): string {
    if (error.status === 0) {
      return "Le service IA n'est pas joignable pour le moment. Verifiez votre connexion et reessayez.";
    }

    if (error.status === 400) {
      return this.extractBackendMessage(error)
        ?? 'La demande envoyee au chatbot est incomplete. Reformulez votre question.';
    }

    if (error.status === 401) {
      return 'Votre session a expire, reconnectez-vous.';
    }

    if (error.status === 403) {
      return "Vous n'etes pas autorise a utiliser cet assistant pour le moment.";
    }

    if (error.status === 429) {
      return 'Le service IA traite trop de demandes pour le moment. Reessayez dans quelques instants.';
    }

    if (error.status >= 500) {
      return 'Le service IA est temporairement indisponible.';
    }

    return this.extractBackendMessage(error)
      ?? 'Une erreur est survenue lors de la conversation avec Attijari Compass AI.';
  }

  private extractBackendMessage(error: HttpErrorResponse): string | null {
    const backendError = error.error;

    if (typeof backendError === 'string' && backendError.trim()) {
      return backendError.trim();
    }

    if (backendError && typeof backendError === 'object') {
      const candidate = (backendError as { message?: unknown }).message;
      if (typeof candidate === 'string' && candidate.trim()) {
        return candidate.trim();
      }
    }

    return null;
  }
}
