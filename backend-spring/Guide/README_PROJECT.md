# 🏦 Attijari Compass - Personal Finance Management System

**Une application fintech complète pour gérer ses finances personnelles de manière intelligente.**

---

## 📋 Table of Contents

1. [Vue d'ensemble](#vue-densemble)
2. [Architecture](#architecture)
3. [Features](#features)
4. [Technologies](#technologies)
5. [Installation](#installation)
6. [API Documentation](#api-documentation)
7. [Development](#development)

---

## 👀 Vue d'ensemble

**Attijari Compass** est une **application fintech de gestion de finances personnelles** (PFM) bâtie avec une **architecture modulaire monolithique** en Spring Boot.

### Objectifs principaux :
- 🔐 **Authentification sécurisée** avec JWT
- 💳 **Gestion intelligente des transactions** avec catégorisation automatique
- 📊 **Dashboard financier** en temps réel
- 🎯 **Suivi des objectifs financiers**
- 💡 **Recommandations financières** basées sur les habitudes

---

## 🏗️ Architecture

### Backend Stack
```
Spring Boot 3.4+
├── Spring Security (JWT)
├── Spring Data JPA (Hibernate)
├── PostgreSQL
└── Spring WebSocket
```

### Frontend Stack (À venir)
```
Angular 17+
├── Bootstrap/Material Design
├── Chart.js (visualisations)
└── RxJS (state management)
```

### Structure modulaire
```
src/main/java/com/adem/attijari_compass/
├── config/           → Configuration Spring
├── controller/       → REST endpoints
├── service/          → Business logic
├── repository/       → Data access
├── entity/           → JPA entities
├── dto/              → DTOs (requests/responses)
├── exception/        → Exception handling
└── security/         → JWT & authentication
```

---

## ✨ Features

### 1️⃣ Authentication & Security
- ✅ Registration avec validation
- ✅ Login avec JWT
- ✅ Refresh tokens (7 jours)
- ✅ Password hashing (BCrypt)
- ✅ Role-based access control (USER, ADMIN)
- ✅ Protected endpoints

### 2️⃣ Transaction Management
- ✅ **Enregistrement de paiements carte**
- ✅ **Auto-catégorisation intelligente**
- ✅ **Correction manuelle des catégories**
- ✅ Métadonnées enrichies (commerçant, carte, source)
- ✅ Full CRUD operations
- ✅ User isolation (chacun voit ses transactions)

### 3️⃣ Financial Goals
- ✅ Création d'objectifs (épargne, investissement)
- ✅ Suivi de progression
- ✅ Calcul de l'épargne nécessaire
- ✅ Ajustement automatique

### 4️⃣ Dashboard
- ✅ Vue globale financière
- ✅ Graphiques par catégorie
- ✅ Stats mensuelles
- ✅ Taux d'épargne calculé

### 5️⃣ Recommandations
- ✅ Recommandations basées sur les dépenses
- ✅ Alertes automatiques
- ✅ Suggestions d'optimisation

### 6️⃣ Storytelling (Innovant)
- ✅ Résumé mensuel narratif
- ✅ Gamification des objectifs
- ✅ Messages personnalisés

---

## 🛠️ Technologies

### Backend
- **Language**: Java 21+
- **Framework**: Spring Boot 3.4.3
- **Database**: PostgreSQL 14+
- **Authentication**: JWT
- **Build**: Maven
- **ORM**: Hibernate/JPA
- **Real-time**: WebSocket
- **Documentation**: Springdoc OpenAPI (Swagger)

### Frontend (coming soon)
- **Language**: TypeScript
- **Framework**: Angular 17+
- **Styling**: Bootstrap 5 / SCSS
- **State**: RxJS
- **HTTP**: Angular HttpClient
- **Charts**: Chart.js / ng2-charts

### DevOps
- **Container**: Docker (planned)
- **CI/CD**: GitHub Actions (planned)
- **Monitoring**: Prometheus/Grafana (future)

---

## 🚀 Installation

### Prérequis
```
Java 21+
Maven 3.8+
PostgreSQL 14+
Node.js 18+ (pour Angular)
Git
```

### Étape 1 : Cloner le repository
```bash
git clone https://github.com/yourusername/Attijari_Compass.git
cd Attijari_Compass
```

### Étape 2 : Configurer la base de données
Créer une base PostgreSQL :
```sql
CREATE DATABASE attijari_compass;
CREATE USER postgres WITH PASSWORD 'Leaders2003';
GRANT ALL PRIVILEGES ON DATABASE attijari_compass TO postgres;
```

### Étape 3 : Configurer les variables d'environnement
Créer `src/main/resources/application.yml` :
```yaml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/attijari_compass
    username: postgres
    password: Leaders2003
    driver-class-name: org.postgresql.Driver
  jpa:
    hibernate:
      ddl-auto: update
    show-sql: true

server:
  port: 8081

jwt:
  secret: 404E635266556A586E3272357538782F413F4428472B4B6250645367566B5970
  expiration: 86400000
```

### Étape 4 : Compiler et démarrer
```bash
# Compiler
.\mvnw.cmd clean compile

# Démarrer le serveur
.\mvnw.cmd spring-boot:run
```

### Vérifier que ça marche
```
✅ Backend démarre sur http://localhost:8081
✅ Swagger UI disponible sur http://localhost:8081/swagger-ui/index.html
✅ PostgreSQL is running
```

---

## 📡 API Documentation

### Accès
- **Swagger UI**: http://localhost:8081/swagger-ui/index.html
- **OpenAPI JSON**: http://localhost:8081/v3/api-docs

### Endpoints principaux

#### Authentication
```
POST   /api/auth/register         → Créer un compte
POST   /api/auth/login            → Se connecter
POST   /api/auth/refresh-token    → Rafraîchir token
```

#### Transactions (NOUVEAU - Auto-catégorisation)
```
POST   /api/transactions/card-payment   → Enregistrer paiement carte (AUTO-CATEGORIZED!)
GET    /api/transactions                → Lister mes transactions
GET    /api/transactions/{id}           → Transaction spécifique
PATCH  /api/transactions/{id}/category  → Corriger catégorie manuelle
PUT    /api/transactions/{id}           → Modifier transaction
DELETE /api/transactions/{id}           → Supprimer transaction
```

#### Dashboard
```
GET    /api/dashboard              → Vue globale financière
```

#### Users
```
GET    /api/users/me               → Informations utilisateur courant
GET    /api/users                  → Tous les users (ADMIN)
PUT    /api/users/{id}             → Modifier user
DELETE /api/users/{id}             → Supprimer user (ADMIN)
```

### Exemple d'utilisation

#### 1. Register
```bash
curl -X POST http://localhost:8081/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "email": "user@example.com",
    "password": "SecurePassword123!",
    "role": "USER"
  }'
```

#### 2. Enregistrer un paiement (AUTO-CATEGORIZED!)
```bash
curl -X POST http://localhost:8081/api/transactions/card-payment \
  -H "Authorization: Bearer YOUR_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "merchantName": "Carrefour Market",
    "amount": 45.50,
    "date": "2026-03-22",
    "description": "Weekly groceries",
    "cardLast4": "1234"
  }'
```

Réponse :
```json
{
  "id": 1,
  "category": "ALIMENTATION",  ← AUTO-DETECTED!
  "merchantName": "Carrefour Market",
  "amount": 45.50,
  "date": "2026-03-22",
  "cardLast4": "1234",
  ...
}
```

#### 3. Corriger la catégorie si besoin
```bash
curl -X PATCH http://localhost:8081/api/transactions/1/category \
  -H "Authorization: Bearer YOUR_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"category": "SHOPPING"}'
```

---

## 📚 Documentation complète

- 📖 **API_TESTING_GUIDE.md** → Guide complet pour tester tous les endpoints
- 📖 **IMPLEMENTATION_SUMMARY.md** → Résumé détaillé des changements
- 📖 **FRONTEND_INSTRUCTIONS.md** → Instructions pour créer le frontend Angular
- 📖 **GIT_COMMIT_GUIDE.md** → Guide pour versionner le code

---

## 🔄 Workflow de développement

### Pour une nouvelle feature
```bash
# 1. Créer une branche
git checkout -b feature/feature-name

# 2. Coder la feature
# 3. Tester avec Swagger
# 4. Committer
git add .
git commit -m "feat(scope): description"

# 5. Push
git push origin feature/feature-name

# 6. Créer une Pull Request
# 7. Merger après review
```

### Convention de noms de branches
```
feature/xxx        → Nouvelle fonctionnalité
bugfix/xxx         → Correction de bug
refactor/xxx       → Refactorisation
docs/xxx           → Documentation
```

---

## 🧪 Tests

### Compiler et tester
```bash
# Compiler uniquement
mvn clean compile

# Compiler + tests
mvn clean test

# Compiler + tests + jar
mvn clean package
```

### Tester avec Swagger UI
1. Aller sur http://localhost:8081/swagger-ui/index.html
2. Cliquer sur "Authorize" (cadenas)
3. Entrer votre token JWT
4. Tester les endpoints

---

## 📝 Conventions de code

### Java
- Naming: `camelCase` pour variables/méthodes, `PascalCase` pour classes
- Annotations: `@NotNull`, `@Valid`, `@Transactional`
- Comments: JavaDoc pour méthodes publiques

### SQL
- Table names: `snake_case`
- Columns: `snake_case`
- Indexes: `idx_{table}_{column}`

### DTOs
- Request DTOs: suffixe `Request`
- Response DTOs: suffixe `Response`

---

## 🔒 Sécurité

### JWT Tokens
- Expiration: 24 heures
- Refresh token: 7 jours
- Secret: 256-bit key
- Algorithm: HS256

### Données sensibles
- Passwords hashés avec BCrypt
- Jamais retourner passwords en API
- HTTPS en production (enforcer)
- CORS configuré

### Validation
- Tous les inputs validés (@Valid)
- SQL injection protection (ORM)
- XSS protection (Jackson)
- CSRF token (à venir)

---

## 📊 Catégories de transactions

```
ALIMENTATION   - Supermarches, épiceries
RESTAURANT     - Restaurants, cafés
TRANSPORT      - Taxis, essence, transit
LOGEMENT       - Loyers, électricité
SANTE          - Pharmacies, médecins
LOISIRS        - Streaming, cinéma, jeux
SHOPPING       - Retail, vêtements
EDUCATION      - Écoles, formations
SALAIRE        - Salaires, revenus
EPARGNE        - Investissements
FACTURES       - Factures, abonnements
AUTRE          - Autres
```

---

## 🎯 Roadmap

### Phase 1 (✅ DONE)
- [x] Setup Spring Boot backend
- [x] Auth with JWT
- [x] User management
- [x] Basic transactions
- [x] **Auto-categorization with card payments**
- [x] Dashboard

### Phase 2 (🚧 IN PROGRESS)
- [ ] Angular frontend
- [ ] Payment form UI
- [ ] Category correction UI
- [ ] Dashboard visualizations

### Phase 3 (📋 PLANNED)
- [ ] Financial goals system
- [ ] Recommendations engine
- [ ] Storytelling module
- [ ] WebSocket real-time updates
- [ ] Mobile app (React Native)

### Phase 4 (🔮 FUTURE)
- [ ] Bank API integration
- [ ] Machine learning recommendations
- [ ] Multi-user families
- [ ] Docker deployment
- [ ] CI/CD pipeline

---

## 🤝 Contributing

1. Fork le repository
2. Créer une branche (`git checkout -b feature/xyz`)
3. Commit vos changements (`git commit -m 'feat(xyz): description'`)
4. Push vers la branche (`git push origin feature/xyz`)
5. Ouvrir une Pull Request

---

## 📄 License

MIT License - Voir LICENSE.md

---

## 👤 Author

- **Adem Zitouni** (@ademz)
- Email: ademzitouni05@gmail.com

---

## 📞 Support

- 📧 Email: support@attijaricompass.com
- 💬 GitHub Issues: [Create an issue](https://github.com/yourusername/Attijari_Compass/issues)
- 📖 Wiki: [Documentation](https://github.com/yourusername/Attijari_Compass/wiki)

---

## 🙏 Acknowledgments

- Spring Boot community
- JWT for security
- PostgreSQL team
- Angular community

---

## 📈 Project Stats

```
Backend:        Java/Spring Boot ✅
Frontend:       Angular (coming soon) ⏳
Database:       PostgreSQL ✅
Authentication: JWT ✅
API Docs:       Swagger ✅
Deployment:     Docker (planned)
```

---

**Last Updated:** 22 March 2026

**Status:** ✅ **STABLE** - Production-Ready Backend + Phase 2 Starting

---

## 🚀 Commencer maintenant

```bash
# Cloner
git clone https://github.com/yourusername/Attijari_Compass.git

# Installer
cd Attijari_Compass
mvn clean compile

# Démarrer
mvn spring-boot:run

# Accéder
Swagger: http://localhost:8081/swagger-ui/index.html
```

**Enjoy building the future of personal finance! 💪**

