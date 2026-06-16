import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, catchError, map } from 'rxjs';
import { environment } from '../../../environments/environment';
import {
  StorytellingRequest,
  StorytellingResponse
} from '../models/storytelling-assistant.models';

interface ChatbotResponse {
  message: string;
}

@Injectable({ providedIn: 'root' })
export class StorytellingService {
  private readonly http = inject(HttpClient);
  private readonly apiUrl = `${environment.apiUrl}/storytelling/chat`;
  private readonly chatbotUrl = environment.chatbotUrl ? `${environment.chatbotUrl}/api/chat` : '';

  chat(payload: StorytellingRequest): Observable<StorytellingResponse> {
    if (this.chatbotUrl) {
      return this.chatWithChatbot(payload).pipe(
        catchError(() => this.chatWithApi(payload))
      );
    }

    return this.chatWithApi(payload);
  }

  private chatWithApi(payload: StorytellingRequest): Observable<StorytellingResponse> {
    return this.http.post<StorytellingResponse & Partial<ChatbotResponse>>(this.apiUrl, payload).pipe(
      map((response) => this.normalizeResponse(response))
    );
  }

  private chatWithChatbot(payload: StorytellingRequest): Observable<StorytellingResponse> {
    return this.http.post<ChatbotResponse>(this.chatbotUrl, payload).pipe(
      map((response) => this.normalizeResponse(response))
    );
  }

  private normalizeResponse(
    response: Partial<StorytellingResponse> & Partial<ChatbotResponse>
  ): StorytellingResponse {
    return {
      reply: response.reply?.trim() || response.message?.trim() || '',
      emotion: response.emotion ?? null,
      action: response.action ?? null,
      intent: response.intent ?? null
    };
  }
}
