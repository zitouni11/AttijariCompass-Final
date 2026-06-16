import { CommonModule } from '@angular/common';
import { Component, OnInit, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { AuthService } from '../../../core/services/auth.service';
import { NotificationService } from '../../../core/services/notification.service';

@Component({
  selector: 'app-restore-account',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, RouterLink],
  template: `
    <main class="restore-shell">
      <section class="restore-card">
        <div class="auth-logo-wrap">
          <img src="assets/images/attijari-compass-logo.png" alt="Attijari Compass" class="auth-logo" />
        </div>
        <span class="eyebrow">Restauration de compte</span>
        <h1>Demander la restauration</h1>
        <p class="intro">
          Verifiez d abord votre adresse e-mail. Un administrateur devra ensuite approuver la restauration du compte.
        </p>

        @if (!codeSent()) {
          <form [formGroup]="requestForm" (ngSubmit)="requestCode()" class="form">
            <label for="email">Adresse email</label>
            <input id="email" type="email" formControlName="email" placeholder="votre@email.com" />
            @if (getRequestError()) {
              <span class="error">{{ getRequestError() }}</span>
            }

            <button type="submit" [disabled]="requestForm.invalid || loading()">
              {{ loading() ? 'Envoi en cours...' : 'Envoyer le code' }}
            </button>
          </form>
        } @else if (!verified()) {
          <form [formGroup]="verifyForm" (ngSubmit)="verifyCode()" class="form">
            <label for="verify-email">Adresse email</label>
            <input id="verify-email" type="email" formControlName="email" />

            <label for="code">Code a 6 chiffres</label>
            <input id="code" type="text" inputmode="numeric" maxlength="6" formControlName="code" placeholder="123456" />
            @if (getVerifyError()) {
              <span class="error">{{ getVerifyError() }}</span>
            }

            <button type="submit" [disabled]="verifyForm.invalid || loading()">
              {{ loading() ? 'Verification...' : 'Verifier ma demande' }}
            </button>
            <button type="button" class="secondary" (click)="requestCode()" [disabled]="loading()">
              Renvoyer le code
            </button>
          </form>
        } @else {
          <div class="success-state">
            <span class="material-symbols-rounded">mark_email_read</span>
            <h2>Demande envoyee</h2>
            <p>Votre demande est en attente de validation par un administrateur.</p>
            <a routerLink="/auth/login">Retour a la connexion</a>
          </div>
        }

        <a class="back-link" routerLink="/auth/register">Retour a l inscription</a>
      </section>
    </main>
  `,
  styles: [`
    :host { display: block; min-height: 100vh; background: #f7f7f8; font-family: 'Sora', sans-serif; color: #171717; }
    .restore-shell { min-height: 100vh; display: grid; place-items: center; padding: 1.25rem; }
    .restore-card { width: min(100%, 500px); display: grid; gap: 1rem; padding: 1.5rem; border-radius: 8px; background: #fff; border: 1px solid #ececf0; box-shadow: 0 18px 50px rgba(17,17,17,.08); }
    .auth-logo-wrap { width: 100%; display: flex; justify-content: center; align-items: center; margin-bottom: .35rem; background: transparent !important; border: none !important; box-shadow: none !important; overflow: visible; }
    .auth-logo { width: 84px; height: auto; display: block; object-fit: contain; background: transparent !important; border: none !important; box-shadow: none !important; outline: none !important; }
    .eyebrow { width: fit-content; color: #f28c28; font-weight: 800; text-transform: uppercase; font-size: .72rem; letter-spacing: .08em; }
    h1, h2, p { margin: 0; }
    h1 { font-size: 1.75rem; }
    .intro { color: #666; line-height: 1.65; }
    .form { display: grid; gap: .75rem; }
    label { font-weight: 800; font-size: .86rem; }
    input { min-height: 46px; border-radius: 8px; border: 1px solid #ddd; padding: .75rem .85rem; font: inherit; outline: none; }
    input:focus { border-color: #f28c28; box-shadow: 0 0 0 4px rgba(242,140,40,.12); }
    button, .success-state a { border: 0; border-radius: 8px; background: #111; color: #fff; min-height: 44px; padding: .7rem .9rem; font-weight: 800; cursor: pointer; text-decoration: none; display: inline-flex; justify-content: center; align-items: center; }
    button.secondary { background: #fff; color: #111; border: 1px solid #ddd; }
    button:disabled { opacity: .6; cursor: not-allowed; }
    .error { color: #c85a4f; font-size: .8rem; font-weight: 700; }
    .success-state { display: grid; gap: .75rem; padding: 1rem; border-radius: 8px; background: #eef8f0; color: #276737; }
    .success-state .material-symbols-rounded { font-size: 2rem; }
    .success-state a { width: fit-content; }
    .back-link { color: #111; font-weight: 800; text-decoration: none; width: fit-content; }
    .back-link:hover { text-decoration: underline; }
  `]
})
export class RestoreAccountComponent implements OnInit {
  private readonly fb = inject(FormBuilder);
  private readonly route = inject(ActivatedRoute);
  private readonly authService = inject(AuthService);
  private readonly notificationService = inject(NotificationService);

  readonly loading = signal(false);
  readonly codeSent = signal(false);
  readonly verified = signal(false);

  readonly requestForm = this.fb.group({
    email: ['', [Validators.required, Validators.email]]
  });

  readonly verifyForm = this.fb.group({
    email: ['', [Validators.required, Validators.email]],
    code: ['', [Validators.required, Validators.pattern(/^\d{6}$/)]]
  });

  ngOnInit(): void {
    this.route.queryParams.subscribe(params => {
      const email = params['email'];
      if (email) {
        this.requestForm.patchValue({ email });
        this.verifyForm.patchValue({ email });
      }
    });
  }

  requestCode(): void {
    if (this.requestForm.invalid) {
      this.requestForm.markAllAsTouched();
      return;
    }

    const email = this.requestForm.value.email!;
    this.loading.set(true);
    this.authService.requestAccountRestore({ email }).subscribe({
      next: (response) => {
        this.loading.set(false);
        this.codeSent.set(true);
        this.verifyForm.patchValue({ email });
        this.notificationService.success(response.message || 'Un code de verification a ete envoye.');
      },
      error: (error) => {
        this.loading.set(false);
        this.notificationService.error(error.error?.message || 'Impossible d envoyer le code.');
      }
    });
  }

  verifyCode(): void {
    if (this.verifyForm.invalid) {
      this.verifyForm.markAllAsTouched();
      return;
    }

    const { email, code } = this.verifyForm.value;
    this.loading.set(true);
    this.authService.verifyAccountRestore({ email: email!, code: code! }).subscribe({
      next: (response) => {
        this.loading.set(false);
        this.verified.set(true);
        this.notificationService.success(response.message || 'Votre demande est en attente de validation.');
      },
      error: (error) => {
        this.loading.set(false);
        this.notificationService.error(error.error?.message || 'Code de verification invalide.');
      }
    });
  }

  getRequestError(): string | null {
    const control = this.requestForm.get('email');
    if (!control?.touched || !control.errors) {
      return null;
    }
    return control.errors['email'] ? 'Email invalide' : 'Ce champ est requis';
  }

  getVerifyError(): string | null {
    const email = this.verifyForm.get('email');
    const code = this.verifyForm.get('code');
    if (email?.touched && email.errors) {
      return email.errors['email'] ? 'Email invalide' : 'Ce champ est requis';
    }
    if (code?.touched && code.errors) {
      return code.errors['pattern'] ? 'Le code doit contenir 6 chiffres' : 'Ce champ est requis';
    }
    return null;
  }
}
