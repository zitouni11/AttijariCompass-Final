# 🎨 Guide Frontend Angular - Intégration avec Backend

## 📋 Étapes pour intégrer le frontend avec le backend

### 1. Configuration du proxy (proxy.conf.json)

Assurez-vous que votre fichier `proxy.conf.json` existe à la racine du projet Angular :

```json
{
  "/api": {
    "target": "http://localhost:8081",
    "pathRewrite": { "^/api": "/api" },
    "changeOrigin": true,
    "logLevel": "debug"
  }
}
```

### 2. Démarrage du frontend

```bash
cd attijari-compass-frontend
ng serve --proxy-config proxy.conf.json
# OU
npm start --proxy-config proxy.conf.json
```

Le frontend devrait être accessible sur : `http://localhost:4200`

---

## 🔐 Intercepteur HTTP pour JWT

Créez `src/app/core/interceptors/auth.interceptor.ts` :

```typescript
import { Injectable } from '@angular/core';
import { HttpInterceptor, HttpRequest, HttpHandler, HttpEvent } from '@angular/common/http';
import { Observable } from 'rxjs';
import { AuthService } from '../services/auth.service';

@Injectable()
export class AuthInterceptor implements HttpInterceptor {
  constructor(private authService: AuthService) {}

  intercept(req: HttpRequest<any>, next: HttpHandler): Observable<HttpEvent<any>> {
    const token = localStorage.getItem('token');
    
    if (token) {
      req = req.clone({
        setHeaders: {
          Authorization: `Bearer ${token}`
        }
      });
    }
    
    return next.handle(req);
  }
}
```

Enregistrez dans `app.config.ts` ou `app.module.ts` :

```typescript
// app.config.ts
import { ApplicationConfig } from '@angular/core';
import { provideHttpClient, withInterceptors, HTTP_INTERCEPTORS } from '@angular/common/http';
import { AuthInterceptor } from './core/interceptors/auth.interceptor';

export const appConfig: ApplicationConfig = {
  providers: [
    provideHttpClient(),
    { provide: HTTP_INTERCEPTORS, useClass: AuthInterceptor, multi: true }
  ]
};
```

---

## 🔒 Service d'authentification

Créez `src/app/core/services/auth.service.ts` :

```typescript
import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, BehaviorSubject } from 'rxjs';
import { tap } from 'rxjs/operators';

export interface AuthResponse {
  token: string;
  refreshToken: string;
  email: string;
  role: string;
}

@Injectable({
  providedIn: 'root'
})
export class AuthService {
  private apiUrl = '/api/auth';
  private currentUserSubject = new BehaviorSubject<any>(null);
  public currentUser$ = this.currentUserSubject.asObservable();

  constructor(private http: HttpClient) {
    this.loadUser();
  }

  register(email: string, password: string): Observable<AuthResponse> {
    return this.http.post<AuthResponse>(`${this.apiUrl}/register`, {
      email,
      password
    }).pipe(
      tap(response => {
        localStorage.setItem('token', response.token);
        localStorage.setItem('refreshToken', response.refreshToken);
        this.currentUserSubject.next(response);
      })
    );
  }

  login(email: string, password: string): Observable<AuthResponse> {
    return this.http.post<AuthResponse>(`${this.apiUrl}/login`, {
      email,
      password
    }).pipe(
      tap(response => {
        localStorage.setItem('token', response.token);
        localStorage.setItem('refreshToken', response.refreshToken);
        this.currentUserSubject.next(response);
      })
    );
  }

  logout(): void {
    localStorage.removeItem('token');
    localStorage.removeItem('refreshToken');
    this.currentUserSubject.next(null);
  }

  refreshToken(): Observable<AuthResponse> {
    const refreshToken = localStorage.getItem('refreshToken');
    return this.http.post<AuthResponse>(`${this.apiUrl}/refresh-token`, {
      refreshToken
    }).pipe(
      tap(response => {
        localStorage.setItem('token', response.token);
        localStorage.setItem('refreshToken', response.refreshToken);
      })
    );
  }

  private loadUser(): void {
    const token = localStorage.getItem('token');
    if (token) {
      this.http.get('/api/users/me').subscribe(
        (user: any) => this.currentUserSubject.next(user)
      );
    }
  }

  isLoggedIn(): boolean {
    return !!localStorage.getItem('token');
  }

  getToken(): string | null {
    return localStorage.getItem('token');
  }
}
```

