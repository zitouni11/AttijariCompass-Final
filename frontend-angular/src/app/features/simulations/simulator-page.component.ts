import { CommonModule } from '@angular/common';
import { Component, ElementRef, inject, signal } from '@angular/core';
import { CreditSimulatorComponent } from './components/credit-simulator.component';
import { SavingsSimulatorComponent } from './components/savings-simulator.component';

@Component({
  selector: 'app-simulator-page',
  standalone: true,
  imports: [CommonModule, SavingsSimulatorComponent, CreditSimulatorComponent],
  template: `
    <div class="sim-page">
      <section class="sim-hero">
        <div class="sim-hero-copy">
          <span class="sim-kicker">Studio de simulation</span>

          <h1>Simulateurs financiers</h1>
          <p>Testez vos hypothèses d’épargne ou de crédit et visualisez rapidement l’impact sur vos objectifs.</p>

          <div class="hero-pill-row">
            <span class="hero-pill">Épargne</span>
            <span class="hero-pill">Crédit</span>
            <span class="hero-pill">Projection instantanée</span>
          </div>
        </div>
      </section>

      <section class="entry-section">
        <div class="entry-head">
          <span class="entry-kicker">Choisir un espace</span>
          <h2>Choisissez le simulateur adapté à votre besoin</h2>
          <p>Commencez par une hypothèse simple, puis ouvrez les détails uniquement si vous en avez besoin.</p>
        </div>

        <div class="entry-grid">
          <article class="entry-card" [class.entry-card-active]="activeMode() === 'savings'">
            <div class="entry-card-top">
              <span class="entry-icon material-symbols-rounded">savings</span>
              <span class="entry-pill">Objectif</span>
            </div>

            <h3>Simulateur Épargne</h3>
            <p>Voyez quand votre objectif peut être atteint selon votre apport et votre effort mensuel.</p>

            <ul class="entry-points">
              <li>Date estimée d’atteinte</li>
              <li>Montant restant lisible</li>
              <li>Scénarios simples</li>
            </ul>

            <button type="button" class="entry-action" (click)="openMode('savings')">
              Ouvrir le simulateur épargne
            </button>
          </article>

          <article class="entry-card entry-card-strong" [class.entry-card-active]="activeMode() === 'credit'">
            <div class="entry-card-top">
              <span class="entry-icon material-symbols-rounded">credit_score</span>
              <span class="entry-pill">Financement</span>
            </div>

            <h3>Simulateur Crédit</h3>
            <p>Estimez une mensualité, le coût total du crédit et l’effet d’un remboursement anticipé.</p>

            <ul class="entry-points">
              <li>Mensualité estimée</li>
              <li>Intérêts et coût total</li>
              <li>Comparaison de durées</li>
            </ul>

            <button type="button" class="entry-action" (click)="openMode('credit')">
              Ouvrir le simulateur crédit
            </button>
          </article>
        </div>
      </section>

      <section class="workspace-shell" id="simulator-workspace">
        @if (activeMode(); as mode) {
          <div class="workspace-head">
            <div>
              <span class="workspace-kicker">Espace actif</span>
              <h2>{{ mode === 'credit' ? 'Simulateur Crédit' : 'Simulateur Épargne' }}</h2>
              <p>
                @if (mode === 'credit') {
                  Renseignez quelques paramètres et obtenez une synthèse claire.
                } @else {
                  Estimez la date d’atteinte de votre objectif en quelques champs.
                }
              </p>
            </div>

            <div class="mode-switch" aria-label="Changer de simulateur">
              <button type="button" class="mode-tab" [class.mode-tab-active]="mode === 'savings'" (click)="openMode('savings')">Épargne</button>
              <button type="button" class="mode-tab" [class.mode-tab-active]="mode === 'credit'" (click)="openMode('credit')">Crédit</button>
            </div>
          </div>

          @if (mode === 'savings') {
            <app-savings-simulator />
          } @else {
            <app-credit-simulator />
          }
        } @else {
          <div class="workspace-empty">
            <div class="workspace-empty-copy">
              <span class="workspace-kicker">Activation</span>
              <h2>Ouvrez un simulateur pour lancer les calculs</h2>
              <p>Remplissez les champs puis lancez la simulation pour afficher le résultat.</p>
            </div>

            <div class="workspace-empty-actions">
              <button type="button" class="entry-action" (click)="openMode('savings')">
                Ouvrir Épargne
              </button>
              <button type="button" class="workspace-secondary-action" (click)="openMode('credit')">
                Ouvrir Crédit
              </button>
            </div>
          </div>
        }
      </section>
    </div>
  `,
  styleUrl: './simulator-page.component.scss'
})
export class SimulatorPageComponent {
  private readonly elementRef = inject(ElementRef<HTMLElement>);
  readonly activeMode = signal<'savings' | 'credit' | null>(null);

  openMode(mode: 'savings' | 'credit'): void {
    this.activeMode.set(mode);
    setTimeout(() => {
      this.elementRef.nativeElement
        .querySelector('#simulator-workspace')
        ?.scrollIntoView({ behavior: 'smooth', block: 'start' });
    });
  }
}
