import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { AbstractControl, FormBuilder, ReactiveFormsModule, ValidationErrors, Validators } from '@angular/forms';
import { UserService } from '../../core/services/api.services';
import { AuthService } from '../../core/services/auth.service';
import { NotificationService } from '../../core/services/notification.service';
import { ProfilePhotoService } from '../../core/services/profile-photo.service';
import { UserResponse } from '../../core/models';

@Component({
  selector: 'app-profile',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule],
  template: `
    <section class="profile-page">
      <header class="profile-hero">
        <div>
          <span class="profile-eyebrow">Espace personnel</span>
          <h1>Mon profil</h1>
          <p>Gérez votre identité, vos accès et les actions sensibles de votre compte Attijari Compass.</p>
        </div>
      </header>

      @if (loadingProfile()) {
        <div class="profile-skeleton"></div>
      } @else {
        <div class="profile-main-grid">
          <article class="profile-card identity-card" aria-label="Identité utilisateur">
            <div class="avatar-frame">
              @if (profilePhoto(); as photo) {
                <img [src]="photo" alt="Photo de profil" />
              } @else {
                <span>{{ initials() }}</span>
              }
            </div>

            <div class="identity-copy">
              <h2>{{ displayName() }}</h2>
              <p>{{ user()?.email }}</p>
              <div class="identity-badges">
                <span>{{ roleLabel() }}</span>
                <span [class.is-active]="user()?.active !== false">{{ accountStatusLabel() }}</span>
              </div>
              <small>Membre depuis {{ formatDate(user()?.createdAt) }}</small>
            </div>

            <div class="photo-actions">
              <label class="btn-secondary">
                <span class="material-symbols-rounded">photo_camera</span>
                <span>Changer la photo</span>
                <input type="file" accept="image/*" (change)="onPhotoSelected($event)" />
              </label>

              @if (profilePhoto()) {
                <button class="btn-ghost" type="button" (click)="removePhoto()">
                  <span class="material-symbols-rounded">delete</span>
                  <span>Supprimer la photo</span>
                </button>
              }
            </div>

          </article>

          <div class="profile-form-column">
            <article class="profile-card" id="settings">
              <div class="card-head">
                <span class="material-symbols-rounded">badge</span>
                <div>
                  <h3>Informations personnelles</h3>
                  <p>Conservez une adresse email fiable pour sécuriser votre accès.</p>
                </div>
              </div>

              <form [formGroup]="identityForm" (ngSubmit)="updateIdentity()" class="profile-form">
                <label>
                  <span>Adresse email</span>
                  <input type="email" formControlName="email" autocomplete="email" />
                </label>

                @if (identityForm.controls.email.invalid && identityForm.controls.email.touched) {
                  <p class="form-error">Adresse email invalide.</p>
                }

                <button class="btn-primary" type="submit" [disabled]="identityForm.invalid || savingIdentity()">
                  {{ savingIdentity() ? 'Mise à jour...' : 'Mettre à jour' }}
                </button>
              </form>
            </article>

            <article class="profile-card security-card">
              <div class="card-head">
                <span class="material-symbols-rounded">lock</span>
                <div>
                  <h3>Sécurité du compte</h3>
                  <p>Choisissez un mot de passe robuste et confirmez-le avant validation.</p>
                </div>
              </div>

              <form [formGroup]="passwordForm" (ngSubmit)="updatePassword()" class="profile-form">
                <label>
                  <span>Mot de passe actuel</span>
                  <div class="password-field">
                    <input [type]="showCurrentPassword() ? 'text' : 'password'" formControlName="currentPassword" autocomplete="current-password" />
                    <button type="button" (click)="toggleCurrentPassword()" [attr.aria-label]="showCurrentPassword() ? 'Masquer le mot de passe actuel' : 'Afficher le mot de passe actuel'">
                      <span class="material-symbols-rounded">{{ showCurrentPassword() ? 'visibility_off' : 'visibility' }}</span>
                    </button>
                  </div>
                </label>

                <div class="form-row">
                  <label>
                    <span>Nouveau mot de passe</span>
                    <div class="password-field">
                      <input [type]="showNewPassword() ? 'text' : 'password'" formControlName="password" autocomplete="new-password" />
                      <button type="button" (click)="toggleNewPassword()" [attr.aria-label]="showNewPassword() ? 'Masquer le nouveau mot de passe' : 'Afficher le nouveau mot de passe'">
                        <span class="material-symbols-rounded">{{ showNewPassword() ? 'visibility_off' : 'visibility' }}</span>
                      </button>
                    </div>
                  </label>

                  <label>
                    <span>Confirmer le nouveau mot de passe</span>
                    <div class="password-field">
                      <input [type]="showConfirmPassword() ? 'text' : 'password'" formControlName="confirmPassword" autocomplete="new-password" />
                      <button type="button" (click)="toggleConfirmPassword()" [attr.aria-label]="showConfirmPassword() ? 'Masquer la confirmation' : 'Afficher la confirmation'">
                        <span class="material-symbols-rounded">{{ showConfirmPassword() ? 'visibility_off' : 'visibility' }}</span>
                      </button>
                    </div>
                  </label>
                </div>

                @if (passwordForm.controls.password.value) {
                  <div class="password-strength" [attr.data-strength]="passwordStrength().tone">
                    <span></span>
                    <strong>{{ passwordStrength().label }}</strong>
                  </div>
                }

                @if (passwordForm.hasError('passwordMismatch') && passwordForm.controls.confirmPassword.touched) {
                  <p class="form-error">La confirmation ne correspond pas au nouveau mot de passe.</p>
                }

                @if (passwordForm.controls.password.invalid && passwordForm.controls.password.touched) {
                  <p class="form-error">Le nouveau mot de passe doit contenir au moins 6 caractères.</p>
                }

                <button class="btn-primary" type="submit" [disabled]="passwordForm.invalid || savingPassword()">
                  {{ savingPassword() ? 'Changement...' : 'Changer le mot de passe' }}
                </button>
              </form>
            </article>
          </div>
        </div>

        <article class="profile-card danger-card">
          <div class="card-head">
            <span class="material-symbols-rounded">warning</span>
            <div>
              <h3>Zone dangereuse</h3>
              <p>Ces actions ont un impact immédiat sur votre session et votre accès.</p>
            </div>
          </div>

          <div class="danger-actions">
            <button class="btn-secondary" type="button" (click)="logout()">
              <span class="material-symbols-rounded">logout</span>
              <span>Se déconnecter</span>
            </button>

            <button class="btn-danger" type="button" (click)="openDeleteModal()">
              <span class="material-symbols-rounded">delete_forever</span>
              <span>Supprimer mon compte</span>
            </button>
          </div>
        </article>
      }

      @if (deleteModalOpen()) {
        <div class="modal-backdrop" role="presentation" (click)="closeDeleteModal()"></div>
        <section class="confirm-modal" role="dialog" aria-modal="true" aria-labelledby="delete-title">
          <h2 id="delete-title">Supprimer mon compte</h2>
          <p>Cette action désactivera votre compte et mettra fin à votre accès à Attijari Compass.</p>
          <div class="modal-actions">
            <button class="btn-secondary" type="button" (click)="closeDeleteModal()">Annuler</button>
            <button class="btn-danger" type="button" [disabled]="deletingAccount()" (click)="confirmDeleteAccount()">
              {{ deletingAccount() ? 'Suppression...' : 'Confirmer la suppression' }}
            </button>
          </div>
        </section>
      }
    </section>
  `,
  styles: [`
    .profile-page { display: grid; gap: 1.5rem; font-family: 'Sora', sans-serif; min-width: 0; }
    .profile-hero { padding: clamp(1.5rem, 3vw, 2.3rem); border-radius: 24px; color: #fffaf6; background: radial-gradient(circle at 82% 18%, rgba(232,80,10,.24), transparent 22%), linear-gradient(135deg, #111 0%, #211813 100%); box-shadow: 0 22px 42px rgba(17,17,17,.14); }
    .profile-eyebrow { display: inline-flex; margin-bottom: .55rem; padding: .38rem .7rem; border-radius: 999px; background: rgba(245,130,32,.16); color: #ffd1ae; font-size: .72rem; font-weight: 800; letter-spacing: .1em; }
    h1, h2, h3, p { margin: 0; }
    .profile-hero h1 { color: #fffaf6; font-family: 'Playfair Display', serif; font-size: clamp(2.2rem, 5vw, 4rem); line-height: 1.05; text-shadow: 0 1px 20px rgba(0,0,0,.26); }
    .profile-hero p { max-width: 44rem; margin-top: .55rem; color: rgba(255,250,246,.72); line-height: 1.7; }
    .profile-main-grid { display: grid; grid-template-columns: minmax(17rem, 22rem) minmax(0, 1fr); gap: 1rem; align-items: start; }
    .profile-form-column { display: grid; gap: 1rem; min-width: 0; }
    .profile-card { min-width: 0; padding: 1.25rem; border-radius: 20px; border: 1px solid rgba(17,17,17,.06); background: linear-gradient(180deg, #fff 0%, #fcfaf7 100%); box-shadow: 0 14px 28px rgba(17,17,17,.05); }
    .identity-card { display: grid; justify-items: center; align-content: start; gap: .95rem; text-align: center; position: sticky; top: 5.5rem; }
    .avatar-frame { width: 8rem; height: 8rem; max-width: 140px; max-height: 140px; display: grid; place-items: center; border-radius: 34px; overflow: hidden; background: linear-gradient(135deg, var(--attijari-orange), var(--attijari-orange-dark)); color: white; font-size: 2.2rem; font-weight: 900; box-shadow: 0 18px 34px rgba(245,130,32,.22); }
    .avatar-frame img { width: 100%; height: 100%; object-fit: cover; display: block; }
    .identity-copy { display: grid; gap: .45rem; min-width: 0; }
    .identity-copy h2 { color: var(--attijari-black); font-size: 1.15rem; overflow-wrap: anywhere; }
    .identity-copy p, .identity-copy small, .card-head p { color: var(--attijari-muted); line-height: 1.6; }
    .identity-badges { display: flex; justify-content: center; gap: .45rem; flex-wrap: wrap; }
    .identity-badges span { padding: .34rem .65rem; border-radius: 999px; background: rgba(245,130,32,.1); color: var(--attijari-orange-dark); font-size: .74rem; font-weight: 800; }
    .identity-badges .is-active { background: rgba(91,142,99,.12); color: #3f7b47; }
    .photo-actions, .danger-actions, .modal-actions { display: flex; gap: .7rem; flex-wrap: wrap; align-items: center; }
    .photo-actions { justify-content: center; }
    .photo-actions input { display: none; }
    .card-head { display: flex; align-items: flex-start; gap: .85rem; margin-bottom: 1.1rem; }
    .card-head > .material-symbols-rounded { width: 2.6rem; height: 2.6rem; display: grid; place-items: center; border-radius: 16px; background: rgba(245,130,32,.12); color: var(--attijari-orange-dark); flex: 0 0 2.6rem; }
    .card-head h3 { color: var(--attijari-black); font-size: 1rem; }
    .profile-form { display: grid; gap: .9rem; }
    .form-row { display: grid; grid-template-columns: repeat(2, minmax(0, 1fr)); gap: .8rem; }
    label { display: grid; gap: .35rem; color: var(--attijari-text); font-size: .8rem; font-weight: 700; min-width: 0; }
    input { width: 100%; box-sizing: border-box; padding: .78rem .9rem; border: 1.5px solid var(--attijari-border); border-radius: 12px; font: inherit; outline: none; background: white; }
    input:focus { border-color: var(--attijari-orange); box-shadow: 0 0 0 3px rgba(245,130,32,.12); }
    .password-field { display: grid; grid-template-columns: minmax(0, 1fr) auto; align-items: center; border: 1.5px solid var(--attijari-border); border-radius: 12px; background: #fff; overflow: hidden; }
    .password-field:focus-within { border-color: var(--attijari-orange); box-shadow: 0 0 0 3px rgba(245,130,32,.12); }
    .password-field input { border: none; box-shadow: none; border-radius: 0; }
    .password-field button { width: 2.7rem; height: 2.7rem; border: none; background: transparent; color: var(--attijari-muted); cursor: pointer; }
    .password-field .material-symbols-rounded { font-size: 1.15rem; }
    .btn-primary, .btn-secondary, .btn-ghost, .btn-danger { min-height: 2.8rem; display: inline-flex; align-items: center; justify-content: center; gap: .45rem; padding: .72rem 1rem; border-radius: 999px; border: 1px solid transparent; font: inherit; font-size: .84rem; font-weight: 800; cursor: pointer; text-decoration: none; transition: transform .2s ease, box-shadow .2s ease, background-color .2s ease; }
    .btn-primary { width: fit-content; color: white; background: linear-gradient(135deg, var(--attijari-orange), var(--attijari-orange-dark)); box-shadow: 0 12px 24px rgba(245,130,32,.2); }
    .btn-secondary, .btn-ghost { color: var(--attijari-black); background: white; border-color: rgba(17,17,17,.08); }
    .btn-danger { color: #fff; background: #c85a4f; box-shadow: 0 12px 24px rgba(200,90,79,.18); }
    button:disabled { opacity: .58; cursor: not-allowed; box-shadow: none; }
    .form-error { color: #c85a4f; font-size: .78rem; font-weight: 700; }
    .password-strength { display: flex; align-items: center; gap: .55rem; color: var(--attijari-muted); font-size: .78rem; font-weight: 800; }
    .password-strength span { width: 5.5rem; height: .45rem; border-radius: 999px; background: #eadfd6; overflow: hidden; }
    .password-strength span::after { content: ''; display: block; height: 100%; width: 33%; background: #c85a4f; border-radius: inherit; }
    .password-strength[data-strength='medium'] span::after { width: 66%; background: var(--attijari-orange); }
    .password-strength[data-strength='strong'] span::after { width: 100%; background: #5b8e63; }
    .danger-card { display: flex; align-items: center; justify-content: space-between; gap: 1rem; border-color: rgba(200,90,79,.2); }
    .danger-card .card-head { margin-bottom: 0; }
    .danger-actions { justify-content: flex-end; }
    .profile-skeleton { min-height: 24rem; border-radius: 24px; background: linear-gradient(90deg, #f4efea, #fffaf6, #f4efea); background-size: 200% 100%; animation: shimmer 1.2s linear infinite; }
    .modal-backdrop { position: fixed; inset: 0; z-index: 80; background: rgba(17,17,17,.42); backdrop-filter: blur(8px); }
    .confirm-modal { position: fixed; z-index: 81; inset: 50% auto auto 50%; transform: translate(-50%, -50%); width: min(28rem, calc(100vw - 2rem)); padding: 1.4rem; border-radius: 22px; background: white; box-shadow: 0 28px 70px rgba(17,17,17,.24); }
    .confirm-modal h2 { color: var(--attijari-black); margin-bottom: .5rem; }
    .confirm-modal p { color: var(--attijari-muted); line-height: 1.7; margin-bottom: 1.1rem; }
    @keyframes shimmer { to { background-position: -200% 0; } }
    @media (max-width: 980px) { .profile-main-grid { grid-template-columns: 1fr; } .identity-card { position: static; } }
    @media (max-width: 760px) { .form-row, .danger-card { grid-template-columns: 1fr; } .danger-card { display: grid; align-items: start; } .danger-actions { justify-content: flex-start; } }
    @media (max-width: 560px) { .profile-card { padding: 1rem; border-radius: 18px; } .photo-actions, .danger-actions, .modal-actions { flex-direction: column; align-items: stretch; } .btn-primary, .btn-secondary, .btn-ghost, .btn-danger { width: 100%; } .avatar-frame { width: 7rem; height: 7rem; border-radius: 28px; } }
  `]
})
export class ProfileComponent implements OnInit {
  private readonly fb = inject(FormBuilder);
  private readonly userService = inject(UserService);
  private readonly authService = inject(AuthService);
  private readonly notificationService = inject(NotificationService);
  private readonly profilePhotoService = inject(ProfilePhotoService);

