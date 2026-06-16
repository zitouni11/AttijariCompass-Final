import { CommonModule } from '@angular/common';
import { Component, OnDestroy, OnInit, inject } from '@angular/core';
import {
  animate,
  style,
  transition,
  trigger
} from '@angular/animations';
import { Subscription, firstValueFrom } from 'rxjs';
import { ALERT_ICONS, Alert, AlertType } from '../alert.model';
import { AlertService } from '../alert.service';

@Component({
  selector: 'app-toast',
  standalone: true,
  imports: [CommonModule],
  template: `
    <div class="toast-stack">
      <div
        *ngFor="let alert of visibleToasts; trackBy: trackById"
        class="toast-item"
        [@toastAnim]
        [class.severity-low]="alert.severity === 'low'"
        [class.severity-medium]="alert.severity === 'medium'"
        [class.severity-high]="alert.severity === 'high'"
        [class.severity-critical]="alert.severity === 'critical'"
        (click)="dismiss(alert.id)"
      >
        <div class="toast-icon">{{ getIcon(alert.type) }}</div>

        <div class="toast-body">
        <div class="toast-head">
          <strong>{{ alert.title }}</strong>
          <button type="button" aria-label="Fermer la notification" (click)="dismiss(alert.id); $event.stopPropagation()">&times;</button>
        </div>
        <p>{{ alert.message }}</p>
          <span *ngIf="alert.amount !== undefined" class="toast-amount">
            {{ alert.amount | number:'1.0-0' }} {{ alert.currency || 'DT' }}
          </span>
        </div>

        <div class="toast-progress" [class]="'toast-progress severity-' + alert.severity"></div>
      </div>
    </div>
  `,
  styles: [`
    :host {
      position: fixed;
      right: 24px;
      bottom: 24px;
      z-index: 1200;
      pointer-events: none;
    }

    .toast-stack {
      display: grid;
      gap: 0.8rem;
      justify-items: end;
    }

    .toast-item {
      position: relative;
      display: grid;
      grid-template-columns: auto minmax(0, 1fr);
      gap: 0.85rem;
      width: min(360px, calc(100vw - 32px));
      padding: 0.95rem 1rem 1.05rem;
      border-left: 4px solid #e8621a;
      border-radius: 14px;
      background: linear-gradient(180deg, rgba(41, 23, 0, 0.97), rgba(24, 13, 0, 0.97));
      color: #fff4ea;
      box-shadow: 0 22px 40px rgba(0, 0, 0, 0.28);
      backdrop-filter: blur(14px);
      cursor: pointer;
      pointer-events: auto;
    }

    .toast-icon {
      width: 38px;
      height: 38px;
      display: inline-flex;
      align-items: center;
      justify-content: center;
      border-radius: 12px;
      background: rgba(255, 255, 255, 0.08);
      font-size: 1.1rem;
    }

    .toast-body {
      min-width: 0;
    }

    .toast-item.severity-low {
      border-left-color: #77d69b;
    }

    .toast-item.severity-medium {
      border-left-color: #f3c05a;
    }

    .toast-item.severity-high {
      border-left-color: #ff9352;
    }

    .toast-item.severity-critical {
      border-left-color: #ff5a5a;
    }

    .toast-head {
      display: flex;
      align-items: center;
      justify-content: space-between;
      gap: 0.75rem;
    }

    .toast-head strong {
      font-size: 0.92rem;
      font-weight: 700;
    }

    .toast-head button {
      width: 26px;
      height: 26px;
      border: none;
      border-radius: 50%;
      background: rgba(255, 255, 255, 0.08);
      color: #fff4ea;
      cursor: pointer;
    }

    .toast-item p {
      margin: 0.45rem 0 0;
      color: rgba(255, 240, 228, 0.82);
      font-size: 0.79rem;
      line-height: 1.5;
    }

    .toast-amount {
      display: inline-block;
      margin-top: 0.5rem;
      color: #ffd3bb;
      font-size: 0.79rem;
      font-weight: 700;
    }

    .toast-progress {
      position: absolute;
      left: 0;
      bottom: 0;
      height: 3px;
      width: 100%;
      border-radius: 0 0 14px 14px;
      animation: toastProgress 5s linear forwards;
    }

    .toast-progress.severity-low {
      background: #77d69b;
    }

    .toast-progress.severity-medium {
      background: #f3c05a;
    }

    .toast-progress.severity-high {
      background: #ff9352;
    }

    .toast-progress.severity-critical {
      background: #ff5a5a;
    }

    @keyframes toastProgress {
      from {
        width: 100%;
      }

      to {
        width: 0;
      }
    }

    @media (max-width: 640px) {
      :host {
        right: 14px;
        bottom: 14px;
      }
    }
  `],
  animations: [
    trigger('toastAnim', [
      transition(':enter', [
        style({ opacity: 0, transform: 'translateX(32px)' }),
        animate('220ms ease-out', style({ opacity: 1, transform: 'translateX(0)' }))
      ]),
      transition(':leave', [
        animate('180ms ease-in', style({ opacity: 0, transform: 'translateX(24px)' }))
      ])
    ])
  ]
})
export class ToastComponent implements OnInit, OnDestroy {
  private readonly alertService = inject(AlertService);
  private readonly timers = new Map<string, ReturnType<typeof setTimeout>>();
  private subscription?: Subscription;

  visibleToasts: Alert[] = [];

  ngOnInit(): void {
    this.subscription = this.alertService.getNewAlerts().subscribe((alerts) => {
      alerts.forEach((alert) => {
        if (!alert.read) {
          this.pushToast(alert);
        }
      });
    });

    void firstValueFrom(this.alertService.getAlerts()).then((alerts) => {
      const currentIds = new Set(alerts.map((alert) => alert.id));
      this.visibleToasts = this.visibleToasts.filter((alert) => currentIds.has(alert.id));
    });

    this.subscription.add(this.alertService.getAlerts().subscribe((alerts) => {
      const currentIds = new Set(alerts.map((alert) => alert.id));
      this.visibleToasts = this.visibleToasts.filter((alert) => currentIds.has(alert.id));
    }));
  }

  ngOnDestroy(): void {
    this.subscription?.unsubscribe();
    this.timers.forEach((timer) => clearTimeout(timer));
    this.timers.clear();
  }

  dismiss(id: string): void {
    this.visibleToasts = this.visibleToasts.filter((alert) => alert.id !== id);
    void firstValueFrom(this.alertService.markAsRead(id));

    const timer = this.timers.get(id);
    if (timer) {
      clearTimeout(timer);
      this.timers.delete(id);
    }
  }

  trackById(_: number, alert: Alert): string {
    return alert.id;
  }

  getIcon(type: AlertType): string {
    return ALERT_ICONS[type] ?? '🔔';
  }

  private pushToast(alert: Alert): void {
    if (this.visibleToasts.some((item) => item.id === alert.id)) {
      return;
    }

    this.visibleToasts = [alert, ...this.visibleToasts];
    const timer = setTimeout(() => {
      this.dismiss(alert.id);
    }, 5000);

    this.timers.set(alert.id, timer);
  }
}
