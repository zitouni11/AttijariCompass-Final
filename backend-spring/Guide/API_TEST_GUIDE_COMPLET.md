# Guide Complet de Test de l'API Attijari Compass

## 🚀 Démarrage de l'Application

### 1. Préalables
- PostgreSQL en cours d'exécution sur `localhost:5432`
- Base de données `attijari_compass` créée
- Configuration dans `application.yml` correctement définie

### 2. Lancer le backend Spring Boot
```bash
cd attijari-compass
./mvnw spring-boot:run
# OU
java -jar target/attijari-compass-0.0.1-SNAPSHOT.jar
```

Le backend devrait démarrer sur `http://localhost:8081`

### 3. Lancer le frontend Angular
```bash
cd attijari-compass-frontend
ng serve --proxy-config proxy.conf.json
# OU
npm start
```

Le frontend devrait démarrer sur `http://localhost:4200`

---

## 📚 Endpoints API

### 🔐 Authentification

#### 1. **Enregistrement (Register)**
```http
POST /api/auth/register
Content-Type: application/json

{
  "email": "user@example.com",
  "password": "SecurePass123!"
}
```

**Réponse (201 Created):**
```json
{
  "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "refreshToken": "550e8400-e29b-41d4-a716-446655440000",
  "email": "user@example.com",
  "role": "USER"
}
```

---

#### 2. **Connexion (Login)**
```http
POST /api/auth/login
Content-Type: application/json

{
  "email": "user@example.com",
  "password": "SecurePass123!"
}
```

**Réponse (200 OK):**
```json
{
  "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "refreshToken": "550e8400-e29b-41d4-a716-446655440000",
  "email": "user@example.com",
  "role": "USER"
}
```

---

#### 3. **Rafraîchir le Token**
```http
POST /api/auth/refresh-token
Content-Type: application/json

{
  "refreshToken": "550e8400-e29b-41d4-a716-446655440000"
}
```

---

### 👤 Utilisateurs

#### 4. **Obtenir l'utilisateur actuel**
```http
GET /api/users/me
Authorization: Bearer {token}
```

**Réponse (200 OK):**
```json
{
  "id": 1,
  "email": "user@example.com",
  "role": "USER",
  "createdAt": "2026-03-27T00:00:00"
}
```

---

#### 5. **Lister tous les utilisateurs (ADMIN)**
```http
GET /api/users
Authorization: Bearer {admin_token}
```

---

#### 6. **Mettre à jour un utilisateur**
```http
PUT /api/users/{id}
Authorization: Bearer {token}
Content-Type: application/json

{
  "email": "newemail@example.com"
}
```

---

#### 7. **Supprimer un utilisateur (ADMIN)**
```http
DELETE /api/users/{id}
Authorization: Bearer {admin_token}
```

---

### 💳 Transactions

#### 8. **Créer une transaction (Paiement par carte)**
```http
POST /api/transactions/card-payment
Authorization: Bearer {token}
Content-Type: application/json

{
  "merchantName": "Restaurant XYZ",
  "description": "Dîner en famille",
  "amount": 125.50,
  "date": "2026-03-27",
  "cardLast4": "1234"
}
```

**Réponse (201 Created):**
```json
{
  "id": 1,
  "description": "Dîner en famille",
  "merchantName": "Restaurant XYZ",
  "amount": 125.50,
  "date": "2026-03-27",
  "category": "RESTAURATION",
  "type": "DEPENSE",
  "paymentMethod": "CARD",
  "source": "MANUAL_CARD",
  "cardLast4": "1234",
  "createdAt": "2026-03-27T14:30:00"
}
```

---

#### 9. **Créer une transaction (Entrée manuelle)**
```http
POST /api/transactions
Authorization: Bearer {token}
Content-Type: application/json

{
  "description": "Salaire mensuel",
  "amount": 3000.00,
  "date": "2026-03-27",
  "category": "REVENU",
  "type": "REVENU",
  "paymentMethod": "BANK_TRANSFER"
}
```

---

#### 10. **Lister les transactions de l'utilisateur**
```http
GET /api/transactions
Authorization: Bearer {token}
```

