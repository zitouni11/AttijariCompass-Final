import { CommonModule } from '@angular/common';
import { HttpErrorResponse } from '@angular/common/http';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { Component, DestroyRef, EventEmitter, HostListener, Input, OnChanges, Output, SimpleChanges, inject, signal } from '@angular/core';
import { finalize } from 'rxjs';
import { BudgetTargetResponse } from '../../../core/models';
import { BudgetTargetService } from '../../../core/services/budget-target.service';
import { NotificationService } from '../../../core/services/notification.service';
import {
  BudgetRecommendationActionData,
  BudgetRecommendationFrameKey,
  BudgetRecommendationFrameOption,
  buildBudgetTargetCreateRequest
} from '../recommendation-ui';

@Component({
  selector: 'app-recommendation-budget-action-modal',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './recommendation-budget-action-modal.component.html',
  styleUrl: './recommendation-budget-action-modal.component.scss'
})
export class RecommendationBudgetActionModalComponent implements OnChanges {
  private readonly budgetTargetService = inject(BudgetTargetService);
  private readonly notificationService = inject(NotificationService);
  private readonly destroyRef = inject(DestroyRef);

  @Input({ required: true }) data!: BudgetRecommendationActionData;

  @Output() readonly closed = new EventEmitter<void>();
  @Output() readonly budgetSaved = new EventEmitter<BudgetTargetResponse>();

  readonly selectedFrameKey = signal<BudgetRecommendationFrameKey>('equilibre');
  readonly submitting = signal(false);
  readonly errorMessage = signal<string | null>(null);

  ngOnChanges(changes: SimpleChanges): void {
    if (changes['data']?.currentValue) {
      const recommendedKey = this.data.frames.find((frame) => frame.recommended)?.key ?? 'equilibre';
      this.selectedFrameKey.set(recommendedKey);
      this.errorMessage.set(null);
      this.submitting.set(false);
    }
  }

  @HostListener('document:keydown.escape')
  onEscape(): void {
    this.close();
  }

  close(): void {
    if (!this.submitting()) {
      this.closed.emit();
    }
  }

  closeFromBackdrop(event: MouseEvent): void {
    if (event.target === event.currentTarget) {
      this.close();
    }
  }

  selectFrame(key: BudgetRecommendationFrameKey): void {
    if (!this.submitting()) {
      this.selectedFrameKey.set(key);
    }
  }

  isSelectedFrame(key: BudgetRecommendationFrameKey): boolean {
    return this.selectedFrameKey() === key;
  }

  apply(): void {
    const selected = this.selectedFrame;

    if (!selected || this.submitting()) {
      return;
    }

    this.submitting.set(true);
    this.errorMessage.set(null);

    this.budgetTargetService.createBudgetTarget(buildBudgetTargetCreateRequest(this.data, selected))
      .pipe(
        takeUntilDestroyed(this.destroyRef),
        finalize(() => this.submitting.set(false))
      )
      .subscribe({
        next: (response) => {
          this.notificationService.success('Budget enregistre avec succes.');
          this.budgetSaved.emit(response);
        },
        error: (error: unknown) => {
          const message = this.resolveSaveErrorMessage(error);
          this.errorMessage.set(message);

          if (!this.isHandledByAuthInterceptor(error)) {
            const notifier = error instanceof HttpErrorResponse && error.status === 409
              ? this.notificationService.warning.bind(this.notificationService)
              : this.notificationService.error.bind(this.notificationService);
            notifier(message);
          }
        }
      });
  }

  get selectedFrame(): BudgetRecommendationFrameOption | null {
    return this.data.frames.find((frame) => frame.key === this.selectedFrameKey())
      ?? this.data.frames.find((frame) => frame.recommended)
      ?? this.data.frames[0]
      ?? null;
  }

  private resolveSaveErrorMessage(error: unknown): string {
    if (!(error instanceof HttpErrorResponse)) {
      return 'Une erreur est survenue lors de l enregistrement du budget.';
    }

    if (error.status === 400) {
      return 'Impossible d enregistrer ce budget. Verifiez les informations proposees.';
    }

    if (error.status === 409) {
      return 'Un budget cible existe deja pour cette categorie. Vous pouvez le mettre a jour ensuite.';
    }

    if (error.status === 401 || error.status === 403) {
      return 'Votre session doit etre revalidee.';
    }

    return 'Une erreur est survenue lors de l enregistrement du budget.';
  }

  private isHandledByAuthInterceptor(error: unknown): boolean {
    return error instanceof HttpErrorResponse && (error.status === 401 || error.status === 403);
  }
}
