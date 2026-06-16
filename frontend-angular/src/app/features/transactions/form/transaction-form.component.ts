import { CommonModule } from '@angular/common';
import { Component, DestroyRef, OnInit, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { debounceTime, distinctUntilChanged, merge } from 'rxjs';
import { CategorizationService } from '../../../core/services/categorization.service';
import { CardService } from '../../../core/services/card.service';
import { TransactionService } from '../../../core/services/api.services';
import { NotificationService } from '../../../core/services/notification.service';
import {
  CategorizationRequest,
  CategorizationResult,
  PaymentMethod,
  TransactionCategory,
  TransactionRequest,
  TransactionType,
  UserCardDto,
  getTransactionCategoryLabel
} from '../../../core/models';
import {
  formatConfidence,
  getCategorizationSourceMeta,
  getTransactionCategoryBackground,
  getTransactionCategoryColor,
  getTransactionCategoryIcon,
  PAYMENT_METHOD_OPTIONS,
  resolveTransactionCategory,
  TRANSACTION_CATEGORIES,
  TRANSACTION_TYPE_OPTIONS
} from '../transaction-ui';

@Component({
  selector: 'app-transaction-form',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, RouterLink],
  templateUrl: './transaction-form.component.html',
  styleUrl: './transaction-form.component.css'
})
export class TransactionFormComponent implements OnInit {
  private fb = inject(FormBuilder);
  private route = inject(ActivatedRoute);
  private router = inject(Router);
  private txService = inject(TransactionService);
  private cardService = inject(CardService);
  private categorizationService = inject(CategorizationService);
  private notifService = inject(NotificationService);
  private destroyRef = inject(DestroyRef);

  readonly categories = TRANSACTION_CATEGORIES;
  readonly typeOptions = TRANSACTION_TYPE_OPTIONS;
  readonly paymentMethodOptions = PAYMENT_METHOD_OPTIONS;

  readonly isEdit = signal(false);
  readonly editId = signal<number | null>(null);
  readonly isSaving = signal(false);
  readonly isPredicting = signal(false);
  readonly predictionError = signal<string | null>(null);
  readonly categorizationResult = signal<CategorizationResult | null>(null);
  readonly userCards = signal<UserCardDto[]>([]);
  readonly isLoadingUserCards = signal(false);
  readonly userCardsError = signal<string | null>(null);

  private lastAnalyzedSignature: string | null = null;
  private lastSuggestedCategory: string | null = null;

  readonly form = this.fb.nonNullable.group({
    merchantName: [''],
    description: ['', [Validators.required, Validators.maxLength(255)]],
    amount: [null as number | null, [Validators.required, Validators.min(0.01)]],
    date: [this.today(), Validators.required],
    type: ['DEPENSE' as TransactionType, Validators.required],
    category: ['', Validators.required],
    paymentMethod: ['CARD' as PaymentMethod, Validators.required],
    userCardId: [null as number | null]
  });

  ngOnInit(): void {
    this.initializeEditMode();
    this.setupPaymentMethodBehavior();
    this.setupAutomaticAnalysis();
    this.loadUserCards();
  }

  pageTitle(): string {
    return this.isEdit() ? 'Modifier la transaction' : 'Nouvelle transaction';
  }

  pageSubtitle(): string {
    if (this.isEdit()) {
      return 'Mettez a jour la transaction, reutilisez la suggestion IA existante et corrigez librement la categorie si besoin.';
    }

    return 'Ajoutez une transaction manuellement, laissez l IA proposer une categorie, puis corrigez librement avant sauvegarde.';
  }

  canAnalyze(): boolean {
    const request = this.buildCategorizationPayload();
    return request.merchantName.length > 1 || request.description.length > 1;
  }