**Réponse (200 OK):**
```json
[
  {
    "id": 1,
    "description": "Dîner en famille",
    "merchantName": "Restaurant XYZ",
    "amount": 125.50,
    "date": "2026-03-27",
    "category": "RESTAURATION",
    "type": "DEPENSE",
    "paymentMethod": "CARD",
    "source": "MANUAL_CARD",
    "cardLast4": "1234",
    "createdAt": "2026-03-27T14:30:00"
  }
]
```

---

#### 11. **Obtenir une transaction par ID**
```http
GET /api/transactions/{id}
Authorization: Bearer {token}
```

---

#### 12. **Mettre à jour une transaction**
```http
PUT /api/transactions/{id}
Authorization: Bearer {token}
Content-Type: application/json

{
  "description": "Dîner mise à jour",
  "amount": 130.00,
  "date": "2026-03-27",
  "category": "RESTAURATION",
  "type": "DEPENSE",
  "paymentMethod": "CARD"
}
```

---

#### 13. **Corriger manuellement la catégorie**
```http
PATCH /api/transactions/{id}/category
Authorization: Bearer {token}
Content-Type: application/json

{
  "category": "LOISIRS"
}
```

---

#### 14. **Importer des transactions depuis un fichier** ⭐ **NOUVEAU**
```http
POST /api/transactions/import
Authorization: Bearer {token}
Content-Type: multipart/form-data

file: [fichier CSV ou Excel]
```

**Formats supportés:**

**CSV:**
```csv
date,description,amount,category,type,paymentMethod,merchantName
27/03/2026,Restaurant,125.50,RESTAURATION,DEPENSE,CARD,Restaurant XYZ
27/03/2026,Épicerie,50.00,,DEPENSE,CARD,Carrefour
```

**Excel:** (colonnes dans le même ordre)

**Réponse (201 Created):**
```json
{
  "totalProcessed": 2,
  "successCount": 2,
  "errorCount": 0,
  "errors": [],
  "message": "2 transactions importées avec succès, 0 erreurs"
}
```

---

#### 15. **Supprimer une transaction**
```http
DELETE /api/transactions/{id}
Authorization: Bearer {token}
```

---

### 📊 Dashboard

#### 16. **Obtenir le dashboard financier**
```http
GET /api/dashboard
Authorization: Bearer {token}
```

**Réponse (200 OK):**
```json
{
  "totalBalance": 3000.00,
  "totalIncome": 3000.00,
  "totalExpenses": 125.50,
  "savingsRate": 95.81,
  "savingsGoal": 0.00,
  "savingsProgress": 0.00,
  "categoryBreakdown": {
    "RESTAURATION": 125.50
  }
}
```

---

### 🎯 Objectifs Financiers

#### 17. **Créer un objectif financier**
```http
POST /api/goals
Authorization: Bearer {token}
Content-Type: application/json

{
  "name": "Vacances été",
  "targetAmount": 2000.00,
  "currentAmount": 500.00,
  "deadline": "2026-09-01",
  "description": "Vacances en famille"
}
```

---

#### 18. **Lister les objectifs**
```http
GET /api/goals
Authorization: Bearer {token}
```

---

### 🔄 Recommandations

#### 19. **Obtenir les recommandations**
```http
GET /api/recommendations
Authorization: Bearer {token}
```

---

### 📈 Simulations

#### 20. **Simuler l'épargne**
```http
POST /api/simulations/savings
Authorization: Bearer {token}
Content-Type: application/json

{
  "monthlyAmount": 300.00,
  "targetAmount": 5000.00,
  "currentAmount": 1000.00
}
```

---

### 📖 Storytelling

#### 21. **Obtenir le résumé narratif mensuel**
```http
GET /api/storytelling/monthly
Authorization: Bearer {token}
```

---

## 🧪 Scénario de Test Complet

### Étape 1 : Enregistrement
```bash
curl -X POST http://localhost:8081/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{"email":"test@example.com","password":"Test1234!"}'
```

Mémoriser le token reçu : `{TOKEN}`

### Étape 2 : Connexion
```bash
curl -X POST http://localhost:8081/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"test@example.com","password":"Test1234!"}'
```

### Étape 3 : Obtenir l'utilisateur courant
```bash
curl -X GET http://localhost:8081/api/users/me \
  -H "Authorization: Bearer {TOKEN}"
```

