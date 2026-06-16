import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable, map } from 'rxjs';
import { environment } from '../../../environments/environment';
import {
  ReportCashBreakdown,
  ReportCategory,
  ReportSummary,
  getTransactionCategoryLabel,
  getTransactionCategoryMaterialIcon,
  normalizeTransactionCategory
} from '../models';

@Injectable({ providedIn: 'root' })
export class ReportsService {
  private readonly http = inject(HttpClient);
  private readonly apiUrl = `${environment.apiUrl}/reports`;

  getSummary(month: string): Observable<ReportSummary> {
    const params = new HttpParams().set('month', this.normalizeMonth(month));

    return this.http.get<unknown>(`${this.apiUrl}/summary`, { params }).pipe(
      map((response) => this.normalizeSummary(response, month))
    );
  }

  private normalizeSummary(response: unknown, requestedMonth: string): ReportSummary {
    const source = this.asRecord(response);
    const categories = Array.isArray(source['categories'])
      ? source['categories'].map((item) => this.normalizeCategory(item))
      : [];
    const cashBreakdown = this.normalizeCashBreakdown(source['cashBreakdown']);

    return {
      month: this.pickString(source, ['month']) ?? this.normalizeMonth(requestedMonth),
      monthLabel: this.pickString(source, ['monthLabel']) ?? this.normalizeMonth(requestedMonth),
      income: this.pickNumber(source, ['income']) ?? 0,
      expenses: this.pickNumber(source, ['expenses']) ?? 0,
      netBalance: this.pickNumber(source, ['netBalance']) ?? 0,
      savingsRate: this.pickNumber(source, ['savingsRate']) ?? 0,
      trackedTransactions: this.pickNumber(source, ['trackedTransactions']) ?? 0,
      alertCount: this.pickNumber(source, ['alertCount']) ?? 0,
      categories,
      cashBreakdown
    };
  }

  private normalizeCategory(value: unknown): ReportCategory {
    const source = this.asRecord(value);
    const category = normalizeTransactionCategory(this.pickString(source, ['category']) ?? 'AUTRES');

    return {
      category,
      categoryLabel: this.pickString(source, ['categoryLabel']) ?? getTransactionCategoryLabel(category),
      icon: this.pickString(source, ['icon']) ?? getTransactionCategoryMaterialIcon(category),
      budget: this.pickNumber(source, ['budget']) ?? 0,
      spent: this.pickNumber(source, ['spent']) ?? 0,
      usagePercent: this.pickNumber(source, ['usagePercent']),
      remainingAmount: this.pickNumber(source, ['remainingAmount']),
      advice: this.pickString(source, ['advice']) ?? 'Lecture en cours.',
      status: this.pickString(source, ['status']) ?? 'INFO'
    };
  }

  private normalizeCashBreakdown(value: unknown): ReportCashBreakdown {
    const source = this.asRecord(value);
    const categories = Array.isArray(source['categories'])
      ? source['categories'].map((item) => {
        const itemSource = this.asRecord(item);
        const category = normalizeTransactionCategory(this.pickString(itemSource, ['category']) ?? 'AUTRES');

        return {
          category,
          categoryLabel: this.pickString(itemSource, ['categoryLabel']) ?? getTransactionCategoryLabel(category),
          amount: this.pickNumber(itemSource, ['amount']) ?? 0,
          share: this.pickNumber(itemSource, ['share']) ?? 0,
          transactionCount: this.pickNumber(itemSource, ['transactionCount']) ?? 0
        };
      })
      : [];

    return {
      totalCashExpenses: this.pickNumber(source, ['totalCashExpenses']) ?? 0,
      shareOfExpenses: this.pickNumber(source, ['shareOfExpenses']) ?? 0,
      transactionCount: this.pickNumber(source, ['transactionCount']) ?? 0,
      completedBreakdowns: this.pickNumber(source, ['completedBreakdowns']) ?? 0,
      pendingBreakdowns: this.pickNumber(source, ['pendingBreakdowns']) ?? 0,
      averageTransactionAmount: this.pickNumber(source, ['averageTransactionAmount']) ?? 0,
      categories
    };
  }

  private normalizeMonth(value: string): string {
    const trimmed = `${value ?? ''}`.trim();

    if (/^\d{4}-\d{2}$/.test(trimmed)) {
      return trimmed;
    }

    const now = new Date();
    const month = String(now.getMonth() + 1).padStart(2, '0');
    return `${now.getFullYear()}-${month}`;
  }

  private asRecord(value: unknown): Record<string, unknown> {
    return value && typeof value === 'object' && !Array.isArray(value)
      ? value as Record<string, unknown>
      : {};
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
      const value = source[key];

      if (typeof value === 'number' && Number.isFinite(value)) {
        return value;
      }

      if (typeof value === 'string' && value.trim()) {
        const parsed = Number(value);

        if (Number.isFinite(parsed)) {
          return parsed;
        }
      }
    }

    return null;
  }
}
