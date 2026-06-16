import { CommonModule } from '@angular/common';
import { Component, DestroyRef, ElementRef, OnInit, ViewChild, computed, inject, signal } from '@angular/core';
import { takeUntilDestroyed, toSignal } from '@angular/core/rxjs-interop';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Params, Router, RouterLink } from '@angular/router';
import { finalize } from 'rxjs';
import {
  ImportTransactionError,
  IncomeClassificationResult,
  PaginatedResponse,
  TransactionCategory,
  TransactionResponse,
  TransactionType,
  getTransactionCategoryLabel,
  normalizeTransactionCategoryOrNull
} from '../../../core/models';
import { DEFAULT_PUBLIC_APP_SETTINGS } from '../../../core/models/app-settings.models';
import { AppSettingsService } from '../../../core/services/app-settings.service';
import { TransactionService } from '../../../core/services/api.services';
import { IncomeClassificationService } from '../../../core/services/income-classification.service';
import { NotificationService } from '../../../core/services/notification.service';
import { IncomeClassificationDisplayComponent } from '../../../shared/components/income-classification-display/income-classification-display.component';
import {
  formatConfidence,
  getCategorizationSourceMeta,
  getPaymentMethodMeta,
  getTransactionCategoryBackground,
  getTransactionCategoryColor,
  getTransactionCategoryIcon,
  getTransactionSourceMeta,
  TRANSACTION_CATEGORIES
} from '../transaction-ui';

type RecommendationAnalysisMode = 'category' | 'global_expense';

type TransactionListItem = TransactionResponse & {
  incomeType: string | null;
  incomeConfidence: number | null;
  incomeReason: string | null;
  incomeExplanation: string | null;
  isIncomeClassified: boolean;
};

interface TransactionsAnalysisContext {
  title: string;
  description: string;
  tipTitle: string;
  tipDescription: string;
  budgetCategory: TransactionCategory | null;
}

interface TransactionsAnalysisSummaryCard {
  label: string;
  value: string;
  detail: string;
  icon: string;
}

interface TransactionsShowcaseCard {
  label: string;
  value: string;
  detail: string;
  icon: string;
  tone: 'accent' | 'positive' | 'warning' | 'neutral';
}

@Component({
  selector: 'app-transactions-list',
  standalone: true,
  imports: [CommonModule, RouterLink, FormsModule, IncomeClassificationDisplayComponent],
  templateUrl: './transactions-list.component.html',
  styleUrls: ['./transactions-list.component.css', '../transactions.theme.scss']
})
export class TransactionsListComponent implements OnInit {
  private readonly txService = inject(TransactionService);
  private readonly appSettingsService = inject(AppSettingsService);
  private readonly incomeClassificationService = inject(IncomeClassificationService);
  private readonly notifService = inject(NotificationService);
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly destroyRef = inject(DestroyRef);

  private readonly amountFormatter = new Intl.NumberFormat('fr-FR', {
    minimumFractionDigits: 2,
    maximumFractionDigits: 2
  });
  private readonly integerFormatter = new Intl.NumberFormat('fr-FR', {
    minimumFractionDigits: 0,
    maximumFractionDigits: 0
  });

  @ViewChild('batchImportSection') batchImportSection?: ElementRef<HTMLElement>;
  @ViewChild('batchMlInput') batchMlInput?: ElementRef<HTMLInputElement>;

  readonly paginatedTransactions = signal<TransactionListItem[]>([]);
  readonly allLoadedTransactions = signal<TransactionListItem[]>([]);
  readonly currentPage = signal(0);
  readonly pageSize = signal(25);
  readonly serverTotalPages = signal(0);
  readonly serverTotalElements = signal(0);
  readonly loading = signal(true);
  readonly loadError = signal<string | null>(null);
  readonly searchQuery = signal('');
  readonly filterType = signal('');
  readonly filterCategory = signal('');
  readonly sortBy = signal('date-desc');
  readonly analysisSource = signal<string | null>(null);
  readonly analysisMode = signal<RecommendationAnalysisMode | null>(null);
  readonly analysisCategory = signal<TransactionCategory | null>(null);
  readonly analysisPeriod = signal<string | null>(null);
  readonly batchMlSelectedFile = signal<File | null>(null);
  readonly batchMlUploading = signal(false);
  readonly batchMlError = signal<string | null>(null);
  readonly batchMlSuccess = signal<string | null>(null);
  readonly batchMlImportedCount = signal(0);
  readonly batchMlErrorCount = signal(0);
  readonly batchMlRowErrors = signal<ImportTransactionError[]>([]);
  readonly publicSettings = toSignal(this.appSettingsService.publicSettings$, {
    initialValue: DEFAULT_PUBLIC_APP_SETTINGS
  });
  readonly importsDisabled = computed(() => this.publicSettings().maintenanceMode || !this.publicSettings().importsEnabled);
  readonly maxImportFileSizeMb = computed(() => this.publicSettings().maxImportFileSizeMb || DEFAULT_PUBLIC_APP_SETTINGS.maxImportFileSizeMb);
  readonly maxImportFileSizeBytes = computed(() => this.maxImportFileSizeMb() * 1024 * 1024);
  readonly importDisabledMessage = computed(() =>
    this.publicSettings().maintenanceMode
      ? 'L application est temporairement en maintenance.'
      : 'L import de transactions est temporairement desactive par l administrateur.'
  );

  showDeleteAllModal = false;
  isDeletingAll = false;
  deleteAllConfirmationText = '';
  deleteAllError = '';
  showImportModal = false;

