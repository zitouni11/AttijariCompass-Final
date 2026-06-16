import { CommonModule } from '@angular/common';
import { Component, computed, input } from '@angular/core';
import { BaseChartDirective } from 'ng2-charts';
import {
  BarController,
  BarElement,
  CategoryScale,
  Chart,
  ChartData,
  ChartOptions,
  Filler,
  Legend,
  LinearScale,
  LineController,
  LineElement,
  PointElement,
  Tooltip
} from 'chart.js';
import { BarChartModel, LineChartModel } from '../simulations.models';

Chart.register(
  CategoryScale,
  LinearScale,
  PointElement,
  LineElement,
  LineController,
  BarController,
  BarElement,
  Filler,
  Legend,
  Tooltip
);

@Component({
  selector: 'app-simulator-charts',
  standalone: true,
  imports: [CommonModule, BaseChartDirective],
  template: `
    <div class="charts-grid">
      <section class="chart-card chart-card-wide">
        <div class="chart-head">
          <span class="chart-kicker">{{ kicker() }}</span>
          <h3>{{ lineModel().title }}</h3>
          <p>{{ lineModel().subtitle }}</p>
        </div>

        <div class="chart-shell">
          <canvas
            baseChart
            [data]="lineData()"
            [options]="lineOptions"
            [type]="'line'"
          ></canvas>
        </div>
      </section>

      <section class="chart-card">
        <div class="chart-head">
          <span class="chart-kicker">Comparatif</span>
          <h3>{{ barModel().title }}</h3>
          <p>{{ barModel().subtitle }}</p>
        </div>

        <div class="chart-shell chart-shell-small">
          <canvas
            baseChart
            [data]="barData()"
            [options]="barOptions"
            [type]="'bar'"
          ></canvas>
        </div>

        <div class="bar-meta-list">
          @for (item of barModel().items; track item.label) {
            <div class="bar-meta-item">
              <span class="bar-meta-dot" [style.background]="item.color"></span>
              <div>
                <strong>{{ item.label }}</strong>
                <small>{{ item.meta }}</small>
              </div>
            </div>
          }
        </div>
      </section>
    </div>
  `,
  styles: [`
    :host {
      display: block;
    }

    .charts-grid {
      display: grid;
      grid-template-columns: 1.45fr 1fr;
      gap: 1rem;
    }

    .chart-card {
      border-radius: 34px;
      padding: 1.45rem;
      background:
        radial-gradient(circle at top right, rgba(245, 130, 32, 0.08), transparent 22%),
        linear-gradient(180deg, rgba(255, 255, 255, 0.96), rgba(248, 245, 241, 0.98));
      border: 1px solid rgba(17, 17, 17, 0.08);
      box-shadow: 0 22px 52px rgba(17, 17, 17, 0.07);
      backdrop-filter: blur(16px);
    }

    .chart-card-wide {
      min-height: 27.5rem;
    }

    .chart-head {
      margin-bottom: 1rem;
    }

    .chart-kicker {
      display: inline-block;
      margin-bottom: 0.35rem;
      font-size: 0.72rem;
      font-weight: 700;
      letter-spacing: 0.16em;
      text-transform: uppercase;
      color: var(--attijari-orange-dark);
    }

    .chart-head h3 {
      margin: 0 0 0.25rem;
      font-size: 1.14rem;
      letter-spacing: -0.03em;
      color: var(--attijari-black);
    }

    .chart-head p {
      margin: 0;
      font-size: 0.9rem;
      color: var(--attijari-muted);
      line-height: 1.58;
    }

    .chart-shell {
      min-height: 21rem;
      padding: 0.8rem 0.2rem 0.1rem;
      border-radius: 24px;
      background: rgba(17, 17, 17, 0.025);
      border: 1px solid rgba(17, 17, 17, 0.04);
    }

    .chart-shell-small {
      min-height: 17rem;
    }

    .bar-meta-list {
      display: grid;
      gap: 0.7rem;
      margin-top: 1rem;
    }

    .bar-meta-item {
      display: flex;
      align-items: center;
      gap: 0.75rem;
      padding: 0.82rem 0.9rem;
      border-radius: 18px;
      background: rgba(17, 17, 17, 0.04);
      border: 1px solid rgba(17, 17, 17, 0.05);
    }

    .bar-meta-dot {
      width: 12px;
      height: 12px;
      border-radius: 50%;
      flex: 0 0 12px;
      box-shadow: 0 0 0 5px rgba(17, 17, 17, 0.04);
    }

    .bar-meta-item strong {
      display: block;
      margin-bottom: 0.1rem;
      font-size: 0.84rem;
      color: var(--attijari-black);
    }

    .bar-meta-item small {
      color: var(--attijari-muted);
      font-size: 0.76rem;
      line-height: 1.45;
    }

    @media (max-width: 1080px) {
      .charts-grid {
        grid-template-columns: 1fr;
      }

      .chart-card-wide {
        min-height: auto;
      }
    }
  `]
})
export class SimulatorChartsComponent {
  readonly kicker = input('Courbe');
  readonly lineModel = input.required<LineChartModel>();
  readonly barModel = input.required<BarChartModel>();

  readonly lineData = computed<ChartData<'line'>>(() => ({
    labels: this.lineModel().labels,
    datasets: this.lineModel().series.map((series) => ({
      label: series.label,
      data: series.values,
      borderColor: series.color,
      backgroundColor: series.fillColor,
      fill: true,
      pointRadius: 0,
      pointHoverRadius: 4,
      tension: series.tension ?? 0.35,
      borderWidth: 2.5
    }))
  }));

  readonly barData = computed<ChartData<'bar'>>(() => ({
    labels: this.barModel().items.map((item) => item.label),
    datasets: [
      {
        data: this.barModel().items.map((item) => item.value),
        backgroundColor: this.barModel().items.map((item) => item.color),
        borderRadius: 12,
        borderSkipped: false
      }
    ]
  }));

  readonly lineOptions: ChartOptions<'line'> = {
    maintainAspectRatio: false,
    responsive: true,
    plugins: {
      legend: {
        display: true,
        position: 'bottom',
        labels: {
          usePointStyle: true,
          boxWidth: 10,
          color: '#4C4C4C',
          font: {
            family: 'Sora',
            size: 12,
            weight: 600
          }
        }
      }
    },
    scales: {
      x: {
        grid: {
          display: false
        },
        ticks: {
          color: '#8C837B',
          font: {
            family: 'Sora',
            size: 11
          }
        }
      },
      y: {
        grid: {
          color: 'rgba(17, 17, 17, 0.06)'
        },
        ticks: {
          color: '#8C837B',
          font: {
            family: 'Sora',
            size: 11
          }
        }
      }
    }
  };

  readonly barOptions: ChartOptions<'bar'> = {
    maintainAspectRatio: false,
    responsive: true,
    plugins: {
      legend: {
        display: false
      }
    },
    scales: {
      x: {
        grid: {
          display: false
        },
        ticks: {
          color: '#8C837B',
          font: {
            family: 'Sora',
            size: 11
          }
        }
      },
      y: {
        grid: {
          color: 'rgba(17, 17, 17, 0.06)'
        },
        ticks: {
          color: '#8C837B',
          font: {
            family: 'Sora',
            size: 11
          }
        }
      }
    }
  };
}
