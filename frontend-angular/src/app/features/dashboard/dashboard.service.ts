import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable, map } from 'rxjs';
import { environment } from '../../../environments/environment';
import { normalizeTransactionCategory } from '../../core/models';
import {
  DashboardCategoryApiItem,
  DashboardCategorySummary,
  DashboardFinancialHealth,
  DashboardFinancialHealthApi,
  DashboardSummary,
  DashboardSummaryApiResponse
} from './dashboard.models';

@Injectable({ providedIn: 'root' })
export class DashboardService {
  private readonly http = inject(HttpClient);
  private readonly apiUrl = `${environment.apiUrl}/dashboard`;

  getSummary(month: string): Observable<DashboardSummary> {
    const normalizedMonth = this.normalizeMonth(month);
    const params = new HttpParams().set('month', normalizedMonth);

    return this.http.get<unknown>(`${this.apiUrl}/summary`, { params }).pipe(
      map((response) => this.normalizeSummary(response, normalizedMonth))
    );
  }

  private normalizeSummary(response: unknown, requestedMonth: string): DashboardSummary {
    const source = this.asRecord(response);
    const typedSource = source as DashboardSummaryApiResponse;
    const month =
      this.pickString(source, ['month', 'monthKey', 'period', 'currentMonth', 'moisCourant']) ??
      requestedMonth;
    const income = this.pickNumber(source, ['income', 'totalIncome', 'totalRevenu', 'revenue']) ?? 0;
    const expenses = this.pickNumber(source, ['expenses', 'totalExpenses', 'totalDepenses', 'spending']) ?? 0;
    const netBalance = this.pickNumber(source, ['netBalance', 'balance', 'net', 'soldeActuel']) ?? 0;
    const savingsRate = this.pickNumber(source, ['savingsRate', 'savingsRatePercent', 'tauxEpargne']) ?? 0;
    const trackedTransactions =
      this.pickNumber(source, ['trackedTransactions', 'transactionCount', 'totalTransactions', 'nombreTransactions']) ?? 0;
    const financialHealthScore = this.pickNumber(source, ['financialHealthScore']);
    const expenseByCategory = this.normalizeCategories(
      typedSource.expenseByCategory ?? typedSource.depensesParCategorie ?? null
    );
    const financialHealth = this.normalizeFinancialHealth(
      (this.pickRecord(source, ['financialHealth', 'health']) as DashboardFinancialHealthApi | null) ??
      (financialHealthScore !== null ? { score: financialHealthScore } : null)
    );
    const hasData = this.pickBoolean(source, ['hasData']) ?? (
      income !== 0 ||
      expenses !== 0 ||
      netBalance !== 0 ||
      trackedTransactions !== 0 ||
      expenseByCategory.length > 0
    );

    return {
      month,
      monthLabel: this.formatMonthLabel(
        this.pickString(source, ['monthLabel']) ?? month
      ),
      income,
      expenses,
      netBalance,
      savingsRate,
      trackedTransactions,
      expenseByCategory,
      financialHealth,
      currency: 'DT',
      hasData
    };
  }

  private normalizeCategories(value: unknown): DashboardCategorySummary[] {
    const entries: Array<{ category: string; amount: number }> = [];

    if (Array.isArray(value)) {
      for (const item of value) {
        const source = this.asRecord(item);
        const category = normalizeTransactionCategory(
          this.pickString(source, ['category', 'name', 'label']) ?? 'AUTRES'
        );
        const amount = this.pickNumber(source, ['amount', 'total', 'value']) ?? 0;

        if (amount !== 0) {
          entries.push({ category, amount });
        }
      }
    } else {
      const record = this.asRecord(value);
      for (const [category, rawAmount] of Object.entries(record)) {
        const amount = this.parseNumber(rawAmount);

        if (amount !== null && amount !== 0) {
          entries.push({ category: normalizeTransactionCategory(category), amount });
        }
      }
    }

    const total = entries.reduce((sum, item) => sum + item.amount, 0);

    return entries
      .sort((left, right) => right.amount - left.amount)
      .map((item) => ({
        category: item.category,
        amount: item.amount,
        share: total > 0 ? (item.amount / total) * 100 : 0
      }));
  }

  private normalizeFinancialHealth(value: DashboardFinancialHealthApi | null): DashboardFinancialHealth {
    const source = this.asRecord(value);

    return {
      score: this.pickNumber(source, ['score']),
      status: this.pickString(source, ['status']),
      label: this.pickString(source, ['label']),
      message: this.pickString(source, ['message', 'insight', 'summary'])
    };
  }

  private formatMonthLabel(value: string): string {
    const normalized = this.normalizeMonth(value);
    const [year, month] = normalized.split('-').map((part) => Number(part));
    const date = new Date(year, (month || 1) - 1, 1);

    return new Intl.DateTimeFormat('fr-FR', {
      month: 'long',
      year: 'numeric'
    }).format(date);
  }

  private normalizeMonth(value: string): string {
    const trimmed = `${value ?? ''}`.trim();

    if (/^\d{4}-\d{2}$/.test(trimmed)) {
      return trimmed;
    }

    const today = new Date();
    const month = String(today.getMonth() + 1).padStart(2, '0');
    return `${today.getFullYear()}-${month}`;
  }

  private asRecord(value: unknown): Record<string, unknown> {
    return value && typeof value === 'object' && !Array.isArray(value)
      ? value as Record<string, unknown>
      : {};
  }

  private pickRecord(source: Record<string, unknown>, keys: string[]): Record<string, unknown> | null {
    for (const key of keys) {
      const value = source[key];

      if (value && typeof value === 'object' && !Array.isArray(value)) {
        return value as Record<string, unknown>;
      }
    }

    return null;
  }

  private pickString(source: Record<string, unknown>, keys: string[]): string | null {
    for (const key of keys) {
      const value = source[key];

      if (typeof value === 'string' && value.trim()) {
        return value.trim();
      }
    }

    return null;
  }

  private pickNumber(source: Record<string, unknown>, keys: string[]): number | null {
    for (const key of keys) {
      const parsed = this.parseNumber(source[key]);

      if (parsed !== null) {
        return parsed;
      }
    }

    return null;
  }

  private pickBoolean(source: Record<string, unknown>, keys: string[]): boolean | null {
    for (const key of keys) {
      const value = source[key];

      if (typeof value === 'boolean') {
        return value;
      }

      if (typeof value === 'string') {
        const normalized = value.trim().toLowerCase();

        if (normalized === 'true') {
          return true;
        }

        if (normalized === 'false') {
          return false;
        }
      }
    }

    return null;
  }

  private parseNumber(value: unknown): number | null {
    if (typeof value === 'number' && Number.isFinite(value)) {
      return value;
    }

    if (typeof value === 'string' && value.trim()) {
      const parsed = Number(value);

      if (Number.isFinite(parsed)) {
        return parsed;
      }
    }

    return null;
  }
}