  readonly categories = TRANSACTION_CATEGORIES;
  readonly isRecommendationAnalysisMode = computed(() =>
    this.analysisSource() === 'recommendation' && this.analysisMode() !== null
  );
  readonly filteredTransactions = computed(() => {
    const query = this.searchQuery().toLowerCase();
    const activeType = this.normalizeComparableTransactionType(this.filterType());
    const activeCategory = this.normalizeComparableTransactionCategory(this.filterCategory());
    const sourceTransactions = this.isRecommendationAnalysisMode()
      ? this.allLoadedTransactions()
      : this.paginatedTransactions();
    const analysisPeriodDays = this.isRecommendationAnalysisMode()
      ? this.resolveAnalysisPeriodDays(this.analysisPeriod())
      : null;

    return [...sourceTransactions]
      .filter((tx) => {
        const transactionType = this.normalizeComparableTransactionType(tx.type);
        const transactionCategory = this.normalizeComparableTransactionCategory(tx.category);
        const matchesAnalysisPeriod = !analysisPeriodDays || this.isWithinLastDays(tx.date, analysisPeriodDays);
        const matchesSearch = !query
          || tx.description.toLowerCase().includes(query)
          || (tx.merchantName || '').toLowerCase().includes(query);
        const matchesType = !activeType || transactionType === activeType;
        const matchesCategory = !activeCategory || transactionCategory === activeCategory;

        return matchesAnalysisPeriod && matchesSearch && matchesType && matchesCategory;
      })
      .sort((a, b) => this.sortTransactions(a, b));
  });
  readonly totalElements = computed(() =>
    this.isRecommendationAnalysisMode()
      ? this.filteredTransactions().length
      : Math.max(this.serverTotalElements(), this.paginatedTransactions().length)
  );
  readonly totalPages = computed(() =>
    this.isRecommendationAnalysisMode()
      ? Math.max(1, Math.ceil(this.filteredTransactions().length / Math.max(this.pageSize(), 1)))
      : this.serverTotalPages()
  );
  readonly visiblePageIndex = computed(() => Math.min(this.currentPage(), Math.max(0, this.totalPages() - 1)));
  readonly visibleTransactions = computed(() => {
    const filteredTransactions = this.filteredTransactions();

    if (!this.isRecommendationAnalysisMode()) {
      return filteredTransactions;
    }

    const pageSize = Math.max(this.pageSize(), 1);
    const start = this.visiblePageIndex() * pageSize;

    return filteredTransactions.slice(start, start + pageSize);
  });
  readonly filteredCount = computed(() => this.visibleTransactions().length);
  readonly filteredTotalCount = computed(() => this.filteredTransactions().length);
  readonly totalTransactionCount = computed(() =>
    this.isRecommendationAnalysisMode()
      ? this.allLoadedTransactions().length
      : Math.max(this.serverTotalElements(), this.paginatedTransactions().length)
  );
  readonly hasAnyTransactions = computed(() => this.totalTransactionCount() > 0);
  readonly hasActiveFilters = computed(() =>
    Boolean(this.searchQuery().trim() || this.filterType() || this.filterCategory() || this.sortBy() !== 'date-desc')
  );
  readonly pageSubtitle = computed(() =>
    this.isRecommendationAnalysisMode()
      ? `Vue d enquete guidee ouverte depuis vos recommandations IA. ${this.filteredCount()} transaction(s) visibles sur la page ${this.visiblePageIndex() + 1} / ${this.totalPages() || 1}`
      : `Vue unifiee de vos imports, saisies manuelles et transactions cartes. ${this.filteredCount()} transaction(s) visibles sur la page ${this.visiblePageIndex() + 1} / ${this.totalPages() || 1}`
  );
  readonly analysisContext = computed<TransactionsAnalysisContext>(() => this.buildAnalysisContext());
  readonly analysisAppliedFiltersLabel = computed(() => this.buildAnalysisAppliedFiltersLabel());
  readonly analysisSummaryCards = computed<TransactionsAnalysisSummaryCard[]>(() =>
    this.isRecommendationAnalysisMode() ? this.buildAnalysisSummary(this.filteredTransactions()) : []
  );
  readonly showcaseCards = computed<TransactionsShowcaseCard[]>(() =>
    this.isRecommendationAnalysisMode() ? [] : this.buildShowcaseCards(this.filteredTransactions())
  );
  readonly categorizedVisibleCount = computed(() =>
    this.filteredTransactions().filter((transaction) => this.hasCategorization(transaction)).length
  );
  readonly userFeedbackVisibleCount = computed(() =>
    this.filteredTransactions().filter((transaction) => transaction.categorizationSource === 'USER_FEEDBACK').length
  );
  readonly mlCoverageRate = computed(() => {
    const total = this.filteredTransactions().length;

    if (!total) {
      return 0;
    }

    return Math.round((this.categorizedVisibleCount() / total) * 100);
  });
  readonly totalDepenses = computed(() =>
    this.visibleTransactions()
      .filter((transaction) => this.isExpense(transaction))
      .reduce((sum, transaction) => sum + Math.abs(transaction.amount), 0)
  );

  readonly totalRevenus = computed(() =>
    this.visibleTransactions()
      .filter((transaction) => this.isIncome(transaction))
      .reduce((sum, transaction) => sum + Math.abs(transaction.amount), 0)
  );

  readonly net = computed(() => this.totalRevenus() - this.totalDepenses());

  ngOnInit(): void {
    this.route.queryParams
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe((params) => {
        this.applyRecommendationAnalysisFromRoute(params);
        this.loadTransactions(0);
      });
  }

  loadTransactions(page = 0): void {
    this.loading.set(true);
    this.loadError.set(null);
    this.currentPage.set(page);

    if (this.isRecommendationAnalysisMode()) {
      this.loadRecommendationAnalysisTransactions();
      return;
    }

    this.loadStandardTransactions(page);
  }

