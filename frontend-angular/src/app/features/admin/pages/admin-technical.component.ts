import { CommonModule, DatePipe } from '@angular/common';
import { Component, OnInit, inject, signal } from '@angular/core';
import { TechnicalStatusDto } from '../../../core/models/admin.models';
import { AdminService } from '../../../core/services/admin.service';

@Component({
  selector: 'app-admin-technical',
  standalone: true,
  imports: [CommonModule, DatePipe],
  template: `
    <div class="page">
      <button type="button" (click)="load()">
        <span class="material-symbols-rounded">refresh</span>
        Rafraichir le statut
      </button>
      @if (status(); as s) {
        <section class="grid">
          @for (item of cards(s); track item.label) {
            <article>
              <span>{{ item.label }}</span>
              <strong class="badge" [class.up]="item.value === 'UP'" [class.down]="item.value === 'DOWN'">{{ item.value }}</strong>
            </article>
          }
          <article><span>Uptime</span><strong>{{ s.uptime }}</strong></article>
          <article><span>Temps reponse check</span><strong>{{ s.apiAverageResponseTime }} ms</strong></article>
          <article><span>Derniere verification</span><strong>{{ s.lastCheckedAt | date:'short' }}</strong></article>
        </section>
      }
    </div>
  `,
  styles: [`
    .page { display: grid; gap: 1rem; }
    button { width: fit-content; border: 0; border-radius: 8px; background: #111; color: #fff; padding: .65rem .85rem; font-weight: 800; display: inline-flex; align-items: center; gap: .45rem; cursor: pointer; }
    .grid { display: grid; grid-template-columns: repeat(auto-fit, minmax(210px, 1fr)); gap: .9rem; }
    article { background: #fff; border: 1px solid #ececf0; border-radius: 8px; padding: 1rem; }
    article span { display: block; color: #6b7280; font-size: .78rem; font-weight: 800; }
    article strong { display: inline-block; margin-top: .6rem; font-size: 1rem; }
    .badge { padding: .35rem .65rem; border-radius: 999px; background: #f3f4f6; color: #6b7280; font-size: .8rem; }
    .badge.up { background: #dcfce7; color: #166534; }
    .badge.down { background: #fee2e2; color: #991b1b; }
  `]
})
export class AdminTechnicalComponent implements OnInit {
  private readonly adminService = inject(AdminService);
  readonly status = signal<TechnicalStatusDto | null>(null);
  ngOnInit(): void { this.load(); }
  load(): void { this.adminService.getTechnicalStatus().subscribe(status => this.status.set(status)); }
  cards(s: TechnicalStatusDto) {
    return [
      { label: 'Backend API', value: s.backendStatus },
      { label: 'PostgreSQL', value: s.databaseStatus },
      { label: 'FastAPI ML', value: s.fastApiStatus },
      { label: 'Chatbot / Groq', value: s.chatbotStatus },
      { label: 'Power BI', value: this.resolvePowerBiStatus(s.powerBiStatus) }
    ];
  }

  private resolvePowerBiStatus(status: TechnicalStatusDto['powerBiStatus']): TechnicalStatusDto['powerBiStatus'] {
    return status === 'DOWN' ? 'DOWN' : 'UP';
  }
}
