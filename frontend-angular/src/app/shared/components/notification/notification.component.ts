import { Component, inject } from '@angular/core';
import { CommonModule, AsyncPipe } from '@angular/common';
import { NotificationService } from '../../../core/services/notification.service';
import { Notification } from '../../../core/models';

@Component({
  selector: 'app-notification',
  standalone: true,
  imports: [CommonModule, AsyncPipe],
  template: `
    <div class="notifications-container">
      @for (notif of notificationService.notifications$ | async; track notif.id) {
        <div class="notification" [class]="'notification-' + notif.type" (click)="remove(notif)">
          <span class="notif-icon">{{ getIcon(notif.type) }}</span>
          <span class="notif-message">{{ notif.message }}</span>
          <button class="notif-close">✕</button>
        </div>
      }
    </div>
  `,
  styles: [`
    .notifications-container {
      position: fixed;
      top: 1.5rem;
      right: 1.5rem;
      z-index: 10000;
      display: flex;
      flex-direction: column;
      gap: 0.5rem;
      max-width: 380px;
    }
    .notification {
      display: flex;
      align-items: center;
      gap: 0.75rem;
      padding: 0.875rem 1rem;
      border-radius: 16px;
      box-shadow: 0 14px 32px rgba(17,17,17,0.12);
      cursor: pointer;
      animation: slideIn 0.3s ease;
      font-family: 'Sora', sans-serif;
      font-size: 0.875rem;
      border-left: 4px solid transparent;
      background: var(--attijari-white);
      border: 1px solid var(--attijari-border);
    }
    @keyframes slideIn {
      from { transform: translateX(120%); opacity: 0; }
      to { transform: translateX(0); opacity: 1; }
    }
    .notification-success { background: var(--attijari-success-soft); border-left-color: var(--attijari-success); color: var(--attijari-success); }
    .notification-error { background: var(--attijari-danger-soft); border-left-color: var(--attijari-danger); color: var(--attijari-danger); }
    .notification-warning { background: var(--attijari-orange-soft); border-left-color: var(--attijari-orange-dark); color: var(--attijari-orange-dark); }
    .notification-info { background: var(--attijari-info-soft); border-left-color: var(--attijari-anthracite); color: var(--attijari-info); }
    .notif-icon { font-size: 1.1rem; flex-shrink: 0; }
    .notif-message { flex: 1; line-height: 1.4; }
    .notif-close { background: none; border: none; cursor: pointer; opacity: 0.5; font-size: 0.8rem; }
    .notif-close:hover { opacity: 1; }
  `]
})
export class NotificationComponent {
  notificationService = inject(NotificationService);

  getIcon(type: Notification['type']): string {
    const icons = { success: '✅', error: '❌', warning: '⚠️', info: 'ℹ️' };
    return icons[type];
  }

  remove(notif: Notification): void {
    this.notificationService.remove(notif.id);
  }
}
