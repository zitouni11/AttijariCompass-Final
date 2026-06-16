import { CommonModule } from '@angular/common';
import { Component, OnInit, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { AuthService } from '../../../core/services/auth.service';
import { NotificationService } from '../../../core/services/notification.service';

@Component({
  selector: 'app-verify-email',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, RouterLink],
  template: `
    <main class="verify-page">
      <section class="verify-card">
        <div class="header">
          <span class="eyebrow">Verification e-mail</span>
          <h1>Activer votre compte</h1>
          <p>Entrez le code a 6 chiffres recu par e-mail. Vous pourrez ensuite vous connecter normalement.</p>
        </div>

        @if (successMessage()) {
          <div class="success-message">{{ successMessage() }}</div>
        }

        <form [formGroup]="form" (ngSubmit)="verify()" class="form">
          <label>
            <span>Adresse e-mail</span>
            <input type="email" formControlName="email" autocomplete="email" />
          </label>

          <label>
            <span>Code de verification</span>
            <input type="text" formControlName="code" inputmode="numeric" maxlength="6" placeholder="123456" />
          </label>

          <button type="submit" [disabled]="loading() || form.invalid || !!successMessage()">
            {{ loading() ? 'Verification...' : 'Verifier mon compte' }}
          </button>
        </form>

        <div class="actions">
          <button class="secondary" type="button" (click)="resend()" [disabled]="resending() || !form.controls.email.valid || !!successMessage()">
            {{ resending() ? 'Envoi...' : 'Renvoyer le code' }}
          </button>
          <a routerLink="/auth/login">Aller a la connexion</a>
        </div>
      </section>
    </main>
  `,
  styles: [`
    :host { display: block; min-height: 100vh; background: #f8f8f8; color: #111; font-family: 'Sora', sans-serif; }
    .verify-page { min-height: 100vh; display: grid; place-items: center; padding: 1.5rem; }
    .verify-card { width: min(100%, 500px); background: #fff; border: 1px solid #ececf0; border-radius: 8px; padding: 1.5rem; box-shadow: 0 18px 42px rgba(17,17,17,.08); }
    .header { display: grid; gap: .45rem; margin-bottom: 1rem; }
    .eyebrow { color: #f28c28; font-weight: 800; text-transform: uppercase; font-size: .74rem; letter-spacing: .08em; }
    h1 { margin: 0; font-size: 1.55rem; color: #111; }
    p { margin: 0; color: #666; line-height: 1.6; }
    .form { display: grid; gap: .85rem; }
    label { display: grid; gap: .35rem; font-weight: 700; color: #222; }
    input { width: 100%; border: 1px solid #ddd; border-radius: 8px; padding: .75rem .8rem; font: inherit; outline: none; }
    input:focus { border-color: #f28c28; box-shadow: 0 0 0 3px rgba(242,140,40,.14); }
    button { border: 0; border-radius: 8px; background: #111; color: #fff; padding: .85rem 1rem; font-weight: 800; cursor: pointer; }
    button.secondary { background: #fff; color: #111; border: 1px solid #ddd; }
    button:disabled { opacity: .6; cursor: not-allowed; }
    .success-message { border: 1px solid #cfe8d4; background: #eef8f0; color: #276737; border-radius: 8px; padding: .8rem; margin-bottom: 1rem; font-weight: 700; }
    .actions { display: flex; align-items: center; justify-content: space-between; gap: .75rem; margin-top: 1rem; flex-wrap: wrap; }
    a { color: #111; font-weight: 800; text-decoration: none; }
    a:hover { color: #f28c28; }
  `]
})
export class VerifyEmailComponent implements OnInit {
  private readonly fb = inject(FormBuilder);
  private readonly route = inject(ActivatedRoute);
  private readonly authService = inject(AuthService);
  private readonly notificationService = inject(NotificationService);

  readonly loading = signal(false);
  readonly resending = signal(false);
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

  verify(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }

    const value = this.form.getRawValue();
    this.loading.set(true);
    this.authService.verifyEmail({ email: value.email!, code: value.code! }).subscribe({
      next: (response) => {
        const message = response.message || 'Votre compte a ete verifie avec succes. Vous pouvez maintenant vous connecter.';
        this.successMessage.set(message);
        this.notificationService.success(message);
        this.loading.set(false);
      },
      error: (error) => {
        this.loading.set(false);
        this.notificationService.error(error.error?.message || 'Impossible de verifier le code.');
      }
    });
  }

  resend(): void {
    const email = this.form.controls.email.value;
    if (!email || this.form.controls.email.invalid) {
      this.form.controls.email.markAsTouched();
      return;
    }

    this.resending.set(true);
    this.authService.resendVerificationCode({ email }).subscribe({
      next: (response) => {
        this.resending.set(false);
        this.form.controls.code.reset('');
        this.notificationService.success(response.message || 'Un nouveau code de verification a ete envoye.');
      },
      error: (error) => {
        this.resending.set(false);
        this.notificationService.error(error.error?.message || 'Impossible de renvoyer le code.');
      }
    });
  }
}
