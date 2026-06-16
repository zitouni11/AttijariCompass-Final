import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { BehaviorSubject, Observable, catchError, finalize, forkJoin, map, of, switchMap, tap, throwError } from 'rxjs';
import { environment } from '../../../environments/environment';
import {
  Alert,
  AlertSeverity,
  AlertType,
  BackendAlertResponse,
  BackendAlertSeverity,
  BackendAlertType
} from './alert.model';

@Injectable({ providedIn: 'root' })
export class AlertService {
  private readonly http = inject(HttpClient);
  private readonly apiUrl = `${environment.apiUrl}/notifications`;

  private readonly alertsSubject = new BehaviorSubject<Alert[]>([]);
  private readonly unreadCountSubject = new BehaviorSubject<number>(0);
  private readonly loadingSubject = new BehaviorSubject<boolean>(false);
  private readonly errorSubject = new BehaviorSubject<string | null>(null);
  private readonly hydratedSubject = new BehaviorSubject<boolean>(false);
  private readonly newAlertsSubject = new BehaviorSubject<Alert[]>([]);
  private knownAlertIds = new Set<string>();

  getAlerts(): Observable<Alert[]> {
    return this.alertsSubject.asObservable();
  }

  getUnreadCount(): Observable<number> {
    return this.unreadCountSubject.asObservable();
  }

  getLoading(): Observable<boolean> {
    return this.loadingSubject.asObservable();
  }

  getError(): Observable<string | null> {
    return this.errorSubject.asObservable();
  }

  getHydrated(): Observable<boolean> {
    return this.hydratedSubject.asObservable();
  }

  getNewAlerts(): Observable<Alert[]> {
    return this.newAlertsSubject.asObservable();
  }

  refreshAlerts(): Observable<Alert[]> {
    this.loadingSubject.next(true);
    this.errorSubject.next(null);

    return forkJoin({
      alerts: this.http.get<unknown>(`${this.apiUrl}/my`).pipe(
        map((response) => this.normalizeAlertsResponse(response))
      ),
      unreadCount: this.http.get<unknown>(`${this.apiUrl}/my/unread-count`).pipe(
        map((response) => this.normalizeUnreadCountResponse(response))
      )
    }).pipe(
      tap(({ alerts, unreadCount }) => {
        this.emitNewUnreadAlerts(alerts);
        this.alertsSubject.next(alerts);
        this.unreadCountSubject.next(unreadCount);
        this.hydratedSubject.next(true);
      }),
      map(({ alerts }) => alerts),
      catchError((error: unknown) => {
        this.errorSubject.next(this.extractErrorMessage(error));
        return throwError(() => error);
      }),
      finalize(() => this.loadingSubject.next(false))
    );
  }

  refreshAlertsWithToast(): Observable<Alert[]> {
    return this.refreshAlerts();
  }

  markAsRead(id: string): Observable<void> {
    return this.http.patch<void>(`${this.apiUrl}/${id}/read`, {}).pipe(
      tap(() => {
        this.patchLocalRead(id);
      }),
      switchMap(() => this.refreshAlerts().pipe(map(() => void 0))),
      catchError((error) => {
        this.errorSubject.next(this.extractErrorMessage(error));
        return throwError(() => error);
      })
    );
  }

  markAllAsRead(): Observable<void> {
    return this.http.patch<void>(`${this.apiUrl}/read-all`, {}).pipe(
      tap(() => {
        this.alertsSubject.next(this.alertsSubject.value.map((alert) => ({ ...alert, read: true })));
        this.unreadCountSubject.next(0);
      }),
      switchMap(() => this.refreshAlerts().pipe(map(() => void 0))),
      catchError((error) => {
        this.errorSubject.next(this.extractErrorMessage(error));
        return throwError(() => error);
      })
    );
  }