  readonly user = signal<UserResponse | null>(null);
  readonly loadingProfile = signal(true);
  readonly savingIdentity = signal(false);
  readonly savingPassword = signal(false);
  readonly deletingAccount = signal(false);
  readonly deleteModalOpen = signal(false);
  readonly profilePhoto = this.profilePhotoService.photoSignal;
  readonly showCurrentPassword = signal(false);
  readonly showNewPassword = signal(false);
  readonly showConfirmPassword = signal(false);

  readonly identityForm = this.fb.group({
    email: ['', [Validators.required, Validators.email]]
  });

  readonly passwordForm = this.fb.group({
    currentPassword: ['', [Validators.required]],
    password: ['', [Validators.required, Validators.minLength(6)]],
    confirmPassword: ['', [Validators.required]]
  }, { validators: ProfileComponent.passwordsMatch });

  readonly initials = computed(() => {
    const source = this.user()?.fullName?.trim() || this.user()?.email || 'U';
    return source
      .split(/[.@_\-\s]+/)
      .filter(Boolean)
      .slice(0, 2)
      .map((part) => part.charAt(0).toUpperCase())
      .join('') || 'U';
  });

  readonly displayName = computed(() => this.user()?.fullName?.trim() || this.user()?.email?.split('@')[0] || 'Client');
  readonly roleLabel = computed(() => this.user()?.role === 'ADMIN' ? 'Administrateur' : 'Utilisateur');
  readonly accountStatusLabel = computed(() => this.user()?.active === false ? 'Compte inactif' : 'Compte actif');
  readonly passwordStrength = computed(() => {
    const value = this.passwordForm.controls.password.value ?? '';

    if (value.length >= 10 && /[A-Z]/.test(value) && /\d/.test(value)) {
      return { label: 'Robustesse élevée', tone: 'strong' };
    }

    if (value.length >= 6) {
      return { label: 'Robustesse moyenne', tone: 'medium' };
    }

    return { label: 'Robustesse faible', tone: 'weak' };
  });

