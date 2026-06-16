import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import {
  AdminDashboardDto,
  AdminRegistrationResponseDto,
  AdminRole,
  AdminUserDto,
  AccountRestoreRequestDto,
  AppSettingDto,
  AuditLogDto,
  GeneralNotificationDto,
  GeneralNotificationRequest,
  SupportTicketDto,
  SupportTicketStatus,
  TechnicalStatusDto
} from '../models/admin.models';

@Injectable({ providedIn: 'root' })
export class AdminService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = `${environment.apiUrl}/admin`;

  getDashboard(): Observable<AdminDashboardDto> {
    return this.http.get<AdminDashboardDto>(`${this.baseUrl}/dashboard`);
  }

  getUsers(): Observable<AdminUserDto[]> {
    return this.http.get<AdminUserDto[]>(`${this.baseUrl}/users`);
  }

  getDeletedUsers(): Observable<AdminUserDto[]> {
    return this.http.get<AdminUserDto[]>(`${this.baseUrl}/users/deleted`);
  }

  activateUser(id: number): Observable<AdminUserDto> {
    return this.http.patch<AdminUserDto>(`${this.baseUrl}/users/${id}/activate`, {});
  }

  deactivateUser(id: number): Observable<AdminUserDto> {
    return this.http.patch<AdminUserDto>(`${this.baseUrl}/users/${id}/deactivate`, {});
  }

  changeUserRole(id: number, role: AdminRole): Observable<AdminUserDto> {
    return this.http.patch<AdminUserDto>(`${this.baseUrl}/users/${id}/role`, { role });
  }

  deleteUser(id: number, reason: string): Observable<{ message: string }> {
    return this.http.patch<{ message: string }>(`${this.baseUrl}/users/${id}/delete`, { reason });
  }

  restoreUser(id: number): Observable<{ message: string }> {
    return this.http.patch<{ message: string }>(`${this.baseUrl}/users/${id}/restore`, {});
  }

  getSupportTickets(): Observable<SupportTicketDto[]> {
    return this.http.get<SupportTicketDto[]>(`${this.baseUrl}/support/tickets`);
  }

  updateTicketStatus(id: number, status: SupportTicketStatus): Observable<SupportTicketDto> {
    return this.http.patch<SupportTicketDto>(`${this.baseUrl}/support/tickets/${id}/status`, { status });
  }

  replyTicket(id: number, adminReply: string): Observable<SupportTicketDto> {
    return this.http.post<SupportTicketDto>(`${this.baseUrl}/support/tickets/${id}/reply`, { adminReply });
  }

  getNotifications(): Observable<GeneralNotificationDto[]> {
    return this.http.get<GeneralNotificationDto[]>(`${this.baseUrl}/notifications`);
  }

  createNotification(request: GeneralNotificationRequest): Observable<GeneralNotificationDto> {
    return this.http.post<GeneralNotificationDto>(`${this.baseUrl}/notifications`, request);
  }

  updateNotification(id: number, request: GeneralNotificationRequest): Observable<GeneralNotificationDto> {
    return this.http.put<GeneralNotificationDto>(`${this.baseUrl}/notifications/${id}`, request);
  }

  publishNotification(id: number): Observable<GeneralNotificationDto> {
    return this.http.patch<GeneralNotificationDto>(`${this.baseUrl}/notifications/${id}/publish`, {});
  }

  disableNotification(id: number): Observable<GeneralNotificationDto> {
    return this.http.patch<GeneralNotificationDto>(`${this.baseUrl}/notifications/${id}/disable`, {});
  }

  deleteNotification(id: number): Observable<void> {
    return this.http.delete<void>(`${this.baseUrl}/notifications/${id}`);
  }

  getAuditLogs(): Observable<AuditLogDto[]> {
    return this.http.get<AuditLogDto[]>(`${this.baseUrl}/audit-logs`);
  }

  getTechnicalStatus(): Observable<TechnicalStatusDto> {
    return this.http.get<TechnicalStatusDto>(`${this.baseUrl}/technical/status`);
  }

  getSettings(): Observable<AppSettingDto[]> {
    return this.http.get<AppSettingDto[]>(`${this.baseUrl}/settings`);
  }

  updateSetting(key: string, settingValue: string): Observable<AppSettingDto> {
    return this.http.put<AppSettingDto>(`${this.baseUrl}/settings/${key}`, { settingValue });
  }

  getAdminRequests(): Observable<AdminRegistrationResponseDto[]> {
    return this.http.get<AdminRegistrationResponseDto[]>(`${this.baseUrl}/admin-requests`);
  }

  approveAdminRequest(id: number): Observable<AdminRegistrationResponseDto> {
    return this.http.patch<AdminRegistrationResponseDto>(`${this.baseUrl}/admin-requests/${id}/approve`, {});
  }

  rejectAdminRequest(id: number, reason: string): Observable<AdminRegistrationResponseDto> {
    return this.http.patch<AdminRegistrationResponseDto>(`${this.baseUrl}/admin-requests/${id}/reject`, { reason });
  }

  getAccountRestoreRequests(): Observable<AccountRestoreRequestDto[]> {
    return this.http.get<AccountRestoreRequestDto[]>(`${this.baseUrl}/account-restore-requests`);
  }

  approveAccountRestoreRequest(id: number): Observable<{ message: string }> {
    return this.http.patch<{ message: string }>(`${this.baseUrl}/account-restore-requests/${id}/approve`, {});
  }

  rejectAccountRestoreRequest(id: number, reason: string): Observable<{ message: string }> {
    return this.http.patch<{ message: string }>(`${this.baseUrl}/account-restore-requests/${id}/reject`, { reason });
  }
}
