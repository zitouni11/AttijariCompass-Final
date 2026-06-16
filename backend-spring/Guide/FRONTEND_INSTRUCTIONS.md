# 🎯 INSTRUCTIONS FRONTEND ANGULAR - PROCHAINES ÉTAPES

## 📋 Roadmap Frontend

Le backend est maintenant **prêt et optimisé pour fintech**. Voici exactement ce que tu dois faire côté Angular.

---

## 🔧 Phase 1 : Configuration Angular + Intégration Backend

### Étape 1.1 : Créer le projet Angular
```bash
ng new attijari-compass-frontend --routing --style=scss
cd attijari-compass-frontend
```

### Étape 1.2 : Installer les dépendances essentielles
```bash
npm install @angular/common @angular/forms @angular/http
npm install axios  # ou utiliser HttpClient d'Angular
npm install ngx-bootstrap  # ou Angular Material
npm install chart.js ng2-charts  # Pour les graphiques
npm install date-fns  # Gestion des dates
```

### Étape 1.3 : Configurer l'environnement
Créer `src/environments/environment.ts` :
```typescript
export const environment = {
  production: false,
  apiUrl: 'http://localhost:8081/api'
};
```

### Étape 1.4 : Créer le service d'API
Créer `src/app/services/api.service.ts` :
```typescript
import { Injectable } from '@angular/core';
import { HttpClient, HttpHeaders } from '@angular/common/http';
import { environment } from '../../environments/environment';
import { Observable } from 'rxjs';

@Injectable({
  providedIn: 'root'
})
export class ApiService {
  private apiUrl = environment.apiUrl;
  private token: string | null = localStorage.getItem('token');

  constructor(private http: HttpClient) {}

  private getHeaders() {
    return new HttpHeaders({
      'Authorization': `Bearer ${this.token}`,
      'Content-Type': 'application/json'
    });
  }

  // AUTH
  register(email: string, password: string, role: string) {
    return this.http.post(`${this.apiUrl}/auth/register`, {
      email, password, role
    });
  }

  login(email: string, password: string) {
    return this.http.post(`${this.apiUrl}/auth/login`, {
      email, password
    });
  }

  // TRANSACTIONS - NOUVEAU ENDPOINT
  createCardPayment(payment: any) {
    return this.http.post(
      `${this.apiUrl}/transactions/card-payment`,
      payment,
      { headers: this.getHeaders() }
    );
  }

  // TRANSACTIONS - ANCIEN ENDPOINT (compatibilité)
  createTransaction(transaction: any) {
    return this.http.post(
      `${this.apiUrl}/transactions`,
      transaction,
      { headers: this.getHeaders() }
    );
  }

  getTransactions(): Observable<any[]> {
    return this.http.get<any[]>(
      `${this.apiUrl}/transactions`,
      { headers: this.getHeaders() }
    );
  }

  getTransaction(id: number): Observable<any> {
    return this.http.get<any>(
      `${this.apiUrl}/transactions/${id}`,
      { headers: this.getHeaders() }
    );
  }

  // NOUVEAU ENDPOINT - Corriger catégorie
  updateTransactionCategory(id: number, category: string) {
    return this.http.patch(
      `${this.apiUrl}/transactions/${id}/category`,
      { category },
      { headers: this.getHeaders() }
    );
  }

  updateTransaction(id: number, transaction: any) {
    return this.http.put(
      `${this.apiUrl}/transactions/${id}`,
      transaction,
      { headers: this.getHeaders() }
    );
  }

  deleteTransaction(id: number) {
    return this.http.delete(
      `${this.apiUrl}/transactions/${id}`,
      { headers: this.getHeaders() }
    );
  }

  // DASHBOARD
  getDashboard(): Observable<any> {
    return this.http.get<any>(
      `${this.apiUrl}/dashboard`,
      { headers: this.getHeaders() }
    );
  }
}
```

---

## 📱 Phase 2 : Components essentiels à créer

### Composant 2.1 : Payment Card Form
**Fichier:** `src/app/components/payment-form/payment-form.component.ts`