  ngOnInit(): void {
    this.userService.getMe().subscribe({
      next: (user) => {
        this.user.set(user);
        this.identityForm.patchValue({ email: user.email });
        this.profilePhotoService.setFromUser(user);
        this.loadingProfile.set(false);
      },
      error: () => {
        this.loadingProfile.set(false);
        this.notificationService.error('Impossible de charger votre profil.');
      }
    });
  }

  updateIdentity(): void {
    const current = this.user();

    if (!current || this.identityForm.invalid) {
      this.identityForm.markAllAsTouched();
      return;
    }

    this.savingIdentity.set(true);
    this.userService.update(current.id, { email: this.identityForm.controls.email.value!, password: null }).subscribe({
      next: (updated) => {
        this.user.set(updated);
        this.profilePhotoService.setFromUser(updated);
        this.savingIdentity.set(false);
        this.notificationService.success('Profil mis à jour.');
      },
      error: () => {
        this.savingIdentity.set(false);
        this.notificationService.error('Erreur lors de la mise à jour du profil.');
      }
    });
  }

  updatePassword(): void {
    const current = this.user();

    if (!current || this.passwordForm.invalid) {
      this.passwordForm.markAllAsTouched();
      return;
    }

    this.savingPassword.set(true);
    this.userService.update(current.id, {
      email: current.email,
      password: this.passwordForm.controls.password.value!
    }).subscribe({
      next: (updated) => {
        this.user.set(updated);
        this.passwordForm.reset();
        this.savingPassword.set(false);
        this.notificationService.success('Mot de passe mis à jour.');
      },
      error: () => {
        this.savingPassword.set(false);
        this.notificationService.error('Erreur lors du changement de mot de passe.');
      }
    });
  }

