import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import {
  StorytellingChatRequest,
  StorytellingChatResponse
} from '../models/storytelling-assistant.models';

@Injectable({ providedIn: 'root' })
export class StorytellingAssistantService {
  private http = inject(HttpClient);
  private apiUrl = `${environment.apiUrl}/storytelling/chat`;

  chat(payload: StorytellingChatRequest): Observable<StorytellingChatResponse> {
    return this.http.post<StorytellingChatResponse>(this.apiUrl, payload);
  }
}
