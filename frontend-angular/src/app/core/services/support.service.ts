import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { SupportTicketCreateRequest, SupportTicketDto } from '../models/admin.models';

@Injectable({ providedIn: 'root' })
export class SupportService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = `${environment.apiUrl}/support/tickets`;

  createTicket(request: SupportTicketCreateRequest): Observable<SupportTicketDto> {
    return this.http.post<SupportTicketDto>(this.baseUrl, request);
  }

  getMyTickets(): Observable<SupportTicketDto[]> {
    return this.http.get<SupportTicketDto[]>(`${this.baseUrl}/my`);
  }
}
