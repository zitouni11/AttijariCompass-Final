import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import {
  AdminRegistrationRequest,
  AdminRegistrationVerifyRequest
} from '../models';
import { AdminRegistrationResponseDto } from '../models/admin.models';

@Injectable({ providedIn: 'root' })
export class AdminRegistrationService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = `${environment.apiUrl}/auth/admin-registration`;

  requestAdminAccess(request: AdminRegistrationRequest): Observable<AdminRegistrationResponseDto> {
    return this.http.post<AdminRegistrationResponseDto>(`${this.baseUrl}/request`, request);
  }

  verifyEmail(request: AdminRegistrationVerifyRequest): Observable<AdminRegistrationResponseDto> {
    return this.http.post<AdminRegistrationResponseDto>(`${this.baseUrl}/verify`, request);
  }
}
