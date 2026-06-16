import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable, catchError, map, of, tap } from 'rxjs';
import { environment } from '../../../environments/environment';
import { AlertService } from '../../shared/alerts/alert.service';
import {
  TransactionResponse, TransactionRequest, CardPaymentRequest,
  PaginatedResponse,
  ImportTransactionError,
  ImportTransactionsResponse,
  ImportTransactionsSummary,
  TransactionCashBreakdownRequest, TransactionCashBreakdownResponse,
  UpdateCategoryRequest, GoalResponse, GoalRequest,
  GoalAnalysisResponse, GoalPredictionResponse,
  GoalRecommendationResponse, GoalRecommendationsResponse,
  GoalScenarioResponse, GoalSimulationsResponse,
  GoalStorytellingRequest, GoalStorytellingResponse,
  SavingsSimulationRequest, SavingsSimulationResponse,
  CreditSimulationRequest, CreditSimulationResponse,
  MonthlyStoryResponse, UserResponse, UserRequest,
  normalizeTransactionCategory
} from '../models';

// ==================== TRANSACTION SERVICE ====================
@Injectable({ providedIn: 'root' })
export class TransactionService {
  private http = inject(HttpClient);
  private apiUrl = `${environment.apiUrl}/transactions`;
  private readonly alertService = inject(AlertService);

  getAll(): Observable<TransactionResponse[]> {
    return this.http.get<unknown>(this.apiUrl).pipe(
      map((response) => this.normalizeTransactionsArrayResponse(response))
    );
  }

  getAllPaginated(page: number = 0, size: number = 20): Observable<PaginatedResponse<TransactionResponse>> {
    const params = new HttpParams()
      .set('page', page.toString())
      .set('size', size.toString());
    return this.http.get<unknown>(this.apiUrl, { params }).pipe(
      map((response) => this.normalizePaginatedTransactionsResponse(response, page, size))
    );
  }

  getById(id: number): Observable<TransactionResponse> {
    return this.http.get<unknown>(`${this.apiUrl}/${id}`).pipe(
      map((response) => this.normalizeTransactionResponse(response, id))
    );
  }

  create(request: TransactionRequest): Observable<TransactionResponse> {
    return this.http.post<unknown>(this.apiUrl, request).pipe(
      map((response) => this.normalizeTransactionResponse(response)),
      tap(() => this.triggerAlertsRefresh())
    );
  }

  createCardPayment(request: CardPaymentRequest): Observable<TransactionResponse> {
    return this.http.post<unknown>(`${this.apiUrl}/card-payment`, request).pipe(
      map((response) => this.normalizeTransactionResponse(response)),
      tap(() => this.triggerAlertsRefresh())
    );
  }

  importTransactions(file: File): Observable<ImportTransactionsResponse> {
    const formData = new FormData();
    formData.append('file', file);
    return this.http.post<unknown>(`${this.apiUrl}/import`, formData).pipe(
      map((response) => this.normalizeImportTransactionsResponse(response)),
      tap(() => this.triggerAlertsRefresh())
    );
  }

  update(id: number, request: TransactionRequest): Observable<TransactionResponse> {
    return this.http.put<unknown>(`${this.apiUrl}/${id}`, request).pipe(
      map((response) => this.normalizeTransactionResponse(response, id)),
      tap(() => this.triggerAlertsRefresh())
    );
  }

  updateCategory(id: number, request: UpdateCategoryRequest): Observable<TransactionResponse> {
    return this.http.patch<unknown>(`${this.apiUrl}/${id}/category`, request).pipe(
      map((response) => this.normalizeTransactionResponse(response, id))
    );
  }

  getCashBreakdown(id: number): Observable<TransactionCashBreakdownResponse> {
    return this.http.get<unknown>(`${this.apiUrl}/${id}/cash-breakdown`).pipe(
      map((response) => this.normalizeCashBreakdownResponse(response, id))
    );
  }

