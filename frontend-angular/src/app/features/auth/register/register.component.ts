import { CommonModule } from '@angular/common';
import { Component, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { AuthService } from '../../../core/services/auth.service';
import { NotificationService } from '../../../core/services/notification.service';

@Component({
  selector: 'app-register',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, RouterLink],
  template: `
    <div class="auth-shell">
      <section class="auth-showcase">
        <div class="showcase-glow showcase-glow-one"></div>
        <div class="showcase-glow showcase-glow-two"></div>

        <div class="showcase-card">
          <div class="auth-logo-wrap">
            <img
              class="auth-logo"
              src="assets/images/attijari-compass-logo.png"
              alt="Attijari Compass"
            />
          </div>

          <p class="showcase-tagline">
            Une experience premium pour suivre vos depenses, piloter vos objectifs et activer vos leviers IA.
          </p>

          <div class="feature-list">
            <div class="feature-item">
              <span class="material-symbols-rounded">auto_graph</span>
              <span>Suivi intelligent des flux et de l epargne</span>
            </div>

            <div class="feature-item">
              <span class="material-symbols-rounded">auto_awesome</span>
              <span>Recommandations IA pour arbitrer vos priorites</span>
            </div>

            <div class="feature-item">
              <span class="material-symbols-rounded">timeline</span>
              <span>Simulations pour mesurer votre trajectoire future</span>
            </div>
          </div>
        </div>
      </section>

      <section class="auth-panel">
        <div class="auth-card">
          <div class="auth-card-header">
            <span class="eyebrow">Ouverture de compte</span>
            <h1>Creer un compte</h1>
            <p>Rejoignez Attijari Compass et activez votre tableau de bord financier premium.</p>
          </div>

          <form [formGroup]="registerForm" (ngSubmit)="onSubmit()" class="auth-form">
            <div class="form-group" [class.has-error]="getError('email')">
              <label for="email">Adresse email</label>

              <div class="input-shell">
                <span class="material-symbols-rounded input-icon">mail</span>
                <input id="email" type="email" formControlName="email" placeholder="votre@email.com" />
              </div>

              @if (getError('email')) {
                <span class="error-msg">{{ getError('email') }}</span>
              }
            </div>

            <div class="form-group" [class.has-error]="getError('password')">
              <label for="password">Mot de passe</label>

              <div class="input-shell">
                <span class="material-symbols-rounded input-icon">lock</span>
                <input
                  id="password"
                  [type]="showPassword ? 'text' : 'password'"
                  formControlName="password"
                  placeholder="Minimum 6 caracteres"
                />
                <button
                  class="toggle-password"
                  type="button"
                  aria-label="Afficher ou masquer le mot de passe"
                  (click)="showPassword = !showPassword"
                >
                  <span class="material-symbols-rounded">
                    {{ showPassword ? 'visibility_off' : 'visibility' }}
                  </span>
                </button>
              </div>

              @if (getError('password')) {
                <span class="error-msg">{{ getError('password') }}</span>
              }
            </div>

            <button type="submit" class="btn-primary" [disabled]="isLoading || registerForm.invalid">
              @if (isLoading) {
                <span class="spinner"></span>
                Creation en cours...
              } @else {
                Ouvrir mon espace
              }
            </button>
          </form>

          <div class="auth-footer auth-links">
            <p>Deja un compte ? <a routerLink="/auth/login">Se connecter</a></p>
            <p>Besoin du Back Office ? <a routerLink="/admin-register">Demander un acces administrateur</a></p>
            <button type="button" class="auth-link restore-link" (click)="goToRestoreAccount()">
              Compte supprime ? Demander la restauration
            </button>
          </div>

          @if (restoreEmail()) {
            <div class="restore-account">
              <p>{{ restoreMessage() }}</p>
              <button type="button" (click)="goToRestoreAccount()">
                Demander la restauration du compte
              </button>
            </div>
          }
        </div>
      </section>
    </div>
  `,
  styles: [`
    :host {
      display: block;
      min-height: 100vh;
      background:
        radial-gradient(circle at 12% 12%, rgba(246, 178, 60, 0.12), transparent 18%),
        radial-gradient(circle at 88% 18%, rgba(217, 74, 47, 0.10), transparent 16%),
        var(--color-bg-soft);
    }

    .auth-shell {
      min-height: 100vh;
      display: grid;
      grid-template-columns: minmax(0, 1.05fr) minmax(440px, 0.95fr);
      font-family: 'Sora', sans-serif;
    }

    .auth-showcase,
    .showcase-card,
    .feature-list,
    .feature-item,
    .auth-panel,
    .auth-card,
    .auth-card-header,
    .auth-form {
      display: flex;
      flex-direction: column;
    }

    .auth-showcase {
      position: relative;
      overflow: hidden;
      justify-content: center;
      align-items: center;
      padding: 2.5rem;
      background: #090909;
    }

    .auth-showcase::before {
      content: "";
      position: absolute;
      inset: 0;
      background-image: url('/assets/images/attijari-headquarters.jpg');
      background-size: cover;
      background-position: center;
      filter: blur(7px);
      transform: scale(1.08);
      opacity: 0.55;
      z-index: 0;
    }

    .auth-showcase::after {
      content: "";
      position: absolute;
      inset: 0;
      background:
        linear-gradient(90deg, rgba(0, 0, 0, 0.78), rgba(0, 0, 0, 0.55)),
        radial-gradient(circle at bottom left, rgba(242, 140, 40, 0.28), transparent 42%);
      z-index: 1;
    }

    .showcase-glow {
      position: absolute;
      width: 300px;
      height: 300px;
      border-radius: 50%;
      filter: blur(26px);
      opacity: 0.4;
      pointer-events: none;
    }

    .showcase-glow-one {
      top: -90px;
      right: -20px;
      background: rgba(246, 178, 60, 0.34);
    }

    .showcase-glow-two {
      bottom: -70px;
      left: -10px;
      background: rgba(217, 74, 47, 0.26);
    }

    .showcase-card {
      position: relative;
      z-index: 2;
      overflow: hidden;
      width: min(100%, 560px);
      gap: 1.55rem;
      padding: 2rem;
      border-radius: var(--radius-card-lg);
      background: rgba(20, 20, 20, 0.58);
      border: 1px solid rgba(255, 255, 255, 0.14);
      backdrop-filter: blur(14px);
      box-shadow: 0 28px 70px rgba(0, 0, 0, 0.45);
    }

    .showcase-card::before,
    .showcase-card::after {
      display: none !important;
    }

    .auth-logo-wrap {
      width: 100%;
      display: flex;
      justify-content: center;
      align-items: center;
      margin-bottom: 0.65rem;
      background: transparent !important;
      border: none !important;
      box-shadow: none !important;
      overflow: visible;
    }

    .auth-logo {
      width: 96px;
      height: auto;
      display: block;
      object-fit: contain;
      background: transparent !important;
      border: none !important;
      box-shadow: none !important;
      outline: none !important;
    }

    .showcase-tagline {
      color: rgba(255, 255, 255, 0.84);
      font-size: 1rem;
      line-height: 1.7;
      text-align: center;
    }

    .feature-list {
      gap: 0.85rem;
    }

    .feature-item {
      flex-direction: row;
      align-items: flex-start;
      gap: 0.85rem;
      padding: 1rem 1.05rem;
      border-radius: 22px;
      background: rgba(255, 255, 255, 0.10);
      border: 1px solid rgba(255, 255, 255, 0.16);
      backdrop-filter: blur(10px);
      color: rgba(255, 255, 255, 0.86);
      line-height: 1.55;
    }

    .feature-item .material-symbols-rounded {
      color: #FFE0A6;
    }

    .auth-panel {
      justify-content: center;
      align-items: center;
      padding: 2rem;
    }

    .auth-card {
      width: min(100%, 500px);
      gap: 1.8rem;
      padding: 2rem;
      border-radius: var(--radius-card-lg);
      background: rgba(255, 255, 255, 0.90);
      border: 1px solid rgba(255, 255, 255, 0.76);
      box-shadow: var(--shadow-card);
      backdrop-filter: blur(18px);
    }

    .auth-card-header {
      gap: 0.55rem;
    }

    .eyebrow {
      display: inline-flex;
      width: fit-content;
      align-items: center;
      padding: 0.4rem 0.78rem;
      border-radius: 999px;
      background: rgba(242, 140, 40, 0.12);
      color: var(--color-accent-red);
      font-size: 0.72rem;
      font-weight: 700;
      letter-spacing: 0.1em;
      text-transform: uppercase;
    }

    h1 {
      margin: 0;
      font-size: clamp(2rem, 3vw, 2.45rem);
      letter-spacing: -0.05em;
      color: var(--color-text-main);
    }

    .auth-card-header p {
      margin: 0;
      color: var(--color-text-muted);
      line-height: 1.7;
    }

    .auth-form {
      gap: 1rem;
    }

    .form-group {
      display: flex;
      flex-direction: column;
      gap: 0.45rem;
    }

    label {
      color: var(--color-text-main);
      font-size: 0.88rem;
      font-weight: 600;
    }

    .input-shell {
      position: relative;
      display: flex;
      align-items: center;
    }

    .input-icon {
      position: absolute;
      left: 1rem;
      color: var(--color-primary-navy);
      opacity: 0.78;
      pointer-events: none;
    }

    input[type='email'],
    input[type='password'],
    input[type='text'] {
      width: 100%;
      padding: 0.95rem 1rem 0.95rem 3rem;
      border: 1px solid var(--color-line-soft);
      border-radius: 18px;
      background: #FFFFFF;
      color: var(--color-text-main);
      font-size: 0.94rem;
      outline: none;
      box-shadow: inset 0 1px 0 rgba(255, 255, 255, 0.80);
      transition: border-color 0.2s ease, box-shadow 0.2s ease;
    }

    input::placeholder {
      color: #99A4B7;
    }

    input:focus {
      border-color: rgba(242, 140, 40, 0.46);
      box-shadow: 0 0 0 4px rgba(242, 140, 40, 0.12);
    }

    .has-error input {
      border-color: rgba(217, 74, 47, 0.44);
      box-shadow: 0 0 0 4px rgba(217, 74, 47, 0.08);
    }

    .toggle-password {
      position: absolute;
      right: 0.95rem;
      border: none;
      background: transparent;
      color: var(--color-text-muted);
      cursor: pointer;
      display: inline-flex;
      align-items: center;
      justify-content: center;
    }

    .error-msg {
      color: var(--color-state-critical);
      font-size: 0.78rem;
      font-weight: 600;
    }

    .btn-primary {
      width: 100%;
      min-height: 54px;
      margin-top: 0.4rem;
      border: none;
      border-radius: 20px;
      background: var(--gradient-brand-accent);
      color: var(--color-deep-navy);
      font-size: 0.95rem;
      font-weight: 800;
      letter-spacing: -0.01em;
      display: inline-flex;
      align-items: center;
      justify-content: center;
      gap: 0.65rem;
      cursor: pointer;
      box-shadow: var(--shadow-glow);
    }

    .btn-primary:hover:not(:disabled) {
      transform: translateY(-2px);
    }

    .btn-primary:disabled {
      opacity: 0.68;
      cursor: not-allowed;
      box-shadow: none;
    }

    .spinner {
      width: 18px;
      height: 18px;
      border: 2px solid rgba(8, 28, 74, 0.20);
      border-top-color: var(--color-deep-navy);
      border-radius: 50%;
      animation: spin 0.7s linear infinite;
    }

    .auth-footer {
      display: flex;
      flex-direction: column;
      gap: 6px;
      text-align: center;
      color: var(--color-text-muted);
      font-size: 0.9rem;
    }

    .auth-footer p {
      margin: 0;
    }

    .auth-footer a {
      color: var(--color-primary-navy);
      font-weight: 700;
      text-decoration: none;
    }

    .auth-footer a:hover {
      text-decoration: underline;
    }

    .auth-links {
      display: flex;
      flex-direction: column;
      gap: 6px;
      text-align: center;
      overflow: visible;
    }

    .auth-link,
    .restore-link {
      display: inline-block;
      width: fit-content;
      margin: 0 auto;
      padding: 0;
      font: inherit;
      font-weight: 700;
      color: #111;
      text-decoration: none;
      cursor: pointer;
      background: transparent;
      border: none;
      opacity: 1;
      visibility: visible;
    }

    .restore-link:hover {
      color: #f36f21;
    }

    .restore-account {
      display: grid;
      gap: 0.7rem;
      padding: 1rem;
      border: 1px solid rgba(242, 140, 40, 0.32);
      border-radius: 18px;
      background: rgba(242, 140, 40, 0.08);
      color: var(--color-text-main);
    }

    .restore-account p {
      margin: 0;
      color: var(--color-text-muted);
      line-height: 1.55;
      font-size: 0.9rem;
    }

    .restore-account button {
      width: fit-content;
      display: inline-flex;
      align-items: center;
      min-height: 40px;
      padding: 0.65rem 0.85rem;
      border-radius: 14px;
      border: 0;
      background: #111;
      color: #fff;
      font-weight: 800;
      cursor: pointer;
    }

    @media (max-width: 960px) {
      .auth-shell {
        grid-template-columns: 1fr;
      }

      .auth-showcase {
        min-height: 320px;
        padding: 1.5rem;
      }
    }

    @media (max-width: 640px) {
      .auth-panel,
      .auth-showcase {
        padding: 1rem;
      }

      .auth-card,
      .showcase-card {
        padding: 1.35rem;
        border-radius: 26px;
      }

    }
  `]
})
export class RegisterComponent {
  private readonly fb = inject(FormBuilder);
  private readonly authService = inject(AuthService);
  private readonly router = inject(Router);
  private readonly notificationService = inject(NotificationService);