---

## 💳 Service de transactions

Créez `src/app/core/services/transaction.service.ts` :

```typescript
import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

export interface Transaction {
  id: number;
  description: string;
  merchantName: string;
  amount: number;
  date: string;
  category: string;
  type: string;
  paymentMethod: string;
  cardLast4?: string;
  createdAt: string;
}

export interface ImportResponse {
  totalProcessed: number;
  successCount: number;
  errorCount: number;
  errors: string[];
  message: string;
}

@Injectable({
  providedIn: 'root'
})
export class TransactionService {
  private apiUrl = '/api/transactions';

  constructor(private http: HttpClient) {}

  // Créer une transaction par carte
  createCardPayment(merchantName: string, description: string, amount: number, 
                    date: string, cardLast4: string): Observable<Transaction> {
    return this.http.post<Transaction>(`${this.apiUrl}/card-payment`, {
      merchantName,
      description,
      amount,
      date,
      cardLast4
    });
  }

  // Créer une transaction manuelle
  createTransaction(description: string, amount: number, date: string, 
                    category: string, type: string, paymentMethod: string): Observable<Transaction> {
    return this.http.post<Transaction>(this.apiUrl, {
      description,
      amount,
      date,
      category,
      type,
      paymentMethod
    });
  }

  // Lister les transactions de l'utilisateur
  getAllTransactions(): Observable<Transaction[]> {
    return this.http.get<Transaction[]>(this.apiUrl);
  }

  // Obtenir une transaction par ID
  getTransactionById(id: number): Observable<Transaction> {
    return this.http.get<Transaction>(`${this.apiUrl}/${id}`);
  }

  // Mettre à jour une transaction
  updateTransaction(id: number, data: any): Observable<Transaction> {
    return this.http.put<Transaction>(`${this.apiUrl}/${id}`, data);
  }

  // Corriger la catégorie
  updateCategory(id: number, category: string): Observable<Transaction> {
    return this.http.patch<Transaction>(`${this.apiUrl}/${id}/category`, {
      category
    });
  }

  // Supprimer une transaction
  deleteTransaction(id: number): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/${id}`);
  }

  // ⭐ NOUVEAU: Importer des transactions
  importTransactions(file: File): Observable<ImportResponse> {
    const formData = new FormData();
    formData.append('file', file);
    
    return this.http.post<ImportResponse>(`${this.apiUrl}/import`, formData);
  }
}
```

---

## 📁 Composant d'import de transactions

Créez `src/app/features/transactions/import/transaction-import.component.ts` :

```typescript
import { Component } from '@angular/core';
import { TransactionService, ImportResponse } from '../../../core/services/transaction.service';
import { MatSnackBar } from '@angular/material/snack-bar';

@Component({
  selector: 'app-transaction-import',
  templateUrl: './transaction-import.component.html',
  styleUrls: ['./transaction-import.component.scss']
})
export class TransactionImportComponent {
  selectedFile: File | null = null;
  isLoading = false;
  importResult: ImportResponse | null = null;

  constructor(
    private transactionService: TransactionService,
    private snackBar: MatSnackBar
  ) {}

  onFileSelected(event: any): void {
    const file: File = event.target.files[0];
    
    if (file) {
      // Valider le type de fichier
      const validTypes = ['text/csv', 'application/vnd.ms-excel', 
                         'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet'];
      
      if (validTypes.includes(file.type) || file.name.endsWith('.csv') || 
          file.name.endsWith('.xlsx') || file.name.endsWith('.xls')) {
        this.selectedFile = file;
      } else {
        this.snackBar.open('Format de fichier non supporté. Utilisez CSV ou Excel.', 'Fermer', {
          duration: 5000
        });
      }
    }
  }

