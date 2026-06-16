import { CommonModule } from '@angular/common';
import { Component, EventEmitter, Input, Output, inject } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import {
  GenerateTestCardRequest,
  GenerateTestCardResponse,
  SandboxCardProfile
} from '../../../../core/models';

interface ProfileOption {
  value: SandboxCardProfile;
  label: string;
  description: string;
}

@Component({
  selector: 'app-add-test-card',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule],
  templateUrl: './add-test-card.component.html',
  styleUrl: './add-test-card.component.css'
})
export class AddTestCardComponent {
  private readonly fb = inject(FormBuilder);

  @Input() loading = false;
  @Input() generatedResult: GenerateTestCardResponse | null = null;
  @Input() connectingToAccount = false;
  @Input() connectError: string | null = null;

  @Output() readonly closed = new EventEmitter<void>();
  @Output() readonly submitted = new EventEmitter<GenerateTestCardRequest>();
  @Output() readonly resetRequested = new EventEmitter<void>();
  @Output() readonly connectRequested = new EventEmitter<number>();

  readonly profileOptions: ProfileOption[] = [
    {
      value: 'STUDENT',
      label: 'Etudiant',
      description: 'Flux plus legers, budget serre et paiements du quotidien.'
    },
    {
      value: 'SALARIED',
      label: 'Salarie',
      description: 'Carte equilibree pour revenus reguliers et habitudes mensuelles classiques.'
    },
    {
      value: 'FAMILY',
      label: 'Famille',
      description: 'Plus de depenses recurrentes, loisirs et achats partages.'
    },
    {
      value: 'PREMIUM',
      label: 'Premium',
      description: 'Volume plus eleve, depenses diversifiees et rythme haut de gamme.'
    }
  ];

  readonly form = this.fb.group({
    holderName: ['', [Validators.required, Validators.minLength(3)]],
    profile: ['SALARIED' as SandboxCardProfile, [Validators.required]],
    transactionCount: [30, [Validators.required, Validators.min(5), Validators.max(200)]],
    connectToCurrentUser: [true]
  });

  close(): void {
    if (!this.loading && !this.connectingToAccount) {
      this.closed.emit();
    }
  }

  closeFromBackdrop(event: MouseEvent): void {
    if (event.target === event.currentTarget) {
      this.close();
    }
  }

  submit(): void {
    if (this.loading || this.connectingToAccount) {
      return;
    }

    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }

    const value = this.form.getRawValue();

    this.submitted.emit({
      holderName: value.holderName!.trim(),
      profile: value.profile as SandboxCardProfile,
      transactionCount: Number(value.transactionCount),
      connectToCurrentUser: Boolean(value.connectToCurrentUser)
    });
  }

  resetPreview(): void {
    if (!this.loading && !this.connectingToAccount) {
      this.resetRequested.emit();
    }
  }

  requestConnection(result: GenerateTestCardResponse): void {
    if (!this.loading && !this.connectingToAccount && this.canConnectGeneratedCard(result)) {
      this.connectRequested.emit(result.generatedCard.id);
    }
  }

  getError(field: keyof typeof this.form.controls): string | null {
    const control = this.form.controls[field];

    if (!control.touched || !control.errors) {
      return null;
    }

    if (control.errors['required']) {
      return 'Ce champ est requis.';
    }

    if (control.errors['minlength']) {
      return 'Le nom doit contenir au moins 3 caracteres.';
    }

    if (control.errors['min'] || control.errors['max']) {
      if (field === 'transactionCount') {
        return 'Choisissez entre 5 et 200 transactions a generer.';
      }
    }

    return 'Valeur invalide.';
  }

  getProfileLabel(profile: SandboxCardProfile): string {
    return this.profileOptions.find((option) => option.value === profile)?.label ?? profile;
  }

  formatExpiry(month: number, year: number): string {
    return `${String(month).padStart(2, '0')} / ${year}`;
  }

  canConnectGeneratedCard(result: GenerateTestCardResponse): boolean {
    return !result.connectToCurrentUser && !result.card && result.generatedCard.id > 0;
  }
}
