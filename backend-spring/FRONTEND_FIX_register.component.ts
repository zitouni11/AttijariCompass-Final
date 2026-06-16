// ✅ À COPIER DANS: src/app/components/register/register.component.ts

import { Component, OnInit } from '@angular/core';
import { FormBuilder, FormGroup, Validators } from '@angular/forms';
import { Router } from '@angular/router';
import { AuthService } from '../../services/auth.service';

@Component({
  selector: 'app-register',
  templateUrl: './register.component.html',
  styleUrls: ['./register.component.scss']
})
export class RegisterComponent implements OnInit {
  registerForm: FormGroup;
  loading = false;
  error = '';
  success = '';

  constructor(
    private fb: FormBuilder,
    private authService: AuthService,
    private router: Router
  ) {
    this.registerForm = this.fb.group({
      email: ['', [Validators.required, Validators.email]],
      password: ['', [Validators.required, Validators.minLength(6)]],
      confirmPassword: ['', [Validators.required, Validators.minLength(6)]]
    }, {
      validators: this.passwordMatchValidator
    });
  }

  ngOnInit(): void {
    // Si déjà connecté, rediriger
    if (this.authService.isAuthenticated()) {
      this.router.navigate(['/dashboard']);
    }
  }

  // Validateur personnalisé pour vérifier que les passwords correspondent
  passwordMatchValidator(form: FormGroup) {
    if (form.get('password')?.value !== form.get('confirmPassword')?.value) {
      form.get('confirmPassword')?.setErrors({ 'passwordMismatch': true });
      return { 'passwordMismatch': true };
    }
    return null;
  }

  onSubmit(): void {
    if (this.registerForm.valid) {
      this.loading = true;
      this.error = '';
      this.success = '';

      const { email, password } = this.registerForm.value;

      this.authService.register(email, password, 'USER').subscribe(
        (response: any) => {
          console.log('✅ Register successful:', response);
          this.loading = false;
          this.success = '✅ Compte créé avec succès! Redirection...';

          // Sauvegarder les données
          localStorage.setItem('token', response.token);
          localStorage.setItem('refreshToken', response.refreshToken);
          localStorage.setItem('email', response.email);
          localStorage.setItem('role', response.role);

          // Rediriger après 2 secondes
          setTimeout(() => {
            this.router.navigate(['/dashboard']);
          }, 2000);
        },
        (err) => {
          console.error('❌ Register failed:', err);
          this.loading = false;
          this.error = err.error?.message || 'Erreur lors de la création du compte';
        }
      );
    }
  }
}

