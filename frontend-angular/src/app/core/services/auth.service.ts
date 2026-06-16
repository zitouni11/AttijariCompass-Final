import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Router } from '@angular/router';
import { BehaviorSubject, Observable, finalize, shareReplay, tap, throwError } from 'rxjs';
import { environment } from '../../../environments/environment';
import {
  AuthMessageResponse, AuthResponse, LoginRequest, RegisterRequest,
  RefreshTokenRequest, ResendVerificationCodeRequest, UserResponse, VerifyEmailRequest,
  AccountRestoreRequest, AccountRestoreVerifyRequest
} from '../models';

@Injectable({ providedIn: 'root' })
export class AuthService {
  private http = inject(HttpClient);
  private router = inject(Router);

  private apiUrl = `${environment.apiUrl}/auth`;
  private currentUserSubject = new BehaviorSubject<UserResponse | null>(this.loadUser());
  currentUser$ = this.currentUserSubject.asObservable();
  private refreshInFlight$: Observable<AuthResponse> | null = null;
  private logoutInProgress = false;

  private loadUser(): UserResponse | null {
    const stored = localStorage.getItem(environment.userKey);
    if (!stored) {
      return null;
    }

    try {
      return JSON.parse(stored) as UserResponse;
    } catch {
      localStorage.removeItem(environment.userKey);
      return null;
    }
  }

  get currentUser(): UserResponse | null {
    return this.currentUserSubject.value;
  }

  get isAuthenticated(): boolean {
    return !!this.getToken();
  }

  get isAdmin(): boolean {
    return this.currentUser?.role === 'ADMIN';
  }

  getToken(): string | null {
    return localStorage.getItem(environment.jwtTokenKey);
  }

  getRefreshToken(): string | null {
    return localStorage.getItem(environment.refreshTokenKey);
  }

  login(request: LoginRequest): Observable<AuthResponse> {
    return this.http.post<AuthResponse>(`${this.apiUrl}/login`, request).pipe(
      tap(response => this.handleAuthResponse(response))
    );
  }

  register(request: RegisterRequest): Observable<AuthMessageResponse> {
    return this.http.post<AuthMessageResponse>(`${this.apiUrl}/register`, request);
  }

  verifyEmail(request: VerifyEmailRequest): Observable<AuthMessageResponse> {
    return this.http.post<AuthMessageResponse>(`${this.apiUrl}/verify-email`, request);
  }

  resendVerificationCode(request: ResendVerificationCodeRequest): Observable<AuthMessageResponse> {
    return this.http.post<AuthMessageResponse>(`${this.apiUrl}/resend-verification-code`, request);
  }

  requestAccountRestore(request: AccountRestoreRequest): Observable<AuthMessageResponse> {
    return this.http.post<AuthMessageResponse>(`${this.apiUrl}/account-restore/request`, request);
  }

  verifyAccountRestore(request: AccountRestoreVerifyRequest): Observable<AuthMessageResponse> {
    return this.http.post<AuthMessageResponse>(`${this.apiUrl}/account-restore/verify`, request);
  }

  refreshToken(): Observable<AuthResponse> {
    const refreshToken = this.getRefreshToken();
    if (!refreshToken) {
      return throwError(() => new Error('No refresh token available.'));
    }

    if (this.refreshInFlight$) {
      return this.refreshInFlight$;
    }

    const request: RefreshTokenRequest = { refreshToken };
    this.refreshInFlight$ = this.http.post<AuthResponse>(`${this.apiUrl}/refresh-token`, request).pipe(
      tap(response => this.handleAuthResponse(response)),
      finalize(() => {
        this.refreshInFlight$ = null;
      }),
      shareReplay(1)
    );

    return this.refreshInFlight$;
  }

  private handleAuthResponse(response: AuthResponse): void {
    localStorage.setItem(environment.jwtTokenKey, response.accessToken || response.token);
    localStorage.setItem(environment.refreshTokenKey, response.refreshToken);

    const user: UserResponse = {
      id: 0,
      fullName: response.fullName,
      email: response.email,
      role: response.role,
      active: true,
      createdAt: new Date().toISOString()
    };
    localStorage.setItem(environment.userKey, JSON.stringify(user));
    this.currentUserSubject.next(user);
  }

  logout(): void {
    this.clearSession();

    if (this.logoutInProgress) {
      return;
    }

    this.logoutInProgress = true;
    void this.router.navigate(['/auth/login']).finally(() => {
      this.logoutInProgress = false;
    });
  }

  private clearSession(): void {
    localStorage.removeItem(environment.jwtTokenKey);
    localStorage.removeItem(environment.refreshTokenKey);
    localStorage.removeItem(environment.userKey);
    this.currentUserSubject.next(null);
    this.refreshInFlight$ = null;
  }
}
