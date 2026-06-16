import { Routes } from '@angular/router';

export const adminRoutes: Routes = [
  {
    path: '',
    loadComponent: () => import('./layout/admin-layout.component').then(m => m.AdminLayoutComponent),
    children: [
      { path: '', redirectTo: 'dashboard', pathMatch: 'full' },
      { path: 'dashboard', loadComponent: () => import('./pages/admin-dashboard.component').then(m => m.AdminDashboardComponent) },
      { path: 'users', loadComponent: () => import('./pages/admin-users.component').then(m => m.AdminUsersComponent) },
      { path: 'admin-requests', loadComponent: () => import('./pages/admin-registration-requests.component').then(m => m.AdminRegistrationRequestsComponent) },
      { path: 'account-restore-requests', loadComponent: () => import('./pages/admin-account-restore-requests.component').then(m => m.AdminAccountRestoreRequestsComponent) },
      { path: 'support', loadComponent: () => import('./pages/admin-support.component').then(m => m.AdminSupportComponent) },
      { path: 'notifications', loadComponent: () => import('./pages/admin-notifications.component').then(m => m.AdminNotificationsComponent) },
      { path: 'audit-logs', loadComponent: () => import('./pages/admin-audit-logs.component').then(m => m.AdminAuditLogsComponent) },
      { path: 'technical', loadComponent: () => import('./pages/admin-technical.component').then(m => m.AdminTechnicalComponent) },
      { path: 'settings', loadComponent: () => import('./pages/admin-settings.component').then(m => m.AdminSettingsComponent) }
    ]
  }
];