  analyzeCategory(force = true): void {
    const request = this.buildCategorizationPayload();
    const signature = `${request.merchantName}::${request.description}`;

    if (!request.merchantName && !request.description) {
      this.predictionError.set('Saisissez au moins un commercant ou une description pour lancer la prediction.');
      return;
    }

    if (!force && signature === this.lastAnalyzedSignature) return;

    this.isPredicting.set(true);
    this.predictionError.set(null);

    this.categorizationService.predict(request).subscribe({
      next: (result) => {
        this.isPredicting.set(false);
        this.categorizationResult.set(result);
        this.lastAnalyzedSignature = signature;
        this.applyPredictedCategory(result.category);
      },
      error: (error) => {
        this.isPredicting.set(false);
        const message = error?.error?.message || 'Le backend de categorisation ne repond pas pour le moment.';
        this.predictionError.set(message);
        if (force) {
          this.notifService.error(message);
        }
      }
    });
  }

  submitManualTransaction(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      this.notifService.error('Veuillez corriger les champs obligatoires avant de continuer.');
      return;
    }

    this.isSaving.set(true);
    const raw = this.form.getRawValue();
    const prediction = this.categorizationResult();

    const payload: TransactionRequest = {
      description: raw.description.trim(),
      amount: raw.amount!,
      date: raw.date,
      category: raw.category as TransactionCategory,
      predictedCategory: prediction ? resolveTransactionCategory(prediction.category) : undefined,
      type: raw.type,
      merchantName: raw.merchantName.trim() || undefined,
      paymentMethod: raw.paymentMethod,
      userCardId: raw.paymentMethod === 'CARD' ? raw.userCardId ?? undefined : undefined,
      source: 'MANUAL_ENTRY',
      categorizationSource: prediction?.source,
      categorizationConfidence: prediction?.confidence ?? null,
      normalizedText: prediction?.normalizedText || undefined
    };

    const request$ = this.isEdit() && this.editId()
      ? this.txService.update(this.editId()!, payload)
      : this.txService.create(payload);

