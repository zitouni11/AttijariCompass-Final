import { CommonModule } from '@angular/common';
import { HttpErrorResponse } from '@angular/common/http';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import {
  Component,
  DestroyRef,
  EventEmitter,
  HostBinding,
  HostListener,
  Input,
  OnChanges,
  Output,
  SimpleChanges,
  computed,
  inject,
  signal
} from '@angular/core';
import { catchError, concatMap, finalize, from, map, of, switchMap, toArray } from 'rxjs';
import {
  BudgetTargetCategory,
  BudgetTargetCreateRequest,
  BudgetTargetLevel,
  BudgetTargetResponse,
  TRANSACTION_CATEGORIES,
  getTransactionCategoryLabel,
  getTransactionCategoryMaterialIcon
} from '../../../core/models';
import { BudgetTargetService } from '../../../core/services/budget-target.service';
import { NotificationService } from '../../../core/services/notification.service';
import {
  BudgetRecommendationActionData,
  BudgetRecommendationFrameKey,
  BudgetRecommendationFrameOption,
  buildBudgetTargetCreateRequest
} from '../../recommendations/recommendation-ui';

type BudgetDefinitionMode = 'manual' | 'guided';
type BudgetOperationKind = 'create' | 'replace' | 'disable';

interface BudgetManualRow {
  category: BudgetTargetCategory;
  categoryLabel: string;
  categoryIcon: string;
  amountInput: string;
  selectedLevel: BudgetTargetLevel;
  existingBudget: BudgetTargetResponse | null;
  isGuidedSuggestion: boolean;
}

interface BudgetOperation {
  kind: BudgetOperationKind;
  category: BudgetTargetCategory;
  categoryLabel: string;
  existingBudget: BudgetTargetResponse | null;
  createPayload: BudgetTargetCreateRequest | null;
}

interface BudgetOperationResult {
  ok: boolean;
  kind: BudgetOperationKind;
  categoryLabel: string;
  budget: BudgetTargetResponse | null;
  message: string;
}

@Component({
  selector: 'app-budget-definition-modal',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './budget-definition-modal.component.html',
  styleUrl: './budget-definition-modal.component.scss'
})
export class BudgetDefinitionModalComponent implements OnChanges {
  private readonly budgetTargetService = inject(BudgetTargetService);
  private readonly notificationService = inject(NotificationService);
  private readonly destroyRef = inject(DestroyRef);
  private readonly amountFormatter = new Intl.NumberFormat('fr-FR', {
    minimumFractionDigits: 0,
    maximumFractionDigits: 0
  });

  @Input() budgets: BudgetTargetResponse[] = [];
  @Input() guidedData: BudgetRecommendationActionData | null = null;
  @Input() embedded = false;

  @Output() readonly closed = new EventEmitter<void>();
  @Output() readonly budgetsMutated = new EventEmitter<void>();

  @HostBinding('class.budget-definition-embedded-host')
  get embeddedHost(): boolean {
    return this.embedded;
  }

  readonly mode = signal<BudgetDefinitionMode>('manual');
  readonly manualRows = signal<BudgetManualRow[]>([]);
  readonly selectedBudgetCategory = signal<BudgetTargetCategory | null>(null);
  readonly selectedFrameKey = signal<BudgetRecommendationFrameKey>('equilibre');
  readonly submitting = signal(false);
  readonly errorMessages = signal<string[]>([]);
  readonly summaryMessage = signal<string | null>(null);
  readonly progressLabel = signal<string | null>(null);

