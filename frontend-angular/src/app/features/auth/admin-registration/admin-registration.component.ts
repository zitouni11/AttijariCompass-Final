import { CommonModule } from '@angular/common';
import { Component, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { AdminRegistrationService } from '../../../core/services/admin-registration.service';
import { NotificationService } from '../../../core/services/notification.service';

@Component({
  selector: 'app-admin-registration',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, RouterLink],
  template: `
    <main class="admin-auth-page">
      <section class="admin-auth-card">
        <div class="header">
          <span class="eyebrow">Back Office</span>
          <h1>Demander un acces administrateur</h1>
          <p>Votre e-mail sera verifie avant validation par un administrateur existant.</p>
        </div>

        @if (successMessage()) {
          <div class="success-message">{{ successMessage() }}</div>
        }

        <form [formGroup]="form" (ngSubmit)="submit()" class="form">
          <label>
            <span>Nom complet</span>
            <input type="text" formControlName="fullName" autocomplete="name" />
          </label>

          <label>
            <span>Adresse e-mail</span>
            <input type="email" formControlName="email" autocomplete="email" />
          </label>

          <label>
            <span>Mot de passe</span>
            <input type="password" formControlName="password" autocomplete="new-password" />
          </label>

          <label>
            <span>Confirmer le mot de passe</span>
            <input type="password" formControlName="confirmPassword" autocomplete="new-password" />
          </label>

          @if (passwordMismatch()) {
            <p class="error-message">Les mots de passe ne correspondent pas.</p>
          }

          <button type="submit" [disabled]="loading() || form.invalid || passwordMismatch()">
            {{ loading() ? 'Envoi en cours...' : 'Envoyer la demande' }}
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
    .admin-auth-card { width: min(100%, 520px); background: #fff; border: 1px solid #ececf0; border-radius: 8px; padding: 1.5rem; box-shadow: 0 18px 42px rgba(17,17,17,.08); }
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
    .error-message { color: #c85a4f; font-weight: 700; font-size: .86rem; }
    footer { margin-top: 1rem; text-align: center; }
    a { color: #111; font-weight: 800; text-decoration: none; }
    a:hover { color: #f28c28; }
  `]
})
export class AdminRegistrationComponent {
  private readonly fb = inject(FormBuilder);
  private readonly router = inject(Router);
  private readonly adminRegistrationService = inject(AdminRegistrationService);
  private readonly notificationService = inject(NotificationService);

  readonly loading = signal(false);
  readonly successMessage = signal('');

  readonly form = this.fb.group({
    fullName: ['', [Validators.required, Validators.maxLength(255)]],
    email: ['', [Validators.required, Validators.email, Validators.maxLength(255)]],
    password: ['', [Validators.required, Validators.minLength(6), Validators.maxLength(255)]],
    confirmPassword: ['', [Validators.required, Validators.minLength(6), Validators.maxLength(255)]]
  });

  passwordMismatch(): boolean {
    const password = this.form.controls.password.value;
    const confirmPassword = this.form.controls.confirmPassword.value;
    return !!password && !!confirmPassword && password !== confirmPassword;
  }

  submit(): void {
    if (this.form.invalid || this.passwordMismatch()) {
      this.form.markAllAsTouched();
      return;
    }

    const value = this.form.getRawValue();
    this.loading.set(true);
    this.adminRegistrationService.requestAdminAccess({
      fullName: value.fullName!,
      email: value.email!,
      password: value.password!,
      confirmPassword: value.confirmPassword!
    }).subscribe({
      next: () => {
        const message = 'Un code de verification a ete envoye a votre adresse e-mail.';
        this.successMessage.set(message);
        this.notificationService.success(message);
        void this.router.navigate(['/admin-verify-email'], { queryParams: { email: value.email } });
      },
      error: (error) => {
        this.loading.set(false);
        this.notificationService.error(error.error?.message || 'Impossible d envoyer la demande administrateur.');
      }
    });
  }
}
