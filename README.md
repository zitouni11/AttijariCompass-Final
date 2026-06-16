# 🚀 Attijari Compass

## Présentation

Attijari Compass est une plateforme intelligente de gestion financière personnelle développée dans le cadre d’un Projet de Fin d’Études réalisé chez Attijari Bank.

L’application permet aux utilisateurs de suivre leurs finances, gérer leurs cartes bancaires, analyser leurs habitudes de consommation, définir des objectifs d’épargne et bénéficier d’un accompagnement intelligent grâce à l’intelligence artificielle.

---

## ✨ Fonctionnalités principales

### 🔐 Authentification et sécurité

- Authentification JWT sécurisée
- Gestion des rôles utilisateur et administrateur
- Rafraîchissement automatique des tokens
- Gestion des sessions

### 💳 Gestion des cartes bancaires

- Association de cartes bancaires
- Consultation des cartes liées au compte
- Synchronisation des transactions
- Analyse des dépenses par carte

### 💰 Gestion des transactions

- Import CSV / Excel
- Historique complet des opérations
- Catégorisation automatique des transactions
- Recherche, filtres et statistiques

### 📊 Budgets intelligents

- Création de budgets personnalisés
- Suivi des dépenses par catégorie
- Alertes de dépassement
- Recommandations budgétaires

### 🎯 Objectifs financiers

- Création d’objectifs d’épargne
- Suivi de progression
- Estimation des délais d’atteinte
- Simulations financières

### 🤖 Recommandations IA

- Analyse de la situation financière
- Détection des opportunités d’épargne
- Priorisation des actions à réaliser
- Génération de recommandations personnalisées

### 🧠 Chatbot IA

- Assistant conversationnel intelligent
- Intégration RAG (Retrieval-Augmented Generation)
- Réponses contextualisées basées sur les données utilisateur
- Génération via Groq LLM

### 📈 Business Intelligence

- Dashboard Power BI intégré
- KPIs financiers
- Statistiques d’utilisation de la plateforme
- Supervision Back Office

---

# 🏗️ Architecture technique

## Frontend

- Angular 17
- TypeScript
- Angular Signals
- RxJS
- Bootstrap
- Chart.js

## Backend

- Spring Boot 3
- Java 21
- Spring Security
- JWT
- Spring Data JPA
- PostgreSQL

## Intelligence Artificielle

- FastAPI
- Python
- Machine Learning
- RAG
- Groq API

## Business Intelligence

- Power BI
- PostgreSQL
- KPI Analytics

---

# 📂 Structure du projet

```text
AttijariCompass/
│
├── backend-spring/
│   ├── src/
│   ├── pom.xml
│   └── ...
│
├── frontend-angular/
│   ├── src/
│   ├── package.json
│   └── ...
│
├── powerbi/
│   ├── attijari compass power bi.pbix
│   └── README.md
│
├── docs/
│   ├── uml/
│   └── ...
│
└── README.md
```

---

# ⚙️ Prérequis

- Java 21
- Maven
- Node.js 18+
- npm
- PostgreSQL
- Power BI Desktop

---

# 🚀 Lancement du projet

## Backend

```bash
cd backend-spring
./mvnw spring-boot:run
```

Variables de configuration :

```properties
SPRING_DATASOURCE_URL=
SPRING_DATASOURCE_USERNAME=
SPRING_DATASOURCE_PASSWORD=
JWT_SECRET=
GROQ_API_KEY=
```

---

## Frontend

```bash
cd frontend-angular
npm install
npm start
```

Application disponible sur :

```text
http://localhost:4200
```

---

# 🔨 Build

## Backend

```bash
cd backend-spring
./mvnw clean package
```

## Frontend

```bash
cd frontend-angular
npm run build
```

---

# 📸 Captures d'écran

## Authentification

Ajouter ici les captures du module Login/Register.

## Tableau de bord

Ajouter ici les captures du Dashboard principal.

## Recommandations IA

Ajouter ici les captures du moteur de recommandations.

## Chatbot IA

Ajouter ici les captures du chatbot intelligent.

## Power BI

Ajouter ici les captures du dashboard de supervision.

---

# 👨‍💻 Réalisé par

**Adem Zitouni**
