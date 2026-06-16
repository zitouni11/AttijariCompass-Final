import { CommonModule, DatePipe } from '@angular/common';
import { Component, OnInit, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { GeneralNotificationDto, GeneralNotificationRequest, GeneralNotificationType, NotificationTargetRole } from '../../../core/models/admin.models';
import { AdminService } from '../../../core/services/admin.service';
import { NotificationService } from '../../../core/services/notification.service';

@Component({
  selector: 'app-admin-notifications',
  standalone: true,
  imports: [CommonModule, FormsModule, DatePipe],
  template: `
    <div class="grid">
      <form class="form" (ngSubmit)="save()">
        <h2>{{ editingNotification ? 'Modifier notification' : 'Creer notification' }}</h2>
        <input name="title" [(ngModel)]="form.title" placeholder="Titre" required />
        <textarea name="message" [(ngModel)]="form.message" placeholder="Message" rows="5" required></textarea>
        <div class="row">
          <select name="type" [(ngModel)]="form.type"><option>INFO</option><option>WARNING</option><option>MAINTENANCE</option><option>SECURITY</option><option>FEATURE</option></select>
          <select name="targetRole" [(ngModel)]="form.targetRole"><option>ALL</option><option>USER</option><option>ADMIN</option></select>
        </div>
        <input name="expiresAt" [(ngModel)]="expiresAt" type="datetime-local" />
        <div class="actions"><button type="submit">Enregistrer</button><button type="button" (click)="reset()">Nouveau</button></div>
      </form>
      <div class="table-wrap">
        <table>
          <thead><tr><th>Titre</th><th>Type</th><th>Cible</th><th>Statut</th><th>Visible user</th><th>Publication</th><th>Expiration</th><th>Actions</th></tr></thead>
          <tbody>
            @for (notification of notifications(); track notification.id) {
              <tr>
                <td>{{ notification.title }}</td>
                <td>{{ notification.type }}</td>
                <td>{{ notification.targetRole }}</td>
                <td>
                  <span class="badge" [class.ok]="statusLabel(notification) === 'Publiee'" [class.warn]="statusLabel(notification) === 'Expiree'">
                    {{ statusLabel(notification) }}
                  </span>
                </td>
                <td>
                  <span class="badge" [class.ok]="isVisibleForUser(notification)">
                    {{ isVisibleForUser(notification) ? 'Oui' : 'Non' }}
                  </span>
                </td>
                <td>{{ notification.publishedAt ? (notification.publishedAt | date:'short') : '-' }}</td>
                <td>{{ notification.expiresAt ? (notification.expiresAt | date:'short') : '-' }}</td>
                <td class="actions">
                  <button type="button" (click)="edit(notification)">Modifier</button>
                  <button type="button" (click)="publish(notification.id)">Publier</button>
                  <button type="button" (click)="disable(notification.id)">Desactiver</button>
                  <button type="button" class="danger" (click)="delete(notification.id)">Supprimer</button>
                </td>
              </tr>
            }
          </tbody>
        </table>
      </div>
    </div>
  `,
  styles: [`
    .grid { display: grid; grid-template-columns: 340px minmax(0, 1fr); gap: 1rem; align-items: start; }
    .form, .table-wrap { background: #fff; border: 1px solid #ececf0; border-radius: 8px; }
    .form { padding: 1rem; display: grid; gap: .7rem; }
    h2 { margin: 0; font-size: 1rem; }
    input, textarea, select { border: 1px solid #ddd; border-radius: 8px; padding: .6rem; width: 100%; }
    .row, .actions { display: flex; gap: .5rem; }
    .table-wrap { overflow: auto; }
    table { width: 100%; border-collapse: collapse; min-width: 1000px; }
    th, td { padding: .8rem; border-bottom: 1px solid #f1f1f2; text-align: left; font-size: .84rem; }
    th { background: #fafafa; color: #6b7280; }
    button { border: 0; border-radius: 8px; background: #111; color: #fff; padding: .45rem .6rem; cursor: pointer; }
    .danger { background: #b91c1c; }
    .badge { padding: .3rem .55rem; border-radius: 999px; background: #f3f4f6; font-weight: 800; font-size: .72rem; }
    .badge.ok { background: #dcfce7; color: #166534; }
    .badge.warn { background: #fff1e6; color: #c65d00; }
    @media (max-width: 1100px) { .grid { grid-template-columns: 1fr; } }
  `]
})
export class AdminNotificationsComponent implements OnInit {
  private readonly adminService = inject(AdminService);
  private readonly notificationService = inject(NotificationService);
  readonly notifications = signal<GeneralNotificationDto[]>([]);
  editingNotification: GeneralNotificationDto | null = null;
  expiresAt = '';
  form: GeneralNotificationRequest = { title: '', message: '', type: 'INFO', targetRole: 'ALL', expiresAt: null };

  ngOnInit(): void { this.load(); }

  load(): void {
    this.adminService.getNotifications().subscribe({
      next: items => this.notifications.set(items),
      error: error => this.notificationService.error(error.error?.message || 'Impossible de charger les notifications.')
    });
  }

  save(): void {
    const request = { ...this.form, expiresAt: this.formatExpiresAt(this.expiresAt) };
    if (request.expiresAt && this.isPastDate(request.expiresAt)) {
      this.notificationService.error('Cette notification est expiree et ne sera pas visible cote utilisateur.');
      if (!window.confirm('Cette notification est expiree et ne sera pas visible cote utilisateur. Enregistrer quand meme ?')) {
        return;
      }
    }
    const editingId = this.editingNotification?.id;
    const call = editingId
      ? this.adminService.updateNotification(editingId, request)
      : this.adminService.createNotification(request);

    call.subscribe({
      next: () => {
        this.notificationService.success(editingId ? 'Notification mise a jour.' : 'Notification creee.');
        this.reset();
        this.load();
      },
      error: error => {
        const message = error.status === 404
          ? 'Notification introuvable ou deja supprimee.'
          : error.error?.message || 'Impossible d enregistrer la notification.';
        this.notificationService.error(message);
        if (error.status === 404) {
          this.reset();
          this.load();
        }
      }
    });
  }

  edit(notification: GeneralNotificationDto): void {
    if (!notification?.id) {
      this.notificationService.error('Notification introuvable ou deja supprimee.');
      return;
    }
    this.editingNotification = notification;
    this.form = { title: notification.title, message: notification.message, type: notification.type as GeneralNotificationType, targetRole: notification.targetRole as NotificationTargetRole, expiresAt: notification.expiresAt || null };
    this.expiresAt = notification.expiresAt ? notification.expiresAt.slice(0, 16) : '';
  }

  reset(): void {
    this.editingNotification = null;
    this.expiresAt = '';
    this.form = { title: '', message: '', type: 'INFO', targetRole: 'ALL', expiresAt: null };
  }

  publish(id: number): void {
    this.adminService.publishNotification(id).subscribe({
      next: () => this.load(),
      error: error => this.notificationService.error(error.error?.message || 'Impossible de publier la notification.')
    });
  }

  disable(id: number): void {
    this.adminService.disableNotification(id).subscribe({
      next: () => this.load(),
      error: error => this.notificationService.error(error.error?.message || 'Impossible de desactiver la notification.')
    });
  }

  delete(id: number): void {
    this.adminService.deleteNotification(id).subscribe({
      next: () => {
        if (this.editingNotification?.id === id) {
          this.reset();
        }
        this.load();
      },
      error: error => this.notificationService.error(error.error?.message || 'Impossible de supprimer la notification.')
    });
  }

  private formatExpiresAt(value: string): string | null {
    if (!value) {
      return null;
    }
    return value.length === 16 ? `${value}:00` : value;
  }

  statusLabel(notification: GeneralNotificationDto): string {
    if (notification.expiresAt && this.isPastDate(notification.expiresAt)) {
      return 'Expiree';
    }
    if (!notification.publishedAt) {
      return 'Brouillon';
    }
    return notification.active ? 'Publiee' : 'Desactivee';
  }

  isVisibleForUser(notification: GeneralNotificationDto): boolean {
    return Boolean(notification.active)
      && Boolean(notification.publishedAt)
      && new Date(notification.publishedAt!).getTime() <= Date.now()
      && (notification.targetRole === 'USER' || notification.targetRole === 'ALL')
      && (!notification.expiresAt || !this.isPastDate(notification.expiresAt));
  }

  private isPastDate(value: string): boolean {
    const date = new Date(value);
    return !Number.isNaN(date.getTime()) && date.getTime() < Date.now();
  }
}
