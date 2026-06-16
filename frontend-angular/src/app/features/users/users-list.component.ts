import { Component, inject, OnInit, signal, computed } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { UserService } from '../../core/services/api.services';
import { NotificationService } from '../../core/services/notification.service';
import { UserResponse } from '../../core/models';

@Component({
  selector: 'app-users-list',
  standalone: true,
  imports: [CommonModule, FormsModule],
  template: `
    <div class="page">
      <div class="page-header">
        <div>
          <h1 class="page-title">👥 Gestion des Utilisateurs</h1>
          <p class="page-subtitle">{{ users().length }} utilisateur(s) enregistré(s)</p>
        </div>
        <div class="header-badges">
          <span class="badge-admin">🛡️ ADMIN</span>
        </div>
      </div>

      <div class="filters-bar">
        <div class="search-wrapper">
          <span>🔍</span>
          <input type="text" [(ngModel)]="searchQuery" placeholder="Rechercher par email..." class="search-input" />
        </div>
        <select [(ngModel)]="filterRole" class="filter-select">
          <option value="">Tous les rôles</option>
          <option value="USER">Utilisateur</option>
          <option value="ADMIN">Administrateur</option>
        </select>
      </div>

      <div class="table-card">
        @if (loading()) {
          @for (i of [1,2,3,4,5]; track i) {
            <div class="skeleton-row"></div>
          }
        } @else if (filtered().length === 0) {
          <div class="empty-state">
            <span>👥</span>
            <p>Aucun utilisateur trouvé</p>
          </div>
        } @else {
          <div class="table-wrapper">
            <table>
              <thead>
                <tr>
                  <th>#</th>
                  <th>Email</th>
                  <th>Rôle</th>
                  <th>Inscription</th>
                  <th>Actions</th>
                </tr>
              </thead>
              <tbody>
                @for (user of filtered(); track user.id) {
                  <tr>
                    <td class="id-cell">{{ user.id }}</td>
                    <td>
                      <div class="user-cell">
                        <div class="user-avatar">{{ user.email.charAt(0).toUpperCase() }}</div>
                        <span>{{ user.email }}</span>
                      </div>
                    </td>
                    <td>
                      <span class="role-badge" [class]="'role-' + user.role.toLowerCase()">
                        {{ user.role === 'ADMIN' ? '🛡️ Admin' : '👤 User' }}
                      </span>
                    </td>
                    <td>{{ formatDate(user.createdAt) }}</td>
                    <td>
                      <button class="btn-delete" (click)="deleteUser(user)" title="Supprimer">
                        🗑️ Supprimer
                      </button>
                    </td>
                  </tr>
                }
              </tbody>
            </table>
          </div>
          <div class="table-footer">
            <span class="footer-info">{{ filtered().length }} résultat(s)</span>
            <div class="role-stats">
              <span class="stat-pill admin">{{ adminCount() }} admin(s)</span>
              <span class="stat-pill user">{{ userCount() }} user(s)</span>
            </div>
          </div>
        }
      </div>
    </div>
  `,
  styles: [`
    .page { font-family: 'Sora', sans-serif; }
    .page-header { display: flex; justify-content: space-between; align-items: flex-start; margin-bottom: 1.5rem; }
    .page-title { font-size: 1.75rem; font-weight: 800; color: var(--attijari-black); margin: 0 0 0.25rem; }
    .page-subtitle { color: var(--attijari-muted); font-size: 0.875rem; margin: 0; }
    .badge-admin { background: #fdf0ed; color: #c85a4f; padding: 4px 10px; border-radius: 8px; font-size: 0.78rem; font-weight: 700; }
    .filters-bar { display: flex; gap: 0.75rem; align-items: center; margin-bottom: 1rem; }
    .search-wrapper { display: flex; align-items: center; gap: 0.5rem; padding: 0.5rem 0.875rem; background: var(--attijari-white); border: 1.5px solid var(--attijari-border); border-radius: 10px; flex: 1; }
    .search-input { border: none; outline: none; font-size: 0.875rem; font-family: inherit; width: 100%; }
    .filter-select { padding: 0.5rem 0.875rem; border: 1.5px solid var(--attijari-border); border-radius: 10px; font-size: 0.875rem; font-family: inherit; background: var(--attijari-white); outline: none; }
    .table-card { background: var(--attijari-white); border-radius: 16px; box-shadow: 0 12px 26px rgba(17,17,17,0.06); border: 1px solid var(--attijari-border); overflow: hidden; }
    .table-wrapper { overflow-x: auto; }
    table { width: 100%; border-collapse: collapse; }
    thead { background: var(--attijari-offwhite); }
    th { padding: 0.875rem 1rem; font-size: 0.75rem; font-weight: 700; color: var(--attijari-muted); text-transform: uppercase; letter-spacing: 0.04em; text-align: left; border-bottom: 1px solid var(--attijari-border); }
    td { padding: 0.875rem 1rem; border-bottom: 1px solid var(--attijari-offwhite); font-size: 0.875rem; color: var(--attijari-text); }
    tr:last-child td { border-bottom: none; }
    tr:hover td { background: #fcfaf8; }
    .id-cell { color: var(--attijari-muted); font-size: 0.78rem; font-weight: 600; }
    .user-cell { display: flex; align-items: center; gap: 0.75rem; }
    .user-avatar { width: 32px; height: 32px; border-radius: 50%; background: linear-gradient(135deg, var(--attijari-black), var(--attijari-anthracite)); color: var(--attijari-orange); display: flex; align-items: center; justify-content: center; font-weight: 800; font-size: 0.8rem; flex-shrink: 0; }
    .role-badge { display: inline-block; padding: 3px 10px; border-radius: 8px; font-size: 0.75rem; font-weight: 700; }
    .role-badge.role-admin { background: #fdf0ed; color: #c85a4f; }
    .role-badge.role-user { background: var(--attijari-orange-soft); color: var(--attijari-orange-dark); }
    .btn-delete { display: inline-flex; align-items: center; gap: 0.3rem; padding: 4px 10px; background: var(--attijari-white); border: 1.5px solid #f1b5ab; border-radius: 8px; color: #c85a4f; font-size: 0.78rem; font-weight: 600; cursor: pointer; font-family: inherit; }
    .btn-delete:hover { background: #fef2f2; }
    .skeleton-row { height: 56px; background: linear-gradient(90deg, #efe9e4 25%, #f8f5f2 50%, #efe9e4 75%); background-size: 200%; animation: shimmer 1.5s infinite; border-bottom: 1px solid var(--attijari-border); }
    @keyframes shimmer { from { background-position: 200%; } to { background-position: -200%; } }
    .empty-state { display: flex; flex-direction: column; align-items: center; gap: 0.75rem; padding: 3rem; text-align: center; color: var(--attijari-muted); }
    .empty-state span { font-size: 2.5rem; }
    .table-footer { padding: 1rem 1.5rem; border-top: 1px solid var(--attijari-border); background: var(--attijari-offwhite); display: flex; justify-content: space-between; align-items: center; }
    .footer-info { font-size: 0.8rem; color: var(--attijari-muted); }
    .role-stats { display: flex; gap: 0.5rem; }
    .stat-pill { padding: 3px 10px; border-radius: 8px; font-size: 0.75rem; font-weight: 600; }
    .stat-pill.admin { background: #fdf0ed; color: #c85a4f; }
    .stat-pill.user { background: var(--attijari-orange-soft); color: var(--attijari-orange-dark); }
  `]
})
export class UsersListComponent implements OnInit {
  private userService = inject(UserService);
  private notifService = inject(NotificationService);

