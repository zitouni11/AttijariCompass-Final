# Guide Complet de Résolution des Problèmes
## Attijari Compass - Configuration et Démarrage

---

## 📋 Résumé des Problèmes Identifiés et Corrigés

### ❌ Problèmes Avant Correction
1. **Incohérence des ports** : application.yml configure 8082, mais test-api.ps1 utilisait 8081
2. **Configuration CORS limitée** : seulement `/api/**` était configuré
3. **Base de données PostgreSQL** : nécessaire mais pas toujours en cours d'exécution
4. **Erreur 500 sur les transactions** : causée par l'arrêt du backend

### ✅ Corrections Appliquées
1. ✓ Harmonisé le port à **8082** dans `test-api.ps1`
2. ✓ Amélioré la configuration CORS pour couvrir Swagger et WebSocket
3. ✓ Créé un script de démarrage automatisé `START_PROJECT.ps1`
4. ✓ Validé la configuration PostgreSQL

---

## 🚀 Démarrage Rapide (30 secondes)

### Option 1: Script Automatisé (Recommandé)
```powershell
cd C:\Users\ademz\OneDrive\Bureau\attijari-compass\attijari-compass
.\START_PROJECT.ps1
```

### Option 2: Démarrage Manuel

**Terminal 1 - Backend:**
```powershell
cd C:\Users\ademz\OneDrive\Bureau\attijari-compass\attijari-compass
mvn spring-boot:run
```

**Vérifier que le backend démarre:**
- Attendez le message: `Started AttijariCompassApplication`
- Testez: `curl http://localhost:8082/swagger-ui.html`

---

## 🔧 Configuration Actuelle

### Ports
| Service | Port | URL |
|---------|------|-----|
| Backend Spring Boot | 8082 | http://localhost:8082 |
| Frontend Angular | 4200 | http://localhost:4200 |
| PostgreSQL | 5432 | localhost |

### Identifiants Base de Données
| Paramètre | Valeur |
|-----------|--------|
| Host | localhost |
| Port | 5432 |
| Database | attijari_compass |
| Username | postgres |
| Password | Leaders2003 |

### Configuration Swagger
- URL: `http://localhost:8082/swagger-ui.html`
- OpenAPI Docs: `http://localhost:8082/v3/api-docs`

---

## 🐛 Problèmes Courants et Solutions

### Problème 1: "Port 8082 is already in use"
**Cause**: Une autre instance du backend est en cours d'exécution

**Solution**:
```powershell
# Trouver le processus utilisant le port 8082
netstat -ano | findstr ":8082"

# Arrêter le processus (remplacer PID par le numéro affiché)
Stop-Process -Id PID -Force

# Ou arrêter tous les processus Java
Get-Process java | Stop-Process -Force
```

### Problème 2: "Connection refused" PostgreSQL
**Cause**: PostgreSQL n'est pas en cours d'exécution

**Solution**:
```powershell
# Vérifier l'état de PostgreSQL
netstat -ano | findstr ":5432"

# Si vide, démarrer PostgreSQL via Services Windows
# ou via ligne de commande (si PostgreSQL est installé)
```

### Problème 3: Erreur 500 GET /api/transactions
**Cause**: Le backend n'est pas accessible ou s'est arrêté

**Solution**:
```powershell
# 1. Vérifier si le backend écoute
netstat -ano | findstr ":8082"

# 2. Vérifier les logs du backend
# Cherchez "Started AttijariCompassApplication"

# 3. Redémarrer le backend
.\START_PROJECT.ps1
```

### Problème 4: Erreur 401 Unauthorized
**Cause**: Token JWT absent ou expiré

**Solution**:
```powershell
# 1. Vous connecter d'abord
# POST /api/auth/login avec email et password

# 2. Utiliser le token retourné dans le header:
# Authorization: Bearer {token}
```

---

## ✅ Checklist de Vérification

### Avant de démarrer
- [ ] PostgreSQL est en cours d'exécution (`netstat -ano | findstr ":5432"`)
- [ ] Port 8082 est libre (`netstat -ano | findstr ":8082"`)
- [ ] Maven est installé (`mvn --version`)
- [ ] Java 21+ est disponible (`java --version`)

### Après le démarrage
- [ ] Backend accessible: `http://localhost:8082`
- [ ] Swagger UI accessible: `http://localhost:8082/swagger-ui.html`
- [ ] Enregistrement possible: `POST /api/auth/register`
- [ ] Connexion possible: `POST /api/auth/login`
- [ ] Requête API fonctionne: `GET /api/transactions` (avec token)

