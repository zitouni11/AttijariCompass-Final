import { CommonModule, DatePipe } from '@angular/common';
import { Component, OnInit, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { AdminRole, AdminUserDto } from '../../../core/models/admin.models';
import { AdminService } from '../../../core/services/admin.service';
import { AuthService } from '../../../core/services/auth.service';
import { NotificationService } from '../../../core/services/notification.service';

@Component({
  selector: 'app-admin-users',
  standalone: true,
  imports: [CommonModule, FormsModule, DatePipe],
  template: `
    <div class="page">
      <div class="tabs">
        <button type="button" [class.active]="activeTab === 'active'" (click)="setTab('active')">
          Comptes actifs
        </button>
        <button type="button" [class.active]="activeTab === 'deleted'" (click)="setTab('deleted')">
          Comptes supprimes
        </button>
      </div>

      <div class="toolbar">
        <input [(ngModel)]="search" placeholder="Recherche nom ou email" />
        <select [(ngModel)]="roleFilter"><option value="">Tous les roles</option><option>USER</option><option>ADMIN</option></select>
        @if (activeTab === 'active') {
          <select [(ngModel)]="statusFilter"><option value="">Tous les statuts</option><option value="active">Actifs</option><option value="inactive">Inactifs</option></select>
        }
      </div>

      <div class="table-wrap">
        <table>
          <thead>
            <tr>
              <th>Nom</th>
              <th>Email</th>
              <th>Role</th>
              @if (activeTab === 'active') {
                <th>Statut</th>
                <th>Date creation</th>
                <th>Derniere connexion</th>
              } @else {
                <th>Date suppression</th>
                <th>Motif</th>
              }
              <th>Actions</th>
            </tr>
          </thead>
          <tbody>
            @for (user of filteredUsers(); track user.id) {
              <tr>
                <td>{{ user.fullName || '-' }}</td>
                <td>{{ user.email }}</td>
                <td><span class="badge">{{ user.role }}</span></td>
                @if (activeTab === 'active') {
                  <td><span class="badge" [class.ok]="user.active" [class.warn]="!user.active">{{ user.active ? 'Actif' : 'Inactif' }}</span></td>
                  <td>{{ user.createdAt | date:'short' }}</td>
                  <td>{{ user.lastLoginAt ? (user.lastLoginAt | date:'short') : '-' }}</td>
                } @else {
                  <td>{{ user.deletedAt ? (user.deletedAt | date:'short') : '-' }}</td>
                  <td>{{ user.deletionReason || '-' }}</td>
                }
                <td class="actions">
                  @if (activeTab === 'active') {
                    <button type="button" (click)="toggleActive(user)">{{ user.active ? 'Desactiver' : 'Activer' }}</button>
                    <button type="button" (click)="changeRole(user, user.role === 'ADMIN' ? 'USER' : 'ADMIN')">Role {{ user.role === 'ADMIN' ? 'USER' : 'ADMIN' }}</button>
                    @if (!isCurrentUser(user)) {
                      <button type="button" class="danger" (click)="openDeleteModal(user)">Supprimer</button>
                    }
                  } @else {
                    <button type="button" (click)="restore(user)">Restaurer</button>
                  }
                </td>
              </tr>
            }
            @if (!filteredUsers().length) {
              <tr>
                <td class="empty" [attr.colspan]="activeTab === 'active' ? 7 : 6">
                  Aucun compte a afficher.
                </td>
              </tr>
            }
          </tbody>
        </table>
      </div>

      @if (deleteTarget()) {
        <div class="modal-backdrop" (click)="closeDeleteModal()">
          <section class="delete-modal" role="dialog" aria-modal="true" (click)="$event.stopPropagation()">
            <h2>Supprimer ce compte ?</h2>
            <p>
              Cette action desactivera le compte et empechera toute connexion.
              Les donnees financieres ne seront pas supprimees.
            </p>
            <label>
              <span>Motif optionnel</span>
              <textarea [(ngModel)]="deletionReason" rows="4" placeholder="Compte supprime par l administrateur"></textarea>
            </label>
            <div class="modal-actions">
              <button type="button" class="secondary" (click)="closeDeleteModal()">Annuler</button>
              <button type="button" class="danger" (click)="confirmDelete()">Confirmer</button>
            </div>
          </section>
        </div>
      }
    </div>
  `,
  styles: [`
    .page { display: grid; gap: 1rem; }
    .tabs { display: flex; flex-wrap: wrap; gap: .5rem; }
    .tabs button { background: #fff; color: #111; border: 1px solid #ddd; }
    .tabs button.active { background: #f28c28; color: #111; border-color: #f28c28; }
    .toolbar { display: flex; flex-wrap: wrap; gap: .7rem; }
    input, select { min-height: 40px; border: 1px solid #ddd; border-radius: 8px; padding: .55rem .7rem; background: #fff; }
    textarea { width: 100%; border: 1px solid #ddd; border-radius: 8px; padding: .65rem; font: inherit; resize: vertical; }
    .table-wrap { overflow: auto; background: #fff; border: 1px solid #ececf0; border-radius: 8px; }
    table { width: 100%; border-collapse: collapse; min-width: 900px; }
    th, td { padding: .85rem; border-bottom: 1px solid #f1f1f2; text-align: left; font-size: .86rem; }
    th { color: #6b7280; background: #fafafa; }
    .badge { padding: .3rem .55rem; border-radius: 999px; background: #f3f4f6; font-weight: 800; font-size: .75rem; }
    .badge.ok { background: #dcfce7; color: #166534; }
    .badge.warn { background: #fff7ed; color: #c2410c; }
    .actions { display: flex; gap: .45rem; flex-wrap: wrap; }
    button { border: 0; border-radius: 8px; background: #111; color: #fff; padding: .45rem .65rem; cursor: pointer; }
    button.secondary { background: #fff; color: #111; border: 1px solid #ddd; }
    button.danger { background: #c85a4f; color: #fff; }
    .empty { color: #777; text-align: center; }
    .modal-backdrop { position: fixed; inset: 0; display: grid; place-items: center; background: rgba(17,17,17,.45); z-index: 1000; padding: 1rem; }
    .delete-modal { width: min(100%, 480px); display: grid; gap: .85rem; background: #fff; border-radius: 8px; padding: 1.2rem; box-shadow: 0 24px 60px rgba(0,0,0,.22); }
    .delete-modal h2 { margin: 0; }
    .delete-modal p { margin: 0; color: #666; line-height: 1.55; }
    .delete-modal label { display: grid; gap: .4rem; font-weight: 800; }
    .modal-actions { display: flex; justify-content: flex-end; gap: .55rem; }
  `]
})
export class AdminUsersComponent implements OnInit {
  private readonly adminService = inject(AdminService);
  private readonly authService = inject(AuthService);
  private readonly notificationService = inject(NotificationService);
  readonly users = signal<AdminUserDto[]>([]);
  readonly deletedUsers = signal<AdminUserDto[]>([]);
  readonly deleteTarget = signal<AdminUserDto | null>(null);
  search = '';
  roleFilter = '';
  statusFilter = '';
  activeTab: 'active' | 'deleted' = 'active';
  deletionReason = '';

  filteredUsers(): AdminUserDto[] {
    const q = this.search.trim().toLowerCase();
    const source = this.activeTab === 'active' ? this.users() : this.deletedUsers();
    return source.filter(user => {
      const matchesSearch = !q || user.email.toLowerCase().includes(q) || (user.fullName || '').toLowerCase().includes(q);
      const matchesRole = !this.roleFilter || user.role === this.roleFilter;
      const matchesStatus = this.activeTab === 'deleted'
        || !this.statusFilter
        || (this.statusFilter === 'active' ? user.active : !user.active);
      return matchesSearch && matchesRole && matchesStatus;
    });
  }

  ngOnInit(): void { this.load(); }
  load(): void {
    if (this.activeTab === 'active') {
      this.adminService.getUsers().subscribe(users => this.users.set(users));
      return;
    }

    this.adminService.getDeletedUsers().subscribe(users => this.deletedUsers.set(users));
  }
  setTab(tab: 'active' | 'deleted'): void {
    this.activeTab = tab;
    this.search = '';
    this.roleFilter = '';
    this.statusFilter = '';
    this.load();
  }
  toggleActive(user: AdminUserDto): void {
    const request = user.active ? this.adminService.deactivateUser(user.id) : this.adminService.activateUser(user.id);
    request.subscribe(() => this.load());
  }
  changeRole(user: AdminUserDto, role: AdminRole): void {
    this.adminService.changeUserRole(user.id, role).subscribe(() => this.load());
  }
  openDeleteModal(user: AdminUserDto): void {
    this.deleteTarget.set(user);
    this.deletionReason = '';
  }
  closeDeleteModal(): void {
    this.deleteTarget.set(null);
    this.deletionReason = '';
  }
  confirmDelete(): void {
    const target = this.deleteTarget();
    if (!target) {
      return;
    }

    this.adminService.deleteUser(target.id, this.deletionReason).subscribe({
      next: (response) => {
        this.closeDeleteModal();
        this.notificationService.success(response.message || 'Compte supprime avec succes.');
        this.load();
      },
      error: (error) => {
        this.notificationService.error(error.error?.message || 'Impossible de supprimer le compte.');
      }
    });
  }
  restore(user: AdminUserDto): void {
    this.adminService.restoreUser(user.id).subscribe({
      next: (response) => {
        this.notificationService.success(response.message || 'Compte restaure avec succes.');
        this.load();
      },
      error: (error) => {
        this.notificationService.error(error.error?.message || 'Impossible de restaurer le compte.');
      }
    });
  }
  isCurrentUser(user: AdminUserDto): boolean {
    return this.authService.currentUser?.email === user.email;
  }
}
