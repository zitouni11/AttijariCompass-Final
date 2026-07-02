import { CommonModule } from '@angular/common';
import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { DomSanitizer, SafeResourceUrl } from '@angular/platform-browser';
import jsPDF from 'jspdf';
import autoTable from 'jspdf-autotable';
import {
  AdminDecisionService,
  DecisionTransactionSource,
  TransactionDecisionPriority,
  TransactionSourceDecisionDto,
  TransactionSourceMetricDto
} from '../../../core/services/admin-decision.service';

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
            <button class="powerbi-refresh" type="button" (click)="refreshPowerBiReport()" [disabled]="decisionLoading()">
              <span class="material-symbols-rounded" aria-hidden="true">refresh</span>
              {{ decisionLoading() ? 'Actualisation...' : 'Actualiser' }}
            </button>
            <button class="powerbi-refresh pdf" type="button" (click)="generateDecisionPdf()" [disabled]="pdfGenerating() || decisionLoading() || !decision()">
              <span class="material-symbols-rounded" aria-hidden="true">{{ pdfGenerating() ? 'hourglass_top' : 'picture_as_pdf' }}</span>
              {{ pdfGenerating() ? 'Génération...' : 'Générer rapport PDF' }}
            </button>
            <strong>Live</strong>
          </div>
        </div>

        <div *ngIf="pdfMessage()" class="pdf-message success">
          <span class="material-symbols-rounded" aria-hidden="true">check_circle</span>
          {{ pdfMessage() }}
        </div>
        <div *ngIf="pdfError()" class="pdf-message error">
          <span class="material-symbols-rounded" aria-hidden="true">error</span>
          {{ pdfError() }}
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

      <section class="decision-card">
        <div class="decision-head">
          <div>
            <span>Analyse intelligente des sources de transactions</span>
            <h3>Centre d'aide à la décision</h3>
          </div>
          <div class="decision-actions">
            <button class="decision-button primary" type="button" (click)="loadDecisionAnalysis()" [disabled]="decisionLoading()">
              <span class="material-symbols-rounded" aria-hidden="true">refresh</span>
              {{ decisionLoading() ? 'Actualisation...' : "Actualiser l'analyse" }}
            </button>
            <button class="decision-button secondary" type="button" (click)="toggleDecisionDetails()" [disabled]="!decision() || decisionLoading()">
              <span class="material-symbols-rounded" aria-hidden="true">{{ showDecisionDetails() ? 'visibility_off' : 'visibility' }}</span>
              {{ showDecisionDetails() ? 'Masquer détails' : 'Afficher détails' }}
            </button>
            <button class="decision-button secondary" type="button" (click)="toggleDecisionReasoning()" [disabled]="!decision() || decisionLoading()">
              <span class="material-symbols-rounded" aria-hidden="true">{{ showDecisionReasoning() ? 'psychology_alt' : 'psychology' }}</span>
              {{ showDecisionReasoning() ? 'Masquer raisonnement' : 'Afficher raisonnement' }}
            </button>
          </div>
        </div>

        <div *ngIf="decisionLoading()" class="decision-state">
          <span class="decision-loader" aria-hidden="true"></span>
          Chargement de l'analyse décisionnelle...
        </div>

        <div *ngIf="decisionError() as error" class="decision-error">
          <span class="material-symbols-rounded" aria-hidden="true">error</span>
          {{ error }}
        </div>

        <ng-container *ngIf="!decisionLoading() && decision() as data">
          <div class="diagnostic-hero">
            <div class="diagnostic-title">
              <span class="material-symbols-rounded" aria-hidden="true">query_stats</span>
              <div>
                <span>Diagnostic décisionnel</span>
                <strong>{{ data.diagnostic.globalStatus }}</strong>
              </div>
              <b class="status-badge" [class]="statusClass(data.diagnostic.globalStatus)">{{ data.diagnostic.globalStatus }}</b>
            </div>
            <div class="diagnostic-grid">
              <div><span>Situation actuelle</span><strong>{{ formatPercent(data.digitalisationRate, 2) }}</strong></div>
              <div><span>Objectif</span><strong>{{ formatPercent(data.digitalisationTarget, 0) }}</strong></div>
              <div><span>Transactions digitales</span><strong>{{ data.digitalTransactions }} / {{ data.totalTransactions }}</strong></div>
              <div><span>Écart objectif</span><strong>{{ formatSignedPercent(data.digitalisationGap, 2) }}</strong></div>
            </div>
            <div class="ai-decision">
              <span class="material-symbols-rounded" aria-hidden="true">tips_and_updates</span>
              <p><b>Décision IA courte :</b> {{ data.impact.mainAction }}</p>
            </div>
          </div>

          <div class="decision-kpis">
            <article class="decision-kpi highlight"><span>Indice digitalisation</span><strong>{{ formatPercent(data.digitalisationRate, 2) }}</strong></article>
            <article class="decision-kpi"><span>Source dominante</span><strong>{{ formatSource(data.dominantSource) }}</strong></article>
            <article class="decision-kpi"><span>Transactions digitales</span><strong>{{ data.digitalTransactions }}</strong></article>
            <article class="decision-kpi"><span>Écart objectif</span><strong [class.positive]="data.digitalisationGap >= 0">{{ formatSignedPercent(data.digitalisationGap, 2) }}</strong></article>
            <article class="decision-kpi"><span>Niveau de priorité</span><strong class="priority" [class.low]="data.priorityLevel === 'LOW'" [class.medium]="data.priorityLevel === 'MEDIUM'" [class.high]="data.priorityLevel === 'HIGH'">{{ formatPriority(data.priorityLevel) }}</strong></article>
          </div>

          <div class="visual-grid">
            <section class="viz-panel">
              <div class="panel-title">
                <span class="material-symbols-rounded" aria-hidden="true">track_changes</span>
                <h4>Objectif digitalisation</h4>
              </div>
              <div class="goal-meta">
                <span>Actuel : <b>{{ formatPercent(data.digitalisationRate, 2) }}</b></span>
                <span>Objectif : <b>{{ formatPercent(data.digitalisationTarget, 0) }}</b></span>
              </div>
              <div class="goal-track">
                <span class="goal-fill" [style.width.%]="progressWidth(data.digitalisationRate)"></span>
                <i class="goal-target" [style.left.%]="progressWidth(data.digitalisationTarget)"></i>
              </div>
            </section>

            <section class="viz-panel">
              <div class="panel-title">
                <span class="material-symbols-rounded" aria-hidden="true">donut_large</span>
                <h4>Répartition des sources</h4>
              </div>
              <div class="source-stack" aria-label="Répartition des sources">
                <span class="stack-card" [style.width.%]="sourcePercentage(data, 'CARD')"></span>
                <span class="stack-bank" [style.width.%]="sourcePercentage(data, 'BANK_TRANSFER')"></span>
                <span class="stack-cash" [style.width.%]="sourcePercentage(data, 'CASH')"></span>
              </div>
              <div class="source-legend">
                <span><i class="card-dot"></i>CARD {{ formatPercent(sourcePercentage(data, 'CARD'), 2) }}</span>
                <span><i class="bank-dot"></i>BANK_TRANSFER {{ formatPercent(sourcePercentage(data, 'BANK_TRANSFER'), 2) }}</span>
                <span><i class="cash-dot"></i>CASH {{ formatPercent(sourcePercentage(data, 'CASH'), 2) }}</span>
              </div>
            </section>
          </div>

          <div class="decision-grid">
            <div class="decision-panel analysis-panel">
              <h4><span class="material-symbols-rounded" aria-hidden="true">analytics</span>Constat</h4>
              <p>{{ data.analyse.constat }}</p>
              <h4><span class="material-symbols-rounded" aria-hidden="true">psychology</span>Interprétation</h4>
              <p>{{ data.analyse.interpretation }}</p>
              <h4><span class="material-symbols-rounded" aria-hidden="true">warning</span>Risque</h4>
              <p>{{ data.analyse.risque }}</p>
              <h4><span class="material-symbols-rounded" aria-hidden="true">task_alt</span>Décision recommandée</h4>
              <p>{{ data.analyse.decisionRecommandee }}</p>
            </div>

            <div class="decision-panel strategic">
              <h4><span class="material-symbols-rounded" aria-hidden="true">strategy</span>Décision stratégique proposée</h4>
              <dl>
                <div><dt>Objectif</dt><dd>{{ data.strategicDecision.objectif }}</dd></div>
                <div><dt>Levier principal</dt><dd>{{ data.strategicDecision.levierPrincipal }}</dd></div>
                <div><dt>Levier secondaire</dt><dd>{{ data.strategicDecision.levierSecondaire }}</dd></div>
                <div><dt>Justification</dt><dd>{{ data.strategicDecision.justification }}</dd></div>
                <div><dt>Décision recommandée</dt><dd>{{ data.strategicDecision.decisionRecommandee }}</dd></div>
              </dl>
            </div>
          </div>

          <div class="decision-panel action-plan">
            <div class="panel-title">
              <span class="material-symbols-rounded" aria-hidden="true">format_list_bulleted</span>
              <h4>Plan d'action recommandé</h4>
            </div>
            <div class="action-table">
              <div class="action-row action-header">
                <span>Priorité</span><span>Action</span><span>Justification</span><span>Impact attendu</span><span>Difficulté</span>
              </div>
              <div class="action-row" *ngFor="let item of data.actionPlan">
                <span><b class="priority-badge" [class]="priorityBadgeClass(item.priorite)">{{ item.priorite }}</b></span>
                <span>{{ item.action }}</span>
                <span>{{ item.justification }}</span>
                <span>{{ item.impactAttendu }}</span>
                <span><b class="difficulty-badge" [class]="difficultyBadgeClass(item.difficulte)">{{ item.difficulte }}</b></span>
              </div>
            </div>
          </div>

          <div *ngIf="showDecisionReasoning()" class="decision-reasoning">
            <div class="decision-panel justification-panel">
              <h4><span class="material-symbols-rounded" aria-hidden="true">fact_check</span>Justification décisionnelle</h4>
              <p>{{ decisionJustification(data) }}</p>
              <div class="key-reminders">
                <span>Digital : <b>{{ data.digitalTransactions }} / {{ data.totalTransactions }}</b></span>
                <span>Carte : <b>{{ formatPercent(sourcePercentage(data, 'CARD'), 2) }}</b></span>
                <span>Cash : <b>{{ sourceCount(data, 'CASH') }}</b></span>
                <span>À convertir : <b>{{ data.strategicDecision.transactionsAConvertir }}</b></span>
              </div>
            </div>

            <div class="decision-panel impact-v2">
              <h4><span class="material-symbols-rounded" aria-hidden="true">trending_up</span>Impact attendu</h4>
              <div class="impact-flow">
                <article><span>Situation actuelle</span><strong>{{ formatPercent(data.impact.currentDigitalisationRate, 2) }}</strong></article>
                <span class="flow-arrow">→</span>
                <article><span>Objectif</span><strong>{{ formatPercent(data.impact.targetDigitalisationRate, 0) }}</strong></article>
                <article><span>Gain nécessaire</span><strong>+{{ formatPercent(data.impact.requiredGain, 2) }}</strong></article>
                <article><span>Transactions à convertir</span><strong>{{ data.strategicDecision.transactionsAConvertir }}</strong></article>
              </div>
              <b class="reachable-badge">Objectif atteignable</b>
              <p>Résultat attendu : {{ data.strategicDecision.transactionsAConvertir > 0 ? 'Objectif atteint après conversion ciblée du cash.' : 'Objectif déjà atteint.' }}</p>
            </div>
          </div>

          <div class="decision-panel conclusion">
            <h4><span class="material-symbols-rounded" aria-hidden="true">summarize</span>Conclusion exécutive</h4>
            <p>{{ data.executiveConclusion }}</p>
          </div>

          <div class="final-decision">
            <div class="panel-title">
              <span class="material-symbols-rounded" aria-hidden="true">gavel</span>
              <h4>Décision finale</h4>
            </div>
            <div class="final-grid">
              <article><span>Niveau de priorité</span><strong>{{ formatPriority(data.priorityLevel) }}</strong></article>
              <article><span>Décision recommandée</span><strong>{{ data.strategicDecision.decisionRecommandee }}</strong></article>
              <article><span>Gain attendu</span><strong>+{{ formatPercent(data.impact.requiredGain, 2) }}</strong></article>
              <article><span>Effort estimé</span><strong>{{ estimatedEffort(data) }}</strong></article>
              <article><span>Horizon estimé</span><strong>{{ estimatedHorizon(data) }}</strong></article>
            </div>
            <small>Estimation métier basée sur des règles décisionnelles, sans prédiction machine learning.</small>
          </div>

          <div *ngIf="showDecisionDetails()" class="decision-details">
            <h4>Détails par source</h4>
            <div class="source-table" role="table">
              <div class="source-row source-header" role="row">
                <span>Source</span><span>Transactions</span><span>Part</span><span>Montant total</span><span>Montant moyen</span>
              </div>
              <div class="source-row" role="row" *ngFor="let source of data.sources">
                <span><b class="source-badge" [class]="source.source.toLowerCase()">{{ formatSource(source.source) }}</b></span>
                <span>{{ source.transactionCount }}</span>
                <span>{{ formatPercent(source.percentage, 2) }}</span>
                <span>{{ formatAmount(source.totalAmount) }}</span>
                <span>{{ formatAmount(source.averageAmount) }}</span>
              </div>
            </div>
          </div>
        </ng-container>
      </section>
    </div>
  `,
  styles: [`
    .page { display: grid; gap: 1.2rem; }
    .page-head h2 { margin: 0; font-size: 1.35rem; letter-spacing: 0; }
    .page-head p { margin: .35rem 0 0; color: #6b7280; }
    .powerbi-card, .decision-card { background: #fff; border: 1px solid #ececf0; border-radius: 12px; padding: 1rem; box-shadow: 0 18px 42px rgba(17,17,17,.06); }
    .powerbi-head, .decision-head { display: flex; align-items: flex-start; justify-content: space-between; gap: 1rem; margin-bottom: 1rem; }
    .powerbi-head span, .decision-head span, .diagnostic-title span:not(.material-symbols-rounded) { display: block; color: #f58220; font-size: .75rem; font-weight: 800; letter-spacing: .06em; text-transform: uppercase; }
    .powerbi-head h3, .decision-head h3 { margin: .25rem 0 0; color: #111; font-size: 1.12rem; letter-spacing: 0; }
    .powerbi-actions, .decision-actions { display: inline-flex; align-items: center; gap: .55rem; flex-wrap: wrap; justify-content: flex-end; }
    .powerbi-actions strong { display: inline-flex; align-items: center; padding: .35rem .7rem; border-radius: 999px; background: #fff4e8; color: #9a4d00; font-size: .78rem; }
    .powerbi-refresh, .decision-button { display: inline-flex; align-items: center; justify-content: center; gap: .4rem; min-height: 38px; padding: .5rem .8rem; border-radius: 999px; font-size: .78rem; font-weight: 800; cursor: pointer; transition: transform .18s ease, background .18s ease, box-shadow .18s ease; white-space: nowrap; }
    .powerbi-refresh, .decision-button.primary { border: 1px solid rgba(245,130,32,.22); background: #111; color: #fff; box-shadow: 0 10px 22px rgba(17,17,17,.12); }
    .powerbi-refresh.pdf { background: #f58220; }
    .decision-button.secondary { border: 1px solid rgba(17,17,17,.12); background: #fff; color: #111; }
    .powerbi-refresh:hover:not(:disabled), .decision-button.primary:hover:not(:disabled) { transform: translateY(-1px); background: #f58220; box-shadow: 0 14px 28px rgba(245,130,32,.2); }
    .decision-button.secondary:hover:not(:disabled) { transform: translateY(-1px); background: #fff4e8; border-color: rgba(245,130,32,.28); }
    .powerbi-refresh:disabled, .decision-button:disabled { cursor: not-allowed; opacity: .55; transform: none; box-shadow: none; }
    .material-symbols-rounded { font-size: 1.05rem; line-height: 1; }
    .pdf-message { display: flex; align-items: center; gap: .45rem; margin: 0 0 .8rem; padding: .7rem .85rem; border-radius: 8px; font-weight: 800; }
    .pdf-message.success { color: #047857; background: #d1fae5; }
    .pdf-message.error { color: #991b1b; background: #fee2e2; }
    .powerbi-frame-wrap { position: relative; width: 100%; height: clamp(680px, 76vh, 880px); overflow: hidden; border-radius: 10px; background: #111; }
    .powerbi-frame { position: absolute; left: 0; top: -38px; width: 100%; height: calc(100% + 38px); border: 0; background: #fff; }
    .decision-state, .decision-error { display: flex; align-items: center; gap: .65rem; min-height: 64px; padding: .85rem 1rem; border-radius: 10px; font-weight: 700; }
    .decision-state { color: #4b5563; background: #f8fafc; }
    .decision-error { color: #991b1b; background: #fff1f2; border: 1px solid #fecdd3; }
    .decision-loader { width: 18px; height: 18px; border-radius: 999px; border: 3px solid #f3f4f6; border-top-color: #f58220; animation: decision-spin .8s linear infinite; }
    .diagnostic-hero { display: grid; gap: 1rem; padding: 1.1rem; border-radius: 12px; background: #111; color: #fff; box-shadow: inset 0 0 0 1px rgba(255,255,255,.06); }
    .diagnostic-title { display: grid; grid-template-columns: auto minmax(0, 1fr) auto; align-items: center; gap: .85rem; }
    .diagnostic-title > .material-symbols-rounded { display: grid; place-items: center; width: 42px; height: 42px; border-radius: 10px; background: #f58220; color: #111; font-size: 1.55rem; }
    .diagnostic-title strong { display: block; margin-top: .2rem; font-size: 1.5rem; line-height: 1.1; letter-spacing: 0; }
    .diagnostic-grid { display: grid; grid-template-columns: repeat(4, minmax(0, 1fr)); gap: .7rem; }
    .diagnostic-grid div { padding: .85rem; border-radius: 10px; background: rgba(255,255,255,.07); min-width: 0; }
    .diagnostic-grid span { display: block; color: rgba(255,255,255,.68); font-size: .72rem; font-weight: 800; text-transform: uppercase; }
    .diagnostic-grid strong { display: block; margin-top: .3rem; color: #fff; font-size: 1.08rem; overflow-wrap: anywhere; }
    .ai-decision { display: flex; gap: .65rem; align-items: flex-start; padding: .85rem; border-radius: 10px; background: rgba(245,130,32,.13); color: rgba(255,255,255,.9); }
    .ai-decision p { margin: 0; line-height: 1.45; }
    .status-badge, .priority-badge, .difficulty-badge, .reachable-badge { display: inline-flex; width: fit-content; align-items: center; padding: .34rem .62rem; border-radius: 999px; font-size: .76rem; font-weight: 900; }
    .status-badge.ok, .priority-badge.low, .difficulty-badge.low, .reachable-badge { background: #d1fae5; color: #047857; }
    .status-badge.near, .priority-badge.medium, .difficulty-badge.medium { background: #fff4e8; color: #9a4d00; }
    .status-badge.warning { background: #fef3c7; color: #92400e; }
    .status-badge.critical, .priority-badge.high { background: #fee2e2; color: #991b1b; }
    .decision-kpis { display: grid; grid-template-columns: repeat(5, minmax(0, 1fr)); gap: .75rem; margin-top: .85rem; }
    .decision-kpi { min-height: 96px; padding: .85rem; border: 1px solid #ececf0; border-radius: 10px; background: #fafafa; display: flex; flex-direction: column; justify-content: space-between; gap: .6rem; }
    .decision-kpi.highlight { background: #111; color: #fff; border-color: #111; }
    .decision-kpi span { color: inherit; opacity: .72; font-size: .74rem; font-weight: 800; text-transform: uppercase; letter-spacing: .04em; }
    .decision-kpi strong { color: inherit; font-size: 1.16rem; line-height: 1.15; overflow-wrap: anywhere; }
    .decision-kpi strong.positive { color: #047857; }
    .priority.low { color: #047857; background: #d1fae5; }
    .priority.medium { color: #9a4d00; background: #fff4e8; }
    .priority.high { color: #991b1b; background: #fee2e2; }
    .visual-grid { display: grid; grid-template-columns: minmax(0, 1fr) minmax(0, 1fr); gap: .9rem; margin-top: .9rem; }
    .viz-panel, .decision-panel, .decision-details, .final-decision { padding: .95rem; border: 1px solid #ececf0; border-radius: 10px; background: #fff; }
    .panel-title, .decision-panel h4 { display: flex; align-items: center; gap: .45rem; margin: 0 0 .8rem; color: #111; font-size: .98rem; line-height: 1.25; }
    .panel-title h4 { margin: 0; font-size: .98rem; }
    .panel-title .material-symbols-rounded, .decision-panel h4 .material-symbols-rounded { color: #f58220; }
    .goal-meta { display: flex; justify-content: space-between; gap: .8rem; margin-bottom: .75rem; color: #4b5563; font-size: .86rem; }
    .goal-track { position: relative; height: 15px; border-radius: 999px; background: #f1f5f9; overflow: hidden; }
    .goal-fill { display: block; height: 100%; border-radius: inherit; background: linear-gradient(90deg, #f58220, #111); transition: width .45s ease; }
    .goal-target { position: absolute; top: -3px; bottom: -3px; width: 2px; background: #c8102e; box-shadow: 0 0 0 3px rgba(200,16,46,.14); }
    .source-stack { display: flex; height: 16px; overflow: hidden; border-radius: 999px; background: #f1f5f9; }
    .source-stack span { display: block; transition: width .45s ease; }
    .stack-card { background: #f58220; }
    .stack-bank { background: #111; }
    .stack-cash { background: #378b11; }
    .source-legend { display: flex; flex-wrap: wrap; gap: .55rem .85rem; margin-top: .75rem; color: #4b5563; font-size: .82rem; }
    .source-legend span { display: inline-flex; align-items: center; gap: .35rem; }
    .source-legend i { width: .65rem; height: .65rem; border-radius: 999px; }
    .card-dot { background: #f58220; }
    .bank-dot { background: #111; }
    .cash-dot { background: #378b11; }
    .decision-grid, .decision-reasoning { display: grid; grid-template-columns: minmax(0, 1fr) minmax(320px, 1fr); gap: .9rem; margin-top: .9rem; }
    .analysis-panel { display: grid; gap: .6rem; align-content: start; }
    .analysis-panel p, .decision-panel p, .final-decision small { margin: 0; color: #374151; line-height: 1.5; }
    .decision-panel dl { display: grid; gap: .72rem; margin: 0; }
    .decision-panel dt { color: #f58220; font-weight: 900; font-size: .76rem; text-transform: uppercase; letter-spacing: .04em; }
    .decision-panel dd { margin: .2rem 0 0; color: #374151; line-height: 1.45; }
    .action-plan, .conclusion, .final-decision, .decision-details { margin-top: .9rem; }
    .action-table, .source-table { display: grid; gap: .4rem; overflow-x: auto; padding-bottom: .1rem; }
    .action-row { display: grid; grid-template-columns: 105px minmax(170px, 1fr) minmax(190px, 1fr) minmax(180px, 1fr) 100px; gap: .65rem; min-width: 920px; align-items: center; padding: .78rem .8rem; border-radius: 8px; background: #fafafa; color: #374151; font-size: .84rem; transition: background .18s ease, transform .18s ease; }
    .action-row:not(.action-header):hover { background: #fff7ed; transform: translateY(-1px); }
    .action-header, .source-header { background: #111; color: #fff; font-weight: 900; }
    .justification-panel p { font-size: .94rem; }
    .key-reminders { display: grid; grid-template-columns: repeat(4, minmax(0, 1fr)); gap: .55rem; margin-top: .8rem; }
    .key-reminders span, .impact-flow article, .final-grid article { padding: .72rem; border-radius: 8px; background: #fafafa; color: #4b5563; font-size: .8rem; min-width: 0; }
    .key-reminders b, .impact-flow strong, .final-grid strong { display: block; margin-top: .25rem; color: #111; font-size: .98rem; overflow-wrap: anywhere; }
    .impact-flow { display: grid; grid-template-columns: minmax(0, 1fr) auto minmax(0, 1fr); gap: .55rem; align-items: center; }
    .impact-flow article:nth-of-type(n+3) { grid-column: span 1; }
    .flow-arrow { color: #f58220; font-weight: 900; font-size: 1.25rem; }
    .impact-v2 p { margin-top: .75rem; }
    .reachable-badge { margin-top: .75rem; }
    .final-grid { display: grid; grid-template-columns: repeat(5, minmax(0, 1fr)); gap: .6rem; }
    .final-decision small { display: block; margin-top: .75rem; color: #6b7280; }
    .source-row { display: grid; grid-template-columns: minmax(150px, 1.1fr) repeat(4, minmax(110px, .8fr)); gap: .5rem; min-width: 720px; align-items: center; padding: .7rem .75rem; border-radius: 8px; background: #fafafa; color: #374151; font-size: .84rem; }
    .source-badge { display: inline-flex; width: fit-content; align-items: center; padding: .28rem .55rem; border-radius: 999px; font-size: .76rem; color: #111; background: #f3f4f6; }
    .source-badge.card { color: #9a4d00; background: #fff4e8; }
    .source-badge.cash { color: #047857; background: #d1fae5; }
    .source-badge.bank_transfer { color: #fff; background: #111; }
    .source-badge.unknown { color: #6b7280; background: #f3f4f6; }
    @keyframes decision-spin { to { transform: rotate(360deg); } }
    @media (max-width: 1100px) {
      .decision-kpis, .final-grid { grid-template-columns: repeat(3, minmax(0, 1fr)); }
      .diagnostic-grid { grid-template-columns: repeat(2, minmax(0, 1fr)); }
    }
    @media (max-width: 900px) {
      .visual-grid, .decision-grid, .decision-reasoning { grid-template-columns: 1fr; }
      .key-reminders { grid-template-columns: repeat(2, minmax(0, 1fr)); }
    }
    @media (max-width: 720px) {
      .powerbi-card, .decision-card { padding: .75rem; }
      .powerbi-head, .decision-head { flex-direction: column; }
      .powerbi-actions, .decision-actions { width: 100%; justify-content: stretch; }
      .powerbi-refresh, .decision-button { flex: 1 1 100%; }
      .powerbi-frame-wrap { height: 500px; }
      .powerbi-frame { top: -34px; height: calc(100% + 34px); }
      .diagnostic-title { grid-template-columns: auto minmax(0, 1fr); }
      .diagnostic-title .status-badge { grid-column: 1 / -1; }
      .decision-kpis, .diagnostic-grid, .final-grid, .key-reminders, .impact-flow { grid-template-columns: 1fr; }
      .flow-arrow { display: none; }
      .goal-meta { flex-direction: column; gap: .25rem; }
    }
  `]
})
export class AdminDashboardComponent implements OnInit {
  private readonly sanitizer = inject(DomSanitizer);
  private readonly adminDecisionService = inject(AdminDecisionService);
  private readonly powerBiReloadToken = signal(0);
  readonly decision = signal<TransactionSourceDecisionDto | null>(null);
  readonly decisionLoading = signal(false);
  readonly decisionError = signal<string | null>(null);
  readonly showDecisionDetails = signal(false);
  readonly showDecisionReasoning = signal(false);
  readonly pdfGenerating = signal(false);
  readonly pdfMessage = signal<string | null>(null);
  readonly pdfError = signal<string | null>(null);
  private readonly pdfLogoPath = 'assets/images/attijari-compass-logo.png';
  readonly powerBiReportUrl = computed<SafeResourceUrl>(() =>
    this.sanitizer.bypassSecurityTrustResourceUrl(
      `https://app.powerbi.com/reportEmbed?reportId=4a43b4c7-5e85-4e5e-8010-4911c77efc76&autoAuth=true&embeddedDemo=true&filterPaneEnabled=false&navContentPaneEnabled=false&reload=${this.powerBiReloadToken()}`
    )
  );

  ngOnInit(): void {
    this.loadDecisionAnalysis();
  }

  refreshPowerBiReport(): void {
    this.powerBiReloadToken.update(value => value + 1);
    this.loadDecisionAnalysis();
  }

  loadDecisionAnalysis(): void {
    this.decisionLoading.set(true);
    this.decisionError.set(null);
    this.pdfMessage.set(null);
    this.pdfError.set(null);

    this.adminDecisionService.getTransactionSourceDecision().subscribe({
      next: response => {
        this.decision.set({
          ...response,
          sources: [...(response.sources ?? [])],
          actionPlan: [...(response.actionPlan ?? [])],
          recommendedActions: [...(response.recommendedActions ?? [])]
        });
        this.decisionLoading.set(false);
      },
      error: () => {
        this.decisionLoading.set(false);
        this.decisionError.set("Impossible de charger l'analyse décisionnelle pour le moment.");
      }
    });
  }

  async generateDecisionPdf(): Promise<void> {
    const data = this.decision();
    if (!data || this.pdfGenerating()) {
      return;
    }

    this.pdfGenerating.set(true);
    this.pdfMessage.set(null);
    this.pdfError.set(null);

    try {
      const startedAt = performance.now();
      const logoDataUrl = await this.loadImageAsDataUrl(this.pdfLogoPath);
      const doc = new jsPDF({ orientation: 'portrait', unit: 'mm', format: 'a4' });
      let y = this.drawPdfHeader(doc, logoDataUrl);
      const analysisDurationMs = Math.max(1, Math.round(performance.now() - startedAt));

      y = this.pdfDiagnosticSection(doc, data, y, analysisDurationMs);
      y = this.pdfKpiSection(doc, data, y);
      y = this.pdfSourceChartSection(doc, data, y);
      y = this.pdfDigitalProgressSection(doc, data, y);
      y = this.pdfTextSection(doc, 'Analyse automatique', [
        `Constat : ${data.analyse.constat}`,
        `Interprétation : ${data.analyse.interpretation}`,
        `Risque : ${data.analyse.risque}`,
        `Décision recommandée : ${data.analyse.decisionRecommandee}`
      ], y);
      y = this.pdfTextSection(doc, 'Décision stratégique proposée', [
        `Objectif : ${data.strategicDecision.objectif}`,
        `Levier principal : ${data.strategicDecision.levierPrincipal}`,
        `Levier secondaire : ${data.strategicDecision.levierSecondaire}`,
        `Justification : ${data.strategicDecision.justification}`,
        `Décision recommandée : ${data.strategicDecision.decisionRecommandee}`,
        `Impact attendu : ${data.strategicDecision.impactAttendu}`
      ], y);
      y = this.pdfActionPlanSection(doc, data, y);
      y = this.pdfTextSection(doc, 'Conclusion exécutive', [data.executiveConclusion], y);
      y = this.pdfTextSection(doc, 'Justification décisionnelle', [this.decisionJustification(data)], y);
      y = this.pdfImpactSection(doc, data, y);
      y = this.pdfFinalDecisionSection(doc, data, y);
      y = this.pdfDetailsSection(doc, data, y);
      this.pdfConfidentialityNote(doc, y);
      this.drawPdfFooters(doc);

      const fileDate = new Date().toISOString().slice(0, 10);
      doc.save(`rapport-decisionnel-sources-transactions-${fileDate}.pdf`);
      this.pdfMessage.set('Rapport PDF généré avec succès');
    } catch (error) {
      console.error('PDF generation failed', error);
      this.pdfError.set('Impossible de générer le rapport PDF pour le moment.');
    } finally {
      this.pdfGenerating.set(false);
    }
  }

  toggleDecisionDetails(): void {
    this.showDecisionDetails.update(value => !value);
  }

  toggleDecisionReasoning(): void {
    this.showDecisionReasoning.update(value => !value);
  }

  decisionJustification(data: TransactionSourceDecisionDto): string {
    return `La priorité est ${this.formatPriority(data.priorityLevel).toLowerCase()} car l'objectif est presque atteint : ${data.digitalTransactions} transactions digitales sur ${data.totalTransactions}, soit ${this.formatPercent(data.digitalisationRate, 2)}. L'écart est limité à ${this.formatPercent(Math.abs(data.digitalisationGap), 2)}. Le principal levier n'est pas d'augmenter massivement les cartes, déjà dominantes avec ${this.formatPercent(this.sourcePercentage(data, 'CARD'), 2)}, mais de convertir environ ${data.strategicDecision.transactionsAConvertir} transactions cash vers des moyens digitaux.`;
  }

  estimatedEffort(data: TransactionSourceDecisionDto): string {
    const toConvert = data.strategicDecision.transactionsAConvertir ?? 0;
    if (toConvert <= 5) {
      return 'Faible';
    }
    if (toConvert <= 20) {
      return 'Moyen';
    }
    return 'Élevé';
  }

  estimatedHorizon(data: TransactionSourceDecisionDto): string {
    const toConvert = data.strategicDecision.transactionsAConvertir ?? 0;
    if (toConvert <= 5) {
      return '1 à 2 mois';
    }
    if (toConvert <= 20) {
      return '2 à 3 mois';
    }
    return '3 à 6 mois';
  }

  statusClass(status: string | null | undefined): string {
    switch ((status ?? '').toLowerCase()) {
      case 'objectif atteint':
        return 'ok';
      case 'presque atteint':
        return 'near';
      case 'attention':
        return 'warning';
      case 'critique':
        return 'critical';
      default:
        return 'warning';
    }
  }

  priorityBadgeClass(priority: string | null | undefined): string {
    switch ((priority ?? '').toLowerCase()) {
      case 'haute':
        return 'high';
      case 'moyenne':
        return 'medium';
      case 'faible':
        return 'low';
      default:
        return 'medium';
    }
  }

  difficultyBadgeClass(difficulty: string | null | undefined): string {
    return (difficulty ?? '').toLowerCase() === 'moyenne' ? 'medium' : 'low';
  }

  formatSource(source: DecisionTransactionSource | null | undefined): string {
    switch (source) {
      case 'CARD':
        return 'Carte';
      case 'CASH':
        return 'Cash';
      case 'BANK_TRANSFER':
        return 'Virement bancaire';
      case 'UNKNOWN':
        return 'Inconnue';
      default:
        return 'Non disponible';
    }
  }

  formatPriority(priority: TransactionDecisionPriority | null | undefined): string {
    switch (priority) {
      case 'LOW':
        return 'Faible';
      case 'MEDIUM':
        return 'Moyenne';
      case 'HIGH':
        return 'Haute';
      default:
        return 'Non disponible';
    }
  }

  formatPercent(value: number | null | undefined, digits = 0): string {
    return `${this.normalizeNumber(value).toFixed(digits)} %`;
  }

  formatSignedPercent(value: number | null | undefined, digits = 0): string {
    const normalized = this.normalizeNumber(value);
    return `${normalized > 0 ? '+' : ''}${normalized.toFixed(digits)} %`;
  }

  formatAmount(value: number | null | undefined): string {
    return `${this.normalizeNumber(value).toFixed(2)} DT`;
  }

  progressWidth(value: number | null | undefined): number {
    return Math.min(100, Math.max(0, this.normalizeNumber(value)));
  }

  sourcePercentage(data: TransactionSourceDecisionDto, source: DecisionTransactionSource): number {
    return this.sourceMetric(data, source)?.percentage ?? 0;
  }

  sourceCount(data: TransactionSourceDecisionDto, source: DecisionTransactionSource): number {
    return this.sourceMetric(data, source)?.transactionCount ?? 0;
  }

  private sourceMetric(data: TransactionSourceDecisionDto, source: DecisionTransactionSource): TransactionSourceMetricDto | undefined {
    return data.sources.find(item => item.source === source);
  }

  private drawPdfHeader(doc: jsPDF, logoDataUrl: string | null): number {
    const black = [17, 17, 17] as [number, number, number];
    const orange = [245, 130, 32] as [number, number, number];
    doc.setFillColor(...black);
    doc.rect(0, 0, 210, 34, 'F');
    if (logoDataUrl) {
      doc.addImage(logoDataUrl, 'PNG', 12, 7, 17, 17);
    } else {
      doc.setFillColor(...orange);
      doc.roundedRect(12, 7, 17, 17, 3, 3, 'F');
      doc.setTextColor(...black);
      doc.setFontSize(10);
      doc.text('AC', 16, 18);
    }
    doc.setTextColor(...orange);
    doc.setFontSize(12);
    doc.text('Attijari Compass', 34, 12);
    doc.setTextColor(255, 255, 255);
    doc.setFontSize(9);
    doc.text('Centre d’aide à la décision', 34, 18);
    doc.setFontSize(14);
    doc.text('Rapport décisionnel - Sources de transactions', 34, 27);
    doc.setFontSize(8);
    doc.text(`Date de génération : ${new Date().toLocaleString('fr-FR')}`, 128, 13);
    doc.text('Document confidentiel - Usage interne', 128, 19);
    return 44;
  }

  private drawPdfFooters(doc: jsPDF): void {
    const totalPages = doc.getNumberOfPages();
    for (let index = 1; index <= totalPages; index++) {
      doc.setPage(index);
      doc.setDrawColor(230, 230, 230);
      doc.line(12, 287, 198, 287);
      doc.setTextColor(90, 90, 90);
      doc.setFontSize(8);
      doc.text('Attijari Compass - Centre d’aide à la décision', 12, 292);
      doc.text('Rapport généré automatiquement - Version 1.0', 82, 292);
      doc.text(`Page ${index} / ${totalPages}`, 182, 292);
    }
  }

  private pdfDiagnosticSection(doc: jsPDF, data: TransactionSourceDecisionDto, startY: number, analysisDurationMs: number): number {
    this.pdfSectionTitle(doc, 'Diagnostic décisionnel', startY);
    autoTable(doc, {
      startY: startY + 5,
      theme: 'grid',
      head: [['État', 'Situation actuelle', 'Objectif', 'Transactions digitales', 'Écart', 'Temps d’analyse']],
      body: [[
        data.diagnostic.globalStatus,
        this.formatPercent(data.digitalisationRate, 2),
        this.formatPercent(data.digitalisationTarget, 0),
        `${data.digitalTransactions} / ${data.totalTransactions}`,
        this.formatSignedPercent(data.digitalisationGap, 2),
        `${analysisDurationMs} ms`
      ]],
      headStyles: { fillColor: [17, 17, 17], textColor: [255, 255, 255] },
      styles: { fontSize: 7.8, cellPadding: 2.1 },
      columnStyles: {
        0: { cellWidth: 34 },
        1: { cellWidth: 29 },
        2: { cellWidth: 22 },
        3: { cellWidth: 38 },
        4: { cellWidth: 22 },
        5: { cellWidth: 28 }
      },
      didParseCell: event => {
        if (event.section === 'body' && event.column.index === 0) {
          event.cell.text = [''];
        }
      },
      didDrawCell: event => {
        if (event.section === 'body' && event.column.index === 0) {
          this.drawPdfBadge(doc, String(data.diagnostic.globalStatus), event.cell.x + 1.4, event.cell.y + 2.1, this.statusPdfColor(data.diagnostic.globalStatus), 31);
        }
      }
    });
    let y = this.nextPdfY(doc) - 7;
    y = this.pdfTextBlock(doc, y, `Décision IA courte : ${data.impact.mainAction}`);
    return y + 3;
  }

  private pdfKpiSection(doc: jsPDF, data: TransactionSourceDecisionDto, startY: number): number {
    this.pdfSectionTitle(doc, 'Synthèse KPI', startY);
    autoTable(doc, {
      startY: startY + 5,
      theme: 'striped',
      head: [['Indicateur', 'Valeur']],
      body: [
        ['Total transactions', data.totalTransactions],
        ['CARD', this.sourceCount(data, 'CARD')],
        ['CASH', this.sourceCount(data, 'CASH')],
        ['BANK_TRANSFER', this.sourceCount(data, 'BANK_TRANSFER')],
        ['Transactions digitales', data.digitalTransactions],
        ['Indice de digitalisation', this.formatPercent(data.digitalisationRate, 2)],
        ['Objectif', this.formatPercent(data.digitalisationTarget, 0)],
        ['Écart', this.formatSignedPercent(data.digitalisationGap, 2)],
        ['Priorité', this.formatPriority(data.priorityLevel)]
      ],
      headStyles: { fillColor: [245, 130, 32] },
      styles: { fontSize: 8.2, cellPadding: 1.8 },
      didParseCell: event => {
        const row = event.row.raw as unknown[];
        if (event.section === 'body' && row?.[0] === 'Priorité' && event.column.index === 1) {
          event.cell.text = [''];
        }
      },
      didDrawCell: event => {
        const row = event.row.raw as unknown[];
        if (event.section === 'body' && row?.[0] === 'Priorité' && event.column.index === 1) {
          const priority = this.formatPriority(data.priorityLevel);
          this.drawPdfBadge(doc, priority, event.cell.x + 2, event.cell.y + 1.8, this.priorityPdfColor(priority));
        }
      }
    });
    return this.nextPdfY(doc) - 1;
  }

  private pdfSourceChartSection(doc: jsPDF, data: TransactionSourceDecisionDto, startY: number): number {
    let y = this.ensurePdfSpace(doc, startY, 41);
    this.pdfSectionTitle(doc, 'Graphique des sources', y);
    y += 6;
    const rows: Array<{ source: DecisionTransactionSource; color: [number, number, number] }> = [
      { source: 'CARD', color: [245, 130, 32] },
      { source: 'BANK_TRANSFER', color: [17, 17, 17] },
      { source: 'CASH', color: [55, 139, 17] }
    ];
    const maxWidth = 105;
    for (const row of rows) {
      const metric = this.sourceMetric(data, row.source);
      const percentage = metric?.percentage ?? 0;
      const count = metric?.transactionCount ?? 0;
      doc.setFontSize(8.5);
      doc.setTextColor(17, 17, 17);
      doc.text(`${row.source} : ${count} - ${this.formatPercent(percentage, 2)}`, 14, y + 4);
      doc.setFillColor(241, 245, 249);
      doc.roundedRect(74, y, maxWidth, 4.8, 2.4, 2.4, 'F');
      doc.setFillColor(...row.color);
      doc.roundedRect(74, y, Math.max(2, (percentage / 100) * maxWidth), 4.8, 2.4, 2.4, 'F');
      y += 8.8;
    }
    return y + 3;
  }

  private pdfDigitalProgressSection(doc: jsPDF, data: TransactionSourceDecisionDto, startY: number): number {
    let y = this.ensurePdfSpace(doc, startY, 29);
    this.pdfSectionTitle(doc, 'Barre de digitalisation', y);
    y += 7;
    const x = 14;
    const width = 170;
    doc.setFontSize(8.5);
    doc.setTextColor(55, 65, 81);
    doc.text(`Situation actuelle : ${this.formatPercent(data.digitalisationRate, 2)}`, x, y);
    doc.text(`Objectif : ${this.formatPercent(data.digitalisationTarget, 0)}`, x + 82, y);
    y += 5;
    doc.setFillColor(241, 245, 249);
    doc.roundedRect(x, y, width, 6, 3, 3, 'F');
    doc.setFillColor(245, 130, 32);
    doc.roundedRect(x, y, (this.progressWidth(data.digitalisationRate) / 100) * width, 6, 3, 3, 'F');
    const targetX = x + (this.progressWidth(data.digitalisationTarget) / 100) * width;
    doc.setDrawColor(17, 17, 17);
    doc.setLineWidth(0.7);
    doc.line(targetX, y - 2, targetX, y + 8);
    y += 11;
    doc.setTextColor(17, 17, 17);
    doc.text(`Écart restant : ${this.formatPercent(Math.abs(data.digitalisationGap), 2)}`, x, y);
    return y + 5;
  }

  private pdfActionPlanSection(doc: jsPDF, data: TransactionSourceDecisionDto, startY: number): number {
    const y = this.ensurePdfSpace(doc, startY, 43);
    this.pdfSectionTitle(doc, "Plan d'action recommandé", y);
    autoTable(doc, {
      startY: y + 5,
      theme: 'grid',
      head: [['Priorité', 'Action', 'Justification', 'Impact attendu', 'Difficulté']],
      body: data.actionPlan.map(item => [item.priorite, item.action, item.justification, item.impactAttendu, item.difficulte]),
      headStyles: { fillColor: [245, 130, 32] },
      styles: { fontSize: 7.4, cellPadding: 1.45, minCellHeight: 7 },
      columnStyles: { 0: { cellWidth: 22 }, 1: { cellWidth: 42 }, 2: { cellWidth: 45 }, 3: { cellWidth: 44 }, 4: { cellWidth: 22 } },
      didParseCell: event => {
        if (event.section === 'body' && (event.column.index === 0 || event.column.index === 4)) {
          event.cell.text = [''];
        }
      },
      didDrawCell: event => {
        if (event.section !== 'body') {
          return;
        }
        const row = event.row.raw as string[];
        if (event.column.index === 0) {
          this.drawPdfBadge(doc, row[0], event.cell.x + 1.2, event.cell.y + 1.7, this.priorityPdfColor(row[0]), 18);
        }
        if (event.column.index === 4) {
          this.drawPdfBadge(doc, row[4], event.cell.x + 1.2, event.cell.y + 1.7, this.difficultyPdfColor(row[4]), 18);
        }
      }
    });
    return this.nextPdfY(doc) - 1;
  }

  private pdfImpactSection(doc: jsPDF, data: TransactionSourceDecisionDto, startY: number): number {
    const y = this.ensurePdfSpace(doc, startY, 24);
    this.pdfSectionTitle(doc, 'Impact attendu', y);
    autoTable(doc, {
      startY: y + 5,
      theme: 'grid',
      head: [['Situation actuelle', 'Objectif', 'Gain nécessaire', 'Transactions à convertir', 'Résultat attendu']],
      body: [[
        this.formatPercent(data.impact.currentDigitalisationRate, 2),
        this.formatPercent(data.impact.targetDigitalisationRate, 0),
        `+${this.formatPercent(data.impact.requiredGain, 2)}`,
        data.strategicDecision.transactionsAConvertir,
        data.strategicDecision.transactionsAConvertir > 0 ? 'Objectif atteint après conversion ciblée' : 'Objectif déjà atteint'
      ]],
      headStyles: { fillColor: [17, 17, 17] },
      styles: { fontSize: 7.8, cellPadding: 1.7 }
    });
    return this.nextPdfY(doc) - 1;
  }

  private pdfFinalDecisionSection(doc: jsPDF, data: TransactionSourceDecisionDto, startY: number): number {
    const y = this.ensurePdfSpace(doc, startY, 30);
    this.pdfSectionTitle(doc, 'Décision finale', y);
    autoTable(doc, {
      startY: y + 5,
      theme: 'grid',
      head: [['Priorité', 'Décision recommandée', 'Gain attendu', 'Effort estimé', 'Horizon estimé']],
      body: [[
        this.formatPriority(data.priorityLevel),
        data.strategicDecision.decisionRecommandee,
        `+${this.formatPercent(data.impact.requiredGain, 2)}`,
        this.estimatedEffort(data),
        this.estimatedHorizon(data)
      ]],
      headStyles: { fillColor: [245, 130, 32] },
      styles: { fontSize: 7.7, cellPadding: 1.7 },
      columnStyles: { 0: { cellWidth: 24 } },
      didParseCell: event => {
        if (event.section === 'body' && event.column.index === 0) {
          event.cell.text = [''];
        }
      },
      didDrawCell: event => {
        if (event.section === 'body' && event.column.index === 0) {
          const priority = this.formatPriority(data.priorityLevel);
          this.drawPdfBadge(doc, priority, event.cell.x + 1.2, event.cell.y + 1.7, this.priorityPdfColor(priority), 20);
        }
      }
    });
    return this.nextPdfY(doc) - 1;
  }

  private pdfDetailsSection(doc: jsPDF, data: TransactionSourceDecisionDto, startY: number): number {
    const y = this.ensurePdfSpace(doc, startY, 40);
    this.pdfSectionTitle(doc, 'Tableau détaillé par source', y);
    autoTable(doc, {
      startY: y + 5,
      theme: 'grid',
      head: [['Source', 'Transactions', 'Part', 'Montant total', 'Montant moyen']],
      body: data.sources.map(source => [
        this.formatSource(source.source),
        source.transactionCount,
        this.formatPercent(source.percentage, 2),
        this.formatAmount(source.totalAmount),
        this.formatAmount(source.averageAmount)
      ]),
      headStyles: { fillColor: [17, 17, 17] },
      styles: { fontSize: 7.8, cellPadding: 1.7 }
    });
    return this.nextPdfY(doc) - 2;
  }

  private pdfConfidentialityNote(doc: jsPDF, startY: number): number {
    let y = this.ensurePdfSpace(doc, startY, 27);
    doc.setFillColor(255, 244, 232);
    doc.setDrawColor(245, 130, 32);
    doc.roundedRect(12, y, 186, 22, 3, 3, 'FD');
    doc.setTextColor(17, 17, 17);
    doc.setFontSize(9);
    doc.text('Note :', 16, y + 7);
    const text = 'Ce rapport est généré automatiquement à partir des données disponibles dans Attijari Compass. Les recommandations proposées constituent une aide à la décision et ne remplacent pas la validation humaine.';
    doc.setTextColor(55, 65, 81);
    doc.text(doc.splitTextToSize(text, 174), 16, y + 13);
    return y + 25;
  }

  private pdfSectionTitle(doc: jsPDF, title: string, y: number): void {
    doc.setTextColor(245, 130, 32);
    doc.setFontSize(12);
    doc.text(title, 12, y);
    doc.setTextColor(17, 17, 17);
  }

  private pdfTextSection(doc: jsPDF, title: string, lines: string[], startY: number): number {
    let y = this.ensurePdfSpace(doc, startY, 24);
    this.pdfSectionTitle(doc, title, y);
    y += 5.5;
    doc.setFontSize(8.5);
    doc.setTextColor(55, 65, 81);
    for (const line of lines) {
      const split = doc.splitTextToSize(line, 184);
      if (y + split.length * 4.4 > 280) {
        doc.addPage();
        y = 18;
      }
      doc.text(split, 12, y);
      y += split.length * 4.4 + 1.8;
    }
    doc.setTextColor(17, 17, 17);
    return y + 1.5;
  }

  private ensurePdfSpace(doc: jsPDF, y: number, needed: number): number {
    if (y + needed <= 280) {
      return y;
    }
    doc.addPage();
    return 18;
  }

  private nextPdfY(doc: jsPDF): number {
    const tableY = (doc as unknown as { lastAutoTable?: { finalY?: number } }).lastAutoTable?.finalY ?? 32;
    return tableY + 7;
  }

  private pdfTextBlock(doc: jsPDF, startY: number, text: string): number {
    let y = this.ensurePdfSpace(doc, startY, 18);
    const lines = doc.splitTextToSize(text, 176);
    const blockHeight = Math.max(11, lines.length * 4.25 + 6);
    y = this.ensurePdfSpace(doc, y, blockHeight);
    doc.setFillColor(255, 248, 241);
    doc.setDrawColor(245, 130, 32);
    doc.roundedRect(12, y, 186, blockHeight, 3, 3, 'FD');
    doc.setFontSize(8.5);
    doc.setTextColor(55, 65, 81);
    doc.text(lines, 17, y + 6.5);
    doc.setTextColor(17, 17, 17);
    return y + blockHeight + 5;
  }

  private drawPdfBadge(doc: jsPDF, label: string, x: number, y: number, color: [number, number, number], width?: number): void {
    const badgeWidth = width ?? Math.max(22, Math.min(38, label.length * 1.9 + 7));
    const useDarkText = color[0] > 210 && color[1] > 145;
    doc.setFillColor(...color);
    doc.roundedRect(x, y, badgeWidth, 5.5, 2.75, 2.75, 'F');
    doc.setFontSize(6.5);
    doc.setTextColor(useDarkText ? 17 : 255, useDarkText ? 17 : 255, useDarkText ? 17 : 255);
    doc.text(label, x + badgeWidth / 2, y + 3.75, { align: 'center' });
    doc.setTextColor(17, 17, 17);
  }

  private statusPdfColor(status: string | null | undefined): [number, number, number] {
    switch ((status ?? '').toLowerCase()) {
      case 'objectif atteint':
        return [4, 120, 87];
      case 'presque atteint':
        return [245, 130, 32];
      case 'attention':
        return [245, 158, 11];
      case 'critique':
        return [185, 28, 28];
      default:
        return [100, 116, 139];
    }
  }

  private priorityPdfColor(priority: string | null | undefined): [number, number, number] {
    switch ((priority ?? '').toLowerCase()) {
      case 'faible':
      case 'low':
        return [4, 120, 87];
      case 'moyenne':
      case 'medium':
        return [245, 130, 32];
      case 'haute':
      case 'high':
        return [185, 28, 28];
      default:
        return [100, 116, 139];
    }
  }

  private difficultyPdfColor(difficulty: string | null | undefined): [number, number, number] {
    switch ((difficulty ?? '').toLowerCase()) {
      case 'faible':
        return [4, 120, 87];
      case 'moyenne':
      case 'moyen':
        return [245, 130, 32];
      case 'élevée':
      case 'elevée':
      case 'elevated':
      case 'difficile':
        return [185, 28, 28];
      default:
        return [100, 116, 139];
    }
  }

  private async loadImageAsDataUrl(path: string): Promise<string | null> {
    try {
      const response = await fetch(path, { cache: 'no-store' });
      if (!response.ok) {
        return null;
      }
      const blob = await response.blob();
      return await new Promise<string>((resolve, reject) => {
        const reader = new FileReader();
        reader.onload = () => resolve(String(reader.result));
        reader.onerror = () => reject(reader.error);
        reader.readAsDataURL(blob);
      });
    } catch {
      return null;
    }
  }

  private normalizeNumber(value: number | null | undefined): number {
    return typeof value === 'number' && Number.isFinite(value) ? value : 0;
  }
}
