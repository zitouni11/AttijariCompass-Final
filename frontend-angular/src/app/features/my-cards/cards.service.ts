import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, map } from 'rxjs';
import { environment } from '../../../environments/environment';
import {
  CardCatalogItem,
  CardTransaction,
  LinkCardRequest,
  UserCardDetails,
  UserCardLinkResponse,
  UserCardSummary,
  maskCardNumber,
  resolveLast4
} from './cards.models';

@Injectable({ providedIn: 'root' })
export class CardsService {
  private readonly http = inject(HttpClient);
  private readonly apiUrl = environment.cardsApiUrl || `${environment.apiUrl}/cards`;

  getCatalog(): Observable<CardCatalogItem[]> {
    return this.http.get<unknown>(`${this.apiUrl}/catalog`).pipe(
      map((response) => this.normalizeCatalogResponse(response))
    );
  }

  linkCard(payload: LinkCardRequest): Observable<UserCardLinkResponse> {
    return this.http.post<unknown>(`${this.apiUrl}/link`, this.normalizeLinkRequest(payload)).pipe(
      map((response) => this.normalizeLinkResponse(response))
    );
  }

  getMyCards(): Observable<UserCardSummary[]> {
    return this.http.get<unknown>(`${this.apiUrl}/my-cards`).pipe(
      map((response) => this.normalizeCardsResponse(response))
    );
  }

  getCardDetails(cardId: number): Observable<UserCardDetails> {
    return this.http.get<unknown>(`${this.apiUrl}/my-cards/${cardId}`).pipe(
      map((response) => this.normalizeCardDetailsResponse(response, cardId))
    );
  }

  getCardTransactions(cardId: number): Observable<CardTransaction[]> {
    return this.http.get<unknown>(`${this.apiUrl}/my-cards/${cardId}/transactions`).pipe(
      map((response) => this.normalizeTransactionsResponse(response))
    );
  }