  showPassword = false;
  isLoading = false;
  readonly restoreEmail = signal<string | null>(null);
  readonly restoreMessage = signal('');

  readonly registerForm = this.fb.group({
    email: ['', [Validators.required, Validators.email]],
    password: ['', [Validators.required, Validators.minLength(6)]]
  });

  getError(field: string): string | null {
    const control = this.registerForm.get(field);

    if (control?.touched && control.errors) {
      if (control.errors['required']) {
        return 'Ce champ est requis';
      }

      if (control.errors['email']) {
        return 'Email invalide';
      }

      if (control.errors['minlength']) {
        return 'Minimum 6 caracteres';
      }
    }

    return null;
  }

  onSubmit(): void {
    if (this.registerForm.invalid) {
      this.registerForm.markAllAsTouched();
      return;
    }

    this.isLoading = true;
    this.restoreEmail.set(null);
    this.restoreMessage.set('');
    const { email, password } = this.registerForm.value;

    this.authService.register({ email: email!, password: password!, role: 'USER' }).subscribe({
      next: (response) => {
        this.notificationService.success(response.message || 'Un code de verification a ete envoye a votre adresse e-mail.');
        this.router.navigate(['/verify-email'], { queryParams: { email } });
      },
      error: (error) => {
        this.isLoading = false;
        const message = error.error?.message || 'Erreur lors de la creation du compte';
        if (this.isRestoreAccountMessage(message)) {
          this.restoreEmail.set(email!);
          this.restoreMessage.set('Un compte supprime existe deja avec cette adresse e-mail. Vous pouvez demander sa restauration.');
        }
        this.notificationService.error(message);
      }
    });
  }

  goToRestoreAccount(): void {
    const email = this.registerForm.get('email')?.value;
    this.router.navigate(['/restore-account'], {
      queryParams: email ? { email } : {}
    });
  }

  private isRestoreAccountMessage(message: string): boolean {
    const normalized = message.toLowerCase();
    return normalized.includes('compte supprime')
      || normalized.includes('compte supprimé')
      || normalized.includes('restaurer')
      || normalized.includes('restauration');
  }
}