```typescript
import { Component, OnInit } from '@angular/core';
import { ApiService } from '../../services/api.service';
import { FormBuilder, FormGroup, Validators } from '@angular/forms';

@Component({
  selector: 'app-payment-form',
  templateUrl: './payment-form.component.html',
  styleUrls: ['./payment-form.component.scss']
})
export class PaymentFormComponent implements OnInit {
  paymentForm: FormGroup;
  autoCategory: string | null = null;
  loading = false;
  error = '';

  constructor(
    private apiService: ApiService,
    private fb: FormBuilder
  ) {
    this.paymentForm = this.fb.group({
      merchantName: ['', [Validators.required]],
      amount: ['', [Validators.required, Validators.min(0.01)]],
      date: [new Date().toISOString().split('T')[0], Validators.required],
      description: ['', Validators.maxLength(255)],
      cardLast4: ['', [Validators.required, Validators.pattern(/^\d{4}$/)]]
    });
  }

  ngOnInit() {}

  onSubmit() {
    if (this.paymentForm.valid) {
      this.loading = true;
      this.error = '';

      this.apiService.createCardPayment(this.paymentForm.value).subscribe(
        (response: any) => {
          // 🎯 Afficher la catégorie auto-détectée !
          this.autoCategory = response.category;
          alert(`✅ Paiement enregistré ! Catégorie: ${response.category}`);
          this.paymentForm.reset();
          this.loading = false;
        },
        (err) => {
          this.error = err.error?.message || 'Erreur lors de l\'enregistrement';
          this.loading = false;
        }
      );
    }
  }
}
```

**Template:** `payment-form.component.html`
```html
<div class="payment-form-container">
  <h2>💳 Enregistrer un paiement</h2>
  
  <form [formGroup]="paymentForm" (ngSubmit)="onSubmit()">
    
    <div class="form-group">
      <label>Nom du commerçant</label>
      <input 
        type="text" 
        formControlName="merchantName" 
        placeholder="Ex: Carrefour Market"
        class="form-control"
      >
    </div>

    <div class="form-group">
      <label>Montant (€)</label>
      <input 
        type="number" 
        formControlName="amount" 
        placeholder="Ex: 45.50"
        class="form-control"
        step="0.01"
      >
    </div>

    <div class="form-group">
      <label>Date</label>
      <input 
        type="date" 
        formControlName="date" 
        class="form-control"
      >
    </div>

    <div class="form-group">
      <label>Description (optionnel)</label>
      <textarea 
        formControlName="description" 
        placeholder="Ex: Weekly groceries"
        class="form-control"
      ></textarea>
    </div>

    <div class="form-group">
      <label>Derniers 4 chiffres carte</label>
      <input 
        type="text" 
        formControlName="cardLast4" 
        placeholder="Ex: 1234"
        class="form-control"
        maxlength="4"
      >
    </div>

    <button 
      type="submit" 
      class="btn btn-primary"
      [disabled]="loading || !paymentForm.valid"
    >
      {{ loading ? 'Enregistrement...' : 'Enregistrer paiement' }}
    </button>

    <div *ngIf="autoCategory" class="alert alert-success">
      🎯 Catégorie détectée: <strong>{{ autoCategory }}</strong>
    </div>

    <div *ngIf="error" class="alert alert-danger">
      ❌ {{ error }}
    </div>
  </form>
</div>
```

---

### Composant 2.2 : Transactions List avec correction de catégorie
**Fichier:** `src/app/components/transactions-list/transactions-list.component.ts`

