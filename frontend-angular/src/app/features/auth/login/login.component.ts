import { CommonModule } from '@angular/common';
import { Component, inject } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { AuthService } from '../../../core/services/auth.service';
import { NotificationService } from '../../../core/services/notification.service';

@Component({
  selector: 'app-login',
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
            Votre cockpit premium pour piloter budget, epargne, objectifs et recommandations IA.
          </p>

          <div class="showcase-stat-grid">
            <article class="showcase-stat">
              <span class="showcase-stat-value">IA</span>
              <span class="showcase-stat-label">Decisions mensuelles</span>
            </article>

            <article class="showcase-stat">
              <span class="showcase-stat-value">360</span>
              <span class="showcase-stat-label">Lecture financiere</span>
            </article>

            <article class="showcase-stat">
              <span class="showcase-stat-value">DT</span>
              <span class="showcase-stat-label">Projection de gains</span>
            </article>
          </div>
        </div>
      </section>

      <section class="auth-panel">
        <div class="auth-card">
          <div class="auth-card-header">
            <span class="eyebrow">Espace client</span>
            <h1>Bon retour</h1>
            <p>Connectez-vous pour retrouver vos analyses, vos objectifs et vos recommandations IA.</p>
          </div>

          <form [formGroup]="loginForm" (ngSubmit)="onSubmit()" class="auth-form">
            <div class="form-group" [class.has-error]="emailError">
              <label for="email">Adresse email</label>

              <div class="input-shell">
                <span class="material-symbols-rounded input-icon">mail</span>
                <input
                  id="email"
                  type="email"
                  formControlName="email"
                  placeholder="votre@email.com"
                  autocomplete="email"
                />
              </div>

              @if (emailError) {
                <span class="error-msg">{{ emailError }}</span>
              }
            </div>

            <div class="form-group" [class.has-error]="passwordError">
              <label for="password">Mot de passe</label>

              <div class="input-shell">
                <span class="material-symbols-rounded input-icon">lock</span>
                <input
                  id="password"
                  [type]="showPassword ? 'text' : 'password'"
                  formControlName="password"
                  placeholder="Votre mot de passe"
                  autocomplete="current-password"
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

              @if (passwordError) {
                <span class="error-msg">{{ passwordError }}</span>
              }
            </div>

            <button type="submit" class="btn-primary" [disabled]="isLoading || loginForm.invalid">
              @if (isLoading) {
                <span class="spinner"></span>
                Connexion en cours...
              } @else {
                Acceder a mon espace
              }
            </button>
          </form>

          <div class="auth-footer auth-links">
            <p>Pas encore de compte ? <a routerLink="/auth/register">Creer un compte</a></p>
            <p><a routerLink="/verify-email">Verifier mon compte</a></p>
            <p><a routerLink="/admin-register">Demander un acces administrateur</a></p>
            <button type="button" class="auth-link restore-link" (click)="goToRestoreAccount()">
              Compte supprime ? Demander la restauration
            </button>
          </div>
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
      grid-template-columns: minmax(0, 1.1fr) minmax(420px, 0.9fr);
      font-family: 'Sora', sans-serif;
    }

    .auth-showcase,
    .showcase-card,
    .showcase-stat,
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
      width: 280px;
      height: 280px;
      border-radius: 50%;
      filter: blur(24px);
      opacity: 0.4;
      pointer-events: none;
    }

    .showcase-glow-one {
      top: -80px;
      right: -30px;
      background: rgba(246, 178, 60, 0.34);
    }

    .showcase-glow-two {
      bottom: -60px;
      left: -20px;
      background: rgba(217, 74, 47, 0.26);
    }

    .showcase-card {
      position: relative;
      z-index: 2;
      overflow: hidden;
      width: min(100%, 560px);
      gap: 1.5rem;
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
      max-width: 32rem;
      text-align: center;
      color: rgba(255, 255, 255, 0.84);
      font-size: 1rem;
      line-height: 1.7;
    }

    .showcase-stat-grid {
      display: grid;
      grid-template-columns: repeat(3, minmax(0, 1fr));
      gap: 0.9rem;
    }

    .showcase-stat {
      gap: 0.35rem;
      padding: 1rem 1.05rem;
      border-radius: 22px;
      background: rgba(255, 255, 255, 0.10);
      border: 1px solid rgba(255, 255, 255, 0.16);
      backdrop-filter: blur(10px);
    }

    .showcase-stat-value {
      color: #FFFFFF;
      font-size: 1.7rem;
      line-height: 1;
      font-weight: 800;
      letter-spacing: -0.05em;
    }

    .showcase-stat-label {
      color: rgba(255, 255, 255, 0.70);
      font-size: 0.78rem;
      letter-spacing: 0.05em;
      text-transform: uppercase;
    }

    .auth-panel {
      justify-content: center;
      align-items: center;
      padding: 2rem;
    }

    .auth-card {
      width: min(100%, 460px);
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
      font-size: clamp(2rem, 3vw, 2.5rem);
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

    input {
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

    @media (max-width: 960px) {
      .auth-shell {
        grid-template-columns: 1fr;
      }

      .auth-showcase {
        min-height: 320px;
        padding: 1.5rem;
      }

      .showcase-card {
        width: min(100%, 640px);
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

      .showcase-stat-grid {
        grid-template-columns: 1fr;
      }
    }
  `]
})
export class LoginComponent {
  private readonly fb = inject(FormBuilder);
  private readonly authService = inject(AuthService);
  private readonly router = inject(Router);
  private readonly notificationService = inject(NotificationService);

  showPassword = false;
  isLoading = false;

  readonly loginForm = this.fb.group({
    email: ['', [Validators.required, Validators.email]],
    password: ['', [Validators.required, Validators.minLength(6)]]
  });

  get emailError(): string | null {
    const control = this.loginForm.get('email');

    if (control?.touched && control.errors) {
      if (control.errors['required']) {
        return 'L email est requis';
      }

      if (control.errors['email']) {
        return 'Email invalide';
      }
    }

    return null;
  }

  get passwordError(): string | null {
    const control = this.loginForm.get('password');

    if (control?.touched && control.errors) {
      if (control.errors['required']) {
        return 'Le mot de passe est requis';
      }

      if (control.errors['minlength']) {
        return 'Minimum 6 caracteres';
      }
    }

    return null;
  }

  onSubmit(): void {
    if (this.loginForm.invalid) {
      this.loginForm.markAllAsTouched();
      return;
    }

    this.isLoading = true;
    const { email, password } = this.loginForm.value;

    this.authService.login({ email: email!, password: password! }).subscribe({
      next: (response) => {
        this.notificationService.success('Connexion reussie. Bienvenue.');
        this.router.navigate([response.role === 'ADMIN' ? '/admin/dashboard' : '/dashboard']);
      },
      error: (error) => {
        this.isLoading = false;
        this.notificationService.error(error.error?.message || 'Email ou mot de passe incorrect');
      }
    });
  }

  goToRestoreAccount(): void {
    const email = this.loginForm.get('email')?.value;
    this.router.navigate(['/restore-account'], {
      queryParams: email ? { email } : {}
    });
  }
}