  nextPage(): void {
    if (this.visiblePageIndex() < this.totalPages() - 1) {
      if (this.isRecommendationAnalysisMode()) {
        this.currentPage.set(this.visiblePageIndex() + 1);
        return;
      }

      this.loadTransactions(this.visiblePageIndex() + 1);
    }
  }

  previousPage(): void {
    if (this.visiblePageIndex() > 0) {
      if (this.isRecommendationAnalysisMode()) {
        this.currentPage.set(this.visiblePageIndex() - 1);
        return;
      }

      this.loadTransactions(this.visiblePageIndex() - 1);
    }
  }

  goToPage(page: number): void {
    if (page >= 0 && page < this.totalPages()) {
      if (this.isRecommendationAnalysisMode()) {
        this.currentPage.set(page);
        return;
      }

      this.loadTransactions(page);
    }
  }

  updateSearchQuery(value: string): void {
    this.searchQuery.set(value);

    if (this.isRecommendationAnalysisMode()) {
      this.currentPage.set(0);
    }
  }

  updateSortBy(value: string): void {
    this.sortBy.set(value);

    if (this.isRecommendationAnalysisMode()) {
      this.currentPage.set(0);
    }
  }

  updateFilterType(value: string): void {
    this.filterType.set(value);

    if (this.isRecommendationAnalysisMode()) {
      this.currentPage.set(0);
    }
  }

  updateFilterCategory(value: string): void {
    this.filterCategory.set(value);

    if (this.isRecommendationAnalysisMode()) {
      this.currentPage.set(0);
    }
  }

  updatePageSize(value: string | number): void {
    const nextSize = typeof value === 'number' ? value : Number.parseInt(`${value}`, 10);

    if (!Number.isFinite(nextSize) || nextSize <= 0 || nextSize === this.pageSize()) {
      return;
    }

    this.pageSize.set(nextSize);
    this.currentPage.set(0);

    if (this.isRecommendationAnalysisMode()) {
      return;
    }

    this.loadTransactions(0);
  }

  scrollToBatchImport(): void {
    this.batchImportSection?.nativeElement.scrollIntoView({
      behavior: 'smooth',
      block: 'start'
    });
  }

  openImportModal(): void {
    this.showImportModal = true;
  }

  closeImportModal(): void {
    if (this.batchMlUploading()) {
      return;
    }

    this.showImportModal = false;
  }

  onBatchMlFileSelected(event: Event): void {
    const input = event.target as HTMLInputElement;
    const file = input.files?.[0] ?? null;

    if (this.importsDisabled()) {
      this.batchMlSelectedFile.set(null);
      this.batchMlError.set(this.importDisabledMessage());
      this.notifService.warning(this.importDisabledMessage());
      input.value = '';
      return;
    }

    if (file && file.size > this.maxImportFileSizeBytes()) {
      const message = `La taille maximale autorisee est de ${this.maxImportFileSizeMb()} Mo.`;
      this.batchMlSelectedFile.set(null);
      this.batchMlError.set(message);
      this.notifService.error(message);
      input.value = '';
      return;
    }

    this.batchMlSelectedFile.set(file);
    this.batchMlError.set(null);
    this.batchMlSuccess.set(null);
    this.batchMlImportedCount.set(0);
    this.batchMlErrorCount.set(0);
    this.batchMlRowErrors.set([]);
  }

  clearBatchMlFile(): void {
    this.batchMlSelectedFile.set(null);
    this.batchMlError.set(null);
    this.batchMlSuccess.set(null);
    this.batchMlImportedCount.set(0);
    this.batchMlErrorCount.set(0);
    this.batchMlRowErrors.set([]);

    if (this.batchMlInput) {
      this.batchMlInput.nativeElement.value = '';
    }
  }

  runBatchImport(): void {
    if (this.importsDisabled()) {
      const message = this.importDisabledMessage();
      this.batchMlError.set(message);
      this.notifService.warning(message);
      return;
    }

    const file = this.batchMlSelectedFile();

    if (!file) {
      const message = 'Choisissez un fichier Excel ou CSV avant de lancer l import.';
      this.batchMlError.set(message);
      this.notifService.error(message);
      return;
    }

    if (file.size > this.maxImportFileSizeBytes()) {
      const message = `La taille maximale autorisee est de ${this.maxImportFileSizeMb()} Mo.`;
      this.batchMlError.set(message);
      this.notifService.error(message);
      return;
    }

    this.batchMlUploading.set(true);
    this.batchMlError.set(null);
    this.batchMlSuccess.set(null);
    this.batchMlImportedCount.set(0);
    this.batchMlErrorCount.set(0);
    this.batchMlRowErrors.set([]);

    this.txService.importTransactions(file).subscribe({
      next: (response) => {
        const importedTransactions = response.transactions.map((transaction) => this.initializeTransaction(transaction));

        this.batchMlUploading.set(false);
        this.batchMlImportedCount.set(response.importedCount);
        this.batchMlErrorCount.set(response.errorCount);
        this.batchMlRowErrors.set(response.errors);
        this.applyImportedTransactions(importedTransactions, response.importedCount);
        this.batchMlSuccess.set(response.message);

        if (response.importedCount === 0 && response.errorCount > 0) {
          this.batchMlError.set(response.message);
          this.notifService.error(response.message);
        } else if (response.errorCount > 0) {
          this.notifService.warning(response.message);
        } else {
          this.notifService.success(response.message);
          this.showImportModal = false;
        }

        if (this.batchMlInput) {
          this.batchMlInput.nativeElement.value = '';
        }

        this.batchMlSelectedFile.set(null);
      },
      error: (error: unknown) => {
        this.batchMlUploading.set(false);
        const message = this.extractErrorMessage(error, 'Impossible d importer le fichier pour le moment.');
        this.batchMlError.set(message);
        this.batchMlImportedCount.set(0);
        this.batchMlErrorCount.set(0);
        this.batchMlRowErrors.set([]);
        this.notifService.error(message);
      }
    });
  }

