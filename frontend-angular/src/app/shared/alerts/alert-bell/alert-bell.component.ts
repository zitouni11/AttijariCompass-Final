import { CommonModule } from '@angular/common';
import { ChangeDetectionStrategy, Component, ElementRef, HostListener, OnDestroy, OnInit, inject } from '@angular/core';
import {
  animate,
  style,
  transition,
  trigger
} from '@angular/animations';
import { Router } from '@angular/router';
import { BehaviorSubject, catchError, combineLatest, firstValueFrom, map, of } from 'rxjs';
import { GeneralNotificationDto } from '../../../core/models/admin.models';
import { GeneralNotificationService } from '../../../core/services/general-notification.service';
import { ALERT_ICONS, ALERT_SEVERITY_LABELS, Alert, AlertSeverity, AlertType } from '../alert.model';
import { AlertService } from '../alert.service';
import { TimeAgoPipe } from '../time-ago.pipe';

type AlertFilter = 'all' | 'unread' | AlertType;

interface FilterTab {
  key: AlertFilter;
  label: string;
}

@Component({
  selector: 'app-alert-bell',
  standalone: true,
  imports: [CommonModule, TimeAgoPipe],
  templateUrl: './alert-bell.component.html',
  styleUrl: './alert-bell.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
  animations: [
    trigger('panelAnimation', [
      transition(':enter', [
        style({ opacity: 0, transform: 'translateY(-12px)' }),
        animate('180ms ease-out', style({ opacity: 1, transform: 'translateY(0)' }))
      ]),
      transition(':leave', [
        animate('140ms ease-in', style({ opacity: 0, transform: 'translateY(-8px)' }))
      ])
    ]),
    trigger('itemAnimation', [
      transition(':enter', [
        style({ opacity: 0, transform: 'translateX(-10px)' }),
        animate('180ms ease-out', style({ opacity: 1, transform: 'translateX(0)' }))
      ]),
      transition(':leave', [
        animate('140ms ease-in', style({ opacity: 0, transform: 'translateX(10px)' }))
      ])
    ])
  ]
})
export class AlertBellComponent implements OnInit, OnDestroy {
  private readonly alertService = inject(AlertService);
  private readonly generalNotificationService = inject(GeneralNotificationService);
  private readonly router = inject(Router);
  private readonly elementRef = inject(ElementRef<HTMLElement>);
  private pollingId: ReturnType<typeof setInterval> | null = null;

  readonly filters: FilterTab[] = [
    { key: 'all', label: 'Toutes' },
    { key: 'unread', label: 'Non lues' },
    { key: 'budget', label: 'Budget' },
    { key: 'transaction', label: 'Transaction' },
    { key: 'security', label: 'Securite' },
    { key: 'info', label: 'Info' },
    { key: 'success', label: 'Succes' }
  ];

  private readonly selectedFilterSubject = new BehaviorSubject<AlertFilter>('all');
  readonly selectedFilter$ = this.selectedFilterSubject.asObservable();
  readonly alerts$ = this.alertService.getAlerts();
  readonly generalNotifications$ = this.generalNotificationService.notifications$;
  readonly unreadCount$ = this.alertService.getUnreadCount();
  readonly bellCount$ = combineLatest([this.unreadCount$, this.generalNotifications$]).pipe(
    map(([unreadCount, generalNotifications]) => unreadCount + generalNotifications.length)
  );
  readonly filteredAlerts$ = combineLatest([
    this.alerts$,
    this.selectedFilter$
  ]).pipe(
    map(([alerts, filter]) => this.applyFilter(alerts, filter))
  );

  panelOpen = false;

  ngOnInit(): void {
    void firstValueFrom(
      this.alertService.refreshAlertsWithToast().pipe(
        catchError(() => of([]))
      )
    );
    void firstValueFrom(this.generalNotificationService.refresh());

    this.pollingId = setInterval(() => {
      void firstValueFrom(
        this.alertService.refreshAlerts().pipe(
          catchError(() => of([]))
        )
      );
      void firstValueFrom(this.generalNotificationService.refresh());
    }, 30000);
  }

  ngOnDestroy(): void {
    if (this.pollingId) {
      clearInterval(this.pollingId);
      this.pollingId = null;
    }
  }

  @HostListener('document:click', ['$event'])
  handleDocumentClick(event: MouseEvent): void {
    if (!this.panelOpen) {
      return;
    }

    const target = event.target as Node | null;
    if (target && !this.elementRef.nativeElement.contains(target)) {
      this.panelOpen = false;
    }
  }

  @HostListener('document:keydown.escape')
  handleEscapeKey(): void {
    this.panelOpen = false;
  }

  togglePanel(): void {
    this.panelOpen = !this.panelOpen;
  }

  selectFilter(filter: AlertFilter): void {
    this.selectedFilterSubject.next(filter);
  }

  async markAsRead(id: string): Promise<void> {
    await firstValueFrom(this.alertService.markAsRead(id));
  }

  async markAllRead(): Promise<void> {
    await firstValueFrom(this.alertService.markAllAsRead());
  }

  async deleteAlert(id: string): Promise<void> {
    await firstValueFrom(this.alertService.deleteAlert(id));
  }

  async clearAll(): Promise<void> {
    await firstValueFrom(this.alertService.clearAll());
  }

  navigateToRoute(route: string): void {
    this.panelOpen = false;
    void this.router.navigateByUrl(route);
  }

  async navigate(alert: Alert): Promise<void> {
    await firstValueFrom(this.alertService.markAsRead(alert.id));
    this.panelOpen = false;

    if (alert.actionRoute) {
      void this.router.navigateByUrl(alert.actionRoute);
    }
  }

  getIcon(type: AlertType): string {
    return ALERT_ICONS[type] ?? '🔔';
  }

  getSeverityLabel(severity: AlertSeverity): string {
    return ALERT_SEVERITY_LABELS[severity];
  }

  trackById(_: number, alert: Alert): string {
    return alert.id;
  }

  trackByFilter(_: number, filter: FilterTab): string {
    return filter.key;
  }

  trackByGeneralNotification(_: number, notification: GeneralNotificationDto): number {
    return notification.id;
  }

  getGeneralNotificationIcon(notification: GeneralNotificationDto): string {
    switch (notification.type) {
      case 'MAINTENANCE':
        return 'campaign';
      case 'SECURITY':
        return 'shield';
      case 'FEATURE':
        return 'auto_awesome';
      case 'WARNING':
        return 'warning';
      case 'INFO':
      default:
        return 'info';
    }
  }

  formatAmount(alert: Alert): string | null {
    if (alert.amount === undefined || alert.amount === null) {
      return null;
    }

    const currency = alert.currency ?? 'DT';
    return `${alert.amount.toLocaleString('fr-FR')} ${currency}`;
  }

  private applyFilter(alerts: Alert[], filter: AlertFilter): Alert[] {
    if (filter === 'all') {
      return alerts;
    }

    if (filter === 'unread') {
      return alerts.filter((alert) => !alert.read);
    }

    return alerts.filter((alert) => alert.type === filter);
  }
}
