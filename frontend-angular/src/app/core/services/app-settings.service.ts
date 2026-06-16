import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { BehaviorSubject, Observable, catchError, of, tap } from 'rxjs';
import { environment } from '../../../environments/environment';
import { DEFAULT_PUBLIC_APP_SETTINGS, PublicAppSettings } from '../models/app-settings.models';

@Injectable({ providedIn: 'root' })
export class AppSettingsService {
  private readonly http = inject(HttpClient);
  private readonly settingsSubject = new BehaviorSubject<PublicAppSettings>(DEFAULT_PUBLIC_APP_SETTINGS);

  readonly publicSettings$ = this.settingsSubject.asObservable();

  loadPublicSettings(): Observable<PublicAppSettings> {
    return this.refreshSettings();
  }

  refreshSettings(): Observable<PublicAppSettings> {
    return this.http.get<PublicAppSettings>(`${environment.apiUrl}/app/settings/public`).pipe(
      tap((settings) => this.settingsSubject.next({ ...DEFAULT_PUBLIC_APP_SETTINGS, ...settings })),
      catchError(() => {
        console.warn('[AppSettings] Unable to load public settings. Falling back to defaults.');
        this.settingsSubject.next(DEFAULT_PUBLIC_APP_SETTINGS);
        return of(DEFAULT_PUBLIC_APP_SETTINGS);
      })
    );
  }

  get currentSettings(): PublicAppSettings {
    return this.settingsSubject.value;
  }
}