  private loadStandardTransactions(page: number): void {
    this.txService.getAllPaginated(page, this.pageSize()).subscribe({
      next: (response: PaginatedResponse<TransactionResponse>) => {
        const transactions = (response.content || []).map((transaction) => this.initializeTransaction(transaction));

        this.allLoadedTransactions.set([]);
        this.paginatedTransactions.set(transactions);
        this.serverTotalElements.set(response.totalElements || 0);
        this.serverTotalPages.set(response.totalPages || 0);
        this.currentPage.set(response.pageNumber ?? response.number ?? page);
        this.loading.set(false);
        this.classifyIncomeTransactions(transactions);
      },
      error: (error: unknown) => {
        this.loading.set(false);
        const message = this.extractErrorMessage(error, 'Impossible de charger les transactions.');
        this.loadError.set(message);
        this.notifService.error(message);
      }
    });
  }

  private loadRecommendationAnalysisTransactions(): void {
    this.txService.getAll().subscribe({
      next: (response: TransactionResponse[]) => {
        const transactions = response.map((transaction) => this.initializeTransaction(transaction));

        this.allLoadedTransactions.set(transactions);
        this.paginatedTransactions.set([]);
        this.serverTotalElements.set(transactions.length);
        this.serverTotalPages.set(Math.max(1, Math.ceil(transactions.length / Math.max(this.pageSize(), 1))));
        this.loading.set(false);
        this.classifyIncomeTransactions(transactions);
      },
      error: (error: unknown) => {
        this.loading.set(false);
        const message = this.extractErrorMessage(error, 'Impossible de charger les transactions.');
        this.loadError.set(message);
        this.notifService.error(message);
      }
    });
  }

  clearFilters(): void {
    this.resetLocalFilters();

    if (this.isRecommendationAnalysisMode()) {
      this.exitRecommendationAnalysisMode();
    }
  }

  returnToRecommendations(): void {
    void this.router.navigate(['/recommendations']);
  }

  goToBudgetAction(): void {
    const category = this.analysisContext().budgetCategory;

    void this.router.navigate(['/budgets'], {
      queryParams: {
        category: category ?? null,
        source: 'recommendation',
        action: 'create'
      }
    });
  }

  get isDeleteAllConfirmationValid(): boolean {
    return this.deleteAllConfirmationText === 'SUPPRIMER';
  }

  openDeleteAllModal(event?: Event): void {
    event?.preventDefault();
    event?.stopPropagation();

    if (this.loading() || !this.hasAnyTransactions() || this.isDeletingAll) {
      return;
    }

    this.deleteAllConfirmationText = '';
    this.deleteAllError = '';
    this.showDeleteAllModal = true;
  }

  closeDeleteAllModal(): void {
    if (this.isDeletingAll) {
      return;
    }

    this.resetDeleteAllDialogState();
  }

  updateDeleteAllConfirmationText(value: string): void {
    this.deleteAllConfirmationText = value;

    if (this.deleteAllError) {
      this.deleteAllError = '';
    }
  }

  confirmDeleteAll(): void {
    if (!this.showDeleteAllModal || this.isDeletingAll || !this.isDeleteAllConfirmationValid || !this.hasAnyTransactions()) {
      return;
    }

    const deletedCount = this.totalTransactionCount();
    this.isDeletingAll = true;
    this.deleteAllError = '';

    this.txService.deleteAllTransactions()
      .pipe(finalize(() => {
        this.isDeletingAll = false;
      }))
      .subscribe({
        next: () => {
          this.applyEmptyStateAfterDeleteAll();
          this.resetDeleteAllDialogState();
          this.notifService.success(
            deletedCount > 1
              ? `${deletedCount} transactions supprimees avec succes.`
              : 'La transaction a ete supprimee avec succes.'
          );
          this.loadTransactions(0);
        },
        error: (error: unknown) => {
          this.deleteAllError = this.extractErrorMessage(
            error,
            'Impossible de supprimer toutes les transactions pour le moment.'
          );
        }
      });
  }

  delete(transaction: TransactionListItem): void {
    if (!confirm(`Supprimer la transaction "${transaction.description}" ?`)) {
      return;
    }

    this.txService.delete(transaction.id).subscribe({
      next: () => {
        const shouldGoBackOnePage =
          !this.isRecommendationAnalysisMode()
          && this.visibleTransactions().length === 1
          && this.visiblePageIndex() > 0;

        this.paginatedTransactions.update((items) => items.filter((item) => item.id !== transaction.id));
        this.allLoadedTransactions.update((items) => items.filter((item) => item.id !== transaction.id));
        this.notifService.success('Transaction supprimee.');
        this.loadTransactions(shouldGoBackOnePage ? this.visiblePageIndex() - 1 : this.visiblePageIndex());
      },
      error: () => this.notifService.error('Erreur lors de la suppression.')
    });
  }

  formatDate(date: string): string {
    return new Date(date).toLocaleDateString('fr-FR', { day: '2-digit', month: 'short' });
  }

  formatAmount(transaction: TransactionResponse): string {
    const prefix = this.isExpense(transaction) ? '-' : '+';
    return `${prefix}${Math.abs(transaction.amount).toLocaleString('fr-FR', {
      minimumFractionDigits: 2,
      maximumFractionDigits: 2
    })} DT`;
  }

  amountClass(transaction: TransactionResponse): string {
    return this.isExpense(transaction) ? 'amount-negative' : 'amount-positive';
  }

  isExpense(transaction: TransactionResponse): boolean {
    return transaction.type === 'DEPENSE';
  }

  isIncome(transaction: TransactionResponse): boolean {
    return transaction.type === 'REVENU';
  }