  importTransactions(): void {
    if (!this.selectedFile) {
      this.snackBar.open('Veuillez sélectionner un fichier', 'Fermer', {
        duration: 3000
      });
      return;
    }

    this.isLoading = true;

    this.transactionService.importTransactions(this.selectedFile).subscribe({
      next: (response) => {
        this.importResult = response;
        this.isLoading = false;
        
        if (response.errorCount === 0) {
          this.snackBar.open(
            `✓ ${response.successCount} transactions importées avec succès!`,
            'Fermer',
            { duration: 5000, panelClass: ['success-snackbar'] }
          );
        } else {
          this.snackBar.open(
            `⚠ ${response.successCount} importées, ${response.errorCount} erreurs`,
            'Détails',
            { duration: 5000, panelClass: ['warning-snackbar'] }
          );
        }
      },
      error: (error) => {
        this.isLoading = false;
        console.error('Erreur lors de l\'import:', error);
        this.snackBar.open(
          `✗ Erreur lors de l'import: ${error.error?.message || error.statusText}`,
          'Fermer',
          { duration: 5000, panelClass: ['error-snackbar'] }
        );
      }
    });
  }

  downloadTemplate(): void {
    // Créer un fichier CSV exemple
    const csvContent = `date,description,amount,category,type,paymentMethod,merchantName
27/03/2026,Restaurant,125.50,RESTAURATION,DEPENSE,CARD,Restaurant XYZ
27/03/2026,Salaire,3000.00,SALAIRE,REVENU,BANK_TRANSFER,Employeur
27/03/2026,Essence,60.00,TRANSPORT,DEPENSE,CARD,Station Shell`;

    const element = document.createElement('a');
    element.setAttribute('href', 'data:text/csv;charset=utf-8,' + encodeURIComponent(csvContent));
    element.setAttribute('download', 'template-transactions.csv');
    element.style.display = 'none';
    document.body.appendChild(element);
    element.click();
    document.body.removeChild(element);
  }
}
```

Créez `src/app/features/transactions/import/transaction-import.component.html` :

```html
<div class="import-container">
  <h2>Importer des Transactions</h2>
  
  <div class="import-section">
    <div class="file-input-group">
      <input 
        type="file" 
        #fileInput
        (change)="onFileSelected($event)"
        accept=".csv,.xlsx,.xls"
        class="file-input"
      />
      <label class="file-label">
        <span *ngIf="!selectedFile" class="placeholder">
          📁 Sélectionnez un fichier CSV ou Excel
        </span>
        <span *ngIf="selectedFile" class="selected">
          ✓ {{ selectedFile.name }}
        </span>
      </label>
    </div>

    <div class="button-group">
      <button 
        (click)="importTransactions()" 
        [disabled]="!selectedFile || isLoading"
        class="btn-primary"
      >
        <span *ngIf="!isLoading">📤 Importer</span>
        <span *ngIf="isLoading">⏳ Import en cours...</span>
      </button>
      
      <button 
        (click)="downloadTemplate()" 
        class="btn-secondary"
      >
        📥 Télécharger le modèle
      </button>
    </div>
  </div>

  <!-- Résultat d'import -->
  <div *ngIf="importResult" class="import-result">
    <div [class.success]="importResult.errorCount === 0" 
         [class.warning]="importResult.errorCount > 0" 
         class="result-card">
      
      <h3>{{ importResult.message }}</h3>
      
      <div class="stats">
        <div class="stat">
          <span class="label">Total traité:</span>
          <span class="value">{{ importResult.totalProcessed }}</span>
        </div>
        <div class="stat success">
          <span class="label">✓ Succès:</span>
          <span class="value">{{ importResult.successCount }}</span>
        </div>
        <div *ngIf="importResult.errorCount > 0" class="stat error">
          <span class="label">✗ Erreurs:</span>
          <span class="value">{{ importResult.errorCount }}</span>
        </div>
      </div>

      <!-- Afficher les erreurs détaillées -->
      <div *ngIf="importResult.errors.length > 0" class="errors-section">
        <h4>Détails des erreurs:</h4>
        <ul class="error-list">
          <li *ngFor="let error of importResult.errors" class="error-item">
            {{ error }}
          </li>
        </ul>
      </div>
    </div>
  </div>

  <!-- Guide d'utilisation -->
  <div class="guide-section">
    <h3>📖 Format du fichier</h3>
    <p>Le fichier doit contenir les colonnes suivantes:</p>
    
    <table class="format-table">
      <tr>
        <th>Colonne</th>
        <th>Obligatoire</th>
        <th>Format</th>
      </tr>
      <tr>
        <td><strong>date</strong></td>
        <td>✓</td>
        <td>DD/MM/YYYY ou YYYY-MM-DD</td>
      </tr>
      <tr>
        <td><strong>description</strong></td>
        <td>✓</td>
        <td>Texte libre</td>
      </tr>
      <tr>
        <td><strong>amount</strong></td>
        <td>✓</td>
        <td>Nombre (125.50)</td>
      </tr>
      <tr>
        <td>category</td>
        <td>Non</td>
        <td>RESTAURATION, TRANSPORT, etc.</td>
      </tr>
      <tr>
        <td>type</td>
        <td>Non</td>
        <td>DEPENSE, REVENU</td>
      </tr>
      <tr>
        <td>paymentMethod</td>
        <td>Non</td>
        <td>CARD, BANK_TRANSFER, CASH</td>
      </tr>
      <tr>
        <td>merchantName</td>
        <td>Non</td>
        <td>Nom du commerçant</td>
      </tr>
    </table>
  </div>
