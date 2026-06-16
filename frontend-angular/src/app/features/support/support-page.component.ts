import { CommonModule, DatePipe } from '@angular/common';
import { Component, OnInit, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { SupportTicketCategory, SupportTicketDto } from '../../core/models/admin.models';
import { SupportService } from '../../core/services/support.service';

@Component({
  selector: 'app-support-page',
  standalone: true,
  imports: [CommonModule, FormsModule, DatePipe],
  template: `
    <section class="support-page">
      <form class="support-form" (ngSubmit)="create()">
        <h1>Support</h1>
        <input name="subject" [(ngModel)]="subject" placeholder="Sujet" required />
        <select name="category" [(ngModel)]="category">
          <option>LOGIN_PROBLEM</option>
          <option>IMPORT_PROBLEM</option>
          <option>CARD_PROBLEM</option>
          <option>CHATBOT_PROBLEM</option>
          <option>BUG</option>
          <option>GENERAL</option>
        </select>
        <textarea name="message" [(ngModel)]="message" rows="5" placeholder="Decrivez votre demande" required></textarea>
        <button type="submit" [disabled]="!subject.trim() || !message.trim()">Envoyer</button>
      </form>

      <div class="tickets">
        @for (ticket of tickets(); track ticket.id) {
          <article>
            <div>
              <h3>{{ ticket.subject }}</h3>
              <span>{{ ticket.category }} · {{ ticket.createdAt | date:'short' }}</span>
            </div>
            <strong>{{ ticket.status }}</strong>
            <p>{{ ticket.message }}</p>
            @if (ticket.adminReply) { <div class="reply">{{ ticket.adminReply }}</div> }
          </article>
        }
      </div>
    </section>
  `,
  styles: [`
    .support-page { display: grid; grid-template-columns: 340px minmax(0, 1fr); gap: 1rem; }
    .support-form, article { background: #fff; border: 1px solid #ececf0; border-radius: 8px; padding: 1rem; }
    .support-form { display: grid; gap: .75rem; align-self: start; }
    h1, h3 { margin: 0; }
    input, select, textarea { border: 1px solid #ddd; border-radius: 8px; padding: .65rem; }
    button { border: 0; border-radius: 8px; background: #111; color: #fff; padding: .7rem .9rem; font-weight: 800; cursor: pointer; }
    button:disabled { opacity: .55; cursor: not-allowed; }
    .tickets { display: grid; gap: .8rem; }
    article span { color: #6b7280; font-size: .8rem; }
    article strong { display: inline-flex; margin-top: .6rem; padding: .3rem .55rem; border-radius: 999px; background: #fff7ed; color: #c2410c; font-size: .75rem; }
    .reply { background: #f7f7f8; border-left: 3px solid #f28c28; padding: .75rem; }
    @media (max-width: 900px) { .support-page { grid-template-columns: 1fr; } }
  `]
})
export class SupportPageComponent implements OnInit {
  private readonly supportService = inject(SupportService);
  readonly tickets = signal<SupportTicketDto[]>([]);
  subject = '';
  message = '';
  category: SupportTicketCategory = 'GENERAL';

  ngOnInit(): void { this.load(); }
  load(): void { this.supportService.getMyTickets().subscribe(tickets => this.tickets.set(tickets)); }
  create(): void {
    this.supportService.createTicket({ subject: this.subject, category: this.category, message: this.message }).subscribe(() => {
      this.subject = '';
      this.message = '';
      this.category = 'GENERAL';
      this.load();
    });
  }
}
