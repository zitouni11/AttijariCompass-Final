import { CommonModule } from '@angular/common';
import { Component, OnInit, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { AdminRegistrationService } from '../../../core/services/admin-registration.service';
import { NotificationService } from '../../../core/services/notification.service';

@Component({
  selector: 'app-admin-verify-email',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, RouterLink],
  template: `
    <main class="admin-auth-page">
      <section class="admin-auth-card">
        <div class="header">
          <span class="eyebrow">Verification e-mail</span>
          <h1>Confirmer votre demande admin</h1>
          <p>Saisissez le code a 6 chiffres recu par e-mail. Le compte ne sera pas connecte automatiquement.</p>
        </div>

        @if (successMessage()) {
          <div class="success-message">{{ successMessage() }}</div>
        }

        <form [formGroup]="form" (ngSubmit)="submit()" class="form">
          <label>
            <span>Adresse e-mail</span>
            <input type="email" formControlName="email" autocomplete="email" />
          </label>

          <label>
            <span>Code de verification</span>
            <input type="text" formControlName="code" inputmode="numeric" maxlength="6" placeholder="123456" />
          </label>

          <button type="submit" [disabled]="loading() || form.invalid || successMessage()">
            {{ loading() ? 'Verification...' : 'Verifier mon e-mail' }}
          </button>
        </form>

        <footer>
          <a routerLink="/auth/login">Retour a la connexion</a>
        </footer>
      </section>
    </main>
  `,
  styles: [`
    :host { display: block; min-height: 100vh; background: #f8f8f8; color: #111; font-family: 'Sora', sans-serif; }
    .admin-auth-page { min-height: 100vh; display: grid; place-items: center; padding: 1.5rem; }
    .admin-auth-card { width: min(100%, 500px); background: #fff; border: 1px solid #ececf0; border-radius: 8px; padding: 1.5rem; box-shadow: 0 18px 42px rgba(17,17,17,.08); }
    .header { display: grid; gap: .45rem; margin-bottom: 1rem; }
    .eyebrow { color: #f28c28; font-weight: 800; text-transform: uppercase; font-size: .74rem; letter-spacing: .08em; }
    h1 { margin: 0; font-size: 1.55rem; color: #111; }
    p { margin: 0; color: #666; line-height: 1.6; }
    .form { display: grid; gap: .85rem; }
    label { display: grid; gap: .35rem; font-weight: 700; color: #222; }
    input { width: 100%; border: 1px solid #ddd; border-radius: 8px; padding: .75rem .8rem; font: inherit; outline: none; }
    input:focus { border-color: #f28c28; box-shadow: 0 0 0 3px rgba(242,140,40,.14); }
    button { border: 0; border-radius: 8px; background: #111; color: #fff; padding: .85rem 1rem; font-weight: 800; cursor: pointer; }
    button:disabled { opacity: .6; cursor: not-allowed; }
    .success-message { border: 1px solid #cfe8d4; background: #eef8f0; color: #276737; border-radius: 8px; padding: .8rem; margin-bottom: 1rem; font-weight: 700; }
    footer { margin-top: 1rem; text-align: center; }
    a { color: #111; font-weight: 800; text-decoration: none; }
    a:hover { color: #f28c28; }
  `]
})
export class AdminVerifyEmailComponent implements OnInit {
  private readonly fb = inject(FormBuilder);
  private readonly route = inject(ActivatedRoute);
  private readonly adminRegistrationService = inject(AdminRegistrationService);
  private readonly notificationService = inject(NotificationService);

  readonly loading = signal(false);
  readonly successMessage = signal('');

  readonly form = this.fb.group({
    email: ['', [Validators.required, Validators.email]],
    code: ['', [Validators.required, Validators.pattern(/^\d{6}$/)]]
  });

  ngOnInit(): void {
    const email = this.route.snapshot.queryParamMap.get('email');
    if (email) {
      this.form.patchValue({ email });
    }
  }

  submit(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }

    const value = this.form.getRawValue();
    this.loading.set(true);
    this.adminRegistrationService.verifyEmail({
      email: value.email!,
      code: value.code!
    }).subscribe({
      next: () => {
        const message = 'Votre e-mail est verifie. Votre demande est maintenant en attente de validation par un administrateur.';
        this.successMessage.set(message);
        this.notificationService.success(message);
        this.loading.set(false);
      },
      error: (error) => {
        this.loading.set(false);
        this.notificationService.error(error.error?.message || 'Code de verification invalide ou expire.');
      }
    });
  }
}