</div>
```

Créez `src/app/features/transactions/import/transaction-import.component.scss` :

```scss
.import-container {
  max-width: 800px;
  margin: 0 auto;
  padding: 2rem;

  h2 {
    color: #333;
    margin-bottom: 2rem;
  }
}

.import-section {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  border-radius: 12px;
  padding: 2rem;
  color: white;
  margin-bottom: 2rem;
  box-shadow: 0 4px 15px rgba(0, 0, 0, 0.1);
}

.file-input-group {
  position: relative;
  margin-bottom: 1.5rem;

  input[type="file"] {
    display: none;
  }

  .file-label {
    display: block;
    padding: 1.5rem;
    border: 2px dashed rgba(255, 255, 255, 0.5);
    border-radius: 8px;
    text-align: center;
    cursor: pointer;
    transition: all 0.3s ease;

    &:hover {
      border-color: white;
      background: rgba(255, 255, 255, 0.1);
    }
  }

  .placeholder {
    font-size: 1.1rem;
  }

  .selected {
    color: #4ade80;
    font-weight: bold;
  }
}

.button-group {
  display: flex;
  gap: 1rem;
  flex-wrap: wrap;

  button {
    flex: 1;
    min-width: 150px;
    padding: 0.75rem 1.5rem;
    border: none;
    border-radius: 6px;
    font-weight: 600;
    cursor: pointer;
    transition: all 0.3s ease;

    &:disabled {
      opacity: 0.5;
      cursor: not-allowed;
    }
  }
}

.btn-primary {
  background: white;
  color: #667eea;

  &:hover:not(:disabled) {
    transform: translateY(-2px);
    box-shadow: 0 6px 20px rgba(0, 0, 0, 0.2);
  }
}

.btn-secondary {
  background: rgba(255, 255, 255, 0.2);
  color: white;

  &:hover:not(:disabled) {
    background: rgba(255, 255, 255, 0.3);
  }
}