  deleteAlert(id: string): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/${id}`).pipe(
      tap(() => {
        const nextAlerts = this.alertsSubject.value.filter((alert) => alert.id !== id);
        this.alertsSubject.next(nextAlerts);
        this.unreadCountSubject.next(nextAlerts.filter((alert) => !alert.read).length);
      }),
      switchMap(() => this.refreshAlerts().pipe(map(() => void 0))),
      catchError((error) => {
        this.errorSubject.next(this.extractErrorMessage(error));
        return throwError(() => error);
      })
    );
  }

  clearAll(): Observable<void> {
    const deletions = this.alertsSubject.value.map((alert) => this.http.delete<void>(`${this.apiUrl}/${alert.id}`));

    if (!deletions.length) {
      this.alertsSubject.next([]);
      this.unreadCountSubject.next(0);
      return of(void 0);
    }

    return forkJoin(deletions).pipe(
      tap(() => {
        this.alertsSubject.next([]);
        this.unreadCountSubject.next(0);
      }),
      switchMap(() => this.refreshAlerts().pipe(map(() => void 0))),
      catchError((error) => {
        this.errorSubject.next(this.extractErrorMessage(error));
        return throwError(() => error);
      })
    );
  }

  getByType(type: AlertType): Observable<Alert[]> {
    return this.getAlerts().pipe(
      map((alerts) => alerts.filter((alert) => alert.type === type))
    );
  }

  getBySeverity(severity: AlertSeverity): Observable<Alert[]> {
    return this.getAlerts().pipe(
      map((alerts) => alerts.filter((alert) => alert.severity === severity))
    );
  }

  private patchLocalRead(id: string): void {
    const nextAlerts = this.alertsSubject.value.map((alert) => (
      alert.id === id ? { ...alert, read: true } : alert
    ));
    this.alertsSubject.next(nextAlerts);
    this.unreadCountSubject.next(nextAlerts.filter((alert) => !alert.read).length);
  }

  private emitNewUnreadAlerts(alerts: Alert[]): void {
    const nextIds = new Set(alerts.map((alert) => alert.id));

    if (!this.hydratedSubject.value) {
      this.knownAlertIds = nextIds;
      this.newAlertsSubject.next([]);
      return;
    }

    const newUnreadAlerts = alerts.filter((alert) => !alert.read && !this.knownAlertIds.has(alert.id));
    this.knownAlertIds = nextIds;
    this.newAlertsSubject.next(newUnreadAlerts);
  }

  private normalizeAlertsResponse(response: unknown): Alert[] {
    const root = this.asRecord(response);
    const rawItems = Array.isArray(response)
      ? response
      : this.pickArray(root, ['content', 'items', 'data', 'notifications', 'results']);

    return rawItems.map((item, index) => this.normalizeAlert(item, index));
  }

  private normalizeUnreadCountResponse(response: unknown): number {
    if (typeof response === 'number' && Number.isFinite(response)) {
      return response;
    }

    const root = this.asRecord(response);
    const value = this.pickNumber(root, ['count', 'unreadCount', 'total']);
    return value ?? this.alertsSubject.value.filter((alert) => !alert.read).length;
  }

  private normalizeAlert(value: unknown, fallbackIndex: number): Alert {
    const source = this.asRecord(value) as Partial<BackendAlertResponse> & Record<string, unknown>;
    const backendType = this.pickString(source, ['type']) ?? 'SYSTEM';
    const backendSeverity = this.pickString(source, ['severity']) ?? 'INFO';
    const type = this.mapType(backendType as BackendAlertType);
    const severity = this.mapSeverity(backendSeverity as BackendAlertSeverity, type);
    const title = this.pickString(source, ['title']) ?? 'Notification';
    const message = this.pickString(source, ['message']) ?? '';
    const timestamp = this.pickString(source, ['timestamp', 'generatedAt', 'createdAt', 'updatedAt'])
      ?? new Date().toISOString();

    return {
      id: this.pickString(source, ['id']) ?? `notification-${fallbackIndex}`,
      type,
      severity,
      title,
      message,
      amount: this.pickNumber(source, ['amount']) ?? undefined,
      currency: this.pickString(source, ['currency']) ?? 'DT',
      timestamp,
      read: this.pickBoolean(source, ['read']) ?? false,
      actionLabel: this.pickString(source, ['actionLabel']) ?? this.defaultActionLabel(type),
      actionRoute: this.pickString(source, ['actionRoute']) ?? this.defaultActionRoute(type)
    };
  }

  private mapType(type: BackendAlertType): AlertType {
    switch ((type ?? 'SYSTEM').toUpperCase()) {
      case 'BUDGET':
        return 'budget';
      case 'TRANSACTION':
        return 'transaction';
      case 'SECURITY':
        return 'security';
      case 'GOAL':
      case 'CARD':
      case 'RECOMMENDATION':
      case 'SYSTEM':
      default:
        return 'info';
    }
  }

  private mapSeverity(severity: BackendAlertSeverity, type: AlertType): AlertSeverity {
    switch ((severity ?? 'INFO').toUpperCase()) {
      case 'CRITICAL':
        return 'critical';
      case 'WARNING':
        return type === 'budget' || type === 'transaction' ? 'high' : 'medium';
      case 'SUCCESS':
        return 'low';
      case 'INFO':
      default:
        return 'low';
    }
  }

  private defaultActionLabel(type: AlertType): string {
    switch (type) {
      case 'budget':
        return 'Voir les budgets';
      case 'transaction':
        return 'Voir les transactions';
      case 'security':
        return 'Voir le profil';
      case 'success':
      case 'info':
      default:
        return 'Voir les alertes';
    }
  }

  private defaultActionRoute(type: AlertType): string {
    switch (type) {
      case 'budget':
        return '/budgets';
      case 'transaction':
        return '/transactions';
      case 'security':
        return '/profile';
      case 'success':
      case 'info':
      default:
        return '/notifications';
    }
  }

  private extractErrorMessage(error: unknown): string {
    if (typeof error === 'object' && error !== null) {
      const source = error as Record<string, unknown>;
      const nested = source['error'];

      if (nested && typeof nested === 'object' && !Array.isArray(nested)) {
        const nestedSource = nested as Record<string, unknown>;
        const nestedMessage = nestedSource['message'] ?? nestedSource['detail'];
        if (typeof nestedMessage === 'string' && nestedMessage.trim()) {
          return nestedMessage.trim();
        }
      }

      const message = source['message'];
      if (typeof message === 'string' && message.trim()) {
        return message.trim();
      }
    }

    return 'Impossible de charger les notifications pour le moment.';
  }

  private asRecord(value: unknown): Record<string, unknown> {
    return value && typeof value === 'object' && !Array.isArray(value)
      ? value as Record<string, unknown>
      : {};
  }

  private pickArray(source: Record<string, unknown>, keys: string[]): unknown[] {
    for (const key of keys) {
      if (Array.isArray(source[key])) {
        return source[key] as unknown[];
      }
    }

    return [];
  }

  private pickString(source: Record<string, unknown>, keys: string[]): string | null {
    for (const key of keys) {
      const value = source[key];

      if (typeof value === 'string' && value.trim()) {
        return value.trim();
      }

      if (value instanceof Date) {
        return value.toISOString();
      }
    }

    return null;
  }

  private pickNumber(source: Record<string, unknown>, keys: string[]): number | null {
    for (const key of keys) {
      const value = source[key];

      if (typeof value === 'number' && Number.isFinite(value)) {
        return value;
      }

      if (typeof value === 'string' && value.trim()) {
        const parsed = Number(value);
        if (Number.isFinite(parsed)) {
          return parsed;
        }
      }
    }

    return null;
  }

  private pickBoolean(source: Record<string, unknown>, keys: string[]): boolean | null {
    for (const key of keys) {
      const value = source[key];

      if (typeof value === 'boolean') {
        return value;
      }

      if (typeof value === 'string') {
        const normalized = value.trim().toLowerCase();
        if (normalized === 'true') {
          return true;
        }
        if (normalized === 'false') {
          return false;
        }
      }
    }

    return null;
  }
}