  onPhotoSelected(event: Event): void {
    const input = event.target as HTMLInputElement | null;
    const file = input?.files?.[0];

    if (!file) {
      return;
    }

    if (!file.type.startsWith('image/')) {
      this.notificationService.error('Veuillez sélectionner une image.');
      return;
    }

    if (file.size > 2 * 1024 * 1024) {
      this.notificationService.error('La photo ne doit pas dépasser 2 Mo.');
      return;
    }

    this.profilePhotoService.upload(file).subscribe({
      next: (updated) => {
        this.user.set(updated);
        this.notificationService.success('Photo de profil mise à jour.');
      },
      error: () => {
        this.notificationService.error('Impossible d’enregistrer la photo de profil.');
      }
    });
  }

  removePhoto(): void {
    this.profilePhotoService.remove().subscribe({
      next: (updated) => {
        this.user.set(updated);
        this.notificationService.info('Photo de profil supprimée.');
      },
      error: () => {
        this.notificationService.error('Impossible de supprimer la photo de profil.');
      }
    });
  }

  toggleCurrentPassword(): void {
    this.showCurrentPassword.update((value) => !value);
  }

  toggleNewPassword(): void {
    this.showNewPassword.update((value) => !value);
  }

  toggleConfirmPassword(): void {
    this.showConfirmPassword.update((value) => !value);
  }

  openDeleteModal(): void {
    this.deleteModalOpen.set(true);
  }

  closeDeleteModal(): void {
    if (!this.deletingAccount()) {
      this.deleteModalOpen.set(false);
    }
  }

  confirmDeleteAccount(): void {
    this.deletingAccount.set(true);
    this.userService.deleteMe().subscribe({
      next: () => {
        this.notificationService.success('Compte désactivé.');
        this.authService.logout();
      },
      error: () => {
        this.deletingAccount.set(false);
        this.notificationService.error('La suppression du compte est indisponible pour le moment.');
      }
    });
  }

  logout(): void {
    this.authService.logout();
  }

  formatDate(date?: string): string {
    if (!date) {
      return '—';
    }

    const parsed = new Date(date);
    if (Number.isNaN(parsed.getTime())) {
      return '—';
    }

    return parsed.toLocaleDateString('fr-FR', { day: '2-digit', month: 'long', year: 'numeric' });
  }

  static passwordsMatch(control: AbstractControl): ValidationErrors | null {
    const password = control.get('password')?.value;
    const confirmPassword = control.get('confirmPassword')?.value;

    return password && confirmPassword && password !== confirmPassword
      ? { passwordMismatch: true }
      : null;
  }
}