  saveCashBreakdown(
    id: number,
    request: TransactionCashBreakdownRequest
  ): Observable<TransactionCashBreakdownResponse> {
    return this.http.put<unknown>(`${this.apiUrl}/${id}/cash-breakdown`, request).pipe(
      map((response) => this.normalizeCashBreakdownResponse(response, id))
    );
  }

  delete(id: number): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/${id}`);
  }

  deleteAllTransactions(): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/all`);
  }

  private triggerAlertsRefresh(): void {
    this.alertService.refreshAlertsWithToast().pipe(
      catchError(() => of([]))
    ).subscribe();
  }

  private normalizePaginatedTransactionsResponse(
    response: unknown,
    requestedPage: number,
    requestedSize: number
  ): PaginatedResponse<TransactionResponse> {
    const source = this.asRecord(response);
    const nested = this.pickRecord(source, ['data', 'result', 'payload']);
    const primary = nested ?? source;
    const rawItems = this.extractTransactionItems(primary, response);
    const allTransactions = rawItems.map((item, index) =>
      this.normalizeTransaction(item, this.pickNumber(this.asRecord(item), ['id', 'transactionId']) ?? index + 1)
    );
    const hasPaginationMeta = Boolean(
      Array.isArray(primary['content']) ||
      this.pickNumber(primary, ['totalElements', 'totalCount', 'count', 'pageNumber', 'number', 'currentPage', 'page', 'totalPages', 'pageCount']) !== null ||
      this.pickNumber(source, ['totalElements', 'totalCount', 'count', 'pageNumber', 'number', 'currentPage', 'page', 'totalPages', 'pageCount']) !== null
    );
    const content = hasPaginationMeta
      ? allTransactions
      : allTransactions.slice(requestedPage * requestedSize, (requestedPage + 1) * requestedSize);
    const totalElements =
      this.pickNumber(primary, ['totalElements', 'totalCount', 'count', 'numberOfElements']) ??
      this.pickNumber(source, ['totalElements', 'totalCount', 'count', 'numberOfElements']) ??
      allTransactions.length;
    const pageNumber =
      this.pickNumber(primary, ['pageNumber', 'number', 'currentPage', 'page']) ??
      this.pickNumber(source, ['pageNumber', 'number', 'currentPage', 'page']) ??
      requestedPage;
    const totalPages =
      this.pickNumber(primary, ['totalPages', 'pageCount', 'pages']) ??
      this.pickNumber(source, ['totalPages', 'pageCount', 'pages']) ??
      Math.max(1, Math.ceil(totalElements / Math.max(requestedSize, 1)));
    const size =
      this.pickNumber(primary, ['size', 'pageSize']) ??
      this.pickNumber(source, ['size', 'pageSize']) ??
      requestedSize;
    const numberOfElements =
      this.pickNumber(primary, ['numberOfElements']) ??
      this.pickNumber(source, ['numberOfElements']) ??
      content.length;

    return {
      content,
      totalElements,
      totalPages,
      pageNumber,
      size,
      numberOfElements
    };
  }

  private normalizeTransactionsArrayResponse(response: unknown): TransactionResponse[] {
    const source = this.asRecord(response);
    const items = this.extractTransactionItems(source, response);

    return items.map((item, index) =>
      this.normalizeTransaction(item, this.pickNumber(this.asRecord(item), ['id', 'transactionId']) ?? index + 1)
    );
  }

  private normalizeImportTransactionsResponse(response: unknown): ImportTransactionsResponse {
    const source = this.asRecord(response);
    const nested = this.pickRecord(source, ['data', 'result', 'payload']) ?? source;
    const rawTransactions = Array.isArray(nested['transactions']) ? nested['transactions'] : [];
    const transactions = rawTransactions.map((item, index) =>
      this.normalizeTransaction(item, this.pickNumber(this.asRecord(item), ['id', 'transactionId']) ?? index + 1)
    );
    const summarySource = this.pickRecord(nested, ['summary']);

    return {
      totalProcessed: this.pickNumber(nested, ['totalProcessed', 'processedCount']) ?? transactions.length,
      successCount: this.pickNumber(nested, ['successCount']) ?? transactions.length,
      importedCount:
        this.pickNumber(nested, ['importedCount', 'successCount']) ??
        transactions.length,
      errorCount: this.pickNumber(nested, ['errorCount']) ?? 0,
      errors: this.normalizeImportErrors(nested['errors']),
      message:
        this.pickString(nested, ['message']) ??
        `${transactions.length} transaction(s) importee(s) avec succes.`,
      transactions,
      summary: summarySource ? this.normalizeImportSummary(summarySource) : undefined
    };
  }

  private normalizeTransactionResponse(response: unknown, fallbackId = 0): TransactionResponse {
    const source = this.asRecord(response);
    const nested =
      this.pickRecord(source, ['transaction', 'item', 'data', 'content', 'result']) ??
      source;

    return this.normalizeTransaction(nested, fallbackId);
  }

  private extractTransactionItems(source: Record<string, unknown>, response: unknown): unknown[] {
    if (Array.isArray(response)) {
      return response;
    }

    return this.pickArray(source, ['content', 'transactions', 'items', 'data', 'results', 'operations']);
  }

  private normalizeTransaction(value: unknown, fallbackId: number): TransactionResponse {
    const source = this.asRecord(value);
    const amount = this.pickNumber(source, ['amount', 'transactionAmount', 'value']) ?? 0;
    const rawCategory = this.pickString(source, ['category', 'transactionCategory', 'mccCategory']) ?? 'AUTRES';
    const cardLast4 =
      this.pickString(source, ['cardLast4', 'maskedCardNumberLast4', 'last4']) ??
      this.extractLast4(this.pickString(source, ['maskedCardNumber', 'cardNumberMasked', 'maskedNumber']));
    const userCardId = this.pickNumber(source, ['userCardId', 'linkedUserCardId', 'cardId', 'paymentCardId']);

    return {
      id: this.pickNumber(source, ['id', 'transactionId']) ?? fallbackId,
      description:
        this.pickString(source, ['description', 'label', 'narrative', 'summary']) ??
        this.pickString(source, ['merchantName', 'merchant', 'merchantLabel']) ??
        `Transaction ${fallbackId || 1}`,
      amount: Math.abs(amount),
      date:
        this.pickString(source, ['date', 'transactionDate', 'postedAt', 'bookingDate', 'createdAt']) ??
        new Date().toISOString(),
      category: this.normalizeCategory(rawCategory),
      type: this.normalizeTransactionType(source, amount),
      userId: this.pickNumber(source, ['userId', 'ownerId', 'customerId']) ?? 0,
      merchantName:
        this.pickString(source, ['merchantName', 'merchant', 'merchantLabel', 'beneficiary']) ?? undefined,
      paymentMethod: this.normalizePaymentMethod(
        this.pickString(source, ['paymentMethod', 'method', 'channel']),
        userCardId,
        cardLast4
      ),
      userCardId: userCardId ?? undefined,
      source: this.normalizeTransactionSource(
        this.pickString(source, ['source', 'origin', 'sourceType']),
        userCardId,
        cardLast4
      ),
      cardLast4: cardLast4 ?? undefined,
      categorizationSource: this.normalizeCategorizationSource(
        this.pickString(source, ['categorizationSource', 'categorySource', 'classificationSource'])
      ),
      categorizationConfidence: this.pickNumber(
        source,
        ['categorizationConfidence', 'categoryConfidence', 'confidence']
      ),
      normalizedText:
        this.pickString(
          source,
          ['categorizationNormalizedText', 'normalizedText', 'cleanedText', 'normalizedDescription']
        ) ?? undefined,
      createdAt:
        this.pickString(source, ['createdAt', 'date', 'transactionDate', 'postedAt']) ??
        new Date().toISOString()
    };
  }

  private normalizeCashBreakdownResponse(
    response: unknown,
    fallbackTransactionId = 0
  ): TransactionCashBreakdownResponse {
    const source = this.asRecord(response);
    const nested =
      this.pickRecord(source, ['cashBreakdown', 'data', 'result', 'content']) ??
      source;
    const items = Array.isArray(nested['items'])
      ? nested['items'].map((item) => {
        const itemSource = this.asRecord(item);

        return {
          id: this.pickNumber(itemSource, ['id']) ?? undefined,
          category: this.normalizeCategory(this.pickString(itemSource, ['category']) ?? 'AUTRES'),
          categoryLabel: this.pickString(itemSource, ['categoryLabel']) ?? undefined,
          amount: this.pickNumber(itemSource, ['amount']) ?? 0,
          note: this.pickString(itemSource, ['note']) ?? undefined
        };
      })
      : [];

    return {
      transactionId: this.pickNumber(nested, ['transactionId']) ?? fallbackTransactionId,
      transactionAmount: this.pickNumber(nested, ['transactionAmount']) ?? 0,
      allocatedAmount: this.pickNumber(nested, ['allocatedAmount']) ?? 0,
      remainingAmount: this.pickNumber(nested, ['remainingAmount']) ?? 0,
      complete: this.pickBoolean(nested, ['complete']) ?? false,
      items
    };
  }

  private normalizeCategory(value: string): TransactionResponse['category'] {
    return normalizeTransactionCategory(value);
  }

  private normalizeTransactionType(source: Record<string, unknown>, amount: number): TransactionResponse['type'] {
    const rawType =
      this.pickString(source, ['type', 'transactionType', 'direction', 'flow'])?.trim().toUpperCase() ?? '';

    if (
      rawType.includes('REVENU') ||
      rawType.includes('CREDIT') ||
      rawType.includes('INCOME') ||
      rawType.includes('REFUND') ||
      rawType.includes('SALARY')
    ) {
      return 'REVENU';
    }

    if (
      rawType.includes('DEPENSE') ||
      rawType.includes('DEBIT') ||
      rawType.includes('PAYMENT') ||
      rawType.includes('PURCHASE') ||
      rawType.includes('WITHDRAW')
    ) {
      return 'DEPENSE';
    }

    return amount >= 0 ? 'REVENU' : 'DEPENSE';
  }

  private normalizePaymentMethod(
    rawValue: string | null,
    userCardId: number | null,
    cardLast4: string | null
  ): TransactionResponse['paymentMethod'] {
    const normalized = `${rawValue ?? ''}`.trim().toUpperCase();

    if (userCardId || cardLast4 || normalized.includes('CARD') || normalized.includes('POS')) {
      return 'CARD';
    }

    if (normalized.includes('TRANSFER') || normalized.includes('VIREMENT') || normalized.includes('BANK')) {
      return 'BANK_TRANSFER';
    }

    if (normalized.includes('WALLET') || normalized.includes('DIGITAL') || normalized.includes('MOBILE')) {
      return 'DIGITAL_WALLET';
    }

    if (normalized.includes('CASH') || normalized.includes('ESPECE')) {
      return 'CASH';
    }

    return 'CARD';
  }

  private normalizeTransactionSource(
    rawValue: string | null,
    userCardId: number | null,
    cardLast4: string | null
  ): TransactionResponse['source'] {
    const normalized = `${rawValue ?? ''}`.trim().toUpperCase();

    if (normalized === 'TEST_CARD' || normalized.includes('SANDBOX')) {
      return normalized === 'TEST_CARD' ? 'TEST_CARD' : 'CARD_SANDBOX';
    }

    if (normalized === 'CARD_SYNC' || normalized.includes('SYNC')) {
      return 'CARD_SYNC';
    }

    if (normalized === 'IMPORTED_FILE' || normalized.includes('IMPORT') || normalized.includes('CSV') || normalized.includes('UPLOAD')) {
      return 'IMPORTED_FILE';
    }

    if (normalized === 'BANK_API') {
      return 'BANK_API';
    }

    if (
      userCardId ||
      cardLast4 ||
      normalized.includes('CARD') ||
      normalized.includes('LINKED') ||
      normalized.includes('SYNC')
    ) {
      return 'MANUAL_CARD';
    }

    if (normalized.includes('BANK')) {
      return 'BANK_API';
    }

    return 'MANUAL_ENTRY';
  }

  private normalizeCategorizationSource(
    rawValue: string | null
  ): TransactionResponse['categorizationSource'] | undefined {
    const normalized = `${rawValue ?? ''}`.trim().toUpperCase();

    if (
      normalized === 'RULE_ENGINE' ||
      normalized === 'ML_MODEL' ||
      normalized === 'USER_FEEDBACK' ||
      normalized === 'ML_LOW_CONFIDENCE' ||
      normalized === 'FALLBACK'
    ) {
      return normalized;
    }

    return undefined;
  }

  private extractLast4(value: string | null): string | null {
    const digits = `${value ?? ''}`.replace(/\D+/g, '');
    return digits.length >= 4 ? digits.slice(-4) : null;
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

  private normalizeStringArray(value: unknown): string[] {
    return Array.isArray(value)
      ? value.filter((item): item is string => typeof item === 'string' && item.trim().length > 0)
      : [];
  }

  private normalizeImportErrors(value: unknown): ImportTransactionError[] {
    if (!Array.isArray(value)) {
      return [];
    }

    return value
      .map((item) => {
        if (typeof item === 'string') {
          const match = item.match(/(\d+)/);

          return {
            rowNumber: match ? Number.parseInt(match[1], 10) : 0,
            message: item
          } satisfies ImportTransactionError;
        }

        const source = this.asRecord(item);
        const message = this.pickString(source, ['message', 'error']) ?? '';
        if (!message) {
          return null;
        }

        return {
          rowNumber: this.pickNumber(source, ['rowNumber', 'row', 'line']) ?? 0,
          message
        } satisfies ImportTransactionError;
      })
      .filter((item): item is ImportTransactionError => item !== null);
  }

  private normalizeImportSummary(source: Record<string, unknown>): ImportTransactionsSummary {
    return {
      categorizedCount: this.pickNumber(source, ['categorizedCount']) ?? 0,
      expenseCount: this.pickNumber(source, ['expenseCount']) ?? 0,
      incomeCount: this.pickNumber(source, ['incomeCount']) ?? 0,
      totalExpenses: this.pickNumber(source, ['totalExpenses']) ?? 0,
      totalIncome: this.pickNumber(source, ['totalIncome']) ?? 0,
      netAmount: this.pickNumber(source, ['netAmount']) ?? 0
    };
  }
}