### Étape 4 : Créer une transaction
```bash
curl -X POST http://localhost:8081/api/transactions/card-payment \
  -H "Authorization: Bearer {TOKEN}" \
  -H "Content-Type: application/json" \
  -d '{
    "merchantName": "Restaurant",
    "description": "Dîner",
    "amount": 50.00,
    "date": "2026-03-27",
    "cardLast4": "1234"
  }'
```

### Étape 5 : Lister les transactions
```bash
curl -X GET http://localhost:8081/api/transactions \
  -H "Authorization: Bearer {TOKEN}"
```

### Étape 6 : Importer des transactions 🆕
Créer un fichier `transactions.csv` :
```csv
date,description,amount
27/03/2026,Café,5.00
27/03/2026,Épicerie,50.00
```

```bash
curl -X POST http://localhost:8081/api/transactions/import \
  -H "Authorization: Bearer {TOKEN}" \
  -F "file=@transactions.csv"
```

---

## 🌐 Accès Swagger UI

Une fois l'application démarrée, accédez à:
```
http://localhost:8081/swagger-ui/index.html
```

**Note:** Cliquez sur le bouton **Authorize** 🔒 pour ajouter votre token JWT et tester les endpoints protégés.

---

## 🔑 Comptes de Test

### Utilisateur normal
- **Email:** `user@example.com`
- **Mot de passe:** `User1234!`
- **Rôle:** USER

### Administrateur
- **Email:** `admin@example.com`
- **Mot de passe:** `Admin1234!`
- **Rôle:** ADMIN

---

## ❌ Codes d'Erreur Courants

| Code | Signification |
|------|--------------|
| `400` | Requête invalide (paramètres manquants) |
| `401` | Non authentifié (token manquant/expiré) |
| `403` | Non autorisé (permissions insuffisantes) |
| `404` | Ressource non trouvée |
| `409` | Conflit (email déjà existant) |
| `500` | Erreur serveur |

---

## 📝 Catégories de Transactions Supportées

- `REVENUS` - Revenus
- `SALAIRE` - Salaire
- `RESTAURATION` - Restaurants et cafés
- `TRANSPORT` - Transport et carburant
- `LOISIRS` - Loisirs et divertissement
- `SANTÉ` - Santé et médecin
- `ÉDUCATION` - Éducation
- `LOGEMENT` - Loyer/Hypothèque
- `SERVICES` - Services et abonnements
- `AUTRE` - Autres

---

## 💾 Format d'Import CSV Complet

```csv
date,description,amount,category,type,paymentMethod,merchantName
27/03/2026,Dîner,125.50,RESTAURATION,DEPENSE,CARD,Restaurant XYZ
27/03/2026,Salaire,3000.00,SALAIRE,REVENU,BANK_TRANSFER,Employeur
27/03/2026,Essence,60.00,TRANSPORT,DEPENSE,CARD,Shell
27/03/2026,Cinéma,15.00,LOISIRS,DEPENSE,CARD,Cinéma Palace
```

**Notes:**
- `date` : Format DD/MM/YYYY, DD-MM-YYYY ou YYYY-MM-DD
- `amount` : Montant en décimal (ex: 125.50)
- `category` : Optionnel (si vide, catégorisation automatique)
- `type` : DEPENSE, REVENU, TRANSFERT
- `paymentMethod` : CARD, BANK_TRANSFER, CASH, DIGITAL_WALLET
- Les colonnes manquantes seront remplies avec des valeurs par défaut

---

## 🚀 Prochaines Étapes

1. **Frontend Angular :** Accéder à `http://localhost:4200`
2. **Tests avancés :** Utiliser Postman ou Insomnia pour des scénarios complexes
3. **WebSocket :** Tester les mises à jour temps réel du dashboard
4. **Recommandations :** Générer des recommandations basées sur les comportements
5. **Storytelling :** Voir les résumés narratifs du mois

---

## 📞 Support

En cas de problème:
1. Vérifiez les logs du backend (`target/logs/`)
2. Assurez-vous que PostgreSQL est actif
3. Vérifiez la configuration CORS dans `SecurityConfig.java`
4. Consultez la documentation Swagger


