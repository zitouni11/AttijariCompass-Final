import { CommonModule } from '@angular/common';
import { Component, input, output } from '@angular/core';
import { CompareScenarioCard } from '../simulations.models';

@Component({
  selector: 'app-scenario-compare',
  standalone: true,
  imports: [CommonModule],
  template: `
    <section class="compare-card">
      <div class="compare-head">
        <div>
          <span class="compare-kicker">{{ kicker() }}</span>
          <h3>{{ title() }}</h3>
          <p>{{ subtitle() }}</p>
        </div>
      </div>

      <div class="compare-grid">
        @for (card of cards(); track card.id) {
          <button
            type="button"
            class="compare-item"
            [class.compare-item-active]="card.id === selectedId()"
            [class]="'accent-' + card.accent"
            (click)="scenarioSelected.emit(card.id)"
          >
            <div class="compare-item-glow"></div>

            <div class="compare-item-head">
              <div>
                <span class="compare-label">{{ card.label }}</span>
                <span class="compare-badge">{{ card.badge }}</span>
              </div>

              <span class="compare-state-pill" [class.compare-state-pill-active]="card.id === selectedId()">
                {{ card.id === selectedId() ? selectedLabel() : 'Disponible' }}
              </span>
            </div>

            <strong class="compare-headline">{{ card.headline }}</strong>
            <p class="compare-description">{{ card.description }}</p>

            <div class="compare-metrics">
              @for (metric of card.metrics; track metric.label) {
                <div class="compare-metric" [class]="'tone-' + (metric.tone ?? 'neutral')">
                  <span>{{ metric.label }}</span>
                  <strong>{{ metric.value }}</strong>
                  @if (metric.hint) {
                    <small>{{ metric.hint }}</small>
                  }
                </div>
              }
            </div>

            <div class="compare-footer">
              <p>{{ card.footer }}</p>
              <span class="compare-action" [class.compare-action-active]="card.id === selectedId()">
                {{ card.id === selectedId() ? selectedLabel() : actionLabel() }}
              </span>
            </div>
          </button>
        }
      </div>
    </section>
  `,
  styles: [`
    :host {
      display: block;
    }

    .compare-card {
      border-radius: 34px;
      padding: 1.55rem;
      background:
        radial-gradient(circle at top right, rgba(245, 130, 32, 0.1), transparent 22%),
        linear-gradient(180deg, rgba(255, 255, 255, 0.95), rgba(248, 245, 241, 0.98));
      border: 1px solid rgba(17, 17, 17, 0.08);
      box-shadow: 0 22px 52px rgba(17, 17, 17, 0.07);
      backdrop-filter: blur(16px);
    }

    .compare-head {
      margin-bottom: 1.1rem;
    }

    .compare-kicker {
      display: inline-block;
      margin-bottom: 0.35rem;
      font-size: 0.72rem;
      font-weight: 700;
      letter-spacing: 0.14em;
      text-transform: uppercase;
      color: var(--attijari-orange-dark);
    }

    .compare-head h3 {
      margin: 0 0 0.32rem;
      font-size: 1.18rem;
      letter-spacing: -0.03em;
    }

    .compare-head p {
      margin: 0;
      color: var(--attijari-muted);
      font-size: 0.92rem;
      line-height: 1.6;
    }

    .compare-grid {
      display: grid;
      grid-template-columns: repeat(3, minmax(0, 1fr));
      gap: 0.95rem;
    }

    .compare-item {
      position: relative;
      display: grid;
      gap: 0.9rem;
      text-align: left;
      padding: 1.2rem;
      border-radius: 28px;
      border: 1px solid rgba(17, 17, 17, 0.08);
      background: linear-gradient(180deg, rgba(255, 255, 255, 0.98), rgba(249, 246, 242, 0.96));
      cursor: pointer;
      transition: transform 0.2s ease, box-shadow 0.2s ease, border-color 0.2s ease;
      overflow: hidden;
    }

    .compare-item:hover,
    .compare-item-active {
      transform: translateY(-2px);
      box-shadow: 0 24px 40px rgba(17, 17, 17, 0.1);
    }

    .compare-item-glow {
      position: absolute;
      inset: auto -15% -30% auto;
      width: 9rem;
      height: 9rem;
      border-radius: 50%;
      background: radial-gradient(circle, rgba(245, 130, 32, 0.18), transparent 64%);
      pointer-events: none;
    }

    .compare-item.accent-orange {
      border-color: rgba(245, 130, 32, 0.24);
    }

    .compare-item.accent-charcoal {
      border-color: rgba(17, 17, 17, 0.16);
    }

    .compare-item.accent-sand {
      border-color: rgba(153, 127, 95, 0.24);
    }

    .compare-item-active.accent-orange,
    .compare-item-active.accent-charcoal,
    .compare-item-active.accent-sand {
      border-width: 1.5px;
    }

    .compare-item-head {
      display: flex;
      align-items: flex-start;
      justify-content: space-between;
      gap: 0.75rem;
    }

    .compare-label {
      display: block;
      margin-bottom: 0.4rem;
      font-size: 0.84rem;
      font-weight: 700;
      color: var(--attijari-black);
    }

    .compare-badge {
      display: inline-flex;
      padding: 0.35rem 0.62rem;
      border-radius: 999px;
      background: rgba(17, 17, 17, 0.06);
      color: var(--attijari-muted);
      font-size: 0.72rem;
      font-weight: 700;
      letter-spacing: 0.04em;
    }

    .compare-state-pill {
      padding: 0.38rem 0.68rem;
      border-radius: 999px;
      background: rgba(17, 17, 17, 0.05);
      color: var(--attijari-muted);
      font-size: 0.72rem;
      font-weight: 700;
      white-space: nowrap;
    }

    .compare-state-pill-active {
      background: rgba(245, 130, 32, 0.12);
      color: var(--attijari-orange-dark);
      border: 1px solid rgba(245, 130, 32, 0.18);
    }

    .compare-headline {
      font-size: 1.42rem;
      letter-spacing: -0.04em;
      color: var(--attijari-black);
    }

    .compare-description {
      min-height: 3rem;
      color: var(--attijari-muted);
      font-size: 0.87rem;
      line-height: 1.6;
      margin: 0;
    }

    .compare-metrics {
      display: grid;
      gap: 0.7rem;
    }

    .compare-metric {
      display: grid;
      gap: 0.25rem;
      padding: 0.85rem 0.9rem;
      border-radius: 20px;
      background: rgba(17, 17, 17, 0.04);
      border: 1px solid rgba(17, 17, 17, 0.05);
    }

    .compare-metric span,
    .compare-metric small {
      color: var(--attijari-muted);
      font-size: 0.75rem;
      line-height: 1.45;
    }

    .compare-metric strong {
      color: var(--attijari-black);
      font-size: 0.98rem;
    }

    .compare-metric.tone-positive strong {
      color: var(--attijari-success);
    }

    .compare-metric.tone-warning strong {
      color: var(--attijari-orange-dark);
    }

    .compare-metric.tone-danger strong {
      color: var(--attijari-danger);
    }

    .compare-footer {
      display: grid;
      gap: 0.8rem;
      margin-top: 0.1rem;
    }

    .compare-footer p {
      margin: 0;
      font-size: 0.79rem;
      line-height: 1.55;
      color: var(--attijari-text);
    }

    .compare-action {
      justify-self: start;
      padding: 0.58rem 0.78rem;
      border-radius: 999px;
      background: rgba(17, 17, 17, 0.05);
      color: var(--attijari-black);
      font-size: 0.76rem;
      font-weight: 700;
    }

    .compare-action-active {
      background: linear-gradient(135deg, rgba(245, 130, 32, 0.94), rgba(229, 111, 15, 0.88));
      color: var(--attijari-white);
      box-shadow: 0 14px 28px rgba(245, 130, 32, 0.18);
    }

    @media (max-width: 1080px) {
      .compare-grid {
        grid-template-columns: 1fr;
      }
    }
  `]
})
export class ScenarioCompareComponent {
  readonly title = input.required<string>();
  readonly subtitle = input.required<string>();
  readonly kicker = input('Comparer');
  readonly selectedId = input.required<string>();
  readonly cards = input<CompareScenarioCard[]>([]);
  readonly actionLabel = input('Appliquer ce scenario');
  readonly selectedLabel = input('Scenario actif');
  readonly scenarioSelected = output<string>();
}
