import { CommonModule } from '@angular/common';
import { Component, computed, inject, signal } from '@angular/core';
import { DomSanitizer, SafeResourceUrl } from '@angular/platform-browser';

@Component({
  selector: 'app-admin-dashboard',
  standalone: true,
  imports: [CommonModule],
  template: `
    <div class="page">
      <div class="page-head">
        <h2>Dashboard global anonymisé</h2>
        <p>KPI plateforme uniquement, sans données financières individuelles.</p>
      </div>

      <section class="powerbi-card">
        <div class="powerbi-head">
          <div>
            <span>Analyse décisionnelle</span>
            <h3>Tableau de bord Power BI</h3>
          </div>
          <div class="powerbi-actions">
            <button class="powerbi-refresh" type="button" (click)="refreshPowerBiReport()">
              <span class="material-symbols-rounded" aria-hidden="true">refresh</span>
              Actualiser
            </button>
            <strong>Live</strong>
          </div>
        </div>

        <div class="powerbi-frame-wrap">
          <iframe
            class="powerbi-frame"
            title="attijari compass power bi"
            [src]="powerBiReportUrl()"
            frameborder="0"
            allowfullscreen
          ></iframe>
        </div>
      </section>
    </div>
  `,
  styles: [`
    .page { display: grid; gap: 1.2rem; }
    .page-head h2 { margin: 0; font-size: 1.35rem; }
    .page-head p { margin: .35rem 0 0; color: #6b7280; }
    .powerbi-card { background: #fff; border: 1px solid #ececf0; border-radius: 14px; padding: 1rem; box-shadow: 0 18px 42px rgba(17,17,17,.06); }
    .powerbi-head { display: flex; align-items: center; justify-content: space-between; gap: 1rem; margin-bottom: .9rem; }
    .powerbi-head span { display: block; color: #f58220; font-size: .75rem; font-weight: 800; letter-spacing: .06em; text-transform: uppercase; }
    .powerbi-head h3 { margin: .25rem 0 0; color: #111; font-size: 1.1rem; }
    .powerbi-actions { display: inline-flex; align-items: center; gap: .55rem; flex-wrap: wrap; justify-content: flex-end; }
    .powerbi-actions strong { display: inline-flex; align-items: center; padding: .35rem .7rem; border-radius: 999px; background: #fff4e8; color: #9a4d00; font-size: .78rem; }
    .powerbi-refresh { display: inline-flex; align-items: center; gap: .35rem; padding: .5rem .75rem; border: 1px solid rgba(245,130,32,.22); border-radius: 999px; background: #111; color: #fff; font-weight: 800; font-size: .78rem; cursor: pointer; box-shadow: 0 10px 22px rgba(17,17,17,.12); transition: transform .18s ease, box-shadow .18s ease, background .18s ease; }
    .powerbi-refresh:hover { transform: translateY(-1px); background: #f58220; box-shadow: 0 14px 28px rgba(245,130,32,.2); }
    .powerbi-refresh .material-symbols-rounded { font-size: 1rem; line-height: 1; }
    .powerbi-frame-wrap { position: relative; width: 100%; height: clamp(680px, 76vh, 880px); overflow: hidden; border-radius: 12px; background: #111; }
    .powerbi-frame { position: absolute; left: 0; top: -38px; width: 100%; height: calc(100% + 38px); border: 0; background: #fff; }
    @media (max-width: 720px) {
      .powerbi-card { padding: .75rem; }
      .powerbi-head { align-items: flex-start; flex-direction: column; }
      .powerbi-actions { width: 100%; justify-content: space-between; }
      .powerbi-frame-wrap { height: 500px; }
      .powerbi-frame { top: -34px; height: calc(100% + 34px); }
    }
  `]
})
export class AdminDashboardComponent {
  private readonly sanitizer = inject(DomSanitizer);
  private readonly powerBiReloadToken = signal(0);
  readonly powerBiReportUrl = computed<SafeResourceUrl>(() =>
    this.sanitizer.bypassSecurityTrustResourceUrl(
      `https://app.powerbi.com/reportEmbed?reportId=4a43b4c7-5e85-4e5e-8010-4911c77efc76&autoAuth=true&embeddedDemo=true&filterPaneEnabled=false&navContentPaneEnabled=false&reload=${this.powerBiReloadToken()}`
    )
  );

  refreshPowerBiReport(): void {
    this.powerBiReloadToken.update(value => value + 1);
  }
}
