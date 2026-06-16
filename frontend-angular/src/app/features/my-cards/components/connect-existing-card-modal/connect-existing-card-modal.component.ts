import { CommonModule } from '@angular/common';
import { Component, EventEmitter, Input, Output, inject } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';

export interface PendingSandboxCardOption {
  testCardId: number;
  holderName: string;
  maskedCardNumber: string;
  cardType: string;
  bankName: string;
  profile: string | null;
  transactionCount: number | null;
  createdAt: string;
}

@Component({
  selector: 'app-connect-existing-card-modal',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule],
  templateUrl: './connect-existing-card-modal.component.html',
  styleUrl: './connect-existing-card-modal.component.css'
})
export class ConnectExistingCardModalComponent {
  private readonly fb = inject(FormBuilder);

  @Input() cards: PendingSandboxCardOption[] = [];
  @Input() loading = false;
  @Input() error: string | null = null;

  @Output() readonly closed = new EventEmitter<void>();
  @Output() readonly connectRequested = new EventEmitter<number>();

  readonly form = this.fb.group({
    testCardId: [null as number | null, [Validators.required, Validators.min(1)]]
  });

  close(): void {
    if (!this.loading) {
      this.closed.emit();
    }
  }

  closeFromBackdrop(event: MouseEvent): void {
    if (event.target === event.currentTarget) {
      this.close();
    }
  }

  connectCard(testCardId: number): void {
    if (!this.loading && testCardId > 0) {
      this.connectRequested.emit(testCardId);
    }
  }

  submit(): void {
    if (this.loading) {
      return;
    }

    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }

    const value = Number(this.form.getRawValue().testCardId);
    this.connectCard(value);
  }

  getError(): string | null {
    const control = this.form.controls.testCardId;

    if (!control.touched || !control.errors) {
      return null;
    }

    if (control.errors['required']) {
      return 'Renseignez l identifiant de la carte sandbox.';
    }

    if (control.errors['min']) {
      return 'Utilisez un identifiant de carte valide.';
    }

    return 'Valeur invalide.';
  }

  formatDate(value: string): string {
    return new Intl.DateTimeFormat('fr-FR', {
      day: '2-digit',
      month: 'short',
      year: 'numeric'
    }).format(new Date(value));
  }
}
