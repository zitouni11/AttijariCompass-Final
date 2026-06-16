import { CommonModule } from '@angular/common';
import { Component, input } from '@angular/core';
import { TimelineItem } from '../simulations.models';

@Component({
  selector: 'app-simulator-timeline',
  standalone: true,
  imports: [CommonModule],
  template: `
    <section class="timeline-card">
      <div class="timeline-head">
        <span class="timeline-kicker">{{ kicker() }}</span>
        <h3>{{ title() }}</h3>
        <p>{{ subtitle() }}</p>
      </div>

      <div class="timeline-track">
        <div class="timeline-line"></div>
        @for (item of items(); track item.label) {
          <div class="timeline-node" [style.left.%]="item.progress * 100">
            <span class="timeline-dot"></span>
            <span class="timeline-node-label">{{ item.label }}</span>
          </div>
        }
      </div>

      <div class="timeline-grid">
        @for (item of items(); track item.label) {
          <article class="timeline-item">
            <div class="timeline-item-head">
              <span class="timeline-step">{{ item.label }}</span>
              <span class="timeline-mini-pill">{{ valueLabelTitle() }}</span>
            </div>

            <strong>{{ item.dateLabel }}</strong>
            <span class="timeline-value-label">{{ valueLabelTitle() }}</span>
            <span class="timeline-value">{{ item.valueLabel }}</span>
            <p>{{ item.caption }}</p>
          </article>
        }
      </div>
    </section>
  `,
  styles: [`
    :host {
      display: block;
    }

    .timeline-card {
      border-radius: 34px;
      padding: 1.55rem;
      background:
        radial-gradient(circle at top right, rgba(245, 130, 32, 0.18), transparent 24%),
        linear-gradient(180deg, rgba(17, 17, 17, 0.98), rgba(31, 27, 23, 0.98));
      border: 1px solid rgba(245, 130, 32, 0.18);
      box-shadow: 0 26px 54px rgba(17, 17, 17, 0.18);
      color: var(--attijari-white);
    }

    .timeline-head {
      margin-bottom: 1.3rem;
    }

    .timeline-kicker {
      display: inline-block;
      margin-bottom: 0.35rem;
      font-size: 0.72rem;
      font-weight: 700;
      letter-spacing: 0.16em;
      text-transform: uppercase;
      color: #ffbd87;
    }

    .timeline-head h3 {
      margin: 0 0 0.3rem;
      color: var(--attijari-white);
      font-size: 1.15rem;
      letter-spacing: -0.03em;
    }

    .timeline-head p {
      margin: 0;
      color: rgba(255, 255, 255, 0.72);
      font-size: 0.92rem;
      line-height: 1.62;
      max-width: 54rem;
    }

    .timeline-track {
      position: relative;
      height: 3.7rem;
      margin-bottom: 1.15rem;
    }

    .timeline-line {
      position: absolute;
      top: 1.05rem;
      left: 0;
      right: 0;
      height: 7px;
      border-radius: 999px;
      background: linear-gradient(90deg, rgba(245, 130, 32, 0.22), rgba(245, 130, 32, 0.92));
      box-shadow: inset 0 1px 0 rgba(255, 255, 255, 0.14);
    }

    .timeline-node {
      position: absolute;
      top: 0;
      transform: translateX(-50%);
      display: grid;
      justify-items: center;
      gap: 0.48rem;
    }

    .timeline-dot {
      width: 18px;
      height: 18px;
      border-radius: 50%;
      background: linear-gradient(135deg, #f58220, #ffbc7b);
      border: 3px solid rgba(17, 17, 17, 0.92);
      box-shadow: 0 0 0 6px rgba(245, 130, 32, 0.18);
    }

    .timeline-node-label {
      font-size: 0.7rem;
      font-weight: 700;
      color: rgba(255, 255, 255, 0.72);
    }

    .timeline-grid {
      display: grid;
      grid-template-columns: repeat(4, minmax(0, 1fr));
      gap: 0.85rem;
    }

    .timeline-item {
      display: grid;
      gap: 0.28rem;
      padding: 1rem;
      border-radius: 22px;
      background: rgba(255, 255, 255, 0.06);
      border: 1px solid rgba(255, 255, 255, 0.08);
    }

    .timeline-item-head {
      display: flex;
      align-items: center;
      justify-content: space-between;
      gap: 0.65rem;
      margin-bottom: 0.1rem;
    }

    .timeline-step,
    .timeline-item p,
    .timeline-value-label {
      color: rgba(255, 255, 255, 0.7);
    }

    .timeline-step {
      font-size: 0.72rem;
      font-weight: 700;
      letter-spacing: 0.08em;
      text-transform: uppercase;
    }

    .timeline-mini-pill {
      padding: 0.25rem 0.48rem;
      border-radius: 999px;
      background: rgba(255, 255, 255, 0.08);
      color: rgba(255, 255, 255, 0.66);
      font-size: 0.64rem;
      font-weight: 700;
      text-transform: uppercase;
      letter-spacing: 0.08em;
    }

    .timeline-item strong {
      font-size: 1rem;
      color: var(--attijari-white);
    }

    .timeline-value-label {
      font-size: 0.7rem;
      font-weight: 700;
      letter-spacing: 0.06em;
      text-transform: uppercase;
    }

    .timeline-value {
      font-size: 0.92rem;
      font-weight: 700;
      color: #ffbd87;
    }

    .timeline-item p {
      font-size: 0.8rem;
      line-height: 1.55;
      margin: 0.15rem 0 0;
    }

    @media (max-width: 1080px) {
      .timeline-grid {
        grid-template-columns: 1fr 1fr;
      }
    }

    @media (max-width: 720px) {
      .timeline-track {
        display: none;
      }

      .timeline-grid {
        grid-template-columns: 1fr;
      }
    }
  `]
})
export class SimulatorTimelineComponent {
  readonly title = input.required<string>();
  readonly subtitle = input.required<string>();
  readonly kicker = input('Timeline');
  readonly valueLabelTitle = input('Montant cumule');
  readonly items = input<TimelineItem[]>([]);
}
