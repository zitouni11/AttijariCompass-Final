import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, catchError, forkJoin, map, of, shareReplay } from 'rxjs';
import { environment } from '../../../environments/environment';
import {
  EnrichedWithIncomeClassification,
  IncomeClassificationRequest,
  IncomeClassificationResult,
  IncomeTransactionSnapshot,
  TransactionResponse
} from '../models';

export interface IncomeClassificationAdapter<T extends object> {
  isCredit(item: T): boolean;
  toSnapshot(item: T): IncomeTransactionSnapshot;
}

@Injectable({ providedIn: 'root' })
export class IncomeClassificationService {
  private readonly http = inject(HttpClient);
  private readonly apiUrl = `${environment.apiUrl}/income-classification/test`;
  private readonly transactionCache = new Map<string, Observable<IncomeClassificationResult>>();

  classifyTransaction(
    transaction: Pick<TransactionResponse, 'merchantName' | 'description' | 'amount' | 'date'>
  ): Observable<IncomeClassificationResult> {
    const currentTransaction = {
      merchantName: transaction.merchantName || transaction.description,
      description: transaction.description || '',
      amount: Math.abs(transaction.amount),
      transactionDate: this.normalizeTransactionDate(transaction.date)
    };
    const cacheKey = this.buildCacheKey(currentTransaction.merchantName, currentTransaction.amount);
    const cached = this.transactionCache.get(cacheKey);

    if (cached) {
      return cached;
    }

    const request$ = this.http.post<unknown>(this.apiUrl, {
      currentTransaction,
      historicalCredits: []
    }).pipe(
      map((response) => this.normalizeClassificationResponse(response)),
      catchError(() => of(this.createUnknownResult())),
      shareReplay(1)
    );

    this.transactionCache.set(cacheKey, request$);

    return request$;
  }

  classify(request: IncomeClassificationRequest): Observable<IncomeClassificationResult> {
    return this.http.post<unknown>(this.apiUrl, this.buildRequestPayload(request)).pipe(
      map((response) => this.normalizeClassificationResponse(response)),
      catchError(() => of(this.createUnknownResult()))
    );
  }

  enrichCredits<T extends object>(
    items: readonly T[],
    adapter: IncomeClassificationAdapter<T>
  ): Observable<EnrichedWithIncomeClassification<T>[]> {
    if (!items.length) {
      return of([]);
    }

    const entries = items.map((item, index) => ({
      item,
      index,
      snapshot: adapter.toSnapshot(item),
      isCredit: adapter.isCredit(item)
    }));

    const creditEntries = entries.filter((entry) => entry.isCredit);

    if (!creditEntries.length) {
      return of(entries.map(({ item }) => this.attachClassification(item, null)));
    }

    return forkJoin(
      entries.map((entry) => {
        if (!entry.isCredit) {
          return of(this.attachClassification(entry.item, null));
        }

        const historicalCredits = creditEntries
          .filter((candidate) => candidate.index !== entry.index)
          .map((candidate) => candidate.snapshot);

        return this.classify({
          currentTransaction: entry.snapshot,
          historicalCredits
        }).pipe(
          map((classification) => this.attachClassification(entry.item, classification)),
          catchError(() => of(this.attachClassification(entry.item, this.createUnknownResult())))
        );
      })
    );
  }

  private attachClassification<T extends object>(
    item: T,
    incomeClassification: IncomeClassificationResult | null
  ): EnrichedWithIncomeClassification<T> {
    return {
      ...item,
      incomeClassification
    };
  }

  private buildRequestPayload(request: IncomeClassificationRequest): IncomeClassificationRequest {
    return {
      currentTransaction: this.normalizeSnapshot(request.currentTransaction),
      historicalCredits: (request.historicalCredits ?? []).map((snapshot) => this.normalizeSnapshot(snapshot))
    };
  }

