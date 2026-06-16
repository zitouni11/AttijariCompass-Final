import { CommonModule, DatePipe } from '@angular/common';
import { Component, OnInit, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { SupportTicketDto, SupportTicketStatus } from '../../../core/models/admin.models';
import { AdminService } from '../../../core/services/admin.service';

@Component({
  selector: 'app-admin-support',
  standalone: true,
  imports: [CommonModule, FormsModule, DatePipe],
  template: `
    <div class="page">
      <div class="split">
        <div class="table-wrap">
          <table>
            <thead><tr><th>Sujet</th><th>Categorie</th><th>Email utilisateur</th><th>Statut</th><th>Date</th><th></th></tr></thead>
            <tbody>
              @for (ticket of tickets(); track ticket.id) {
                <tr>
                  <td>{{ ticket.subject }}</td>
                  <td>{{ ticket.category }}</td>
                  <td>{{ ticket.userEmail }}</td>
                  <td><span class="badge">{{ ticket.status }}</span></td>
                  <td>{{ ticket.createdAt | date:'short' }}</td>
                  <td><button type="button" (click)="select(ticket)">Voir / repondre</button></td>
                </tr>
              }
            </tbody>
          </table>
        </div>
        @if (selected(); as ticket) {
          <aside class="panel">
            <h3>{{ ticket.subject }}</h3>
            <p class="meta">{{ ticket.userEmail }} · {{ ticket.category }}</p>
            <p>{{ ticket.message }}</p>
            @if (ticket.adminReply) { <div class="reply">{{ ticket.adminReply }}</div> }
            <label>Statut</label>
            <select [(ngModel)]="status"><option>NEW</option><option>IN_PROGRESS</option><option>RESOLVED</option><option>CLOSED</option></select>
            <label>Reponse admin</label>
            <textarea [(ngModel)]="reply" rows="6"></textarea>
            <div class="actions">
              <button type="button" (click)="saveStatus(ticket.id)">Changer statut</button>
              <button type="button" (click)="sendReply(ticket.id)" [disabled]="!reply.trim()">Repondre</button>
            </div>
          </aside>
        }
      </div>
    </div>
  `,
  styles: [`
    .split { display: grid; grid-template-columns: minmax(0, 1fr) 360px; gap: 1rem; align-items: start; }
    .table-wrap, .panel { background: #fff; border: 1px solid #ececf0; border-radius: 8px; overflow: hidden; }
    table { width: 100%; border-collapse: collapse; min-width: 760px; }
    th, td { padding: .8rem; border-bottom: 1px solid #f1f1f2; text-align: left; font-size: .85rem; }
    th { background: #fafafa; color: #6b7280; }
    .panel { padding: 1rem; display: grid; gap: .7rem; }
    h3 { margin: 0; }
    .meta { color: #6b7280; margin: 0; font-size: .82rem; }
    .reply { background: #fff7ed; border-left: 3px solid #f28c28; padding: .75rem; }
    .badge { padding: .3rem .55rem; border-radius: 999px; background: #f3f4f6; font-weight: 800; font-size: .72rem; }
    select, textarea { border: 1px solid #ddd; border-radius: 8px; padding: .6rem; }
    button { border: 0; border-radius: 8px; background: #111; color: #fff; padding: .55rem .75rem; cursor: pointer; }
    button:disabled { opacity: .55; cursor: not-allowed; }
    .actions { display: flex; gap: .5rem; flex-wrap: wrap; }
    @media (max-width: 1050px) { .split { grid-template-columns: 1fr; } .table-wrap { overflow: auto; } }
  `]
})
export class AdminSupportComponent implements OnInit {
  private readonly adminService = inject(AdminService);
  readonly tickets = signal<SupportTicketDto[]>([]);
  readonly selected = signal<SupportTicketDto | null>(null);
  status: SupportTicketStatus = 'NEW';
  reply = '';

  ngOnInit(): void { this.load(); }
  load(): void { this.adminService.getSupportTickets().subscribe(tickets => this.tickets.set(tickets)); }
  select(ticket: SupportTicketDto): void { this.selected.set(ticket); this.status = ticket.status; this.reply = ticket.adminReply || ''; }
  saveStatus(id: number): void { this.adminService.updateTicketStatus(id, this.status).subscribe(ticket => { this.selected.set(ticket); this.load(); }); }
  sendReply(id: number): void { this.adminService.replyTicket(id, this.reply).subscribe(ticket => { this.selected.set(ticket); this.load(); }); }
}