  getSourceMeta(transaction: TransactionResponse) {
    return getTransactionSourceMeta({
      source: transaction.source,
      userCardId: transaction.userCardId,
      cardLast4: transaction.cardLast4
    });
  }

  getCategoryIcon = getTransactionCategoryIcon;
  getCategoryColor = getTransactionCategoryColor;
  getCategoryBackground = getTransactionCategoryBackground;
  getCategoryLabel = getTransactionCategoryLabel;

  getMethodMeta(method: string) {
    return getPaymentMethodMeta(method);
  }

  getCategorizationMeta(transaction: TransactionResponse) {
    return getCategorizationSourceMeta(transaction.categorizationSource);
  }

  hasCategorization(transaction: TransactionResponse): boolean {
    return !!transaction.categorizationSource || transaction.categorizationConfidence !== undefined;
  }

  canManageCashBreakdown(transaction: TransactionResponse): boolean {
    return transaction.paymentMethod === 'CASH' && transaction.type === 'DEPENSE';
  }

  formatConfidenceValue(confidence?: number | null): string {
    return formatConfidence(confidence);
  }

  formatFileSize(bytes: number): string {
    if (bytes < 1024) {
      return `${bytes} B`;
    }

    if (bytes < 1024 * 1024) {
      return `${(bytes / 1024).toFixed(1)} KB`;
    }

    return `${(bytes / (1024 * 1024)).toFixed(1)} MB`;
  }

  getPageNumbers(): number[] {
    const totalPages = this.totalPages();
    const currentPage = this.visiblePageIndex();
    const maxPagesToShow = 7;

    if (totalPages <= maxPagesToShow) {
      return Array.from({ length: totalPages }, (_, index) => index);
    }

    const half = Math.floor(maxPagesToShow / 2);
    let start = Math.max(0, currentPage - half);
    const end = Math.min(totalPages, start + maxPagesToShow);

    if (end - start < maxPagesToShow) {
      start = Math.max(0, end - maxPagesToShow);
    }

    return Array.from({ length: end - start }, (_, index) => start + index);
  }

  private applyRecommendationAnalysisFromRoute(queryParams: Params): void {
    const source = `${this.readQueryParam(queryParams, 'source') ?? ''}`.trim().toLowerCase();
    const mode = this.normalizeAnalysisMode(this.readQueryParam(queryParams, 'analysisMode'));

    if (source !== 'recommendation' || !mode) {
      this.clearRecommendationAnalysisState();
      return;
    }

    const category = mode === 'category'
      ? this.normalizeAnalysisCategory(this.readQueryParam(queryParams, 'category'))
      : null;

    if (mode === 'category' && !category) {
      this.clearRecommendationAnalysisState();
      return;
    }

    this.analysisSource.set('recommendation');
    this.analysisMode.set(mode);
    this.analysisCategory.set(category);
    this.analysisPeriod.set(this.normalizeAnalysisPeriod(this.readQueryParam(queryParams, 'period')) ?? '30d');
    this.searchQuery.set('');
    this.filterType.set(this.normalizeTransactionType(this.readQueryParam(queryParams, 'type')) ?? 'DEPENSE');
    this.filterCategory.set(category ?? '');
    this.sortBy.set('date-desc');
    this.currentPage.set(0);
  }

  private readQueryParam(queryParams: Params, key: string): string | null {
    const value = queryParams[key];

    if (Array.isArray(value)) {
      return value.length > 0 ? `${value[0] ?? ''}` : null;
    }

    if (value === null || value === undefined) {
      return null;
    }

    return `${value}`;
  }

  private normalizeComparableTransactionType(value: string | null | undefined): TransactionType | '' {
    const normalized = this.normalizeComparableToken(value);

    if (!normalized) {
      return '';
    }

    if (
      normalized.includes('DEPENSE')
      || normalized.includes('EXPENSE')
      || normalized.includes('DEBIT')
      || normalized.includes('PAYMENT')
      || normalized.includes('PURCHASE')
      || normalized.includes('WITHDRAW')
    ) {
      return 'DEPENSE';
    }

    if (
      normalized.includes('REVENU')
      || normalized.includes('INCOME')
      || normalized.includes('CREDIT')
      || normalized.includes('SALARY')
      || normalized.includes('REFUND')
    ) {
      return 'REVENU';
    }

    return normalized === 'DEPENSE' || normalized === 'REVENU' ? normalized : '';
  }

  private normalizeComparableTransactionCategory(value: string | null | undefined): TransactionCategory | '' {
    return normalizeTransactionCategoryOrNull(value) ?? '';
  }

  private normalizeComparableToken(value: string | null | undefined): string {
    return `${value ?? ''}`
      .trim()
      .normalize('NFD')
      .replace(/[\u0300-\u036f]/g, '')
      .toUpperCase()
      .replace(/[\s-]+/g, '_');
  }

  private normalizeAnalysisMode(value: string | null): RecommendationAnalysisMode | null {
    const normalized = `${value ?? ''}`.trim().toLowerCase();

    if (normalized === 'category' || normalized === 'global_expense') {
      return normalized;
    }

    return null;
  }

  private normalizeAnalysisCategory(value: string | null): TransactionCategory | null {
    return normalizeTransactionCategoryOrNull(value);
  }

  private normalizeAnalysisPeriod(value: string | null): string | null {
    const normalized = `${value ?? ''}`.trim().toLowerCase();
    return /^\d+d$/.test(normalized) ? normalized : null;
  }

  private normalizeTransactionType(value: string | null): TransactionType | null {
    const normalized = `${value ?? ''}`.trim().toUpperCase();
    return normalized === 'DEPENSE' || normalized === 'REVENU' ? normalized : null;
  }

