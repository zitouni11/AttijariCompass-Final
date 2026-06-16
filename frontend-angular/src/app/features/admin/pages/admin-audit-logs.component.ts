import { CommonModule, DatePipe } from '@angular/common';
import { Component, OnInit, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { AuditLogDto } from '../../../core/models/admin.models';
import { AdminService } from '../../../core/services/admin.service';

@Component({
  selector: 'app-admin-audit-logs',
  standalone: true,
  imports: [CommonModule, FormsModule, DatePipe],
  template: `
    <div class="page">
      <div class="toolbar">
        <input [(ngModel)]="search" placeholder="Recherche acteur ou message" />
        <input [(ngModel)]="moduleFilter" placeholder="Module" />
        <select [(ngModel)]="statusFilter"><option value="">Tous statuts</option><option>SUCCESS</option><option>FAILED</option></select>
      </div>
      <div class="table-wrap">
        <table>
          <thead><tr><th>Date</th><th>Acteur</th><th>Role</th><th>Module</th><th>Action</th><th>Statut</th><th>Message</th></tr></thead>
          <tbody>
            @for (log of filteredLogs(); track log.id) {
              <tr>
                <td>{{ log.createdAt | date:'short' }}</td>
                <td>{{ log.actorEmail }}</td>
                <td>{{ log.actorRole }}</td>
                <td>{{ log.module }}</td>
                <td>{{ log.action }}</td>
                <td><span class="badge" [class.ok]="log.status === 'SUCCESS'">{{ log.status }}</span></td>
                <td>{{ log.message }}</td>
              </tr>
            }
          </tbody>
        </table>
      </div>
    </div>
  `,
  styles: [`
    .page { display: grid; gap: 1rem; }
    .toolbar { display: flex; flex-wrap: wrap; gap: .7rem; }
    input, select { min-height: 40px; border: 1px solid #ddd; border-radius: 8px; padding: .55rem .7rem; background: #fff; }
    .table-wrap { overflow: auto; background: #fff; border: 1px solid #ececf0; border-radius: 8px; }
    table { width: 100%; border-collapse: collapse; min-width: 980px; }
    th, td { padding: .78rem; border-bottom: 1px solid #f1f1f2; text-align: left; font-size: .82rem; }
    th { background: #fafafa; color: #6b7280; }
    .badge { padding: .3rem .55rem; border-radius: 999px; background: #fee2e2; color: #991b1b; font-weight: 800; font-size: .72rem; }
    .badge.ok { background: #dcfce7; color: #166534; }
  `]
})
export class AdminAuditLogsComponent implements OnInit {
  private readonly adminService = inject(AdminService);
  readonly logs = signal<AuditLogDto[]>([]);
  search = '';
  moduleFilter = '';
  statusFilter = '';

  ngOnInit(): void { this.adminService.getAuditLogs().subscribe(logs => this.logs.set(logs)); }

  filteredLogs(): AuditLogDto[] {
    const q = this.search.trim().toLowerCase();
    const module = this.moduleFilter.trim().toLowerCase();
    return this.logs().filter(log => {
      const matchesSearch = !q || log.actorEmail.toLowerCase().includes(q) || (log.message || '').toLowerCase().includes(q);
      const matchesModule = !module || log.module.toLowerCase().includes(module);
      const matchesStatus = !this.statusFilter || log.status === this.statusFilter;
      return matchesSearch && matchesModule && matchesStatus;
    });
  }
}