  readonly activeBudgetCount = computed(() =>
    this.manualRows().filter((row) => row.existingBudget?.status === 'ACTIVE').length
  );
  readonly filledManualCategoryCount = computed(() =>
    this.manualRows().filter((row) => this.parseAmountInput(row.amountInput) !== null).length
  );
  readonly changedManualCategoryCount = computed(() =>
    this.manualRows().filter((row) => this.computeManualOperation(row) !== null).length
  );
  readonly manualTotalLabel = computed(() => {
    const total = this.manualRows()
      .map((row) => this.parseAmountInput(row.amountInput) ?? 0)
      .reduce((sum, value) => sum + value, 0);

    return total > 0 ? `${this.amountFormatter.format(total)} DT / mois` : 'Aucun budget saisi';
  });
  readonly selectedBudgetRow = computed(() => {
    const selectedCategory = this.selectedBudgetCategory();
    return selectedCategory
      ? this.manualRows().find((row) => row.category === selectedCategory) ?? null
      : null;
  });
  readonly hasGuidedMode = computed(() => this.guidedData !== null);
  readonly guidedSuggestionCategory = computed(() => this.guidedData?.category ?? 'Categorie suggeree');

  get selectedFrame(): BudgetRecommendationFrameOption | null {
    const data = this.guidedData;

    if (!data) {
      return null;
    }

    return data.frames.find((frame) => frame.key === this.selectedFrameKey())
      ?? data.frames.find((frame) => frame.recommended)
      ?? data.frames[0]
      ?? null;
  }

  ngOnChanges(changes: SimpleChanges): void {
    if (changes['budgets'] || changes['guidedData']) {
      this.manualRows.set(this.buildManualRows(this.budgets, this.guidedData));
      this.mode.set('manual');
      this.selectedBudgetCategory.set(null);
      const recommendedKey = this.guidedData?.frames.find((frame) => frame.recommended)?.key ?? 'equilibre';
      this.selectedFrameKey.set(recommendedKey);
      this.errorMessages.set([]);
      this.summaryMessage.set(null);
      this.progressLabel.set(null);
      this.submitting.set(false);
    }
  }

  @HostListener('document:keydown.escape')
  onEscape(): void {
    this.close();
  }

  close(): void {
    if (!this.embedded && !this.submitting()) {
      this.closed.emit();
    }
  }

  closeFromBackdrop(event: MouseEvent): void {
    if (!this.embedded && event.target === event.currentTarget) {
      this.close();
    }
  }

  setMode(mode: BudgetDefinitionMode): void {
    if (mode !== 'manual') {
      return;
    }

    this.mode.set(mode);
    this.errorMessages.set([]);
    this.summaryMessage.set(null);
  }

  isModeSelected(mode: BudgetDefinitionMode): boolean {
    return this.mode() === mode;
  }

  selectBudgetCategory(row: BudgetManualRow): void {
    this.selectedBudgetCategory.set(row.category);
  }

  isBudgetCategorySelected(row: BudgetManualRow): boolean {
    return this.selectedBudgetCategory() === row.category;
  }

  updateAmount(category: BudgetTargetCategory, value: string): void {
    this.manualRows.update((rows) =>
      rows.map((row) => row.category === category ? { ...row, amountInput: value } : row)
    );
  }

  updateLevel(category: BudgetTargetCategory, value: string): void {
    if (value !== 'PRUDENT' && value !== 'EQUILIBRE' && value !== 'RENFORCE') {
      return;
    }

    this.manualRows.update((rows) =>
      rows.map((row) => row.category === category ? { ...row, selectedLevel: value } : row)
    );
  }

  clearRow(category: BudgetTargetCategory): void {
    this.manualRows.update((rows) =>
      rows.map((row) => row.category === category ? { ...row, amountInput: '', selectedLevel: 'EQUILIBRE' } : row)
    );
  }

  restoreRow(category: BudgetTargetCategory): void {
    this.manualRows.update((rows) =>
      rows.map((row) => {
        if (row.category !== category) {
          return row;
        }

        const existing = row.existingBudget;
        const amount = this.resolvePrimaryTargetAmount(existing);

        return {
          ...row,
          amountInput: amount !== null ? String(amount) : '',
          selectedLevel: existing?.selectedLevel ?? 'EQUILIBRE'
        };
      })
    );
  }

  selectGuidedFrame(key: BudgetRecommendationFrameKey): void {
    if (!this.submitting()) {
      this.selectedFrameKey.set(key);
    }
  }

  isSelectedFrame(key: BudgetRecommendationFrameKey): boolean {
    return this.selectedFrameKey() === key;
  }

