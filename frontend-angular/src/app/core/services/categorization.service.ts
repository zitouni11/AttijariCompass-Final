import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { CategorizationRequest, CategorizationResult } from '../models';

@Injectable({ providedIn: 'root' })
export class CategorizationService {
  private http = inject(HttpClient);
  private apiUrl = `${environment.apiUrl}/categorization`;

  predict(request: CategorizationRequest): Observable<CategorizationResult> {
    return this.http.post<CategorizationResult>(`${this.apiUrl}/predict`, request);
  }
}
