import { CommonModule } from '@angular/common';
import { Component, inject, OnInit, signal } from '@angular/core';
import { AbstractControl, FormBuilder, ReactiveFormsModule, ValidationErrors, Validators } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { GoalService } from '../../../core/services/api.services';
import { NotificationService } from '../../../core/services/notification.service';
import { normalizeGoalResponse } from '../goal-ui';

interface GoalTypeOption {
  value: string;
  label: string;
  description: string;
}

@Component({
  selector: 'app-goal-form',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, RouterLink],
  templateUrl: './goal-form.component.html',
  styleUrl: './goal-form.component.css'
})
export class GoalFormComponent implements OnInit {
  private readonly fb = inject(FormBuilder);
  private readonly goalService = inject(GoalService);
  private readonly router = inject(Router);
  private readonly route = inject(ActivatedRoute);
  private readonly notifService = inject(NotificationService);

  readonly isLoading = signal(false);
  readonly isEdit = signal(false);
  readonly editId = signal<number | null>(null);

  readonly goalTypeOptions: GoalTypeOption[] = [
    {
      value: '',
      label: 'Standard',
      description: 'Un objectif polyvalent pour tout projet financier personnel.'
    },
    {
      value: 'EPARGNE',
      label: 'Epargne',
      description: 'Ideal pour construire une reserve ou un capital progressif.'
    },
    {
      value: 'URGENCE',
      label: 'Fonds d urgence',
      description: 'Constituez un filet de securite pour les imprevus du quotidien.'
    },
    {
      value: 'VOYAGE',
      label: 'Voyage',
      description: 'Planifiez une escapade ou une experience importante avec un cap clair.'
    },
    {
      value: 'INVESTISSEMENT',
      label: 'Investissement',
      description: 'Donnez une trajectoire visible a un projet de placement ou d actif.'
    },
    {
      value: 'RETRAITE',
      label: 'Retraite',
      description: 'Construisez un horizon de long terme avec discipline et visibilite.'
    }
  ];

  readonly form = this.fb.group({
    name: ['', [Validators.required, Validators.maxLength(80)]],
    description: ['', [Validators.maxLength(240)]],
    targetAmount: [null as number | null, [Validators.required, Validators.min(1)]],
    currentAmount: [0 as number | null, [Validators.required, Validators.min(0)]],
    type: [''],
    targetDate: ['', [Validators.required, GoalFormComponent.futureDateValidator]]
  }, {
    validators: [GoalFormComponent.goalAmountConsistencyValidator]
  });

  ngOnInit(): void {
    const id = Number(this.route.snapshot.paramMap.get('id'));

    if (!id) {
      return;
    }

    this.isEdit.set(true);
    this.editId.set(id);

    this.goalService.getById(id).subscribe({
      next: (goal) => {
        const normalizedGoal = normalizeGoalResponse(goal);
        this.form.patchValue({
          name: normalizedGoal.title,
          description: normalizedGoal.description || '',
          targetAmount: normalizedGoal.targetAmount,
          currentAmount: normalizedGoal.currentAmount,
          type: normalizedGoal.type || '',
          targetDate: normalizedGoal.targetDate.includes('T')
            ? normalizedGoal.targetDate.split('T')[0]
            : normalizedGoal.targetDate
        });
      },
      error: () => {
        this.notifService.error("Impossible de charger l'objectif a modifier.");
        void this.router.navigate(['/goals']);
      }
    });
  }

  pageTitle(): string {
    return this.isEdit() ? "Modifier l'objectif" : 'Creer un nouvel objectif';
  }

  pageSubtitle(): string {
    return 'Definissez un montant cible, une echeance et suivez votre progression.';
  }

  submitLabel(): string {
    if (this.isLoading()) {
      return this.isEdit() ? 'Mise a jour en cours...' : 'Creation en cours...';
    }

    return this.isEdit() ? "Mettre a jour l'objectif" : "Creer l'objectif";
  }

