// ✅ À CRÉER DANS: src/app/interceptors/auth.interceptor.ts

import { Injectable } from '@angular/core';
import {
  HttpRequest,
  HttpHandler,
  HttpEvent,
  HttpInterceptor,
  HttpErrorResponse
} from '@angular/common/http';
import { Observable, throwError } from 'rxjs';
import { catchError } from 'rxjs/operators';
import { AuthService } from '../services/auth.service';
import { Router } from '@angular/router';

@Injectable()
export class AuthInterceptor implements HttpInterceptor {
  constructor(
    private authService: AuthService,
    private router: Router
  ) { }

  intercept(request: HttpRequest<unknown>, next: HttpHandler): Observable<HttpEvent<unknown>> {
    // ✅ Ajouter le token JWT à TOUTES les requêtes
    const token = this.authService.getToken();

    if (token) {
      request = request.clone({
        setHeaders: {
          Authorization: `Bearer ${token}`
        }
      });
    }

    return next.handle(request).pipe(
      catchError((error: HttpErrorResponse) => {
        // ✅ Si 401 Unauthorized, rediriger vers login
        if (error.status === 401) {
          this.authService.logout();
          this.router.navigate(['/login']);
        }

        // ✅ Si 403 Forbidden, rediriger vers home
        if (error.status === 403) {
          this.router.navigate(['/dashboard']);
        }

        return throwError(() => error);
      })
    );
  }
}