// ==================== GOAL SERVICE ====================
@Injectable({ providedIn: 'root' })
export class GoalService {
  private http = inject(HttpClient);
  private apiUrl = `${environment.apiUrl}/goals`;
  private readonly alertService = inject(AlertService);

  getAll(): Observable<GoalResponse[]> {
    return this.http.get<GoalResponse[]>(this.apiUrl);
  }

  getGoalsByUser(userId: number): Observable<GoalResponse[]> {
    return this.http.get<GoalResponse[]>(`${this.apiUrl}/user/${userId}`);
  }

  getById(id: number): Observable<GoalResponse> {
    return this.http.get<GoalResponse>(`${this.apiUrl}/${id}`);
  }

  getGoalById(id: number): Observable<GoalResponse> {
    return this.getById(id);
  }

  create(request: GoalRequest): Observable<GoalResponse> {
    return this.http.post<GoalResponse>(this.apiUrl, request).pipe(
      tap(() => this.triggerAlertsRefresh())
    );
  }

  createGoal(request: GoalRequest): Observable<GoalResponse> {
    return this.create(request);
  }

  update(id: number, request: GoalRequest): Observable<GoalResponse> {
    return this.http.put<GoalResponse>(`${this.apiUrl}/${id}`, request).pipe(
      tap(() => this.triggerAlertsRefresh())
    );
  }

