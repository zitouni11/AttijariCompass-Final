import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { BehaviorSubject, Observable, catchError, of, tap } from 'rxjs';
import { environment } from '../../../environments/environment';
import { GeneralNotificationDto } from '../models/admin.models';

@Injectable({ providedIn: 'root' })
export class GeneralNotificationService {
  private readonly http = inject(HttpClient);
  private readonly notificationsSubject = new BehaviorSubject<GeneralNotificationDto[]>([]);

  readonly notifications$ = this.notificationsSubject.asObservable();

  refresh(): Observable<GeneralNotificationDto[]> {
    return this.http.get<GeneralNotificationDto[]>(`${environment.apiUrl}/notifications/general`, {
      params: { refresh: Date.now().toString() }
    }).pipe(
      tap((notifications) => this.notificationsSubject.next(notifications)),
      catchError(() => {
        this.notificationsSubject.next([]);
        return of([]);
      })
    );
  }
}