  private resolveAnalysisPeriodDays(value: string | null): number | null {
    if (!value?.trim()) {
      return null;
    }

    const parsed = Number.parseInt(value.replace(/d$/i, ''), 10);
    return Number.isFinite(parsed) && parsed > 0 ? parsed : null;
  }

  private isWithinLastDays(dateValue: string, days: number): boolean {
    const timestamp = new Date(dateValue).getTime();

    if (!Number.isFinite(timestamp)) {
      return false;
    }

    const now = Date.now();
    const windowMs = days * 24 * 60 * 60 * 1000;

    return timestamp >= now - windowMs;
  }

  private buildAnalysisContext(): TransactionsAnalysisContext {
    const periodLabel = this.getAnalysisPeriodLabel(this.analysisPeriod());
    const category = this.analysisCategory();

    if (!this.isRecommendationAnalysisMode()) {
      return {
        title: '',
        description: '',
        tipTitle: '',
        tipDescription: '',
        budgetCategory: null
      };
    }

    if (this.analysisMode() === 'global_expense') {
      return {
        title: 'Analyse ciblee - Hausse globale des depenses',
        description: `Cette vue regroupe vos depenses recentes pour identifier les transactions ayant le plus contribue a la hausse observee sur ${periodLabel}.`,
        tipTitle: 'Conseil IA',
        tipDescription: 'Triez vos depenses recentes par montant pour identifier rapidement les postes responsables de la hausse observee.',
        budgetCategory: null
      };
    }

    switch (category) {
      case 'SHOPPING':
        return {
          title: 'Analyse ciblee - Depenses Shopping',
          description: `Cette vue regroupe les transactions qui expliquent la recommandation IA sur votre budget shopping. Identifiez les achats les plus lourds et les commercants les plus frequents sur ${periodLabel}.`,
          tipTitle: 'Conseil IA',
          tipDescription: 'Reperez les 2 ou 3 transactions les plus lourdes dans cette categorie, puis definissez un budget cible realiste pour le mois prochain.',
          budgetCategory: 'SHOPPING'
        };
      case 'ALIMENTATION':
        return {
          title: 'Analyse ciblee - Depenses Alimentation',
          description: `Cette vue met en evidence les depenses alimentation ayant motive la recommandation IA. Verifiez les montants recents et leur frequence sur ${periodLabel}.`,
          tipTitle: 'Conseil IA',
          tipDescription: 'Reperez les achats les plus repetitifs ou les tickets les plus eleves, puis definissez un budget cible realiste pour le mois prochain.',
          budgetCategory: 'ALIMENTATION'
        };
      case 'CAFES':
        return {
          title: 'Analyse ciblee - Depenses Cafes',
          description: `Cette vue vous aide a comprendre quelles depenses cafes pesent le plus sur votre budget recent sur ${periodLabel}.`,
          tipTitle: 'Conseil IA',
          tipDescription: 'Isolez les depenses cafes les plus couteuses ou les plus frequentes, puis fixez un budget cible tenable pour le mois prochain.',
          budgetCategory: 'CAFES'
        };
      case 'RESTAURANT':
        return {
          title: 'Analyse ciblee - Depenses Restaurant',
          description: `Cette vue vous aide a comprendre quelles depenses restaurant pesent le plus sur votre budget recent sur ${periodLabel}.`,
          tipTitle: 'Conseil IA',
          tipDescription: 'Reperez les repas hors domicile les plus frequents ou les tickets les plus eleves, puis fixez un budget cible tenable pour le mois prochain.',
          budgetCategory: 'RESTAURANT'
        };
      case 'LIVRAISON':
        return {
          title: 'Analyse ciblee - Depenses Livraison',
          description: `Cette vue vous aide a comprendre quelles depenses livraison pesent le plus sur votre budget recent sur ${periodLabel}.`,
          tipTitle: 'Conseil IA',
          tipDescription: 'Reperez les commandes les plus frequentes et les tickets les plus eleves avant de definir un budget cible.',
          budgetCategory: 'LIVRAISON'
        };
      default:
        return {
          title: `Analyse ciblee - Depenses ${this.formatCategoryLabel(category)}`,
          description: `Cette vue regroupe les transactions qui expliquent la recommandation IA sur la categorie ${this.formatCategoryLabel(category).toLowerCase()} sur ${periodLabel}.`,
          tipTitle: 'Conseil IA',
          tipDescription: 'Reperez les transactions les plus lourdes dans cette categorie, puis definissez un budget cible realiste pour le mois prochain.',
          budgetCategory: category
        };
    }
  }

  private buildAnalysisAppliedFiltersLabel(): string {
    if (!this.isRecommendationAnalysisMode()) {
      return '';
    }

    const segments = ['Depenses'];

    if (this.filterCategory()) {
      segments.push(this.formatCategoryLabel(this.filterCategory() as TransactionCategory));
    } else if (this.analysisMode() === 'global_expense') {
      segments.push('Toutes categories');
    }

    segments.push(this.getAnalysisPeriodShortLabel(this.analysisPeriod()));

    return segments.join(' / ');
  }

