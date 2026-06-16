import { Component, inject, OnInit, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { StorytellingService } from '../../core/services/api.services';
import { MonthlyStoryResponse } from '../../core/models';

@Component({
  selector: 'app-storytelling',
  standalone: true,
  imports: [CommonModule],
  template: `
    <div class="page">
      <div class="page-header">
        <div>
          <h1 class="page-title">📖 Mon Histoire Financière</h1>
          <p class="page-subtitle">{{ story()?.mois }}</p>
        </div>
        <button class="btn-refresh" (click)="load()">🔄 Actualiser</button>
      </div>

      @if (loading()) {
        <div class="story-skeleton">
          <div class="sk-block tall"></div>
          <div class="sk-row">
            <div class="sk-block"></div>
            <div class="sk-block"></div>
          </div>
        </div>
      } @else if (story()) {
        <div class="story-layout">
          <!-- Resume -->
          <div class="story-hero">
            <div class="story-header">
              <span class="story-icon">📖</span>
              <div>
                <h2>Résumé de {{ story()!.mois }}</h2>
              </div>
            </div>
            <p class="story-text">{{ story()!.resume }}</p>
            <div class="story-stats">
              <div class="s-stat income">
                <span class="s-icon">📈</span>
                <span class="s-label">Revenus</span>
                <span class="s-value">{{ story()!.totalRevenus | number:'1.0-0' }} DT</span>
              </div>
              <div class="s-stat expenses">
                <span class="s-icon">📉</span>
                <span class="s-label">Dépenses</span>
                <span class="s-value">{{ story()!.totalDepenses | number:'1.0-0' }} DT</span>
              </div>
              <div class="s-stat" [class.savings]="story()!.epargneRealisee >= 0" [class.deficit]="story()!.epargneRealisee < 0">
                <span class="s-icon">{{ story()!.epargneRealisee >= 0 ? '💰' : '⚠️' }}</span>
                <span class="s-label">{{ story()!.epargneRealisee >= 0 ? 'Épargne' : 'Déficit' }}</span>
                <span class="s-value">{{ story()!.epargneRealisee | number:'1.0-0' }} DT</span>
              </div>
            </div>
            @if (story()!.categoriesPrincipales) {
              <div class="top-cats">
                <span class="tc-label">Top catégories :</span>
                <span class="tc-value">{{ story()!.categoriesPrincipales }}</span>
              </div>
            }
          </div>

          <div class="side-cards">
            <!-- Alerts -->
            @if (story()!.alertes && story()!.alertes.length > 0) {
              <div class="alerts-card">
                <h3>⚠️ Alertes</h3>
                <div class="alerts-list">
                  @for (alerte of story()!.alertes; track alerte) {
                    <div class="alert-item">
                      <p>{{ alerte }}</p>
                    </div>
                  }
                </div>
              </div>
            }

            <!-- Missions -->
            @if (story()!.missions && story()!.missions.length > 0) {
              <div class="missions-card">
                <h3>🏆 Missions du mois</h3>
                <div class="missions-list">
                  @for (mission of story()!.missions; track mission) {
                    <div class="mission-item">
                      <div class="mission-check" (click)="toggleMission(mission)">
                        {{ completedMissions.has(mission) ? '✅' : '⬜' }}
                      </div>
                      <p [class.completed]="completedMissions.has(mission)">{{ mission }}</p>
                    </div>
                  }
                </div>
                <div class="mission-progress">
                  <span>{{ completedMissions.size }}/{{ story()!.missions.length }} complétées</span>
                  <div class="mp-bar">
                    <div class="mp-fill" [style.width.%]="(completedMissions.size / story()!.missions.length) * 100"></div>
                  </div>
                </div>
              </div>
            }
          </div>
        </div>
      }
    </div>
  `,
  styles: [`
    .page { font-family: 'Sora', sans-serif; }
    .page-header { display: flex; justify-content: space-between; align-items: flex-start; margin-bottom: 1.5rem; }
    .page-title { font-size: 1.75rem; font-weight: 800; color: var(--attijari-black); margin: 0 0 0.25rem; }
    .page-subtitle { color: var(--attijari-muted); font-size: 0.875rem; margin: 0; text-transform: capitalize; }
    .btn-refresh { padding: 0.6rem 1.25rem; background: var(--attijari-white); border: 1.5px solid var(--attijari-border); border-radius: 10px; cursor: pointer; font-family: inherit; font-size: 0.875rem; font-weight: 600; }
    .story-skeleton { display: flex; flex-direction: column; gap: 1rem; }
    .sk-block { background: linear-gradient(90deg, #efe9e4 25%, #f8f5f2 50%, #efe9e4 75%); background-size: 200%; border-radius: 16px; animation: shimmer 1.5s infinite; height: 180px; }
    .sk-block.tall { height: 300px; }
    .sk-row { display: grid; grid-template-columns: 1fr 1fr; gap: 1rem; }
    @keyframes shimmer { from { background-position: 200%; } to { background-position: -200%; } }
    .story-layout { display: grid; grid-template-columns: 1fr 380px; gap: 1.5rem; align-items: start; }
    .story-hero { background: linear-gradient(135deg, var(--attijari-black) 0%, var(--attijari-anthracite) 100%); border-radius: 20px; padding: 2rem; color: var(--attijari-white); }
    .story-header { display: flex; align-items: center; gap: 1rem; margin-bottom: 1.5rem; }
    .story-icon { font-size: 2.5rem; }
    .story-header h2 { font-size: 1.3rem; font-weight: 800; margin: 0; text-transform: capitalize; }
    .story-text { font-size: 0.95rem; line-height: 1.8; color: rgba(255,255,255,0.85); margin: 0 0 1.5rem; }
    .story-stats { display: flex; gap: 1rem; margin-bottom: 1rem; }
    .s-stat { flex: 1; background: rgba(255,255,255,0.08); border-radius: 12px; padding: 1rem; display: flex; flex-direction: column; align-items: center; gap: 0.3rem; }
    .s-stat.income { background: rgba(16,185,129,0.15); }
    .s-stat.expenses { background: rgba(239,68,68,0.15); }
    .s-stat.savings { background: rgba(59,130,246,0.15); }
    .s-stat.deficit { background: rgba(245,158,11,0.15); }
    .s-icon { font-size: 1.2rem; }
    .s-label { font-size: 0.7rem; color: rgba(255,255,255,0.6); font-weight: 500; }
    .s-value { font-size: 1.1rem; font-weight: 800; color: white; }
    .top-cats { background: rgba(255,255,255,0.07); border-radius: 10px; padding: 0.75rem 1rem; font-size: 0.82rem; display: flex; gap: 0.5rem; flex-wrap: wrap; }
    .tc-label { color: rgba(255,255,255,0.5); }
    .tc-value { color: var(--attijari-orange); font-weight: 600; }
    .side-cards { display: flex; flex-direction: column; gap: 1.25rem; }
    .alerts-card, .missions-card { background: var(--attijari-white); border-radius: 16px; padding: 1.5rem; box-shadow: 0 12px 26px rgba(17,17,17,0.06); border: 1px solid var(--attijari-border); }
    h3 { font-size: 1rem; font-weight: 700; color: var(--attijari-black); margin: 0 0 1rem; }
    .alerts-list { display: flex; flex-direction: column; gap: 0.75rem; }
    .alert-item { background: var(--attijari-orange-soft); border-left: 3px solid var(--attijari-orange); border-radius: 8px; padding: 0.75rem; }
    .alert-item p { margin: 0; font-size: 0.82rem; color: #8c4f1d; line-height: 1.5; }
    .missions-list { display: flex; flex-direction: column; gap: 0.75rem; margin-bottom: 1rem; }
    .mission-item { display: flex; align-items: flex-start; gap: 0.75rem; }
    .mission-check { font-size: 1.1rem; cursor: pointer; flex-shrink: 0; }
    .mission-item p { margin: 0; font-size: 0.82rem; color: var(--attijari-text); line-height: 1.5; }
    .mission-item p.completed { text-decoration: line-through; color: var(--attijari-muted); }
    .mission-progress { display: flex; flex-direction: column; gap: 0.4rem; }
    .mission-progress span { font-size: 0.75rem; color: var(--attijari-muted); font-weight: 600; }
    .mp-bar { background: #f1ece7; border-radius: 4px; height: 6px; overflow: hidden; }
    .mp-fill { height: 100%; background: linear-gradient(90deg, var(--attijari-orange), var(--attijari-orange-dark)); border-radius: 4px; transition: width 0.5s ease; }
    @media (max-width: 1024px) { .story-layout { grid-template-columns: 1fr; } .story-stats { flex-direction: row; } }
    @media (max-width: 640px) { .story-stats { flex-direction: column; } }
  `]
})
export class StorytellingComponent implements OnInit {
  private storyService = inject(StorytellingService);
  story = signal<MonthlyStoryResponse | null>(null);
  loading = signal(true);
  completedMissions = new Set<string>();

  ngOnInit(): void { this.load(); }

  load(): void {
    this.loading.set(true);
    this.storyService.getMonthlyStory().subscribe({
      next: (data) => { this.story.set(data); this.loading.set(false); },
      error: () => this.loading.set(false)
    });
  }

  toggleMission(mission: string): void {
    if (this.completedMissions.has(mission)) this.completedMissions.delete(mission);
    else this.completedMissions.add(mission);
  }
}