  selectedGoalTypeDescription(): string {
    const type = this.form.controls.type.value ?? '';
    return this.goalTypeOptions.find((option) => option.value === type)?.description
      || 'Un objectif polyvalent pour tout projet financier personnel.';
  }

  currentAmountHelper(): string | null {
    const targetAmount = Number(this.form.controls.targetAmount.value || 0);
    const currentAmount = Number(this.form.controls.currentAmount.value || 0);

    if (targetAmount > 0 && currentAmount === targetAmount) {
      return "Le montant actuel atteint deja la cible. L'objectif sera considere comme atteint.";
    }

    return null;
  }

  getError(controlName: keyof typeof this.form.controls): string | null {
    const control = this.form.controls[controlName];
    const formTouched = control.touched || control.dirty;

    if (!formTouched && !this.form.touched) {
      return null;
    }

    if (control.errors?.['required']) {
      switch (controlName) {
        case 'name':
          return "Le titre de l'objectif est obligatoire.";
        case 'targetAmount':
          return 'Le montant cible est obligatoire.';
        case 'currentAmount':
          return 'Le montant actuel est obligatoire.';
        case 'targetDate':
          return 'La date cible est obligatoire.';
        default:
          return 'Ce champ est obligatoire.';
      }
    }

    if (control.errors?.['min']) {
      if (controlName === 'targetAmount') {
        return 'Le montant cible doit etre superieur a 0.';
      }

      if (controlName === 'currentAmount') {
        return 'Le montant actuel doit etre superieur ou egal a 0.';
      }
    }

    if (control.errors?.['maxlength']) {
      return 'Le texte depasse la longueur autorisee.';
    }

    if (controlName === 'targetDate' && control.errors?.['pastDate']) {
      return 'La date cible doit etre dans le futur.';
    }

    if (controlName === 'currentAmount' && this.form.errors?.['currentExceedsTarget']) {
      return 'Le montant actuel ne peut pas depasser le montant cible.';
    }

    return null;
  }

  onSubmit(): void {
    this.form.markAllAsTouched();

    if (this.form.invalid) {
      return;
    }

    this.isLoading.set(true);

    const value = this.form.getRawValue();
    const payload = {
      name: value.name!.trim(),
      title: value.name!.trim(),
      description: value.description?.trim() || undefined,
      targetAmount: Number(value.targetAmount),
      currentAmount: Number(value.currentAmount || 0),
      targetDate: value.targetDate!,
      type: value.type || undefined
    };

    const request$ = this.isEdit() && this.editId()
      ? this.goalService.update(this.editId()!, payload)
      : this.goalService.create(payload);

    request$.subscribe({
      next: () => {
        this.notifService.success(this.isEdit() ? 'Objectif mis a jour.' : 'Objectif cree.');

        if (this.isEdit() && this.editId()) {
          void this.router.navigate(['/goals', this.editId()], { replaceUrl: true });
          return;
        }

        void this.router.navigate(['/goals']);
      },
      error: () => {
        this.isLoading.set(false);
        this.notifService.error('Erreur lors de la sauvegarde de l objectif.');
      }
    });
  }

  private static goalAmountConsistencyValidator(control: AbstractControl): ValidationErrors | null {
    const targetAmount = Number(control.get('targetAmount')?.value || 0);
    const currentAmount = Number(control.get('currentAmount')?.value || 0);

    if (targetAmount > 0 && currentAmount > targetAmount) {
      return { currentExceedsTarget: true };
    }

    return null;
  }

  private static futureDateValidator(control: AbstractControl): ValidationErrors | null {
    const rawValue = control.value;
    if (!rawValue) {
      return null;
    }

    const parsed = new Date(rawValue);
    if (Number.isNaN(parsed.getTime())) {
      return null;
    }

    const today = new Date();
    today.setHours(0, 0, 0, 0);
    parsed.setHours(0, 0, 0, 0);

    return parsed < today ? { pastDate: true } : null;
  }
}