  private buildAnalysisSummary(transactions: readonly TransactionListItem[]): TransactionsAnalysisSummaryCard[] {
    const expenses = transactions.filter((transaction) => this.isExpense(transaction));

    if (!expenses.length) {
      return [];
    }

    const totalObserved = expenses.reduce((sum, transaction) => sum + Math.abs(transaction.amount), 0);
    const transactionCount = expenses.length;
    const averageAmount = this.getAverageAmount(expenses);

    if (this.analysisMode() === 'global_expense') {
      const dominantCategory = this.getDominantCategory(expenses);
      const largestExpense = this.getLargestTransaction(expenses);

      return [
        {
          label: 'Total depenses',
          value: this.formatMoney(totalObserved),
          detail: `Sur ${this.getAnalysisPeriodShortLabel(this.analysisPeriod())}`,
          icon: 'payments'
        },
        {
          label: 'Nb depenses',
          value: this.formatCount(transactionCount),
          detail: 'Transactions sur la periode analysee',
          icon: 'receipt_long'
        },
        {
          label: 'Categorie dominante',
          value: dominantCategory ? this.formatCategoryLabel(dominantCategory.category) : 'Aucune dominante',
          detail: dominantCategory
            ? `${this.formatMoney(dominantCategory.total)} sur la periode`
            : 'Selon les depenses visibles',
          icon: 'category'
        },
        {
          label: 'Plus grosse depense',
          value: largestExpense ? this.describeTransaction(largestExpense) : 'Aucune depense marquee',
          detail: largestExpense ? this.formatMoney(Math.abs(largestExpense.amount)) : 'Selon les depenses visibles',
          icon: 'north_east'
        }
      ];
    }

    const topMerchant = this.getTopMerchant(expenses);
    const largestExpense = this.getLargestTransaction(expenses);

    return [
      {
        label: 'Total observe',
        value: this.formatMoney(totalObserved),
        detail: `Sur ${this.getAnalysisPeriodShortLabel(this.analysisPeriod())}`,
        icon: 'payments'
      },
      {
        label: 'Nb transactions',
        value: this.formatCount(transactionCount),
        detail: 'Transactions concernees',
        icon: 'receipt_long'
      },
      {
        label: 'Ticket moyen',
        value: this.formatMoney(averageAmount),
        detail: 'Montant moyen par transaction',
        icon: 'monitoring'
      },
      {
        label: topMerchant ? 'Commercant principal' : 'Plus grosse transaction',
        value: topMerchant ? topMerchant.label : largestExpense ? this.describeTransaction(largestExpense) : 'Aucune dominante',
        detail: topMerchant
          ? `${topMerchant.count} transaction(s) / ${this.formatMoney(topMerchant.total)}`
          : largestExpense
            ? this.formatMoney(Math.abs(largestExpense.amount))
            : 'Selon les transactions visibles',
        icon: topMerchant ? 'storefront' : 'shopping_bag'
      }
    ];
  }

  private buildShowcaseCards(transactions: readonly TransactionListItem[]): TransactionsShowcaseCard[] {
    const expenses = transactions.filter((transaction) => this.isExpense(transaction));
    const incomes = transactions.filter((transaction) => this.isIncome(transaction));
    return [
      {
        label: 'Transactions visibles',
        value: this.formatCount(transactions.length),
        detail: 'Apres filtres sur la page courante',
        icon: 'receipt_long',
        tone: 'accent'
      },
      {
        label: 'Depenses visibles',
        value: this.formatMoney(expenses.reduce((sum, transaction) => sum + Math.abs(transaction.amount), 0)),
        detail: `${this.formatCount(expenses.length)} depense(s) sur cette vue`,
        icon: 'south_west',
        tone: 'warning'
      },
      {
        label: 'Revenus visibles',
        value: this.formatMoney(incomes.reduce((sum, transaction) => sum + Math.abs(transaction.amount), 0)),
        detail: `${this.formatCount(incomes.length)} revenu(x) sur cette vue`,
        icon: 'north_east',
        tone: 'positive'
      }
    ];
  }

  private getTopMerchant(
    transactions: readonly TransactionListItem[]
  ): { label: string; total: number; count: number } | null {
    const merchantMap = new Map<string, { total: number; count: number }>();

    for (const transaction of transactions) {
      const label = (transaction.merchantName || transaction.description || '').trim();

      if (!label) {
        continue;
      }

      const current = merchantMap.get(label) ?? { total: 0, count: 0 };
      current.total += Math.abs(transaction.amount);
      current.count += 1;
      merchantMap.set(label, current);
    }

    const topMerchant = [...merchantMap.entries()]
      .sort((left, right) => {
        const totalGap = right[1].total - left[1].total;

        if (totalGap !== 0) {
          return totalGap;
        }

        return right[1].count - left[1].count;
      })[0];

    if (!topMerchant) {
      return null;
    }

    return {
      label: topMerchant[0],
      total: topMerchant[1].total,
      count: topMerchant[1].count
    };
  }

  private getDominantCategory(
    transactions: readonly TransactionListItem[]
  ): { category: TransactionCategory; total: number; count: number } | null {
    const categoryMap = new Map<TransactionCategory, { total: number; count: number }>();

    for (const transaction of transactions) {
      const current = categoryMap.get(transaction.category) ?? { total: 0, count: 0 };
      current.total += Math.abs(transaction.amount);
      current.count += 1;
      categoryMap.set(transaction.category, current);
    }

    const dominantCategory = [...categoryMap.entries()]
      .sort((left, right) => {
        const totalGap = right[1].total - left[1].total;

        if (totalGap !== 0) {
          return totalGap;
        }

        return right[1].count - left[1].count;
      })[0];

    if (!dominantCategory) {
      return null;
    }

    return {
      category: dominantCategory[0],
      total: dominantCategory[1].total,
      count: dominantCategory[1].count
    };
  }

  private getAverageAmount(transactions: readonly TransactionListItem[]): number {
    if (!transactions.length) {
      return 0;
    }

    const total = transactions.reduce((sum, transaction) => sum + Math.abs(transaction.amount), 0);
    return total / transactions.length;
  }

  private getLargestTransaction(transactions: readonly TransactionListItem[]): TransactionListItem | null {
    return transactions.reduce<TransactionListItem | null>((largest, transaction) => {
      if (!largest) {
        return transaction;
      }

      return Math.abs(transaction.amount) > Math.abs(largest.amount) ? transaction : largest;
    }, null);
  }