  saveManualDefinition(): void {
    const operations = this.manualRows()
      .map((row) => this.computeManualOperation(row))
      .filter((operation): operation is BudgetOperation => operation !== null);

    if (!operations.length) {
      this.summaryMessage.set('Aucun changement detecte sur vos budgets.');
      this.notificationService.info('Aucun changement detecte sur vos budgets.');
      return;
    }

    this.executeOperations(operations, 'mode manuel');
  }

  saveGuidedDefinition(): void {
    const data = this.guidedData;
    const frame = this.selectedFrame;

    if (!data || !frame) {
      return;
    }

    const existingBudget = this.findLatestActiveBudget(data.categoryCode);
    const payload = buildBudgetTargetCreateRequest(data, frame);
    const operation = this.computeTargetedOperation(existingBudget, payload, data.categoryCode, data.category);

    if (!operation) {
      this.summaryMessage.set('Le cadre guide correspond deja au budget actif de cette categorie.');
      this.notificationService.info('Le cadre guide correspond deja au budget actif de cette categorie.');
      return;
    }

    this.executeOperations([operation], 'mode guide');
  }

  rowCurrentAmountLabel(row: BudgetManualRow): string {
    const amount = this.resolvePrimaryTargetAmount(row.existingBudget);
    return amount !== null ? `${this.amountFormatter.format(amount)} DT / mois` : 'Aucun budget actif';
  }

  rowCurrentSourceLabel(row: BudgetManualRow): string {
    if (!row.existingBudget) {
      return 'Aucun cadre actif';
    }

    const origin = row.existingBudget.source === 'MANUAL' ? 'Manuel' : 'Guide / IA';
    return `${origin} - ${this.resolveLevelLabel(row.existingBudget.selectedLevel)}`;
  }

  rowStateLabel(row: BudgetManualRow): string {
    const operation = this.computeManualOperation(row);

    if (!operation) {
      return row.existingBudget ? 'Inchange' : 'Vide';
    }

    switch (operation.kind) {
      case 'create':
        return 'Nouveau';
      case 'replace':
        return 'Mise a jour';
      case 'disable':
        return 'Suppression';
      default:
        return 'Pret';
    }
  }

  rowStateClass(row: BudgetManualRow): string {
    const operation = this.computeManualOperation(row);

    if (!operation) {
      return row.existingBudget ? 'tone-neutral' : 'tone-muted';
    }

    switch (operation.kind) {
      case 'create':
        return 'tone-create';
      case 'replace':
        return 'tone-update';
      case 'disable':
        return 'tone-remove';
      default:
        return 'tone-neutral';
    }
  }

  private buildManualRows(
    budgets: readonly BudgetTargetResponse[],
    guidedData: BudgetRecommendationActionData | null
  ): BudgetManualRow[] {
    return TRANSACTION_CATEGORIES.map((category) => {
      const existingBudget = this.findLatestActiveBudget(category, budgets);
      const existingAmount = this.resolvePrimaryTargetAmount(existingBudget);

      return {
        category,
        categoryLabel: getTransactionCategoryLabel(category),
        categoryIcon: getTransactionCategoryMaterialIcon(category),
        amountInput: existingAmount !== null ? String(existingAmount) : '',
        selectedLevel: existingBudget?.selectedLevel ?? 'EQUILIBRE',
        existingBudget,
        isGuidedSuggestion: guidedData?.categoryCode === category
      };
    });
  }

  private findLatestActiveBudget(
    category: BudgetTargetCategory,
    budgets: readonly BudgetTargetResponse[] = this.budgets
  ): BudgetTargetResponse | null {
    const activeBudgets = budgets
      .filter((budget) => budget.category === category && budget.status === 'ACTIVE')
      .sort((left, right) => this.toTimestamp(right.createdAt) - this.toTimestamp(left.createdAt));

    return activeBudgets[0] ?? null;
  }

  private computeManualOperation(row: BudgetManualRow): BudgetOperation | null {
    const desiredAmount = this.parseAmountInput(row.amountInput);
    const payload = desiredAmount !== null
      ? this.buildManualCreatePayload(row.category, row.categoryLabel, desiredAmount, row.selectedLevel)
      : null;

    return this.computeTargetedOperation(row.existingBudget, payload, row.category, row.categoryLabel);
  }