    request$.subscribe({
      next: () => {
        this.notifService.success(this.isEdit() ? 'Transaction mise a jour.' : 'Transaction enregistree avec succes.');
        void this.router.navigate(['/transactions']);
      },
      error: (error) => {
        this.isSaving.set(false);
        const message = error?.error?.message || 'Erreur lors de la sauvegarde de la transaction.';
        this.notifService.error(message);
      }
    });
  }

  getError(field: keyof typeof this.form.controls): string | null {
    const control = this.form.controls[field];
    if (!control.touched || !control.errors) return null;
    if (control.errors['required']) return 'Ce champ est requis.';
    if (control.errors['min']) return 'La valeur doit etre strictement positive.';
    if (control.errors['maxlength']) return 'Le texte est trop long.';
    return 'Valeur invalide.';
  }

  predictionSourceMeta() {
    return getCategorizationSourceMeta(this.categorizationResult()?.source);
  }

  resolveCategory(category: string): TransactionCategory {
    return resolveTransactionCategory(category);
  }

  getCategoryIcon(category: TransactionCategory): string {
    return getTransactionCategoryIcon(category);
  }

  getCategoryLabel(category: TransactionCategory | string): string {
    return getTransactionCategoryLabel(category);
  }

  getCategoryColor(category: TransactionCategory): string {
    return getTransactionCategoryColor(category);
  }

  getCategoryBackground(category: TransactionCategory): string {
    return getTransactionCategoryBackground(category);
  }

  formatConfidenceValue(confidence?: number | null): string {
    return formatConfidence(confidence);
  }

  requiresCardSelection(): boolean {
    return this.form.controls.paymentMethod.value === 'CARD';
  }

  hasConnectedCards(): boolean {
    return this.userCards().length > 0;
  }

  cardOptionLabel(card: UserCardDto): string {
    return `${card.maskedCardNumber} · ${card.bankName}`;
  }

  private initializeEditMode(): void {
    const routeId = this.route.snapshot.paramMap.get('id');
    if (!routeId) return;

    this.isEdit.set(true);
    this.editId.set(Number(routeId));

    this.txService.getById(Number(routeId)).subscribe({
      next: (transaction) => {
        this.form.patchValue({
          merchantName: transaction.merchantName ?? '',
          description: transaction.description,
          amount: transaction.amount,
          date: this.toDateInput(transaction.date),
          type: transaction.type,
          category: transaction.category,
          paymentMethod: transaction.paymentMethod,
          userCardId: transaction.userCardId ?? null
        }, { emitEvent: false });

        if (transaction.categorizationSource || transaction.categorizationConfidence !== undefined || transaction.normalizedText) {
          this.categorizationResult.set({
            category: transaction.category,
            confidence: transaction.categorizationConfidence ?? 0,
            source: transaction.categorizationSource ?? 'FALLBACK',
            normalizedText: transaction.normalizedText ?? ''
          });
          this.lastSuggestedCategory = transaction.category;
        }

        this.syncCardSelectionRequirement(this.form.controls.paymentMethod.value);
      },
      error: (error) => {
        const message = error?.error?.message || 'Impossible de charger la transaction a modifier.';
        this.notifService.error(message);
        void this.router.navigate(['/transactions']);
      }
    });
  }

  private setupAutomaticAnalysis(): void {
    merge(
      this.form.controls.merchantName.valueChanges.pipe(distinctUntilChanged()),
      this.form.controls.description.valueChanges.pipe(distinctUntilChanged())
    )
      .pipe(debounceTime(650), takeUntilDestroyed(this.destroyRef))
      .subscribe(() => {
        const payload = this.buildCategorizationPayload();
        if (!payload.merchantName && !payload.description) {
          this.categorizationResult.set(null);
          this.predictionError.set(null);
          this.lastAnalyzedSignature = null;
          return;
        }

        this.analyzeCategory(false);
      });
  }

  private setupPaymentMethodBehavior(): void {
    this.syncCardSelectionRequirement(this.form.controls.paymentMethod.value);

    this.form.controls.paymentMethod.valueChanges
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe((method) => this.syncCardSelectionRequirement(method));
  }

  private loadUserCards(): void {
    this.isLoadingUserCards.set(true);
    this.userCardsError.set(null);

    this.cardService.getMyCards().subscribe({
      next: (cards) => {
        const sortedCards = [...cards].sort((left, right) => {
          if (left.active !== right.active) {
            return left.active ? -1 : 1;
          }

          return new Date(right.lastSyncAt).getTime() - new Date(left.lastSyncAt).getTime();
        });

        this.userCards.set(sortedCards);
        this.isLoadingUserCards.set(false);

        const selectedCardId = this.form.controls.userCardId.value;
        if (selectedCardId && !sortedCards.some((card) => card.id === selectedCardId)) {
          this.form.controls.userCardId.setValue(null, { emitEvent: false });
        }
      },
      error: (error) => {
        this.isLoadingUserCards.set(false);
        const message =
          error?.error?.message ||
          'Impossible de charger vos cartes connectees pour le moment.';
        this.userCardsError.set(message);
      }
    });
  }

  private buildCategorizationPayload(): CategorizationRequest {
    return {
      merchantName: this.form.controls.merchantName.value.trim(),
      description: this.form.controls.description.value.trim()
    };
  }

  private syncCardSelectionRequirement(method: PaymentMethod): void {
    const cardControl = this.form.controls.userCardId;

    if (method === 'CARD') {
      cardControl.addValidators(Validators.required);
    } else {
      cardControl.setValue(null, { emitEvent: false });
      cardControl.clearValidators();
    }

    cardControl.updateValueAndValidity({ emitEvent: false });
  }

  private applyPredictedCategory(category: string): void {
    const currentCategory = this.form.controls.category.value;
    if (!currentCategory || currentCategory === this.lastSuggestedCategory) {
      this.form.controls.category.setValue(resolveTransactionCategory(category));
    }
    this.lastSuggestedCategory = category;
  }

  private today(): string {
    return new Date().toISOString().split('T')[0];
  }

  private toDateInput(value: string): string {
    return value.includes('T') ? value.split('T')[0] : value;
  }
}
