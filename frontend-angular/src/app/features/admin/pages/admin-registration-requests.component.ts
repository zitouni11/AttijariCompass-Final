import { CommonModule, DatePipe } from '@angular/common';
import { Component, OnInit, inject, signal } from '@angular/core';
import { AdminRegistrationResponseDto, AdminRegistrationStatus } from '../../../core/models/admin.models';
import { AdminService } from '../../../core/services/admin.service';
import { NotificationService } from '../../../core/services/notification.service';

@Component({
  selector: 'app-admin-registration-requests',
  standalone: true,
  imports: [CommonModule, DatePipe],
  template: `
    <section class="admin-page">
      <header>
        <div>
          <span class="eyebrow">Validation</span>
          <h2>Demandes admins</h2>
          <p>Approuvez ou refusez les demandes de comptes Back Office apres verification e-mail.</p>
        </div>

        <button type="button" (click)="load()" [disabled]="loading()">Rafraichir</button>
      </header>

      @if (loading()) {
        <div class="state">Chargement des demandes...</div>
      } @else if (!requests().length) {
        <div class="state">Aucune demande administrateur pour le moment.</div>
      } @else {
        <div class="table-wrap">
          <table>
            <thead>
              <tr>
                <th>Nom</th>
                <th>Email</th>
                <th>Statut</th>
                <th>Creation</th>
                <th>Verification</th>
                <th>Traitement</th>
                <th>Actions</th>
              </tr>
            </thead>
            <tbody>
              @for (request of requests(); track request.id) {
                <tr>
                  <td>{{ request.fullName || '-' }}</td>
                  <td>{{ request.email }}</td>
                  <td><span class="status" [attr.data-status]="request.status">{{ statusLabel(request.status) }}</span></td>
                  <td>{{ request.createdAt | date: 'short' }}</td>
                  <td>{{ request.verifiedAt ? (request.verifiedAt | date: 'short') : '-' }}</td>
                  <td>
                    @if (request.reviewedAt) {
                      <span>{{ request.reviewedAt | date: 'short' }}</span>
                      @if (request.reviewedByEmail) {
                        <small>{{ request.reviewedByEmail }}</small>
                      }
                    } @else {
                      -
                    }
                  </td>
                  <td>
                    <div class="actions">
                      <button
                        type="button"
                        (click)="approve(request)"
                        [disabled]="request.status !== 'PENDING_APPROVAL' || actionLoading() === request.id"
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
    table { width: 100%; border-collapse: collapse; min-width: 900px; }
    th, td { padding: .8rem; border-bottom: 1px solid #f0f0f2; text-align: left; vertical-align: top; }
    th { font-size: .76rem; text-transform: uppercase; color: #777; letter-spacing: .04em; }
    small { display: block; color: #777; margin-top: .2rem; }
    button { border: 0; border-radius: 8px; background: #111; color: #fff; padding: .55rem .75rem; font-weight: 800; cursor: pointer; }
    button.secondary { background: #fff; color: #c85a4f; border: 1px solid #f1b5ab; }
    button:disabled { opacity: .55; cursor: not-allowed; }
    .actions { display: flex; flex-wrap: wrap; gap: .45rem; }
    .status { display: inline-flex; border-radius: 999px; padding: .25rem .55rem; font-size: .76rem; font-weight: 800; background: #eee; color: #555; }
    .status[data-status="PENDING_APPROVAL"] { background: #fff1e6; color: #c65d00; }
    .status[data-status="APPROVED"] { background: #eef8f0; color: #276737; }
    .status[data-status="REJECTED"] { background: #faeeec; color: #c85a4f; }
    .status[data-status="EMAIL_VERIFICATION_PENDING"] { background: #f2eeea; color: #3f3a36; }
    .status[data-status="EXPIRED"] { background: #eee; color: #777; }
  `]
})
export class AdminRegistrationRequestsComponent implements OnInit {
  private readonly adminService = inject(AdminService);
  private readonly notificationService = inject(NotificationService);

  readonly requests = signal<AdminRegistrationResponseDto[]>([]);
  readonly loading = signal(false);
  readonly actionLoading = signal<number | null>(null);

  ngOnInit(): void {
    this.load();
  }

  load(): void {
    this.loading.set(true);
    this.adminService.getAdminRequests().subscribe({
      next: (requests) => {
        this.requests.set(requests);
        this.loading.set(false);
      },
      error: () => {
        this.loading.set(false);
        this.notificationService.error('Impossible de charger les demandes administrateur.');
      }
    });
  }

  approve(request: AdminRegistrationResponseDto): void {
    this.actionLoading.set(request.id);
    this.adminService.approveAdminRequest(request.id).subscribe({
      next: (updated) => {
        this.replaceRequest(updated);
        this.actionLoading.set(null);
        this.notificationService.success('Demande administrateur approuvee.');
      },
      error: (error) => {
        this.actionLoading.set(null);
        this.notificationService.error(error.error?.message || 'Impossible d approuver la demande.');
      }
    });
  }

  reject(request: AdminRegistrationResponseDto): void {
    const reason = window.prompt('Motif du refus', request.rejectionReason || '');
    if (reason === null) {
      return;
    }

    this.actionLoading.set(request.id);
    this.adminService.rejectAdminRequest(request.id, reason.trim()).subscribe({
      next: (updated) => {
        this.replaceRequest(updated);
        this.actionLoading.set(null);
        this.notificationService.success('Demande administrateur refusee.');
      },
      error: (error) => {
        this.actionLoading.set(null);
        this.notificationService.error(error.error?.message || 'Impossible de refuser la demande.');
      }
    });
  }

  statusLabel(status: AdminRegistrationStatus): string {
    switch (status) {
      case 'EMAIL_VERIFICATION_PENDING':
        return 'Email a verifier';
      case 'PENDING_APPROVAL':
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

  isFinal(status: AdminRegistrationStatus): boolean {
    return status === 'APPROVED' || status === 'REJECTED';
  }

  private replaceRequest(updated: AdminRegistrationResponseDto): void {
    this.requests.update((requests) => requests.map((request) => request.id === updated.id ? updated : request));
  }
}