.import-result {
  margin-bottom: 2rem;

  .result-card {
    border-radius: 12px;
    padding: 1.5rem;
    box-shadow: 0 4px 15px rgba(0, 0, 0, 0.1);
    border-left: 4px solid;

    &.success {
      border-left-color: #22c55e;
      background: #f0fdf4;
      color: #166534;
    }

    &.warning {
      border-left-color: #f59e0b;
      background: #fffbeb;
      color: #78350f;
    }

    h3 {
      margin-top: 0;
      margin-bottom: 1rem;
    }
  }
}

.stats {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(150px, 1fr));
  gap: 1rem;
  margin-bottom: 1.5rem;

  .stat {
    padding: 1rem;
    background: rgba(255, 255, 255, 0.5);
    border-radius: 8px;
    text-align: center;

    .label {
      display: block;
      font-size: 0.9rem;
      margin-bottom: 0.5rem;
    }

    .value {
      display: block;
      font-size: 1.5rem;
      font-weight: bold;
    }

    &.success .value {
      color: #22c55e;
    }

    &.error .value {
      color: #ef4444;
    }
  }
}

.errors-section {
  margin-top: 1rem;

  h4 {
    margin: 1rem 0 0.5rem;
    color: inherit;
  }

  .error-list {
    list-style: none;
    padding: 0;
    margin: 0;

    .error-item {
      padding: 0.5rem;
      margin: 0.25rem 0;
      background: rgba(0, 0, 0, 0.1);
      border-radius: 4px;
      font-size: 0.9rem;
    }
  }
}

.guide-section {
  background: #f5f5f5;
  padding: 1.5rem;
  border-radius: 12px;

  h3 {
    margin-top: 0;
    color: #333;
  }

  p {
    color: #666;
    margin-bottom: 1rem;
  }

  .format-table {
    width: 100%;
    border-collapse: collapse;
    margin-top: 1rem;

    th, td {
      padding: 0.75rem;
      text-align: left;
      border-bottom: 1px solid #ddd;
    }

    th {
      background: #e5e5e5;
      font-weight: 600;
    }

    tr:last-child td {
      border-bottom: none;
    }
  }
}

@media (max-width: 600px) {
  .button-group {
    flex-direction: column;

    button {
      flex: none;
      width: 100%;
    }
  }

  .stats {
    grid-template-columns: 1fr;
  }
}
```

---

## 🔗 Intégration dans le routing

Ajoutez le composant aux routes `src/app/app.routes.ts` :

```typescript
import { Routes } from '@angular/router';
import { TransactionImportComponent } from './features/transactions/import/transaction-import.component';

export const routes: Routes = [
  // ... autres routes
  {
    path: 'transactions/import',
    component: TransactionImportComponent,
    data: { title: 'Importer des transactions' }
  }
];
```

---

## ✅ Checklist d'intégration

- [ ] Backend Spring Boot démarré sur `localhost:8081`
- [ ] Frontend Angular configuré avec proxy
- [ ] AuthInterceptor ajouté à l'application
- [ ] AuthService créé et fonctionnel
- [ ] TransactionService créé avec méthode `importTransactions()`
- [ ] Composant `TransactionImportComponent` créé
- [ ] Route `/transactions/import` ajoutée
- [ ] Fichier `sample-transactions.csv` prêt pour test
- [ ] Test manuel via l'interface d'import

---

## 🧪 Test de l'import depuis le frontend

1. Accéder à `http://localhost:4200`
2. Se connecter ou s'enregistrer
3. Naviguer vers `/transactions/import`
4. Sélectionner `sample-transactions.csv`
5. Cliquer sur "Importer"
6. Vérifier les résultats

---

## 📚 Ressources supplémentaires

- Documentation Angular HttpClient : https://angular.io/guide/http
- Documentation RxJS : https://rxjs.dev/
- Material Design Icons : https://fonts.google.com/icons
- Guide API complet : `API_TEST_GUIDE_COMPLET.md`