  private computeTargetedOperation(
    existingBudget: BudgetTargetResponse | null,
    createPayload: BudgetTargetCreateRequest | null,
    category: BudgetTargetCategory,
    categoryLabel: string
  ): BudgetOperation | null {
    if (createPayload === null) {
      return existingBudget
        ? {
            kind: 'disable',
            category,
            categoryLabel,
            existingBudget,
            createPayload: null
          }
        : null;
    }

    if (!existingBudget) {
      return {
        kind: 'create',
        category,
        categoryLabel,
        existingBudget: null,
        createPayload
      };
    }

    if (this.isSameBudgetDefinition(existingBudget, createPayload)) {
      return null;
    }

    return {
      kind: 'replace',
      category,
      categoryLabel,
      existingBudget,
      createPayload
    };
  }

  private executeOperations(operations: BudgetOperation[], modeLabel: string): void {
    this.submitting.set(true);
    this.errorMessages.set([]);
    this.summaryMessage.set(null);

    from(operations)
      .pipe(
        concatMap((operation, index) => this.executeOperation(operation, index + 1, operations.length)),
        toArray(),
        takeUntilDestroyed(this.destroyRef),
        finalize(() => {
          this.submitting.set(false);
          this.progressLabel.set(null);
        })
      )
      .subscribe((results) => {
        const successes = results.filter((result) => result.ok);
        const errors = results.filter((result) => !result.ok);

        if (successes.length > 0) {
          this.budgetsMutated.emit();
        }

        if (!errors.length) {
          const savedCount = successes.filter((result) => result.kind !== 'disable').length;
          const disabledCount = successes.filter((result) => result.kind === 'disable').length;
          const fragments: string[] = [];

          if (savedCount > 0) {
            fragments.push(`${savedCount} budget(s) enregistre(s)`);
          }

          if (disabledCount > 0) {
            fragments.push(`${disabledCount} budget(s) desactive(s)`);
          }

          const summary = fragments.length
            ? `${fragments.join(' et ')} via le ${modeLabel}.`
            : `Mise a jour terminee via le ${modeLabel}.`;

          this.summaryMessage.set(summary);
          this.notificationService.success(summary);
          if (!this.embedded) {
            this.closed.emit();
          }
          return;
        }

        this.errorMessages.set(errors.map((result) => result.message));

        if (successes.length > 0) {
          const partialSummary = `${successes.length} operation(s) reussie(s), ${errors.length} en echec.`;
          this.summaryMessage.set(partialSummary);
          this.notificationService.warning(partialSummary);
        } else {
          const failureSummary = 'Impossible d enregistrer les budgets demandes.';
          this.summaryMessage.set(failureSummary);
          this.notificationService.error(failureSummary);
        }
      });
  }

  private executeOperation(
    operation: BudgetOperation,
    index: number,
    total: number
  ) {
    this.progressLabel.set(`Traitement ${index}/${total} - ${operation.categoryLabel}`);

    switch (operation.kind) {
      case 'create':
        return this.budgetTargetService.createBudgetTarget(operation.createPayload!).pipe(
          map((budget) => ({
            ok: true,
            kind: operation.kind,
            categoryLabel: operation.categoryLabel,
            budget,
            message: `${operation.categoryLabel} enregistre.`
          } satisfies BudgetOperationResult)),
          catchError((error: unknown) => of(this.toOperationError(operation, error)))
        );
      case 'replace':
        return this.budgetTargetService.updateBudgetTargetStatus(operation.existingBudget!.id, { status: 'INACTIVE' }).pipe(
          switchMap(() => this.budgetTargetService.createBudgetTarget(operation.createPayload!)),
          map((budget) => ({
            ok: true,
            kind: operation.kind,
            categoryLabel: operation.categoryLabel,
            budget,
            message: `${operation.categoryLabel} mis a jour.`
          } satisfies BudgetOperationResult)),
          catchError((error: unknown) => of(this.toOperationError(operation, error)))
        );
      case 'disable':
        return this.budgetTargetService.updateBudgetTargetStatus(operation.existingBudget!.id, { status: 'INACTIVE' }).pipe(
          map((budget) => ({
            ok: true,
            kind: operation.kind,
            categoryLabel: operation.categoryLabel,
            budget,
            message: `${operation.categoryLabel} desactive.`
          } satisfies BudgetOperationResult)),
          catchError((error: unknown) => of(this.toOperationError(operation, error)))
        );
      default:
        return of({
          ok: false,
          kind: operation.kind,
          categoryLabel: operation.categoryLabel,
          budget: null,
          message: `Operation non prise en charge pour ${operation.categoryLabel}.`
        } satisfies BudgetOperationResult);
    }
  }