  private normalizeSnapshot(snapshot: IncomeTransactionSnapshot): IncomeTransactionSnapshot {
    return {
      merchantName: snapshot.merchantName?.trim() || snapshot.description?.trim() || '',
      description: snapshot.description?.trim() || '',
      amount: Number.isFinite(snapshot.amount) ? snapshot.amount : 0,
      transactionDate: this.normalizeTransactionDate(snapshot.transactionDate)
    };
  }

  private normalizeClassificationResponse(response: unknown): IncomeClassificationResult {
    const source = this.asRecord(response);
    const nested = this.pickRecord(source, ['classification', 'data', 'result', 'content']);
    const payload = nested ?? source;
    const finalConfidence =
      this.pickNumber(payload, ['finalConfidence', 'confidence']) ??
      this.pickNumber(source, ['finalConfidence', 'confidence']) ??
      0;

    return {
      finalType: this.normalizeFinalType(
        this.pickString(payload, ['finalType', 'type']) ??
        this.pickString(source, ['finalType', 'type']) ??
        'unknown'
      ),
      finalConfidence,
      source: this.pickString(payload, ['source']) ?? this.pickString(source, ['source']),
      confidence:
        this.pickNumber(payload, ['confidence', 'finalConfidence']) ??
        this.pickNumber(source, ['confidence', 'finalConfidence']) ??
        finalConfidence,
      reason: this.pickString(payload, ['reason']) ?? this.pickString(source, ['reason']),
      explanation:
        this.pickString(payload, ['explanation']) ??
        this.pickString(source, ['explanation']),
      mlPredictedType:
        this.pickString(payload, ['mlPredictedType']) ??
        this.pickString(source, ['mlPredictedType']),
      mlConfidence:
        this.pickNumber(payload, ['mlConfidence']) ??
        this.pickNumber(source, ['mlConfidence']) ??
        0,
      patternDetected:
        this.pickBoolean(payload, ['patternDetected']) ??
        this.pickBoolean(source, ['patternDetected']) ??
        false,
      patternType:
        this.pickString(payload, ['patternType']) ??
        this.pickString(source, ['patternType']),
      patternConfidence:
        this.pickNumber(payload, ['patternConfidence']) ??
        this.pickNumber(source, ['patternConfidence']) ??
        0
    };
  }

  private createUnknownResult(): IncomeClassificationResult {
    return {
      finalType: 'unknown',
      finalConfidence: 0,
      source: null,
      confidence: 0,
      reason: null,
      explanation: null,
      mlPredictedType: null,
      mlConfidence: 0,
      patternDetected: false,
      patternType: null,
      patternConfidence: 0
    };
  }

  private normalizeFinalType(value: string): string {
    const normalized = value
      .trim()
      .toLowerCase()
      .replace(/[\s-]+/g, '_');

    return normalized || 'unknown';
  }

  private buildCacheKey(merchantName: string, amount: number): string {
    const normalizedMerchant = merchantName.trim().toLowerCase();
    const normalizedAmount = Number.isFinite(amount) ? amount.toFixed(2) : '0.00';

    return `${normalizedMerchant}|${normalizedAmount}`;
  }

  private normalizeTransactionDate(value: string): string {
    const isoDate = value.match(/^\d{4}-\d{2}-\d{2}/)?.[0];

    if (isoDate) {
      return isoDate;
    }

    const parsed = new Date(value);

    if (Number.isNaN(parsed.getTime())) {
      return new Date().toISOString().slice(0, 10);
    }

    return parsed.toISOString().slice(0, 10);
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

  private pickBoolean(source: Record<string, unknown>, keys: string[]): boolean | null {
    for (const key of keys) {
      const value = source[key];

      if (typeof value === 'boolean') {
        return value;
      }

      if (typeof value === 'string') {
        const normalized = value.trim().toLowerCase();

        if (normalized === 'true' || normalized === '1' || normalized === 'yes') {
          return true;
        }

        if (normalized === 'false' || normalized === '0' || normalized === 'no') {
          return false;
        }
      }
    }

    return null;
  }
}
