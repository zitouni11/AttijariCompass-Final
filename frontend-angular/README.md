# 🧭 Attijari Compass — Frontend Angular

Frontend Angular 17 complet pour l'application **Attijari Compass** (gestion de finances personnelles).

---

## 📋 Prérequis

| Outil | Version minimale |
|-------|-----------------|
| Node.js | 18.x ou 20.x |
| npm | 9.x+ |
| Angular CLI | 17.x |
| Backend Spring Boot | Port **8081** |

---

## 🚀 Installation & Lancement

### 1. Cloner / Extraire le projet

```bash
cd attijari-compass-frontend
```

### 2. Installer les dépendances

```bash
npm install
```

### 3. Configurer l'environnement

Vérifier `src/environments/environment.ts` :

```typescript
export const environment = {
  production: false,
  apiUrl: 'http://localhost:8081/api',   // ← URL du backend Spring Boot
  wsUrl: 'http://localhost:8081/ws',
  jwtTokenKey: 'attijari_access_token',
  refreshTokenKey: 'attijari_refresh_token',
  userKey: 'attijari_user'
};
```

### 4. Lancer le backend Spring Boot

Assurez-vous que le backend tourne sur le port **8081** :

```bash
# Dans le dossier attijari-compass (Spring Boot)
./mvnw spring-boot:run
```

### 5. Lancer le frontend Angular

```bash
npm start
# ou
ng serve
```

L'application sera disponible sur : **http://localhost:4200**

---

## 🗂️ Architecture du projet

```
src/
├── app/
│   ├── core/                        # Services globaux, guards, interceptors
│   │   ├── models/
│   │   │   └── index.ts             # Tous les modèles TypeScript
│   │   ├── services/
│   │   │   ├── auth.service.ts      # Authentification JWT
│   │   │   ├── api.services.ts      # Services API (Transaction, Goal, Dashboard...)
│   │   │   ├── notification.service.ts
│   │   │   └── loading.service.ts
│   │   ├── interceptors/
│   │   │   ├── jwt.interceptor.ts   # Injection automatique du token Bearer
│   │   │   └── loading.interceptor.ts
│   │   └── guards/
│   │       └── auth.guard.ts        # AuthGuard, AdminGuard, GuestGuard
│   │
│   ├── shared/                      # Composants réutilisables
│   │   └── components/
│   │       ├── layout/              # Layout principal (sidebar + header)
│   │       ├── loader/              # Barre de chargement globale
│   │       └── notification/        # Toasts de notification
│   │
│   ├── features/                    # Modules fonctionnels (lazy loaded)
│   │   ├── auth/
│   │   │   ├── login/               # Page de connexion
│   │   │   └── register/            # Page d'inscription
│   │   ├── dashboard/               # Tableau de bord avec KPIs
│   │   ├── transactions/
│   │   │   ├── list/                # Liste des transactions
│   │   │   └── form/                # Formulaire (manuel + carte)
│   │   ├── goals/
│   │   │   ├── list/                # Liste des objectifs
│   │   │   └── form/                # Formulaire objectif
│   │   ├── recommendations/         # Recommandations IA
│   │   ├── simulations/             # Simulateurs épargne & crédit
│   │   ├── storytelling/            # Histoire financière mensuelle
│   │   ├── users/                   # Gestion utilisateurs (ADMIN)
│   │   └── profile/                 # Profil utilisateur
│   │
│   ├── app.routes.ts                # Routing principal (lazy loading)
│   ├── app.config.ts                # Configuration Angular
│   └── app.component.ts             # Composant racine
│
├── environments/
│   ├── environment.ts               # Dev config
│   └── environment.prod.ts          # Prod config
├── styles.scss                      # Styles globaux
└── index.html                       # HTML principal
```

---

## 🔗 Mapping API ↔ Frontend