  updateGoal(id: number, request: GoalRequest): Observable<GoalResponse> {
    return this.update(id, request);
  }

  addProgress(id: number, amount: number): Observable<GoalResponse> {
    const params = new HttpParams().set('amount', amount.toString());
    return this.http.patch<GoalResponse>(`${this.apiUrl}/${id}/progress`, null, { params }).pipe(
      tap(() => this.triggerAlertsRefresh())
    );
  }

  delete(id: number): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/${id}`).pipe(
      tap(() => this.triggerAlertsRefresh())
    );
  }

  deleteGoal(id: number): Observable<void> {
    return this.delete(id);
  }

  getGoalAnalysis(goalId: number): Observable<GoalAnalysisResponse> {
    return this.http.get<GoalAnalysisResponse>(`${this.apiUrl}/${goalId}/analysis`);
  }

  getGoalPrediction(goalId: number): Observable<GoalPredictionResponse> {
    return this.http.get<GoalPredictionResponse>(`${this.apiUrl}/${goalId}/prediction`);
  }

  getGoalSimulations(goalId: number): Observable<GoalSimulationsResponse | GoalScenarioResponse[]> {
    return this.http.get<GoalSimulationsResponse | GoalScenarioResponse[]>(`${this.apiUrl}/${goalId}/simulations`);
  }

  getGoalRecommendations(goalId: number): Observable<GoalRecommendationsResponse | GoalRecommendationResponse[]> {
    return this.http.get<GoalRecommendationsResponse | GoalRecommendationResponse[]>(`${this.apiUrl}/${goalId}/recommendations`);
  }

  getGoalStorytelling(goalId: number, request: GoalStorytellingRequest = {}): Observable<GoalStorytellingResponse> {
    return this.http.post<GoalStorytellingResponse>(`${this.apiUrl}/${goalId}/storytelling`, request);
  }

  private triggerAlertsRefresh(): void {
    this.alertService.refreshAlertsWithToast().pipe(
      catchError(() => of([]))
    ).subscribe();
  }
}

// ==================== SIMULATION SERVICE ====================
@Injectable({ providedIn: 'root' })
export class SimulationService {
  private http = inject(HttpClient);
  private apiUrl = `${environment.apiUrl}/simulations`;

  simulateSavings(request: SavingsSimulationRequest): Observable<SavingsSimulationResponse> {
    return this.http.post<SavingsSimulationResponse>(`${this.apiUrl}/savings`, request);
  }

  simulateCredit(request: CreditSimulationRequest): Observable<CreditSimulationResponse> {
    return this.http.post<CreditSimulationResponse>(`${this.apiUrl}/credit`, request);
  }
}

// ==================== STORYTELLING SERVICE ====================
@Injectable({ providedIn: 'root' })
export class StorytellingService {
  private http = inject(HttpClient);
  private apiUrl = `${environment.apiUrl}/storytelling`;

  getMonthlyStory(): Observable<MonthlyStoryResponse> {
    return this.http.get<MonthlyStoryResponse>(`${this.apiUrl}/monthly`);
  }
}

// ==================== USER SERVICE ====================
@Injectable({ providedIn: 'root' })
export class UserService {
  private http = inject(HttpClient);
  private apiUrl = `${environment.apiUrl}/users`;

  getMe(): Observable<UserResponse> {
    return this.http.get<UserResponse>(`${this.apiUrl}/me`);
  }

  getAll(): Observable<UserResponse[]> {
    return this.http.get<UserResponse[]>(this.apiUrl);
  }

  update(id: number, request: UserRequest): Observable<UserResponse> {
    return this.http.put<UserResponse>(`${this.apiUrl}/${id}`, request);
  }

  uploadMyPhoto(file: File): Observable<UserResponse> {
    const formData = new FormData();
    formData.append('file', file);
    return this.http.post<UserResponse>(`${this.apiUrl}/me/photo`, formData);
  }

  deleteMyPhoto(): Observable<UserResponse> {
    return this.http.delete<UserResponse>(`${this.apiUrl}/me/photo`);
  }

  delete(id: number): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/${id}`);
  }

  deleteMe(): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/me`);
  }
}
