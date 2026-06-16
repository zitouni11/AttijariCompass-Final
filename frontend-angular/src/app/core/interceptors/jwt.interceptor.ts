import { HttpContextToken, HttpErrorResponse, HttpHandlerFn, HttpInterceptorFn, HttpRequest } from '@angular/common/http';
import { inject } from '@angular/core';
import { catchError, switchMap, throwError } from 'rxjs';
import { AuthService } from '../services/auth.service';
import { NotificationService } from '../services/notification.service';

const AUTH_RETRY_CONTEXT = new HttpContextToken<boolean>(() => false);
let sessionExpirationHandled = false;

export const jwtInterceptor: HttpInterceptorFn = (
  req: HttpRequest<unknown>,
  next: HttpHandlerFn
) => {
  const authService = inject(AuthService);
  const notificationService = inject(NotificationService);

  const authReq = shouldAttachToken(req)
    ? withAuthorization(req, authService.getToken())
    : req;

  return next(authReq).pipe(
    catchError((error: HttpErrorResponse) => {
      if (shouldRefreshToken(error, req, authService)) {
        return authService.refreshToken().pipe(
          switchMap((authResponse) => next(withRetryContext(withAuthorization(req, authResponse.accessToken || authResponse.token)))),
          catchError((refreshError: HttpErrorResponse) => {
            handleSessionExpiration(authService, notificationService, !isChatRequest(req));
            return throwError(() => refreshError);
          })
        );
      }

      if (error.status === 401 && shouldLogoutOnUnauthorized(req)) {
        handleSessionExpiration(authService, notificationService, !isChatRequest(req));
      }

      if (error.status === 403 && !isChatRequest(req)) {
        notificationService.error('Acces refuse. Vous n avez pas les permissions necessaires.');
      }

      if (error.status === 404 && !isChatRequest(req)) {
        notificationService.error('Ressource non trouvee.');
      }

      if (error.status === 500 && !isChatRequest(req)) {
        notificationService.error('Erreur serveur. Veuillez reessayer plus tard.');
      }

      if (error.status === 0 && !isChatRequest(req)) {
        notificationService.error('Impossible de contacter le serveur. Verifiez votre connexion.');
      }

      return throwError(() => error);
    })
  );
};

function shouldAttachToken(req: HttpRequest<unknown>): boolean {
  return !isAuthRequest(req) && !isPublicSettingsRequest(req);
}

function withAuthorization(req: HttpRequest<unknown>, token: string | null): HttpRequest<unknown> {
  if (!token) {
    return req;
  }

  return req.clone({
    setHeaders: {
      Authorization: `Bearer ${token}`
    }
  });
}

function withRetryContext(req: HttpRequest<unknown>): HttpRequest<unknown> {
  return req.clone({
    context: req.context.set(AUTH_RETRY_CONTEXT, true)
  });
}

function shouldRefreshToken(
  error: HttpErrorResponse,
  req: HttpRequest<unknown>,
  authService: AuthService
): boolean {
  return error.status === 401
    && !isAuthRequest(req)
    && !req.context.get(AUTH_RETRY_CONTEXT)
    && !!authService.getRefreshToken();
}

function shouldLogoutOnUnauthorized(req: HttpRequest<unknown>): boolean {
  return !isAuthRequest(req);
}

function isAuthRequest(req: HttpRequest<unknown>): boolean {
  return req.url.includes('/auth/login')
    || req.url.includes('/auth/register')
    || req.url.includes('/auth/refresh-token');
}

function isPublicSettingsRequest(req: HttpRequest<unknown>): boolean {
  return req.url.includes('/api/app/settings/public');
}

function isChatRequest(req: HttpRequest<unknown>): boolean {
  return req.url.includes('/api/chat');
}

function handleSessionExpiration(
  authService: AuthService,
  notificationService: NotificationService,
  notifyUser: boolean
): void {
  authService.logout();

  if (notifyUser && !sessionExpirationHandled) {
    sessionExpirationHandled = true;
    notificationService.error('Session expiree. Veuillez vous reconnecter.');
    setTimeout(() => {
      sessionExpirationHandled = false;
    }, 1000);
  }
}
