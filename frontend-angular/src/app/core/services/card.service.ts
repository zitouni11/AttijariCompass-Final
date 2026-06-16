import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, catchError, map, of, tap } from 'rxjs';
import { environment } from '../../../environments/environment';
import { AlertService } from '../../shared/alerts/alert.service';
import {
  CardSyncResponse,
  CardTransactionDto,
  ConnectTestCardRequest,
  ConnectTestCardResponse,
  GenerateTestCardRequest,
  GenerateTestCardResponse,
  GeneratedSandboxCardDto,
  SandboxCardProfile,
  UserCardDto
} from '../models';

@Injectable({ providedIn: 'root' })
export class CardService {
  private readonly http = inject(HttpClient);
  private readonly apiUrl = environment.cardsApiUrl || `${environment.apiUrl}/cards`;
  private readonly alertService = inject(AlertService);

  connectTestCard(request: ConnectTestCardRequest): Observable<ConnectTestCardResponse> {
    return this.http.post<unknown>(`${this.apiUrl}/test/connect`, request).pipe(
      map((response) => this.normalizeConnectResponse(response)),
      tap(() => this.triggerAlertsRefresh())
    );
  }

  generateTestCard(request: GenerateTestCardRequest): Observable<GenerateTestCardResponse> {
    return this.http.post<unknown>(`${this.apiUrl}/test/generate`, request).pipe(
      map((response) => this.normalizeGenerateResponse(response, request))
    );
  }

  connectGeneratedTestCard(testCardId: number): Observable<ConnectTestCardResponse> {
    return this.http.post<unknown>(`${this.apiUrl}/test/${testCardId}/connect`, null).pipe(
      map((response) => this.normalizeConnectResponse(response)),
      tap(() => this.triggerAlertsRefresh())
    );
  }

  getMyCards(): Observable<UserCardDto[]> {
    return this.http.get<unknown>(`${this.apiUrl}/my-cards`).pipe(
      map((response) => this.normalizeCardsResponse(response))
    );
  }

  getCardTransactions(cardId: number): Observable<CardTransactionDto[]> {
    return this.http.get<unknown>(`${this.apiUrl}/${cardId}/transactions`).pipe(
      map((response) => this.normalizeTransactionsResponse(response))
    );
  }

  syncCard(cardId: number): Observable<CardSyncResponse> {
    return this.http.post<unknown>(`${this.apiUrl}/${cardId}/sync`, null).pipe(
      map((response) => this.normalizeSyncResponse(response, cardId)),
      tap(() => this.triggerAlertsRefresh())
    );
  }

