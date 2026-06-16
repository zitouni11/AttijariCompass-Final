import { CommonModule } from '@angular/common';
import { Component, EventEmitter, Input, OnChanges, Output, SimpleChanges, inject } from '@angular/core';
import { AbstractControl, FormBuilder, ReactiveFormsModule, ValidationErrors, ValidatorFn, Validators } from '@angular/forms';
import { CardCatalogItem, LinkCardRequest } from '../cards.models';

const exactCardNumberLengthValidator: ValidatorFn = (control: AbstractControl): ValidationErrors | null => {
  const digits = `${control.value ?? ''}`.replace(/\D+/g, '');

  if (!digits.length) {
    return null;
  }

  return digits.length === 16 ? null : { cardLength: true };
};

@Component({
  selector: 'app-add-card-dialog',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule],
  templateUrl: './add-card-dialog.component.html',
  styleUrl: './add-card-dialog.component.scss'
})
export class AddCardDialogComponent implements OnChanges {
  private readonly fb = inject(FormBuilder);

  @Input() open = false;
  @Input() catalog: CardCatalogItem[] = [];
  @Input() submitting = false;
  @Input() errorMessage: string | null = null;

  @Output() readonly closed = new EventEmitter<void>();
  @Output() readonly submitted = new EventEmitter<LinkCardRequest>();

  readonly currentYear = new Date().getFullYear();

  readonly form = this.fb.group({
    cardCatalogId: [null as number | null, [Validators.required]],
    cardNumber: ['', [Validators.required, Validators.pattern(/^[\d\s]+$/), exactCardNumberLengthValidator]],
    expiryMonth: [null as number | null, [Validators.required, Validators.min(1), Validators.max(12)]],
    expiryYear: [this.currentYear, [Validators.required, Validators.min(this.currentYear), Validators.max(this.currentYear + 20)]]
  });

  ngOnChanges(changes: SimpleChanges): void {
    if (changes['open']?.currentValue === true && !changes['open']?.previousValue) {
      this.form.reset({
        cardCatalogId: this.catalog[0]?.id ?? null,
        cardNumber: '',
        expiryMonth: null,
        expiryYear: this.currentYear
      });
    }

    if (this.open && changes['catalog'] && !this.form.controls.cardCatalogId.value && this.catalog.length) {
      this.form.controls.cardCatalogId.setValue(this.catalog[0].id);
    }
  }

  close(): void {
    if (!this.submitting) {
      this.closed.emit();
    }
  }

  submit(): void {
    if (this.submitting) {
      return;
    }

    this.form.markAllAsTouched();

    if (this.form.invalid) {
      return;
    }

    const raw = this.form.getRawValue();
    this.submitted.emit({
      cardCatalogId: Number(raw.cardCatalogId),
      cardNumber: `${raw.cardNumber ?? ''}`.replace(/\D+/g, ''),
      expiryMonth: Number(raw.expiryMonth),
      expiryYear: Number(raw.expiryYear)
    });
  }

  sanitizeCardNumber(event: Event): void {
    const input = event.target as HTMLInputElement;
    const digits = input.value.replace(/\D+/g, '').slice(0, 16);
    const grouped = digits.replace(/(.{4})/g, '$1 ').trim();

    this.form.controls.cardNumber.setValue(grouped, { emitEvent: false });
  }

  hasError(fieldName: keyof typeof this.form.controls): boolean {
    const control = this.form.controls[fieldName];
    return control.invalid && (control.touched || control.dirty);
  }

  errorText(fieldName: keyof typeof this.form.controls): string | null {
    const control = this.form.controls[fieldName];

    if (!this.hasError(fieldName)) {
      return null;
    }

    if (control.hasError('required')) {
      switch (fieldName) {
        case 'cardCatalogId':
          return 'Selectionnez un type de carte.';
        case 'cardNumber':
          return 'Saisissez le numero de carte.';
        case 'expiryMonth':
          return 'Renseignez le mois d expiration.';
        case 'expiryYear':
          return 'Renseignez l annee d expiration.';
      }
    }

    if (fieldName === 'cardNumber' && control.hasError('pattern')) {
      return 'Le numero de carte doit contenir uniquement des chiffres.';
    }

    if (fieldName === 'cardNumber' && control.hasError('cardLength')) {
      return 'Le numero de carte doit contenir exactement 16 chiffres.';
    }

    if (fieldName === 'expiryMonth' && (control.hasError('min') || control.hasError('max'))) {
      return 'Le mois doit etre compris entre 1 et 12.';
    }

    if (fieldName === 'expiryYear' && (control.hasError('min') || control.hasError('max'))) {
      return 'L annee d expiration n est pas valide.';
    }

    return 'Verifiez la valeur saisie.';
  }

  get selectedCatalog(): CardCatalogItem | null {
    const selectedId = this.form.controls.cardCatalogId.value;
    return this.catalog.find((item) => item.id === selectedId) ?? null;
  }
}