  private describeTransaction(transaction: TransactionListItem): string {
    return (transaction.merchantName || transaction.description || 'Transaction').trim();
  }

  private getAnalysisPeriodLabel(period: string | null): string {
    const days = this.resolveAnalysisPeriodDays(period);

    if (!days) {
      return 'la periode analysee';
    }

    return `les ${days} derniers jours`;
  }

  private getAnalysisPeriodShortLabel(period: string | null): string {
    const days = this.resolveAnalysisPeriodDays(period);

    if (!days) {
      return 'Periode analysee';
    }

    return `${days} derniers jours`;
  }

  private formatCategoryLabel(category: TransactionCategory | string | null | undefined): string {
    if (!category) {
      return 'Categorie';
    }

    return getTransactionCategoryLabel(category.toString());
  }

  private formatMoney(amount: number): string {
    return `${this.amountFormatter.format(Math.abs(amount))} DT`;
  }

  private formatCount(value: number): string {
    return this.integerFormatter.format(value);
  }

  private resetLocalFilters(): void {
    this.searchQuery.set('');
    this.filterType.set('');
    this.filterCategory.set('');
    this.sortBy.set('date-desc');
  }

  private clearRecommendationAnalysisState(): void {
    if (this.analysisSource() === 'recommendation' || this.analysisMode() !== null) {
      this.resetLocalFilters();
    }

    this.currentPage.set(0);
    this.resetAnalysisState();
  }

  private resetAnalysisState(): void {
    this.analysisSource.set(null);
    this.analysisMode.set(null);
    this.analysisCategory.set(null);
    this.analysisPeriod.set(null);
  }

  private exitRecommendationAnalysisMode(): void {
    this.resetAnalysisState();

    void this.router.navigate([], {
      relativeTo: this.route,
      queryParams: {
        analysisMode: null,
        category: null,
        type: null,
        period: null,
        source: null
      },
      queryParamsHandling: 'merge'
    });
  }

  private sortTransactions(a: TransactionResponse, b: TransactionResponse): number {
    switch (this.sortBy()) {
      case 'date-asc':
        return new Date(a.date).getTime() - new Date(b.date).getTime();
      case 'amount-asc':
        return a.amount - b.amount;
      case 'amount-desc':
        return b.amount - a.amount;
      case 'date-desc':
      default:
        return new Date(b.date).getTime() - new Date(a.date).getTime();
    }
  }

  private applyEmptyStateAfterDeleteAll(): void {
    this.clearFilters();
    this.allLoadedTransactions.set([]);
    this.paginatedTransactions.set([]);
    this.serverTotalElements.set(0);
    this.serverTotalPages.set(0);
    this.currentPage.set(0);
    this.loadError.set(null);
  }

  private resetDeleteAllDialogState(): void {
    this.showDeleteAllModal = false;
    this.deleteAllConfirmationText = '';
    this.deleteAllError = '';
  }

  private extractErrorMessage(error: unknown, fallback: string): string {
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

    return fallback;
  }

  private initializeTransaction(transaction: TransactionResponse): TransactionListItem {
    return {
      ...transaction,
      incomeType: null,
      incomeConfidence: null,
      incomeReason: null,
      incomeExplanation: null,
      isIncomeClassified: !this.isIncome(transaction)
    };
  }

  private applyImportedTransactions(importedTransactions: TransactionListItem[], importedCount: number): void {
    const existingTransactions = this.paginatedTransactions();
    const mergedTransactions = [
      ...importedTransactions,
      ...existingTransactions.filter((transaction) =>
        !importedTransactions.some((importedTransaction) => importedTransaction.id === transaction.id)
      )
    ].sort((left, right) => this.sortTransactions(left, right));
    const additionalCount = importedTransactions.filter((transaction) =>
      !existingTransactions.some((existingTransaction) => existingTransaction.id === transaction.id)
    ).length;
    const nextTotal = Math.max(
      mergedTransactions.length,
      this.serverTotalElements() + Math.max(additionalCount, importedCount)
    );

    this.paginatedTransactions.set(mergedTransactions);
    this.allLoadedTransactions.set([]);
    this.serverTotalElements.set(nextTotal);
    this.serverTotalPages.set(Math.max(1, Math.ceil(nextTotal / Math.max(this.pageSize(), 1))));
    this.currentPage.set(0);
    this.loadError.set(null);
    this.classifyIncomeTransactions(importedTransactions);
  }

  private classifyIncomeTransactions(transactions: TransactionListItem[]): void {
    transactions
      .filter((transaction) => this.isIncome(transaction) && !transaction.isIncomeClassified)
      .forEach((transaction) => {
        this.incomeClassificationService.classifyTransaction(transaction)
          .subscribe((result) => this.applyIncomeClassification(transaction.id, result));
      });
  }

  private applyIncomeClassification(id: number, result: IncomeClassificationResult): void {
    this.paginatedTransactions.update((transactions) =>
      transactions.map((transaction) =>
        transaction.id === id
          ? {
            ...transaction,
            incomeType: this.normalizeIncomeType(result.finalType),
            incomeConfidence: result.finalConfidence,
            incomeReason: result.reason,
            incomeExplanation: result.explanation,
            isIncomeClassified: true
          }
          : transaction
      )
    );

    this.allLoadedTransactions.update((transactions) =>
      transactions.map((transaction) =>
        transaction.id === id
          ? {
            ...transaction,
            incomeType: this.normalizeIncomeType(result.finalType),
            incomeConfidence: result.finalConfidence,
            incomeReason: result.reason,
            incomeExplanation: result.explanation,
            isIncomeClassified: true
          }
          : transaction
      )
    );
  }

  private normalizeIncomeType(value: string | null | undefined): string {
    return `${value ?? ''}`.trim().toLowerCase().replace(/[\s-]+/g, '_') || 'unknown';
  }
}
