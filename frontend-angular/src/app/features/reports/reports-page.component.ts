import { CommonModule } from '@angular/common';
import { ChangeDetectionStrategy, Component, DestroyRef, computed, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { finalize } from 'rxjs';
import { ReportCategory, ReportSummary } from '../../core/models';
import { ReportsService } from '../../core/services/reports.service';

@Component({
  selector: 'app-reports-page',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink],
  templateUrl: './reports-page.component.html',
  styleUrl: './reports-page.component.css',
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class ReportsPageComponent {
  private readonly reportsService = inject(ReportsService);
  private readonly destroyRef = inject(DestroyRef);
  private readonly moneyFormatter = new Intl.NumberFormat('fr-FR', {
    minimumFractionDigits: 0,
    maximumFractionDigits: 2
  });
  private readonly percentFormatter = new Intl.NumberFormat('fr-FR', {
    minimumFractionDigits: 0,
    maximumFractionDigits: 1
  });

  readonly selectedMonth = signal(this.currentMonthKey());
  readonly summary = signal<ReportSummary | null>(null);
  readonly loading = signal(true);
  readonly errorMessage = signal<string | null>(null);

  readonly categories = computed(() => this.summary()?.categories ?? []);
  readonly topCategories = computed(() => this.categories().slice(0, 6));
  readonly cashBreakdown = computed(() => this.summary()?.cashBreakdown ?? null);
  readonly hasCashTransactions = computed(() => (this.cashBreakdown()?.transactionCount ?? 0) > 0);
  readonly netTone = computed(() => (this.summary()?.netBalance ?? 0) >= 0 ? 'positive' : 'negative');

  constructor() {
    this.loadSummary(this.selectedMonth());
  }

  onMonthChange(value: string): void {
    const normalized = this.normalizeMonth(value);
    this.selectedMonth.set(normalized);
    this.loadSummary(normalized);
  }

  reload(): void {
    this.loadSummary(this.selectedMonth());
  }

  formatMoney(value: number | null | undefined): string {
    return `${this.moneyFormatter.format(value ?? 0)} DT`;
  }

  formatPercent(value: number | null | undefined): string {
    return `${this.percentFormatter.format(value ?? 0)}%`;
  }

  progressWidth(category: ReportCategory): number {
    const usage = category.usagePercent ?? 0;
    return Math.max(0, Math.min(100, Math.round(usage)));
  }

  statusLabel(status: string): string {
    const normalized = `${status ?? ''}`.trim().toUpperCase();

    switch (normalized) {
      case 'OVER_LIMIT':
        return 'Depasse';
      case 'WARNING':
        return 'Critique';
      case 'WATCH':
        return 'A surveiller';
      case 'NO_BUDGET':
        return 'Sans budget';
      default:
        return 'Sous controle';
    }
  }

  statusTone(status: string): string {
    const normalized = `${status ?? ''}`.trim().toUpperCase();

    if (normalized === 'OVER_LIMIT' || normalized === 'WARNING') {
      return 'critical';
    }

    if (normalized === 'WATCH' || normalized === 'NO_BUDGET') {
      return 'warning';
    }

    return 'info';
  }

  private loadSummary(month: string): void {
    this.loading.set(true);
    this.errorMessage.set(null);

    this.reportsService.getSummary(month)
      .pipe(
        takeUntilDestroyed(this.destroyRef),
        finalize(() => this.loading.set(false))
      )
      .subscribe({
        next: (summary) => this.summary.set(summary),
        error: (error: unknown) => {
          this.summary.set(null);
          this.errorMessage.set(this.extractErrorMessage(error));
        }
      });
  }

  private currentMonthKey(): string {
    const now = new Date();
    const month = String(now.getMonth() + 1).padStart(2, '0');
    return `${now.getFullYear()}-${month}`;
  }

  private normalizeMonth(value: string): string {
    return /^\d{4}-\d{2}$/.test(`${value ?? ''}`.trim())
      ? `${value}`.trim()
      : this.currentMonthKey();
  }

  private extractErrorMessage(error: unknown): string {
    if (typeof error === 'object' && error !== null) {
      const source = error as Record<string, unknown>;
      const nested = source['error'];

      if (nested && typeof nested === 'object' && !Array.isArray(nested)) {
        const nestedSource = nested as Record<string, unknown>;
        const nestedMessage = nestedSource['message'] ?? nestedSource['detail'];

        if (typeof nestedMessage === 'string' && nestedMessage.trim()) {
          return nestedMessage.trim();
        }
      }

      const message = source['message'];
      if (typeof message === 'string' && message.trim()) {
        return message.trim();
      }
    }

    return 'Impossible de charger le report mensuel pour le moment.';
  }
}