  unlinkCard(cardId: number): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/my-cards/${cardId}`);
  }

  private normalizeLinkRequest(payload: LinkCardRequest): LinkCardRequest {
    return {
      cardCatalogId: payload.cardCatalogId,
      cardNumber: payload.cardNumber.replace(/\D+/g, ''),
      expiryMonth: payload.expiryMonth,
      expiryYear: payload.expiryYear
    };
  }

  private normalizeCatalogResponse(response: unknown): CardCatalogItem[] {
    const source = this.asRecord(response);
    const rawItems = Array.isArray(response)
      ? response
      : this.pickArray(source, ['items', 'catalog', 'cards', 'data', 'content']);

    return rawItems.map((item, index) => this.normalizeCatalogItem(item, index));
  }

  private normalizeCardsResponse(response: unknown): UserCardSummary[] {
    const source = this.asRecord(response);
    const rawItems = Array.isArray(response)
      ? response
      : this.pickArray(source, ['items', 'cards', 'myCards', 'data', 'content']);

    return rawItems
      .map((item, index) => this.normalizeCardSummary(item, index + 1))
      .sort((left, right) => {
        const rightDate = new Date(right.linkedAt ?? 0).getTime();
        const leftDate = new Date(left.linkedAt ?? 0).getTime();
        return rightDate - leftDate;
      });
  }

  private normalizeCardDetailsResponse(response: unknown, fallbackId: number): UserCardDetails {
    const source = this.asRecord(response);
    const rawCard =
      source['card'] ??
      source['item'] ??
      source['data'] ??
      source['content'] ??
      response;

    return this.normalizeCardDetails(rawCard, fallbackId);
  }

  private normalizeTransactionsResponse(response: unknown): CardTransaction[] {
    const source = this.asRecord(response);
    let rawItems = Array.isArray(response)
      ? response
      : this.pickArray(source, ['items', 'transactions', 'operations', 'data', 'content']);

    if (!rawItems.length) {
      const nested = this.asRecord(source['card'] ?? source['item'] ?? source['data'] ?? source['content']);
      rawItems = this.pickArray(nested, ['transactions', 'operations']);
    }

    return rawItems
      .map((item, index) => this.normalizeTransaction(item, index))
      .sort((left, right) => new Date(right.transactionDate).getTime() - new Date(left.transactionDate).getTime());
  }

  private normalizeLinkResponse(response: unknown): UserCardLinkResponse {
    const source = this.asRecord(response);
    const rawCard =
      source['card'] ??
      source['item'] ??
      source['data'] ??
      source['content'] ??
      (this.looksLikeLinkedCard(source) ? response : null);

    const fallbackId = this.pickNumber(source, ['cardId', 'id', 'userCardId']) ?? 0;
    const card = rawCard ? this.normalizeCardDetails(rawCard, fallbackId) : null;

    return {
      message:
        this.pickString(source, ['message', 'detail', 'statusMessage']) ??
        'La carte a ete associee avec succes.',
      cardId: this.pickNumber(source, ['cardId', 'id', 'userCardId']) ?? card?.id ?? null,
      card
    };
  }

  private normalizeCatalogItem(value: unknown, index: number): CardCatalogItem {
    const source = this.asRecord(value);

    return {
      id: this.pickNumber(source, ['id', 'cardCatalogId']) ?? index + 1,
      name:
        this.pickString(source, ['name', 'cardCatalogName', 'label', 'productName']) ??
        `Carte ${index + 1}`,
      cardCode: this.pickString(source, ['cardCode', 'code', 'catalogCode', 'cardTypeCode', 'productCode']),
      cardType: this.pickString(source, ['type', 'cardType', 'category']),
      description: this.pickString(source, ['description', 'details', 'subtitle'])
    };
  }

  private normalizeCardSummary(value: unknown, fallbackId: number): UserCardSummary {
    const source = this.asRecord(value);
    const catalog = this.asRecord(source['cardCatalog']);
    const catalogue = this.asRecord(source['catalogue']);
    const rawCardNumber =
      this.pickString(source, ['maskedCardNumber', 'cardNumberMasked', 'maskedNumber']) ??
      this.pickString(source, ['cardNumber', 'pan']);
    const maskedCardNumber = maskCardNumber(rawCardNumber);
    const cardStatus = this.pickString(source, ['cardStatus', 'status', 'state']) ?? 'LINKED';

    return {
      id: this.pickNumber(source, ['id', 'userCardId', 'cardId']) ?? fallbackId,
      cardCatalogId:
        this.pickNumber(source, ['cardCatalogId', 'catalogId']) ??
        this.pickNumber(catalog, ['id', 'cardCatalogId']),
      cardCatalogCode:
        this.pickString(source, ['cardCatalogCode', 'catalogCode', 'cardTypeCode']) ??
        this.pickString(catalog, ['code', 'cardCatalogCode', 'catalogCode', 'cardTypeCode']) ??
        this.pickString(catalogue, ['code', 'cardCatalogCode', 'catalogCode', 'cardTypeCode']),
      cardCatalogName:
        this.pickString(source, ['cardCatalogName', 'catalogName', 'cardName', 'productName', 'name']) ??
        this.pickString(catalog, ['name', 'cardCatalogName', 'label']) ??
        'Carte Attijari',
      cardHolderName:
        this.pickString(source, ['cardHolderName', 'holderName', 'name']) ??
        'Titulaire non renseigne',
      maskedCardNumber,
      last4: this.pickString(source, ['last4']) ?? resolveLast4(maskedCardNumber),
      expiryMonth: this.pickNumber(source, ['expiryMonth', 'expMonth', 'month']),
      expiryYear: this.pickNumber(source, ['expiryYear', 'expYear', 'year']),
      cardCode:
        this.pickString(source, ['cardCode', 'code', 'productCode']) ??
        this.pickString(catalog, ['cardCode', 'productCode']) ??
        this.pickString(catalogue, ['cardCode', 'productCode']),
      cardStatus,
      primaryCard: this.pickBoolean(source, ['primaryCard', 'isPrimary']) ?? false,
      sourceType: this.pickString(source, ['sourceType', 'linkSource', 'origin', 'source']),
      linkedAt: this.pickString(source, ['linkedAt', 'createdAt', 'connectedAt', 'linkedDate'])
    };
  }

  private normalizeCardDetails(value: unknown, fallbackId: number): UserCardDetails {
    const summary = this.normalizeCardSummary(value, fallbackId);
    const source = this.asRecord(value);
    const transactions = this.pickArray(source, ['transactions', 'operations']);

    return {
      ...summary,
      description: this.pickString(source, ['description', 'details', 'subtitle', 'cardDescription']),
      transactions: transactions.length ? transactions.map((item, index) => this.normalizeTransaction(item, index)) : null
    };
  }

  private normalizeTransaction(value: unknown, index: number): CardTransaction {
    const source = this.asRecord(value);
    const amount = this.pickNumber(source, ['amount', 'transactionAmount', 'value']) ?? 0;
    const direction = this.resolveTransactionDirection(source, amount);

    return {
      id: this.pickScalar(source, ['id', 'transactionId', 'externalId']) ?? `tx-${index + 1}`,
      amount,
      merchantName:
        this.pickString(source, ['merchantName', 'merchant', 'merchantLabel', 'beneficiary']) ??
        null,
      category: this.pickString(source, ['category', 'transactionCategory']) ?? null,
      transactionDate:
        this.pickString(source, ['transactionDate', 'date', 'postedAt', 'bookingDate']) ??
        new Date().toISOString(),
      channel:
        this.pickString(source, ['channel', 'paymentMethod', 'source', 'origin', 'networkChannel']) ??
        null,
      direction,
      currency: this.pickString(source, ['currency', 'currencyCode'])?.toUpperCase() ?? 'TND'
    };
  }

  private looksLikeLinkedCard(source: Record<string, unknown>): boolean {
    return Boolean(
      this.pickNumber(source, ['id', 'userCardId', 'cardId']) ??
      this.pickString(source, ['maskedCardNumber', 'cardHolderName', 'cardCatalogName', 'cardCode'])
    );
  }

  private resolveTransactionDirection(
    source: Record<string, unknown>,
    amount: number
  ): CardTransaction['direction'] {
    const rawDirection =
      this.pickString(source, ['direction', 'transactionType', 'type', 'flow'])?.toLowerCase() ?? '';

    if (
      rawDirection.includes('credit') ||
      rawDirection.includes('refund') ||
      rawDirection.includes('incoming') ||
      rawDirection === 'cr'
    ) {
      return 'credit';
    }

    if (
      rawDirection.includes('debit') ||
      rawDirection.includes('payment') ||
      rawDirection.includes('purchase') ||
      rawDirection.includes('withdraw') ||
      rawDirection === 'dr'
    ) {
      return 'debit';
    }

    if (amount > 0) {
      return 'credit';
    }

    if (amount < 0) {
      return 'debit';
    }

    return 'unknown';
  }

  private asRecord(value: unknown): Record<string, unknown> {
    return value && typeof value === 'object' && !Array.isArray(value)
      ? value as Record<string, unknown>
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

      if (typeof value === 'number') {
        if (value === 1) {
          return true;
        }

        if (value === 0) {
          return false;
        }
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

  private pickScalar(source: Record<string, unknown>, keys: string[]): string | number | null {
    for (const key of keys) {
      const value = source[key];

      if (typeof value === 'string' || typeof value === 'number') {
        return value;
      }
    }

    return null;
  }
}