  disconnectCard(cardId: number): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/${cardId}`);
  }

  private triggerAlertsRefresh(): void {
    this.alertService.refreshAlertsWithToast().pipe(
      catchError(() => of([]))
    ).subscribe();
  }

  private normalizeConnectResponse(response: unknown): ConnectTestCardResponse {
    const source = this.asRecord(response);
    const card = this.normalizeCard(
      source['card'] ?? source['data'] ?? source['userCard'] ?? source['connectedCard']
    );

    return {
      message:
        this.pickString(source, ['message', 'detail', 'statusMessage']) ??
        'Carte test connectee avec succes.',
      card,
      importedTransactions: this.pickNumber(source, ['importedTransactions', 'imported', 'createdTransactions']) ?? 0,
      skippedTransactions: this.pickNumber(source, ['skippedTransactions', 'skipped']) ?? 0,
      syncedAt:
        this.pickString(source, ['syncedAt', 'lastSyncAt', 'timestamp']) ??
        card.lastSyncAt
    };
  }

  private normalizeGenerateResponse(
    response: unknown,
    request: GenerateTestCardRequest
  ): GenerateTestCardResponse {
    const source = this.asRecord(response);
    const connectToCurrentUser =
      this.pickBoolean(source, ['connectToCurrentUser', 'connectedToCurrentUser', 'autoConnected']) ??
      request.connectToCurrentUser;

    const generatedSource =
      source['generatedCard'] ??
      source['testCard'] ??
      source['sandboxCard'] ??
      source['generated'] ??
      (!connectToCurrentUser ? source['card'] : null) ??
      response;

    const connectedSource =
      source['userCard'] ??
      source['connectedCard'] ??
      (connectToCurrentUser ? source['card'] : null);

    const generatedCard = this.normalizeGeneratedCard(generatedSource, request);
    const connectedCard = connectedSource ? this.normalizeCard(connectedSource) : null;

    return {
      message:
        this.pickString(source, ['message', 'detail', 'statusMessage']) ??
        'Carte sandbox generee avec succes.',
      generatedCard,
      connectToCurrentUser,
      card: connectedCard,
      importedTransactions: this.pickNumber(source, ['importedTransactions', 'imported', 'generatedTransactions']) ?? request.transactionCount,
      skippedTransactions: this.pickNumber(source, ['skippedTransactions', 'skipped']) ?? 0,
      syncedAt: this.pickString(source, ['syncedAt', 'lastSyncAt', 'timestamp'])
    };
  }

  private normalizeSyncResponse(response: unknown, cardId: number): CardSyncResponse {
    const source = this.asRecord(response);
    const normalized = this.normalizeConnectResponse(response);

    return {
      ...normalized,
      card: normalized.card.id
        ? normalized.card
        : {
            ...normalized.card,
            id: cardId
          }
    };
  }

  private normalizeCardsResponse(response: unknown): UserCardDto[] {
    const source = this.asRecord(response);
    const rawCards = Array.isArray(response)
      ? response
      : this.pickArray(source, ['cards', 'items', 'data', 'content']);

    return rawCards.map((item, index) => this.normalizeCard(item, index));
  }

  private normalizeTransactionsResponse(response: unknown): CardTransactionDto[] {
    const source = this.asRecord(response);
    const rawTransactions = Array.isArray(response)
      ? response
      : this.pickArray(source, ['transactions', 'items', 'data', 'content']);

    return rawTransactions
      .map((item, index) => this.normalizeTransaction(item, index))
      .sort((left, right) => new Date(right.date).getTime() - new Date(left.date).getTime());
  }

  private normalizeCard(value: unknown, index = 0): UserCardDto {
    const source = this.asRecord(value);
    const connectedAt =
      this.pickString(source, ['connectedAt', 'createdAt', 'connectedDate']) ??
      new Date().toISOString();
    const lastSyncAt =
      this.pickString(source, ['lastSyncAt', 'syncedAt', 'updatedAt']) ??
      connectedAt;

    return {
      id: this.pickNumber(source, ['id', 'cardId']) ?? index + 1,
      linkedTestCardId: this.pickNumber(source, ['linkedTestCardId', 'testCardId']) ?? 0,
      holderName:
        this.pickString(source, ['holderName', 'name', 'cardHolderName']) ??
        'Titulaire inconnu',
      maskedCardNumber:
        this.pickString(source, ['maskedCardNumber', 'cardNumberMasked', 'maskedNumber']) ??
        this.pickString(source, ['cardNumber']) ??
        '**** **** **** ****',
      cardType:
        this.pickString(source, ['cardType', 'type', 'scheme']) ??
        'CARTE',
      bankName:
        this.pickString(source, ['bankName', 'issuerName', 'bank']) ??
        'Attijari Bank',
      status:
        this.pickString(source, ['status', 'state']) ??
        'UNKNOWN',
      connectedAt,
      lastSyncAt,
      active: this.pickBoolean(source, ['active', 'enabled']) ?? (this.pickString(source, ['status']) !== 'INACTIVE')
    };
  }

  private normalizeGeneratedCard(
    value: unknown,
    request: GenerateTestCardRequest
  ): GeneratedSandboxCardDto {
    const source = this.asRecord(value);
    const fullNumber = this.pickString(source, ['cardNumber', 'number', 'pan']);
    const maskedCardNumber =
      this.pickString(source, ['maskedCardNumber', 'cardNumberMasked', 'maskedNumber']) ??
      this.maskCardNumber(fullNumber) ??
      '**** **** **** ****';

    return {
      id: this.pickNumber(source, ['id', 'cardId', 'testCardId']) ?? 0,
      holderName:
        this.pickString(source, ['holderName', 'name', 'cardHolderName']) ??
        request.holderName,
      maskedCardNumber,
      cardType:
        this.pickString(source, ['cardType', 'type', 'scheme']) ??
        'VISA',
      bankName:
        this.pickString(source, ['bankName', 'issuerName', 'bank']) ??
        'Attijari Bank Tunisie',
      expiryMonth: this.pickNumber(source, ['expiryMonth', 'expMonth', 'month']) ?? 12,
      expiryYear: this.pickNumber(source, ['expiryYear', 'expYear', 'year']) ?? (new Date().getFullYear() + 3),
      cvv:
        this.pickString(source, ['cvv', 'securityCode', 'cvc']) ??
        '***',
      profile:
        this.pickProfile(source, ['profile', 'persona', 'cardProfile']) ??
        request.profile,
      transactionCount:
        this.pickNumber(source, ['transactionCount', 'generatedTransactionCount', 'transactionsGenerated']) ??
        request.transactionCount
    };
  }

  private normalizeTransaction(value: unknown, index: number): CardTransactionDto {
    const source = this.asRecord(value);
    const merchantName = this.pickString(source, ['merchantName', 'merchant', 'merchantLabel']) ?? undefined;
    const description =
      this.pickString(source, ['description', 'label', 'narrative', 'summary']) ??
      merchantName ??
      `Transaction ${index + 1}`;
    const date =
      this.pickString(source, ['date', 'transactionDate', 'postedAt', 'bookingDate', 'createdAt']) ??
      new Date().toISOString();

    return {
      id: this.pickScalar(source, ['id', 'transactionId', 'externalId']) ?? `tx-${index + 1}`,
      description,
      amount: this.pickNumber(source, ['amount', 'transactionAmount', 'value']) ?? 0,
      date,
      currency:
        this.pickString(source, ['currency', 'currencyCode']) ??
        'DT',
      merchantName,
      category: this.pickString(source, ['category', 'transactionCategory', 'mccCategory']) ?? undefined,
      transactionType: this.pickString(source, ['transactionType', 'direction', 'type']) ?? undefined,
      type: this.pickString(source, ['type', 'transactionType', 'direction']) ?? undefined,
      status: this.pickString(source, ['status', 'state']) ?? undefined,
      paymentMethod: this.pickString(source, ['paymentMethod', 'method']) ?? undefined,
      source: this.pickString(source, ['source', 'origin']) ?? undefined,
      cardLast4: this.pickString(source, ['cardLast4', 'maskedCardNumberLast4', 'last4']) ?? undefined
    };
  }

  private asRecord(value: unknown): Record<string, unknown> {
    return value && typeof value === 'object' && !Array.isArray(value)
      ? (value as Record<string, unknown>)
      : {};
  }

  private pickArray(source: Record<string, unknown>, keys: string[]): unknown[] {
    for (const key of keys) {
      const value = source[key];
      if (Array.isArray(value)) {
        return value;
      }
    }

    return [];
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

  private pickProfile(source: Record<string, unknown>, keys: string[]): SandboxCardProfile | null {
    const value = this.pickString(source, keys)?.toUpperCase();

    if (value === 'STUDENT' || value === 'SALARIED' || value === 'FAMILY' || value === 'PREMIUM') {
      return value;
    }

    return null;
  }

  private pickScalar(source: Record<string, unknown>, keys: string[]): number | string | null {
    for (const key of keys) {
      const value = source[key];
      if (typeof value === 'number' || typeof value === 'string') {
        return value;
      }
    }

    return null;
  }

  private maskCardNumber(value: string | null): string | null {
    if (!value) {
      return null;
    }

    const digits = value.replace(/\D+/g, '');

    if (digits.length < 4) {
      return null;
    }

    return `**** **** **** ${digits.slice(-4)}`;
  }
}
