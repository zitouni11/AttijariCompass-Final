import { Routes } from '@angular/router';
import { authGuard, adminGuard, guestGuard } from './core/guards/auth.guard';

export const routes: Routes = [
  {
    path: '',
    redirectTo: 'dashboard',
    pathMatch: 'full'
  },
  {
    path: 'auth',
    canActivate: [guestGuard],
    loadChildren: () => import('./features/auth/auth.routes').then(m => m.authRoutes)
  },
  {
    path: 'admin',
    canActivate: [adminGuard],
    loadChildren: () => import('./features/admin/admin.routes').then(m => m.adminRoutes)
  },
  {
    path: 'admin-register',
    loadComponent: () => import('./features/auth/admin-registration/admin-registration.component').then(m => m.AdminRegistrationComponent)
  },
  {
    path: 'admin-verify-email',
    loadComponent: () => import('./features/auth/admin-registration/admin-verify-email.component').then(m => m.AdminVerifyEmailComponent)
  },
  {
    path: 'verify-email',
    loadComponent: () => import('./features/auth/verify-email/verify-email.component').then(m => m.VerifyEmailComponent)
  },
  {
    path: 'restore-account',
    loadComponent: () => import('./features/auth/restore-account/restore-account.component').then(m => m.RestoreAccountComponent)
  },
  {
    path: '',
    canActivate: [authGuard],
    loadComponent: () => import('./shared/components/layout/layout.component').then(m => m.LayoutComponent),
    children: [
      {
        path: 'dashboard',
        loadComponent: () => import('./features/dashboard/dashboard.component').then(m => m.DashboardComponent)
      },
      {
        path: 'transactions',
        loadComponent: () => import('./features/transactions/list/transactions-list.component').then(m => m.TransactionsListComponent)
      },
      {
        path: 'transactions/import',
        redirectTo: 'transactions',
        pathMatch: 'full'
      },
      {
        path: 'transactions/:id/cash-breakdown',
        loadComponent: () => import('./features/transactions/cash-breakdown/cash-breakdown.component').then(m => m.CashBreakdownComponent)
      },
      {
        path: 'transactions/new',
        loadComponent: () => import('./features/transactions/form/transaction-form.component').then(m => m.TransactionFormComponent)
      },
      {
        path: 'transactions/:id/edit',
        loadComponent: () => import('./features/transactions/form/transaction-form.component').then(m => m.TransactionFormComponent)
      },
      {
        path: 'budgets',
        loadComponent: () => import('./features/budgets/budgets-page.component').then(m => m.BudgetsPageComponent)
      },
      {
        path: 'notifications',
        loadComponent: () => import('./features/notifications/notifications-page.component').then(m => m.NotificationsPageComponent)
      },
      {
        path: 'reports',
        loadComponent: () => import('./features/reports/reports-page.component').then(m => m.ReportsPageComponent)
      },
      {
        path: 'goals',
        loadComponent: () => import('./features/goals/list/goals-list.component').then(m => m.GoalsListComponent)
      },
      {
        path: 'goals/new',
        loadComponent: () => import('./features/goals/form/goal-form.component').then(m => m.GoalFormComponent)
      },
      {
        path: 'goals/:id/edit',
        loadComponent: () => import('./features/goals/form/goal-form.component').then(m => m.GoalFormComponent)
      },
      {
        path: 'goals/:id',
        loadComponent: () => import('./features/goals/detail/goal-detail.component').then(m => m.GoalDetailComponent)
      },
      {
        path: 'recommendations',
        loadComponent: () => import('./features/recommendations/recommendations.component').then(m => m.RecommendationsComponent)
      },
      {
        path: 'my-cards',
        loadChildren: () => import('./features/my-cards/my-cards.routes').then(m => m.myCardsRoutes)
      },
      {
        path: 'simulations',
        loadComponent: () => import('./features/simulations/simulations.component').then(m => m.SimulationsComponent)
      },
      {
        path: 'storytelling',
        loadComponent: () => import('./features/storytelling/chat.component').then(m => m.ChatComponent)
      },
      {
        path: 'support',
        loadComponent: () => import('./features/support/support-page.component').then(m => m.SupportPageComponent)
      },
      {
        path: 'users',
        canActivate: [adminGuard],
        loadComponent: () => import('./features/users/users-list.component').then(m => m.UsersListComponent)
      },
      {
        path: 'profile',
        loadComponent: () => import('./features/profile/profile.component').then(m => m.ProfileComponent)
      }
    ]
  },
  {
    path: '**',
    redirectTo: 'dashboard'
  }
];