  private toOperationError(operation: BudgetOperation, error: unknown): BudgetOperationResult {
    return {
      ok: false,
      kind: operation.kind,
      categoryLabel: operation.categoryLabel,
      budget: null,
      message: `${operation.categoryLabel} : ${this.resolveSaveErrorMessage(error, operation.kind)}`
    };
  }

  private resolveSaveErrorMessage(error: unknown, kind: BudgetOperationKind): string {
    if (!(error instanceof HttpErrorResponse)) {
      return 'erreur inattendue pendant la sauvegarde.';
    }

    if (error.status === 400) {
      return kind === 'disable'
        ? 'la desactivation a ete refusee par le backend.'
        : 'les informations transmises sont refusees par le backend.';
    }

    if (error.status === 409) {
      return 'un budget actif existe deja pour cette categorie.';
    }

    if (error.status === 401 || error.status === 403) {
      return 'votre session doit etre revalidee.';
    }

    return 'la requete n a pas pu aboutir.';
  }

  private buildManualCreatePayload(
    category: BudgetTargetCategory,
    categoryLabel: string,
    amount: number,
    selectedLevel: BudgetTargetLevel
  ): BudgetTargetCreateRequest {
    return {
      category,
      categoryLabel,
      selectedLevel,
      suggestedMonthlyAmount: amount,
      source: 'MANUAL',
      recommendationId: `manual-${category.toLowerCase()}`,
      recommendationTitle: `Definition manuelle ${categoryLabel}`,
      summary: 'Cadre budgetaire defini manuellement depuis la page Budgets.'
    };
  }

  private isSameBudgetDefinition(
    existingBudget: BudgetTargetResponse,
    payload: BudgetTargetCreateRequest
  ): boolean {
    const currentAmount = this.resolvePrimaryTargetAmount(existingBudget);

    return currentAmount !== null
      && Math.round(currentAmount) === Math.round(payload.suggestedMonthlyAmount)
      && existingBudget.selectedLevel === payload.selectedLevel
      && existingBudget.source === payload.source
      && existingBudget.status === 'ACTIVE';
  }

  private resolvePrimaryTargetAmount(budget: BudgetTargetResponse | null): number | null {
    if (!budget) {
      return null;
    }

    if (budget.targetAmount !== null && Number.isFinite(budget.targetAmount)) {
      return budget.targetAmount;
    }

    if (Number.isFinite(budget.suggestedMonthlyAmount) && budget.suggestedMonthlyAmount > 0) {
      return budget.suggestedMonthlyAmount;
    }

    return null;
  }

  private parseAmountInput(value: string): number | null {
    const normalized = value.trim().replace(',', '.');

    if (!normalized) {
      return null;
    }

    const parsed = Number(normalized);

    if (!Number.isFinite(parsed) || parsed <= 0) {
      return null;
    }

    return Math.round(parsed);
  }

  private resolveLevelLabel(level: BudgetTargetLevel): string {
    switch (level) {
      case 'PRUDENT':
        return 'Prudent';
      case 'RENFORCE':
        return 'Renforce';
      case 'EQUILIBRE':
      default:
        return 'Equilibre';
    }
  }

  private toTimestamp(value: string): number {
    const timestamp = new Date(value).getTime();
    return Number.isFinite(timestamp) ? timestamp : 0;
  }
}