```typescript
import { Component, OnInit } from '@angular/core';
import { ApiService } from '../../services/api.service';

@Component({
  selector: 'app-transactions-list',
  templateUrl: './transactions-list.component.html',
  styleUrls: ['./transactions-list.component.scss']
})
export class TransactionsListComponent implements OnInit {
  transactions: any[] = [];
  loading = true;
  error = '';
  editingCategoryId: number | null = null;
  categories = [
    'ALIMENTATION', 'RESTAURANT', 'TRANSPORT', 'LOGEMENT',
    'SANTE', 'LOISIRS', 'SHOPPING', 'EDUCATION',
    'SALAIRE', 'EPARGNE', 'FACTURES', 'AUTRE'
  ];

  constructor(private apiService: ApiService) {}

  ngOnInit() {
    this.loadTransactions();
  }

  loadTransactions() {
    this.apiService.getTransactions().subscribe(
      (data) => {
        this.transactions = data;
        this.loading = false;
      },
      (err) => {
        this.error = 'Erreur lors du chargement des transactions';
        this.loading = false;
      }
    );
  }

  // 🎯 NOUVELLE FONCTIONNALITÉ: Corriger la catégorie
  updateCategory(transactionId: number, newCategory: string) {
    this.apiService.updateTransactionCategory(transactionId, newCategory)
      .subscribe(
        (response) => {
          const tx = this.transactions.find(t => t.id === transactionId);
          if (tx) {
            tx.category = newCategory;
          }
          this.editingCategoryId = null;
          alert('✅ Catégorie mise à jour !');
        },
        (err) => alert('❌ Erreur: ' + err.error?.message)
      );
  }

  deleteTransaction(id: number) {
    if (confirm('Voulez-vous vraiment supprimer cette transaction ?')) {
      this.apiService.deleteTransaction(id).subscribe(
        () => {
          this.transactions = this.transactions.filter(t => t.id !== id);
          alert('✅ Transaction supprimée !');
        },
        (err) => alert('❌ Erreur: ' + err.error?.message)
      );
    }
  }
}
```

**Template:** `transactions-list.component.html`
```html
<div class="transactions-container">
  <h2>📊 Mes transactions</h2>

  <div *ngIf="loading" class="alert alert-info">
    Chargement...
  </div>

  <div *ngIf="error" class="alert alert-danger">
    ❌ {{ error }}
  </div>

  <table class="table" *ngIf="!loading && transactions.length > 0">
    <thead>
      <tr>
        <th>Commerçant</th>
        <th>Montant</th>
        <th>Date</th>
        <th>Catégorie</th>
        <th>Carte</th>
        <th>Actions</th>
      </tr>
    </thead>
    <tbody>
      <tr *ngFor="let tx of transactions">
        <td>{{ tx.merchantName || tx.description }}</td>
        <td>{{ tx.amount }}€</td>
        <td>{{ tx.date }}</td>
        <td>
          <!-- 🎯 CORRECTION DE CATÉGORIE -->
          <div *ngIf="editingCategoryId !== tx.id">
            <span class="badge" [ngClass]="getCategoryClass(tx.category)">
              {{ tx.category }}
            </span>
            <button 
              class="btn btn-sm btn-warning ms-2"
              (click)="editingCategoryId = tx.id"
            >
              Modifier
            </button>
          </div>

          <div *ngIf="editingCategoryId === tx.id">
            <select 
              class="form-select"
              (change)="updateCategory(tx.id, $event.target.value)"
            >
              <option value="">-- Sélectionner --</option>
              <option *ngFor="let cat of categories" [value]="cat">
                {{ cat }}
              </option>
            </select>
          </div>
        </td>
        <td>{{ tx.cardLast4 ? '****' + tx.cardLast4 : '-' }}</td>
        <td>
          <button 
            class="btn btn-sm btn-danger"
            (click)="deleteTransaction(tx.id)"
          >
            Supprimer
          </button>
        </td>
      </tr>
    </tbody>
  </table>

  <div *ngIf="!loading && transactions.length === 0" class="alert alert-warning">
    Aucune transaction. Enregistrez votre premier paiement !
  </div>
</div>
```

---

### Composant 2.3 : Dashboard amélioré
**Afficher les nouvelles données de paiement:**

```typescript
getDashboard() {
  this.apiService.getDashboard().subscribe((data) => {
    this.dashboard = data;
    // Afficher les stats + graphiques
  });
}
```

---

## 🎨 Phase 3 : UI/UX - Ce qu'il faut afficher

### Dashboard
- [ ] Solde total
- [ ] Dépenses ce mois
- [ ] Taux d'épargne
- [ ] Graphique par catégorie (pie chart)
- [ ] Graphique temporel (line chart)

### Transactions
- [ ] Liste avec merchant name
- [ ] Derniers 4 chiffres carte
- [ ] **Catégorie (avec correction possible)**
- [ ] Source de transaction (BANK_API, MANUAL_CARD)
- [ ] Bouton supprimer