### Test Complet
```powershell
# Exécuter le script de test API
.\test-api.ps1
```

---

## 🔍 Vérification Détaillée des Endpoints

### 1. Enregistrement
```bash
curl -X POST http://localhost:8082/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "email": "test@example.com",
    "password": "Test1234!"
  }'
```

### 2. Connexion
```bash
curl -X POST http://localhost:8082/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "email": "test@example.com",
    "password": "Test1234!"
  }'
```

### 3. Récupérer les transactions (avec token)
```bash
curl -X GET http://localhost:8082/api/transactions \
  -H "Authorization: Bearer {TOKEN}"
```

---

## 📊 Architecture du Projet

```
attijari-compass/
├── src/main/java/
│   └── com/adem/attijari_compass/
│       ├── config/          # Configurations (CORS, Swagger, Security)
│       ├── controller/       # REST Controllers
│       ├── service/          # Services métier
│       ├── repository/       # Accès données JPA
│       ├── entity/           # Entités JPA
│       ├── dto/              # Data Transfer Objects
│       ├── security/         # Authentification JWT
│       └── exception/        # Gestion d'exceptions
├── src/main/resources/
│   ├── application.yml       # Configuration Spring (PORT 8082)
│   └── static/               # Fichiers statiques
├── pom.xml                   # Dépendances Maven
├── test-api.ps1             # Script test API (CORRIGÉ: port 8082)
└── START_PROJECT.ps1        # Script démarrage automatisé
```

---

## 🔐 Sécurité

### Configuration CORS
- ✓ Origine autorisée: `http://localhost:*`
- ✓ Endpoints protégés: Auth requise sauf `/api/auth/**`
- ✓ JWT utilisé pour l'authentification
- ✓ Durée de session: Stateless

### Endpoints Publics
```
POST   /api/auth/register    # Enregistrement
POST   /api/auth/login       # Connexion
POST   /api/auth/refresh-token # Rafraîchir token
GET    /swagger-ui/**        # Documentation API
GET    /v3/api-docs/**       # OpenAPI Docs
```

### Endpoints Protégés (Authentification requise)
```
GET    /api/transactions     # Lister les transactions
POST   /api/transactions     # Créer une transaction
PUT    /api/transactions/{id}# Modifier une transaction
DELETE /api/transactions/{id}# Supprimer une transaction
```

---

## 📈 Logs et Debugging

### Voir les logs du backend
```powershell
# Option 1: Afficher le fichier de log
Get-Content backend.log -Tail 50

# Option 2: Suivre les logs en temps réel
Get-Content -Path backend.log -Wait -Tail 50
```

### Logs importants à chercher
- ✓ `Started AttijariCompassApplication in X seconds`
- ✓ `HikariPool initialized` (base de données connectée)
- ✓ `Tomcat started on port 8082` (serveur démarré)

### Erreurs courantes dans les logs
- ❌ `Connection refused` → PostgreSQL non disponible
- ❌ `Address already in use` → Port 8082 utilisé
- ❌ `Failed to validate connection` → Identifiants BD incorrects

---

## 🎯 Prochaines Étapes

### Pour le Frontend Angular (si en cours de développement)
1. Créer le projet Angular avec le proxy configuré
2. Utiliser `proxy.conf.json` pour rediriger `/api` vers `http://localhost:8082`
3. Lancer avec: `ng serve --proxy-config proxy.conf.json`
4. Accéder à: `http://localhost:4200`

### Pour la Production
1. Externaliser les identifiants PostgreSQL (variables d'environnement)
2. Configurer CORS pour les vrais domaines
3. Activer HTTPS
4. Mettre en place les logs structurés (ELK Stack)
5. Configurer les métriques (Prometheus)

---

## 📞 Support Technique

### Points de vérification rapides
1. **Backend actif?** `netstat -ano | findstr ":8082"`
2. **PostgreSQL actif?** `netstat -ano | findstr ":5432"`
3. **Compilé sans erreur?** `mvn clean compile`
4. **Configuration correcte?** Vérifiez `src/main/resources/application.yml`

### Réinitialisation Complète
Si rien ne fonctionne:
```powershell
# 1. Arrêter tous les processus
Get-Process java | Stop-Process -Force
Get-Process node | Stop-Process -Force

# 2. Nettoyer Maven
mvn clean
Remove-Item -Recurse target/ -Force

# 3. Redémarrer
.\START_PROJECT.ps1
```

---

**Dernière mise à jour**: 2026-03-27
**Version du projet**: 0.0.1-SNAPSHOT
**Port de déploiement**: 8082

