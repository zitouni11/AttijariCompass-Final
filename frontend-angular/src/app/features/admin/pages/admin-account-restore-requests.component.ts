import { CommonModule, DatePipe } from '@angular/common';
import { Component, OnInit, inject, signal } from '@angular/core';
import { AccountRestoreRequestDto, AccountRestoreStatus } from '../../../core/models/admin.models';
import { AdminService } from '../../../core/services/admin.service';
import { NotificationService } from '../../../core/services/notification.service';

@Component({
  selector: 'app-admin-account-restore-requests',
  standalone: true,
  imports: [CommonModule, DatePipe],
  template: `
    <section class="admin-page">
      <header>
        <div>
          <span class="eyebrow">Restauration</span>
          <h2>Demandes de restauration</h2>
          <p>Validez uniquement la restauration du compte, sans acceder aux donnees financieres personnelles.</p>
        </div>

        <button type="button" (click)="load()" [disabled]="loading()">Rafraichir</button>
      </header>

      @if (loading()) {
        <div class="state">Chargement des demandes...</div>
      } @else if (!requests().length) {
        <div class="state">Aucune demande de restauration pour le moment.</div>
      } @else {
        <div class="table-wrap">
          <table>
            <thead>
              <tr>
                <th>Email</th>
                <th>Nom</th>
                <th>Statut</th>
                <th>Email verifie</th>
                <th>Demande</th>
                <th>Verification</th>
                <th>Motif refus</th>
                <th>Actions</th>
              </tr>
            </thead>
            <tbody>
              @for (request of requests(); track request.id) {
                <tr>
                  <td>{{ request.email }}</td>
                  <td>{{ request.fullName || '-' }}</td>
                  <td><span class="status" [attr.data-status]="request.status">{{ statusLabel(request.status) }}</span></td>
                  <td>
                    <span class="verified" [class.ok]="request.emailVerified">
                      {{ request.emailVerified ? 'Oui' : 'Non' }}
                    </span>
                  </td>
                  <td>{{ request.requestedAt | date: 'short' }}</td>
                  <td>{{ request.verifiedAt ? (request.verifiedAt | date: 'short') : '-' }}</td>
                  <td>{{ request.rejectionReason || '-' }}</td>
                  <td>
                    <div class="actions">
                      <button
                        type="button"
                        (click)="approve(request)"
                        [disabled]="request.status !== 'PENDING_ADMIN_APPROVAL' || actionLoading() === request.id"
                      >
                        Approuver
                      </button>
                      <button
                        class="secondary"
                        type="button"
                        (click)="reject(request)"
                        [disabled]="isFinal(request.status) || actionLoading() === request.id"
                      >
                        Refuser
                      </button>
                    </div>
                  </td>
                </tr>
              }
            </tbody>
          </table>
        </div>
      }
    </section>
  `,
  styles: [`
    .admin-page { display: grid; gap: 1rem; }
    header { display: flex; justify-content: space-between; gap: 1rem; align-items: flex-start; }
    .eyebrow { color: #f28c28; font-weight: 800; text-transform: uppercase; font-size: .72rem; letter-spacing: .08em; }
    h2 { margin: .15rem 0; font-size: 1.35rem; }
    p { margin: 0; color: #666; }
    .state, .table-wrap { background: #fff; border: 1px solid #ececf0; border-radius: 8px; }
    .state { padding: 1rem; color: #666; }
    .table-wrap { overflow-x: auto; }
    table { width: 100%; border-collapse: collapse; min-width: 980px; }
    th, td { padding: .8rem; border-bottom: 1px solid #f0f0f2; text-align: left; vertical-align: top; }
    th { font-size: .76rem; text-transform: uppercase; color: #777; letter-spacing: .04em; }
    button { border: 0; border-radius: 8px; background: #111; color: #fff; padding: .55rem .75rem; font-weight: 800; cursor: pointer; }
    button.secondary { background: #fff; color: #c85a4f; border: 1px solid #f1b5ab; }
    button:disabled { opacity: .55; cursor: not-allowed; }
    .actions { display: flex; flex-wrap: wrap; gap: .45rem; }
    .status, .verified { display: inline-flex; border-radius: 999px; padding: .25rem .55rem; font-size: .76rem; font-weight: 800; background: #eee; color: #555; }
    .verified.ok { background: #eef8f0; color: #276737; }
    .status[data-status="PENDING_ADMIN_APPROVAL"] { background: #fff1e6; color: #c65d00; }
    .status[data-status="APPROVED"] { background: #eef8f0; color: #276737; }
    .status[data-status="REJECTED"] { background: #faeeec; color: #c85a4f; }
    .status[data-status="PENDING_EMAIL_VERIFICATION"] { background: #f2eeea; color: #3f3a36; }
    .status[data-status="EXPIRED"] { background: #eee; color: #777; }
  `]
})
export class AdminAccountRestoreRequestsComponent implements OnInit {
  private readonly adminService = inject(AdminService);
  private readonly notificationService = inject(NotificationService);

  readonly requests = signal<AccountRestoreRequestDto[]>([]);
  readonly loading = signal(false);
  readonly actionLoading = signal<number | null>(null);

  ngOnInit(): void {
    this.load();
  }

  load(): void {
    this.loading.set(true);
    this.adminService.getAccountRestoreRequests().subscribe({
      next: (requests) => {
        this.requests.set(requests);
        this.loading.set(false);
      },
      error: () => {
        this.loading.set(false);
        this.notificationService.error('Impossible de charger les demandes de restauration.');
      }
    });
  }

  approve(request: AccountRestoreRequestDto): void {
    this.actionLoading.set(request.id);
    this.adminService.approveAccountRestoreRequest(request.id).subscribe({
      next: (response) => {
        this.actionLoading.set(null);
        this.notificationService.success(response.message || 'Compte restaure avec succes.');
        this.load();
      },
      error: (error) => {
        this.actionLoading.set(null);
        this.notificationService.error(error.error?.message || 'Impossible d approuver la demande.');
      }
    });
  }

  reject(request: AccountRestoreRequestDto): void {
    const reason = window.prompt('Motif du refus', request.rejectionReason || '');
    if (reason === null) {
      return;
    }

    this.actionLoading.set(request.id);
    this.adminService.rejectAccountRestoreRequest(request.id, reason.trim()).subscribe({
      next: (response) => {
        this.actionLoading.set(null);
        this.notificationService.success(response.message || 'Demande refusee.');
        this.load();
      },
      error: (error) => {
        this.actionLoading.set(null);
        this.notificationService.error(error.error?.message || 'Impossible de refuser la demande.');
      }
    });
  }

  statusLabel(status: AccountRestoreStatus): string {
    switch (status) {
      case 'PENDING_EMAIL_VERIFICATION':
        return 'Email a verifier';
      case 'PENDING_ADMIN_APPROVAL':
        return 'En attente admin';
      case 'APPROVED':
        return 'Approuvee';
      case 'REJECTED':
        return 'Refusee';
      case 'EXPIRED':
        return 'Expiree';
      default:
        return status;
    }
  }

  isFinal(status: AccountRestoreStatus): boolean {
    return status === 'APPROVED' || status === 'REJECTED';
  }
}