### Form de paiement
- [ ] Input merchant name
- [ ] Input montant
- [ ] Input date
- [ ] Input last 4 digits
- [ ] **Afficher catégorie auto-détectée après submit**
- [ ] Possibilité de corriger immédiatement

---

## 🧪 Phase 4 : Tests à faire

### Test 1 : Enregistrer un paiement carte
```
✅ POST /api/transactions/card-payment
   Merchant: "Carrefour"
   → Doit recevoir category: "ALIMENTATION"
```

### Test 2 : Corriger la catégorie
```
✅ PATCH /api/transactions/{id}/category
   New category: "SHOPPING"
   → Category doit changer dans la liste
```

### Test 3 : Voir le dashboard
```
✅ GET /api/dashboard
   → Affiche stats + breakdown par catégorie
```

---

## 📂 Structure de dossiers recommandée

```
src/
├── app/
│   ├── components/
│   │   ├── payment-form/
│   │   ├── transactions-list/
│   │   ├── dashboard/
│   │   ├── login/
│   │   └── register/
│   ├── services/
│   │   ├── api.service.ts
│   │   ├── auth.service.ts
│   │   └── transaction.service.ts
│   ├── models/
│   │   ├── transaction.model.ts
│   │   ├── user.model.ts
│   │   └── dashboard.model.ts
│   ├── guards/
│   │   └── auth.guard.ts
│   └── app.module.ts
└── assets/
    └── styles/
```

---

## 🔐 Sécurité important

```typescript
// Toujours stocker le token après login
localStorage.setItem('token', response.token);

// Ajouter aux headers d'EVERY requête
Authorization: Bearer ${this.token}

// Interceptor recommandé
@Injectable()
export class AuthInterceptor implements HttpInterceptor {
  constructor(private auth: AuthService) {}
  
  intercept(req, next) {
    const token = localStorage.getItem('token');
    if (token) {
      req = req.clone({
        setHeaders: { Authorization: `Bearer ${token}` }
      });
    }
    return next.handle(req);
  }
}
```

---

## 📌 Checklist avant de commencer

- [ ] Angular CLI installé (`ng version`)
- [ ] Backend tourne sur `http://localhost:8081`
- [ ] Projet Angular créé
- [ ] HttpClientModule importé dans AppModule
- [ ] Environnement configuré
- [ ] ApiService créé

---

## ⏭️ Ordre d'implémentation recommandé

1. **Configuration** - Projet Angular + ApiService
2. **Auth** - Login/Register (tu as déjà l'interface)
3. **Payment Form** - Enregistrer paiement (NOUVEAU)
4. **Transactions List** - Afficher + corriger catégorie (NOUVEAU)
5. **Dashboard** - Afficher données enrichies
6. **Styling** - Bootstrap/Material/Custom CSS
7. **Tests** - Vérifier tous les endpoints

---

## 🎯 Focus sur les nouveautés

Les changements **majeurs** vs avant :

```
❌ AVANT:  User doit choisir la catégorie manuellement
✅ APRÈS:  Catégorie auto-détectée ! User peut corriger si besoin

❌ AVANT:  Juste "description" générique
✅ APRÈS:  Merchant Name + Card Last 4 + Source de paiement

❌ AVANT:  Flux pas réaliste pour fintech
✅ APRÈS:  Flux comme une vraie app bancaire
```

---

## 📞 Questions fréquentes

**Q: Et si la catégorisation auto est mauvaise ?**
A: L'utilisateur utilise `PATCH /api/transactions/{id}/category` pour corriger

**Q: Comment afficher les anciens paiements ?**
A: GET /api/transactions retourne tout avec les nouveaux champs

**Q: Dois-je créer les users ?**
A: Oui, via POST /api/auth/register d'abord

**Q: Comment tester sans frontend d'abord ?**
A: Utilise Swagger UI : http://localhost:8081/swagger-ui/index.html

---

## 🚀 C'est parti !

Tu es maintenant prêt pour créer le frontend Angular ! 🎉

Les endpoints sont documentés, les DTOs sont ready, l'API est stable.

**Vas-y ! Crée l'interface ! 💪**