| Module | Endpoint Backend | Service Angular |
|--------|-----------------|-----------------|
| Auth | `POST /api/auth/login` | `AuthService.login()` |
| Auth | `POST /api/auth/register` | `AuthService.register()` |
| Auth | `POST /api/auth/refresh-token` | `AuthService.refreshToken()` |
| Dashboard | `GET /api/dashboard` | `DashboardService.getDashboard()` |
| Transactions | `GET /api/transactions` | `TransactionService.getAll()` |
| Transactions | `POST /api/transactions` | `TransactionService.create()` |
| Transactions | `POST /api/transactions/card-payment` | `TransactionService.createCardPayment()` |
| Transactions | `PUT /api/transactions/{id}` | `TransactionService.update()` |
| Transactions | `PATCH /api/transactions/{id}/category` | `TransactionService.updateCategory()` |
| Transactions | `DELETE /api/transactions/{id}` | `TransactionService.delete()` |
| Goals | `GET /api/goals` | `GoalService.getAll()` |
| Goals | `POST /api/goals` | `GoalService.create()` |
| Goals | `PUT /api/goals/{id}` | `GoalService.update()` |
| Goals | `PATCH /api/goals/{id}/progress` | `GoalService.addProgress()` |
| Goals | `DELETE /api/goals/{id}` | `GoalService.delete()` |
| Recommendations | `GET /api/recommendations` | `RecommendationService.getRecommendations()` |
| Simulations | `POST /api/simulations/savings` | `SimulationService.simulateSavings()` |
| Simulations | `POST /api/simulations/credit` | `SimulationService.simulateCredit()` |
| Storytelling | `GET /api/storytelling/monthly` | `StorytellingService.getMonthlyStory()` |
| Users | `GET /api/users/me` | `UserService.getMe()` |
| Users | `GET /api/users` *(ADMIN)* | `UserService.getAll()` |
| Users | `PUT /api/users/{id}` | `UserService.update()` |
| Users | `DELETE /api/users/{id}` *(ADMIN)* | `UserService.delete()` |

---

## 🔐 Sécurité & JWT

- **Storage** : `localStorage` (clés configurables dans `environment.ts`)
- **Interceptor** : Injection automatique du header `Authorization: Bearer <token>`
- **Refresh Token** : Tentative automatique de renouvellement en cas de 401
- **Guards** :
  - `authGuard` → Protège toutes les routes privées
  - `adminGuard` → Réservé au rôle ADMIN (page utilisateurs)
  - `guestGuard` → Redirige vers le dashboard si déjà connecté

---

## 🎨 UI & Design

- **Police** : [Sora](https://fonts.google.com/specimen/Sora) (Google Fonts)
- **Couleurs** :
  - Primaire : `#0a1628` (bleu marine)
  - Accent : `#ffbc00` (or Attijari)
  - Succès : `#10b981` | Erreur : `#ef4444` | Alerte : `#f59e0b`
- **Design system** : CSS pur (Standalone Components, pas de dépendance UI externe)
- **Responsive** : Mobile-first avec breakpoints 768px / 1024px / 1200px

---

## 🏗️ Build de production

```bash
# Build optimisé
ng build --configuration production

# Les fichiers sont dans dist/attijari-compass-frontend/
```

### Déploiement sur nginx (exemple) :

```nginx
server {
    listen 80;
    root /var/www/attijari-compass;
    index index.html;

    location / {
        try_files $uri $uri/ /index.html;
    }

    location /api {
        proxy_pass http://localhost:8081;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
    }
}
```

---

## 🧪 Compte de test

Créez un compte via la page `/auth/register` :

```
Email    : admin@attijari.com
Password : admin123
Role     : ADMIN
```

---

## 📦 Dépendances principales

| Package | Version | Usage |
|---------|---------|-------|
| `@angular/core` | 17.3 | Framework principal |
| `@angular/router` | 17.3 | Routing avec lazy loading |
| `@angular/forms` | 17.3 | Reactive Forms |
| `@angular/common/http` | 17.3 | Client HTTP + interceptors |
| `rxjs` | 7.8 | Gestion des streams |
| `zone.js` | 0.14 | Change detection |

---

## ❓ Problèmes courants

**CORS Error** → Vérifiez que le backend tourne sur le port 8081 et que CORS est configuré pour `http://localhost:4200`.

**401 Unauthorized** → Le token a expiré. L'interceptor tentera un refresh automatique. Sinon, reconnectez-vous.

**404 Not Found** → Vérifiez l'URL de l'API dans `environment.ts`.

**Module not found** → Lancez `npm install` pour installer les dépendances manquantes.
