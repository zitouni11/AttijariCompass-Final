import { CommonModule } from '@angular/common';
import { Component, inject } from '@angular/core';
import { RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';
import { AuthService } from '../../../core/services/auth.service';

@Component({
  selector: 'app-admin-layout',
  standalone: true,
  imports: [CommonModule, RouterOutlet, RouterLink, RouterLinkActive],
  template: `
    <div class="admin-shell">
      <aside class="sidebar">
        <div class="brand">
          <img src="assets/branding/logo-attijari-icon-transparent.svg" alt="Attijari Compass" />
          <div>
            <strong>Attijari Compass</strong>
            <span>Back Office</span>
          </div>
        </div>

        <nav>
          @for (item of navItems; track item.path) {
            <a [routerLink]="item.path" routerLinkActive="active">
              <span class="material-symbols-rounded">{{ item.icon }}</span>
              {{ item.label }}
            </a>
          }
        </nav>
      </aside>

      <main class="workspace">
        <header class="topbar">
          <div>
            <span class="eyebrow">Administration</span>
            <h1>Gestion plateforme</h1>
          </div>
          <div class="admin-profile">
            <div>
              <strong>{{ auth.currentUser?.fullName || auth.currentUser?.email }}</strong>
              <span>{{ auth.currentUser?.email }}</span>
            </div>
            <button type="button" (click)="auth.logout()">
              <span class="material-symbols-rounded">logout</span>
              Logout
            </button>
          </div>
        </header>

        <section class="content">
          <router-outlet />
        </section>
      </main>
    </div>
  `,
  styles: [`
    :host { display: block; min-height: 100vh; background: #f7f7f8; color: #171717; font-family: 'Sora', sans-serif; }
    .admin-shell { min-height: 100vh; display: grid; grid-template-columns: 280px minmax(0, 1fr); }
    .sidebar { background: #111; color: #fff; padding: 1.2rem; display: flex; flex-direction: column; gap: 1.4rem; }
    .brand { display: flex; align-items: center; gap: .85rem; padding: .35rem .2rem 1rem; border-bottom: 1px solid rgba(255,255,255,.12); }
    .brand img { width: 52px; height: 52px; object-fit: contain; flex: 0 0 52px; filter: drop-shadow(0 8px 18px rgba(245,140,40,.22)); }
    .brand strong, .brand span { display: block; }
    .brand span { color: #f28c28; font-size: .78rem; margin-top: .2rem; }
    nav { display: grid; gap: .35rem; }
    nav a { min-height: 44px; display: flex; align-items: center; gap: .75rem; padding: .7rem .8rem; color: rgba(255,255,255,.78); text-decoration: none; border-radius: 8px; font-size: .9rem; }
    nav a.active, nav a:hover { background: #f28c28; color: #111; }
    .workspace { min-width: 0; display: flex; flex-direction: column; }
    .topbar { min-height: 78px; padding: 1rem 1.5rem; background: #fff; border-bottom: 1px solid #ececf0; display: flex; align-items: center; justify-content: space-between; gap: 1rem; }
    .eyebrow { color: #f28c28; font-weight: 800; text-transform: uppercase; font-size: .72rem; letter-spacing: .08em; }
    h1 { margin: .15rem 0 0; font-size: 1.25rem; }
    .admin-profile { display: flex; align-items: center; gap: 1rem; text-align: right; }
    .admin-profile span { display: block; color: #777; font-size: .78rem; }
    button { border: 0; border-radius: 8px; background: #111; color: #fff; padding: .7rem .85rem; font-weight: 800; display: inline-flex; gap: .45rem; align-items: center; cursor: pointer; }
    .content { padding: 1.5rem; }
    :host, .admin-shell, .admin-shell * { cursor: auto; }
    .admin-shell button,
    .admin-shell a,
    .admin-shell [role="button"],
    .admin-shell input[type="checkbox"],
    .admin-shell select {
      cursor: pointer;
    }
    .admin-shell input:not([type="checkbox"]),
    .admin-shell textarea {
      cursor: text;
    }
    .admin-shell button:disabled,
    .admin-shell [aria-disabled="true"] {
      cursor: not-allowed;
    }
    @media (max-width: 900px) {
      .admin-shell { grid-template-columns: 1fr; }
      .sidebar { position: static; }
      nav { grid-template-columns: repeat(auto-fit, minmax(180px, 1fr)); }
      .topbar { align-items: flex-start; flex-direction: column; }
      .admin-profile { width: 100%; justify-content: space-between; text-align: left; }
    }
  `]
})
export class AdminLayoutComponent {
  readonly auth = inject(AuthService);
  readonly navItems = [
    { path: '/admin/dashboard', icon: 'dashboard', label: 'Dashboard' },
    { path: '/admin/users', icon: 'group', label: 'Comptes utilisateurs' },
    { path: '/admin/admin-requests', icon: 'admin_panel_settings', label: 'Demandes admins' },
    { path: '/admin/account-restore-requests', icon: 'restore', label: 'Demandes de restauration' },
    { path: '/admin/support', icon: 'support_agent', label: 'Demandes support' },
    { path: '/admin/notifications', icon: 'campaign', label: 'Notifications generales' },
    { path: '/admin/audit-logs', icon: 'fact_check', label: 'Logs & audit' },
    { path: '/admin/technical', icon: 'monitor_heart', label: 'Supervision technique' },
    { path: '/admin/settings', icon: 'settings', label: 'Parametres application' }
  ];
}
