// ✅ À COPIER DANS: src/app/services/auth.service.ts

import { Injectable } from '@angular/core';
import { HttpClient, HttpHeaders } from '@angular/common/http';
import { Observable, BehaviorSubject } from 'rxjs';
import { tap } from 'rxjs/operators';
import { environment } from '../../../environments/environment';

@Injectable({
  providedIn: 'root'
})
export class AuthService {
  private apiUrl = environment.apiUrl;
  private tokenSubject = new BehaviorSubject<string | null>(localStorage.getItem('token'));
  public token$ = this.tokenSubject.asObservable();

  constructor(private http: HttpClient) { }

  // ✅ Register
  register(email: string, password: string, role: string = 'USER'): Observable<any> {
    const headers = new HttpHeaders({ 'Content-Type': 'application/json' });
    return this.http.post(
      `${this.apiUrl}/auth/register`,
      { email, password, role },
      { headers }
    ).pipe(
      tap((response: any) => {
        if (response.token) {
          localStorage.setItem('token', response.token);
          localStorage.setItem('refreshToken', response.refreshToken);
          this.tokenSubject.next(response.token);
        }
      })
    );
  }

  // ✅ Login
  login(email: string, password: string): Observable<any> {
    const headers = new HttpHeaders({ 'Content-Type': 'application/json' });
    return this.http.post(
      `${this.apiUrl}/auth/login`,
      { email, password },
      { headers }
    ).pipe(
      tap((response: any) => {
        if (response.token) {
          localStorage.setItem('token', response.token);
          localStorage.setItem('refreshToken', response.refreshToken);
          localStorage.setItem('email', response.email);
          localStorage.setItem('role', response.role);
          this.tokenSubject.next(response.token);
        }
      })
    );
  }

  // ✅ Refresh Token
  refreshToken(): Observable<any> {
    const refreshToken = localStorage.getItem('refreshToken');
    const headers = new HttpHeaders({ 'Content-Type': 'application/json' });
    return this.http.post(
      `${this.apiUrl}/auth/refresh-token`,
      { refreshToken },
      { headers }
    ).pipe(
      tap((response: any) => {
        if (response.token) {
          localStorage.setItem('token', response.token);
          localStorage.setItem('refreshToken', response.refreshToken);
          this.tokenSubject.next(response.token);
        }
      })
    );
  }

  // ✅ Logout
  logout(): void {
    localStorage.removeItem('token');
    localStorage.removeItem('refreshToken');
    localStorage.removeItem('email');
    localStorage.removeItem('role');
    this.tokenSubject.next(null);
  }

  // ✅ Obtenir le token actuel
  getToken(): string | null {
    return localStorage.getItem('token');
  }

  // ✅ Vérifier si connecté
  isAuthenticated(): boolean {
    return !!this.getToken();
  }

  // ✅ Obtenir l'email de l'utilisateur connecté
  getCurrentUserEmail(): string | null {
    return localStorage.getItem('email');
  }

  // ✅ Obtenir le rôle de l'utilisateur
  getCurrentUserRole(): string | null {
    return localStorage.getItem('role');
  }
}