  users = signal<UserResponse[]>([]);
  loading = signal(true);
  searchQuery = '';
  filterRole = '';

  filtered = computed(() => this.users().filter(u => {
    const ms = !this.searchQuery || u.email.toLowerCase().includes(this.searchQuery.toLowerCase());
    const mr = !this.filterRole || u.role === this.filterRole;
    return ms && mr;
  }));

  adminCount = computed(() => this.users().filter(u => u.role === 'ADMIN').length);
  userCount = computed(() => this.users().filter(u => u.role === 'USER').length);

  ngOnInit(): void {
    this.userService.getAll().subscribe({
      next: (data) => { this.users.set(data); this.loading.set(false); },
      error: () => this.loading.set(false)
    });
  }

  deleteUser(user: UserResponse): void {
    if (!confirm(`Supprimer l'utilisateur "${user.email}" ?`)) return;
    this.userService.delete(user.id).subscribe({
      next: () => { this.users.update(list => list.filter(u => u.id !== user.id)); this.notifService.success('Utilisateur supprimé'); },
      error: () => this.notifService.error('Erreur lors de la suppression')
    });
  }

  formatDate(date: string): string {
    return new Date(date).toLocaleDateString('fr-FR', { day: '2-digit', month: 'short', year: 'numeric' });
  }
}
