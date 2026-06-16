# 🚀 GUIDE COMPLET DE RÉSOLUTION - ATTIJARI COMPASS

## 📋 Table des Matières
1. [Vue d'ensemble des problèmes](#vue-densemble)
2. [Prérequis](#prérequis)
3. [Démarrage complet du projet](#démarrage)
4. [Test des endpoints](#tests)
5. [Résolution des problèmes courants](#troubleshooting)

---

## 🔍 Vue d'ensemble des problèmes

### ❌ Problèmes Signalés
1. **Erreur 500 GET /api/transactions**
   - Cause : Le backend n'était pas disponible
   - Solution : Démarrer le backend correctement

2. **Port 8082 déjà en utilisation**
   - Cause : Un processus Java en arrière-plan
   - Solution : Arrêter tous les processus Java et redémarrer

3. **Pas de connexion à la base de données**
   - Cause : PostgreSQL n'était pas accessible ou la BDD n'existait pas
   - Solution : Vérifier et créer la base de données

4. **Frontend ne se connecte pas au backend**
   - Cause : Frontend non configuré ou non démarré
   - Solution : Configurer le frontend Angular avec le proxy

---

## ✅ Prérequis

Avant de démarrer, assurez-vous d'avoir :

### Logiciels Requis
- **Java 21+** : `java --version`
- **Maven 3.6+** : `mvn --version`
- **PostgreSQL 12+** : Service démarré et écoute le port 5432
- **Node.js 18+** (optionnel, pour le frontend) : `node --version`
- **Angular CLI 17+** (optionnel) : `ng --version`

### Configuration PostgreSQL
- **Host** : localhost
- **Port** : 5432
- **Username** : postgres
- **Password** : Leaders2003
- **Database** : attijari_compass

### Vérifier PostgreSQL

```powershell
# Vérifier que PostgreSQL écoute le port 5432
Test-NetConnection localhost -Port 5432 -WarningAction SilentlyContinue

# Résultat attendu:
# TcpTestSucceeded : True
```

Si PostgreSQL n'est pas en cours d'exécution :
1. Ouvrez Services Windows (`services.msc`)
2. Cherchez "postgresql*"
3. Clic droit → Démarrer

---

## 🚀 Démarrage Complet du Projet

### **Option 1 : Démarrage Automatisé (Recommandé)**

```powershell
cd C:\Users\ademz\OneDrive\Bureau\attijari-compass\attijari-compass
.\START_BACKEND.ps1
```

Ce script va automatiquement :
- ✓ Arrêter les processus Java existants
- ✓ Vérifier PostgreSQL
- ✓ Créer la base de données si nécessaire
- ✓ Compiler le projet Maven
- ✓ Démarrer le backend Spring Boot

### **Option 2 : Démarrage Manuel (Pas à Pas)**

#### Étape 1 : Arrêter les processus Java existants

```powershell
Get-Process java -ErrorAction SilentlyContinue | Stop-Process -Force
```

#### Étape 2 : Vérifier PostgreSQL

```powershell
# Vérifier la connexion
Test-NetConnection localhost -Port 5432

# Résultat attendu: TcpTestSucceeded = True
```

#### Étape 3 : Créer la base de données (si nécessaire)

```powershell
# Affectez le mot de passe
$env:PGPASSWORD = "Leaders2003"

# Créez la base de données
psql -h localhost -U postgres -c "CREATE DATABASE attijari_compass;"

# Vérifiez la création
psql -h localhost -U postgres -l | findstr attijari_compass
```

#### Étape 4 : Compiler le projet

```powershell
cd C:\Users\ademz\OneDrive\Bureau\attijari-compass\attijari-compass
mvn clean install -DskipTests
```

#### Étape 5 : Démarrer le backend

```powershell
mvn spring-boot:run
```

**Attendez ce message :**
```
...
Started AttijariCompassApplication in XX.XXX seconds (JVM running for XX.XXX)
```

---

## 🧪 Test des Endpoints

### Test Automatisé (Recommandé)

Une fois le backend démarré, dans une **NOUVELLE** fenêtre PowerShell :

```powershell
cd C:\Users\ademz\OneDrive\Bureau\attijari-compass\attijari-compass
.\TEST_API_COMPLETE.ps1
```

Ce script va tester :
- ✓ Connectivité au backend
- ✓ Enregistrement d'un utilisateur
- ✓ Connexion de l'utilisateur
- ✓ Récupération des transactions
- ✓ Création de nouvelle transaction

### Test Manuel (cURL/PowerShell)

#### 1. Enregistrement

```powershell
$registerBody = @{
    email = "test@example.com"
    password = "Test1234!"
} | ConvertTo-Json

Invoke-WebRequest `
    -Uri "http://localhost:8082/api/auth/register" `
    -Method Post `
    -Body $registerBody `
    -ContentType "application/json"
```

#### 2. Connexion

```powershell
$loginBody = @{
    email = "test@example.com"
    password = "Test1234!"
} | ConvertTo-Json

$loginResponse = Invoke-WebRequest `
    -Uri "http://localhost:8082/api/auth/login" `
    -Method Post `
    -Body $loginBody `
    -ContentType "application/json"

$token = ($loginResponse.Content | ConvertFrom-Json).token
Write-Host "Token: $token"
```

#### 3. Récupérer les transactions

```powershell
$headers = @{
    "Authorization" = "Bearer $token"
}

Invoke-WebRequest `
    -Uri "http://localhost:8082/api/transactions" `
    -Method Get `
    -Headers $headers
```

### Swagger UI

Ouvrez dans votre navigateur :
```
http://localhost:8082/swagger-ui.html
```

Vous pouvez tester tous les endpoints directement depuis l'interface Swagger.

---

## 🔧 Résolution des Problèmes Courants

### ❌ Erreur : "Port 8082 is already in use"

```powershell
# Solution 1 : Arrêter les processus Java
Get-Process java | Stop-Process -Force

# Solution 2 : Trouver le processus spécifique
netstat -ano | findstr ":8082"

# Solution 3 : Arrêter le processus par ID (PID)
Stop-Process -Id <PID> -Force
```

### ❌ Erreur : "Connection refused: PostgreSQL"

```powershell
# Vérifier que PostgreSQL écoute
Test-NetConnection localhost -Port 5432

# Si la connexion échoue, démarrer PostgreSQL via Services Windows
# services.msc → chercher "postgresql" → Démarrer
```

### ❌ Erreur : "Database attijari_compass does not exist"

```powershell
# Créer la base de données
$env:PGPASSWORD = "Leaders2003"
psql -h localhost -U postgres -c "CREATE DATABASE attijari_compass;"

# Vérifier
psql -h localhost -U postgres -l | findstr attijari_compass
```

### ❌ Erreur 500 sur /api/transactions

**Cause possible :** Le backend s'est arrêté ou n'a pas de token

```powershell
# Vérifier que le backend écoute
Test-NetConnection localhost -Port 8082

# Redémarrer le backend
.\START_BACKEND.ps1

# Vérifier les logs pour les erreurs
# Cherchez "ERROR" dans la console du backend
```

### ❌ Erreur 401 Unauthorized

**Cause :** Token JWT manquant ou expiré

```powershell
# Solution : Se reconnecter pour obtenir un nouveau token
$loginBody = @{
    email = "test@example.com"
    password = "Test1234!"
} | ConvertTo-Json

$loginResponse = Invoke-WebRequest `
    -Uri "http://localhost:8082/api/auth/login" `
    -Method Post `
    -Body $loginBody `
    -ContentType "application/json"

# Utiliser le nouveau token
$newToken = ($loginResponse.Content | ConvertFrom-Json).token
```

### ❌ Erreur de compilation Maven

```powershell
# Nettoyer et reconstruire
mvn clean

# Installer les dépendances
mvn install -DskipTests

# Vérifier les erreurs spécifiques
mvn compile
```

---

## 📊 Architecture du Projet

```
attijari-compass/
├── src/main/java/com/adem/attijari_compass/
│   ├── AttijariCompassApplication.java      # Point d'entrée
│   ├── config/                               # Configurations
│   │   ├── AppConfig.java                   # Beans Spring
│   │   ├── CorsConfig.java                  # Configuration CORS
│   │   ├── PasswordConfig.java              # Encodage des mots de passe
│   │   ├── SecurityConfig.java              # Sécurité JWT
│   │   ├── SwaggerConfig.java               # Documentation API
│   │   └── WebSocketConfig.java             # WebSocket
│   ├── controller/                           # REST Controllers
│   │   ├── AuthController.java              # Authentification
│   │   ├── TransactionController.java       # Transactions
│   │   └── ...autres controllers
│   ├── service/                              # Services métier
│   │   ├── AuthService.java                 # Authentification
│   │   ├── TransactionService.java          # Gestion transactions
│   │   └── ...autres services
│   ├── entity/                               # Entités JPA
│   ├── dto/                                  # Data Transfer Objects
│   ├── repository/                           # Accès données
│   ├── security/                             # JWT & Sécurité
│   │   ├── JwtService.java
│   │   ├── JwtAuthenticationFilter.java
│   │   └── SecurityConfig.java
│   └── exception/                            # Gestion d'erreurs
│
├── src/main/resources/
│   ├── application.yml                       # Configuration Spring
│   └── static/                               # Fichiers statiques
│
├── pom.xml                                   # Dépendances Maven
├── proxy.conf.json                           # Proxy pour le frontend
├── START_BACKEND.ps1                         # Démarrage backend
├── TEST_API_COMPLETE.ps1                     # Tests API
└── README.md                                 # Documentation
```

---

## 🔐 Configuration de Sécurité

### JWT (JSON Web Token)

Le projet utilise JWT pour l'authentification :

1. **Enregistrement/Connexion** → Obtenir un token
2. **Token dans le header** : `Authorization: Bearer {token}`
3. **Token valide pendant** : 24 heures (86400000 ms)

### Endpoints Publics (pas de token requis)

```
POST   /api/auth/register
POST   /api/auth/login
POST   /api/auth/refresh-token
GET    /swagger-ui/**
GET    /v3/api-docs/**
GET    /webjars/**
```

### Endpoints Protégés (token requis)

```
GET    /api/transactions
POST   /api/transactions
PUT    /api/transactions/{id}
DELETE /api/transactions/{id}
GET    /api/users
POST   /api/users
... et autres endpoints
```

---

## 📱 Frontend Angular (Optionnel)

Si vous avez un frontend Angular :

### Configuration du Proxy

Le fichier `proxy.conf.json` est déjà configuré pour rediriger les requêtes API vers le backend :

```json
{
  "/api": {
    "target": "http://localhost:8082",
    "secure": false,
    "changeOrigin": true
  }
}
```

### Démarrage du Frontend

```powershell
# Installer les dépendances
npm install

# Démarrer le frontend avec le proxy
ng serve --proxy-config proxy.conf.json

# Ou depuis package.json si configuré
npm start
```

Le frontend sera accessible sur `http://localhost:4200`

---

## ✅ Checklist de Vérification

Avant de considérer le projet comme fonctionnel :

### Vérifications Système
- [ ] PostgreSQL est en cours d'exécution (port 5432)
- [ ] La base de données `attijari_compass` existe
- [ ] Java 21+ est installé
- [ ] Maven 3.6+ est installé
- [ ] Port 8082 est libre

### Vérifications Backend
- [ ] Backend compile sans erreur (`mvn clean install`)
- [ ] Backend démarre sans erreur
- [ ] Message "Started AttijariCompassApplication" s'affiche
- [ ] Swagger UI accessible : `http://localhost:8082/swagger-ui.html`

### Vérifications API
- [ ] Enregistrement fonctionne : `POST /api/auth/register`
- [ ] Connexion fonctionne : `POST /api/auth/login`
- [ ] Token JWT est retourné
- [ ] Transactions accessibles : `GET /api/transactions` (avec token)
- [ ] Créer transaction fonctionne : `POST /api/transactions` (avec token)

### Vérifications Frontend (si applicable)
- [ ] Frontend compile sans erreur
- [ ] Frontend accessible sur `http://localhost:4200`
- [ ] Proxy configuré pour rediriger `/api` vers `http://localhost:8082`
- [ ] Connexion utilisateur fonctionne
- [ ] Affichage des transactions fonctionne

---

## 📞 Support Technique

### Commandes Utiles

```powershell
# Vérifier les processus Java
Get-Process java

# Arrêter tous les processus Java
Get-Process java | Stop-Process -Force

# Vérifier les ports en utilisation
netstat -ano | findstr ":8082"
netstat -ano | findstr ":5432"
netstat -ano | findstr ":4200"

# Vérifier PostgreSQL
Test-NetConnection localhost -Port 5432

# Vérifier la version de Maven
mvn --version

# Vérifier la version de Java
java --version

# Nettoyer le cache Maven
mvn clean
```

### Réinitialisation Complète

Si rien ne fonctionne :

```powershell
# 1. Arrêter tous les processus
Get-Process java | Stop-Process -Force
Get-Process node | Stop-Process -Force

# 2. Nettoyer Maven
mvn clean
Remove-Item -Recurse target/ -Force

# 3. Supprimer les fichiers de cache
Remove-Item -Recurse .m2/repository -Force -ErrorAction SilentlyContinue

# 4. Redémarrer PostgreSQL via Services Windows
# services.msc → postgresql → Redémarrer

# 5. Reconstruire et redémarrer
mvn clean install -DskipTests
.\START_BACKEND.ps1
```

---

## 🎯 Prochaines Étapes

Une fois le projet en fonctionnement :

1. **Développement Frontend** : Créer l'interface Angular
2. **Tests E2E** : Tester les scénarios complets
3. **Déploiement** : Préparer pour la production
4. **Sécurité** : Revue de sécurité complète
5. **Performance** : Optimisation des requêtes

---

**Version** : 1.0  
**Date de création** : 2026-03-27  
**Dernière mise à jour** : 2026-03-27

