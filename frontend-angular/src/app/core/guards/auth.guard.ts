import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { AuthService } from '../services/auth.service';
import { NotificationService } from '../services/notification.service';

export const authGuard: CanActivateFn = (_route, state) => {
  const authService = inject(AuthService);
  const router = inject(Router);

  if (authService.isAuthenticated) {
    if (authService.isAdmin && !state.url.startsWith('/admin')) {
      router.navigate(['/admin/dashboard']);
      return false;
    }
    return true;
  }

  router.navigate(['/auth/login']);
  return false;
};

export const adminGuard: CanActivateFn = () => {
  const authService = inject(AuthService);
  const router = inject(Router);
  const notificationService = inject(NotificationService);

  if (!authService.isAuthenticated) {
    router.navigate(['/auth/login']);
    return false;
  }

  if (authService.isAuthenticated && authService.isAdmin) {
    return true;
  }

  notificationService.error('Accès réservé aux administrateurs.');
  router.navigate(['/dashboard']);
  return false;
};

export const guestGuard: CanActivateFn = () => {
  const authService = inject(AuthService);
  const router = inject(Router);

  if (!authService.isAuthenticated) {
    return true;
  }

  router.navigate([authService.isAdmin ? '/admin/dashboard' : '/dashboard']);
  return false;
};
