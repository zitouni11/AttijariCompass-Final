import { CommonModule } from '@angular/common';
import { Component, input } from '@angular/core';
import { BuilderHighlight } from '../simulations.models';

@Component({
  selector: 'app-scenario-builder',
  standalone: true,
  imports: [CommonModule],
  template: `
    <section class="builder-card" [class.builder-card-dark]="variant() === 'dark'">
      <div class="builder-head">
        <div class="builder-copy">
          <span class="builder-kicker">{{ kicker() }}</span>
          <h2>{{ title() }}</h2>
          <p>{{ subtitle() }}</p>
        </div>

        @if (status()) {
          <span class="builder-status">{{ status() }}</span>
        }
      </div>

      @if (highlights().length > 0) {
        <div class="builder-highlights">
          @for (highlight of highlights(); track highlight.label) {
            <div class="builder-highlight" [class]="'tone-' + (highlight.tone ?? 'neutral')">
              <span class="builder-highlight-label">{{ highlight.label }}</span>
              <strong>{{ highlight.value }}</strong>
            </div>
          }
        </div>
      }

      <div class="builder-content">
        <ng-content />
      </div>
    </section>
  `,
  styles: [`
    :host {
      display: block;
    }

    .builder-card {
      position: relative;
      overflow: hidden;
      border-radius: 30px;
      padding: 1.5rem;
      background:
        radial-gradient(circle at top right, rgba(245, 130, 32, 0.12), transparent 28%),
        linear-gradient(180deg, rgba(255, 255, 255, 0.96) 0%, rgba(250, 247, 244, 0.98) 100%);
      border: 1px solid rgba(245, 130, 32, 0.14);
      box-shadow: 0 24px 54px rgba(17, 17, 17, 0.08);
    }

    .builder-card-dark {
      background:
        radial-gradient(circle at top right, rgba(245, 130, 32, 0.24), transparent 22%),
        linear-gradient(180deg, #161311 0%, #25201b 100%);
      border-color: rgba(245, 130, 32, 0.18);
      color: var(--attijari-white);
    }

    .builder-head {
      display: flex;
      align-items: flex-start;
      justify-content: space-between;
      gap: 1rem;
      margin-bottom: 1.25rem;
    }

    .builder-copy {
      display: grid;
      gap: 0.45rem;
    }

    .builder-kicker {
      font-size: 0.72rem;
      font-weight: 700;
      letter-spacing: 0.16em;
      text-transform: uppercase;
      color: var(--attijari-orange-dark);
    }

    .builder-card-dark .builder-kicker {
      color: #ffbd87;
    }

    .builder-copy h2 {
      margin: 0;
      font-size: 1.3rem;
      letter-spacing: -0.03em;
    }

    .builder-card-dark .builder-copy h2 {
      color: var(--attijari-white);
    }

    .builder-copy p {
      margin: 0;
      max-width: 40rem;
      color: var(--attijari-muted);
      font-size: 0.95rem;
      line-height: 1.55;
    }

    .builder-card-dark .builder-copy p {
      color: rgba(255, 255, 255, 0.72);
    }

    .builder-status {
      flex: 0 0 auto;
      padding: 0.55rem 0.85rem;
      border-radius: 999px;
      background: rgba(17, 17, 17, 0.06);
      border: 1px solid rgba(17, 17, 17, 0.08);
      font-size: 0.78rem;
      font-weight: 700;
      color: var(--attijari-text);
    }

    .builder-card-dark .builder-status {
      background: rgba(255, 255, 255, 0.08);
      border-color: rgba(255, 255, 255, 0.12);
      color: rgba(255, 255, 255, 0.84);
    }

    .builder-highlights {
      display: grid;
      grid-template-columns: repeat(3, minmax(0, 1fr));
      gap: 0.85rem;
      margin-bottom: 1.35rem;
    }

    .builder-highlight {
      display: grid;
      gap: 0.3rem;
      padding: 0.95rem 1rem;
      border-radius: 20px;
      background: rgba(255, 255, 255, 0.82);
      border: 1px solid rgba(17, 17, 17, 0.06);
    }

    .builder-card-dark .builder-highlight {
      background: rgba(255, 255, 255, 0.06);
      border-color: rgba(255, 255, 255, 0.08);
    }

    .builder-highlight-label {
      font-size: 0.74rem;
      font-weight: 600;
      color: var(--attijari-muted);
    }

    .builder-card-dark .builder-highlight-label {
      color: rgba(255, 255, 255, 0.62);
    }

    .builder-highlight strong {
      font-size: 1rem;
      letter-spacing: -0.02em;
      color: var(--attijari-black);
    }

    .builder-card-dark .builder-highlight strong {
      color: var(--attijari-white);
    }

    .builder-highlight.tone-strong {
      background: linear-gradient(135deg, rgba(245, 130, 32, 0.18), rgba(229, 111, 15, 0.08));
      border-color: rgba(245, 130, 32, 0.2);
    }

    .builder-highlight.tone-positive strong {
      color: var(--attijari-success);
    }

    .builder-highlight.tone-warning strong {
      color: var(--attijari-orange-dark);
    }

    .builder-content {
      position: relative;
      z-index: 1;
    }

    @media (max-width: 960px) {
      .builder-head {
        flex-direction: column;
      }

      .builder-highlights {
        grid-template-columns: 1fr;
      }
    }
  `]
})
export class ScenarioBuilderComponent {
  readonly title = input.required<string>();
  readonly subtitle = input.required<string>();
  readonly kicker = input('Projection interactive');
  readonly status = input('');
  readonly variant = input<'light' | 'dark'>('light');
  readonly highlights = input<BuilderHighlight[]>([]);
}
