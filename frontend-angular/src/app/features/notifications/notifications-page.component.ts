import { CommonModule } from '@angular/common';
import { ChangeDetectionStrategy, Component, computed, inject } from '@angular/core';
import { toSignal } from '@angular/core/rxjs-interop';
import { RouterLink } from '@angular/router';
import { catchError, firstValueFrom, forkJoin, of } from 'rxjs';
import { GeneralNotificationDto } from '../../core/models/admin.models';
import { GeneralNotificationService } from '../../core/services/general-notification.service';
import { Alert } from '../../shared/alerts/alert.model';
import { AlertService } from '../../shared/alerts/alert.service';

type AlertTone = 'critical' | 'warning' | 'info';

interface NotificationAlertVm {
  id: string;
  title: string;
  message: string;
  typeLabel: string;
  severityLabel: string;
  tone: AlertTone;
  metricLabel: string | null;
  generatedAtLabel: string | null;
  actionLabel: string | null;
  actionRoute: string | null;
}

interface GeneralNotificationVm {
  id: number;
  title: string;
  message: string;
  typeLabel: string;
  tone: AlertTone;
  publishedAtLabel: string | null;
  expiresAtLabel: string | null;
}

@Component({
  selector: 'app-notifications-page',
  standalone: true,
  imports: [CommonModule, RouterLink],
  templateUrl: './notifications-page.component.html',
  styleUrl: './notifications-page.component.css',
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class NotificationsPageComponent {
  private readonly alertService = inject(AlertService);
  private readonly generalNotificationService = inject(GeneralNotificationService);
  private readonly amountFormatter = new Intl.NumberFormat('fr-FR', {
    minimumFractionDigits: 0,
    maximumFractionDigits: 0
  });
  private readonly dateFormatter = new Intl.DateTimeFormat('fr-FR', {
    day: 'numeric',
    month: 'short',
    hour: '2-digit',
    minute: '2-digit'
  });

  readonly alerts = toSignal(this.alertService.getAlerts(), { initialValue: [] as Alert[] });
  readonly generalNotifications = toSignal(this.generalNotificationService.notifications$, {
    initialValue: [] as GeneralNotificationDto[]
  });
  readonly loading = toSignal(this.alertService.getLoading(), { initialValue: true });
  readonly errorMessage = toSignal(this.alertService.getError(), { initialValue: null });

  readonly alertCards = computed<NotificationAlertVm[]>(() =>
    [...this.alerts()]
      .sort((left, right) => this.compareAlerts(left, right))
      .map((alert) => this.mapAlert(alert))
  );
  readonly generalNotificationCards = computed<GeneralNotificationVm[]>(() =>
    this.generalNotifications().map((notification) => this.mapGeneralNotification(notification))
  );
  readonly generalNotificationCount = computed(() => this.generalNotificationCards().length);
  readonly criticalCount = computed(() => this.alertCards().filter((alert) => alert.tone === 'critical').length);
  readonly warningCount = computed(() => this.alertCards().filter((alert) => alert.tone === 'warning').length);
  readonly infoCount = computed(() => this.alertCards().filter((alert) => alert.tone === 'info').length);
  readonly hasAlerts = computed(() => this.alertCards().length > 0);
  readonly hasGeneralNotifications = computed(() => this.generalNotificationCards().length > 0);

  constructor() {
    this.reload();
  }

  reload(): void {
    void firstValueFrom(
      forkJoin([
        this.alertService.refreshAlerts().pipe(catchError(() => of([]))),
        this.generalNotificationService.refresh().pipe(catchError(() => of([])))
      ])
    );
  }

  trackAlert(_: number, alert: NotificationAlertVm): string {
    return alert.id;
  }

  trackGeneralNotification(_: number, notification: GeneralNotificationVm): number {
    return notification.id;
  }

  private mapGeneralNotification(notification: GeneralNotificationDto): GeneralNotificationVm {
    return {
      id: notification.id,
      title: notification.title,
      message: notification.message,
      typeLabel: this.resolveGeneralTypeLabel(notification.type),
      tone: this.resolveGeneralTone(notification.type),
      publishedAtLabel: notification.publishedAt ? this.formatGeneratedAt(notification.publishedAt) : null,
      expiresAtLabel: notification.expiresAt ? this.formatGeneratedAt(notification.expiresAt) : null
    };
  }

  private mapAlert(alert: Alert): NotificationAlertVm {
    const tone = this.resolveTone(alert.severity);

    return {
      id: alert.id,
      title: alert.title,
      message: alert.message,
      typeLabel: this.resolveTypeLabel(alert.type),
      severityLabel: this.resolveSeverityLabel(alert.severity),
      tone,
      metricLabel: typeof alert.amount === 'number' ? `${this.amountFormatter.format(Math.round(alert.amount))} ${alert.currency ?? 'DT'}` : null,
      generatedAtLabel: this.formatGeneratedAt(alert.timestamp),
      actionLabel: alert.actionLabel ?? null,
      actionRoute: alert.actionRoute ?? null
    };
  }

  private compareAlerts(left: Alert, right: Alert): number {
    if (left.read !== right.read) {
      return left.read ? 1 : -1;
    }

    const severityGap = this.severityWeight(right.severity) - this.severityWeight(left.severity);
    if (severityGap !== 0) {
      return severityGap;
    }

    return new Date(right.timestamp).getTime() - new Date(left.timestamp).getTime();
  }

  private resolveTone(severity: Alert['severity']): AlertTone {
    if (severity === 'critical') {
      return 'critical';
    }

    if (severity === 'high' || severity === 'medium') {
      return 'warning';
    }

    return 'info';
  }

  private resolveSeverityLabel(severity: Alert['severity']): string {
    switch (severity) {
      case 'critical':
        return 'Critique';
      case 'high':
        return 'Elevee';
      case 'medium':
        return 'A surveiller';
      case 'low':
      default:
        return 'Information';
    }
  }

  private resolveTypeLabel(type: Alert['type']): string {
    switch (type) {
      case 'budget':
        return 'Budget';
      case 'transaction':
        return 'Transaction';
      case 'security':
        return 'Securite';
      case 'success':
        return 'Succes';
      case 'info':
      default:
        return 'Information';
    }
  }

  private resolveGeneralTypeLabel(type: GeneralNotificationDto['type']): string {
    switch (type) {
      case 'MAINTENANCE':
        return 'Maintenance';
      case 'SECURITY':
        return 'Securite';
      case 'FEATURE':
        return 'Nouvelle fonctionnalite';
      case 'WARNING':
        return 'Avertissement';
      case 'INFO':
      default:
        return 'Information';
    }
  }

  private resolveGeneralTone(type: GeneralNotificationDto['type']): AlertTone {
    if (type === 'SECURITY' || type === 'MAINTENANCE') {
      return 'warning';
    }

    return 'info';
  }

  private severityWeight(severity: Alert['severity']): number {
    switch (severity) {
      case 'critical':
        return 4;
      case 'high':
        return 3;
      case 'medium':
        return 2;
      case 'low':
      default:
        return 1;
    }
  }

  private formatGeneratedAt(value: string | Date): string | null {
    const date = value instanceof Date ? value : new Date(value);

    if (Number.isNaN(date.getTime())) {
      return null;
    }

    const formatted = this.dateFormatter.format(date);
    return `${formatted.charAt(0).toUpperCase()}${formatted.slice(1)}`;
  }
}
