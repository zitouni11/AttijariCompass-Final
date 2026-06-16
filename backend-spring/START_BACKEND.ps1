# ===================================================================
# SCRIPT DE DÉMARRAGE COMPLET - ATTIJARI COMPASS
# Configure PostgreSQL, compile et lance le backend
# ===================================================================

Write-Host ""
Write-Host "╔════════════════════════════════════════════════════════════════════════╗" -ForegroundColor Cyan
Write-Host "║                   DÉMARRAGE COMPLET DU PROJET                          ║" -ForegroundColor Cyan
Write-Host "║              Attijari Compass - Backend + Base de Données              ║" -ForegroundColor Cyan
Write-Host "╚════════════════════════════════════════════════════════════════════════╝" -ForegroundColor Cyan
Write-Host ""

# Fonctions utilitaires
function Show-Step { param([string]$msg); Write-Host "▶ $msg" -ForegroundColor Yellow }
function Show-Success { param([string]$msg); Write-Host "✓ $msg" -ForegroundColor Green }
function Show-Error { param([string]$msg); Write-Host "✗ $msg" -ForegroundColor Red }
function Show-Info { param([string]$msg); Write-Host "ℹ $msg" -ForegroundColor Cyan }
function Show-Warning { param([string]$msg); Write-Host "⚠ $msg" -ForegroundColor Magenta }

# ===================================================================
# ÉTAPE 1: ARRÊTER LES PROCESSUS EXISTANTS
# ===================================================================
Write-Host ""
Write-Host "═══════════════════════════════════════════════════════════════════════════" -ForegroundColor Yellow
Write-Host "ÉTAPE 1: NETTOYAGE DES PROCESSUS" -ForegroundColor Yellow
Write-Host "═══════════════════════════════════════════════════════════════════════════" -ForegroundColor Yellow
Write-Host ""

Show-Step "Arrêt des processus Java existants..."
$javaProcs = Get-Process java -ErrorAction SilentlyContinue
if ($javaProcs) {
    $javaProcs | Stop-Process -Force -ErrorAction SilentlyContinue
    Start-Sleep -Seconds 2
    Show-Success "Processus Java arrêtés"
} else {
    Show-Info "Aucun processus Java actif"
}

# ===================================================================
# ÉTAPE 2: VÉRIFIER POSTGRESQL
# ===================================================================
Write-Host ""
Write-Host "═══════════════════════════════════════════════════════════════════════════" -ForegroundColor Yellow
Write-Host "ÉTAPE 2: VÉRIFICATION DE POSTGRESQL" -ForegroundColor Yellow
Write-Host "═══════════════════════════════════════════════════════════════════════════" -ForegroundColor Yellow
Write-Host ""

Show-Step "Vérification de la connectivité PostgreSQL..."
$pgConnection = Test-NetConnection localhost -Port 5432 -WarningAction SilentlyContinue
if ($pgConnection.TcpTestSucceeded) {
    Show-Success "PostgreSQL accessible sur le port 5432"
} else {
    Show-Error "PostgreSQL n'est pas accessible"
    Show-Warning "Actions à faire manuellement:"
    Write-Host "  1. Ouvrez pgAdmin 4 ou Services Windows"
    Write-Host "  2. Démarrez le service PostgreSQL"
    Write-Host "  3. Vérifiez que PostgreSQL écoute sur le port 5432"
    Write-Host "  4. Relancez ce script"
    Write-Host ""
    pause
}

# ===================================================================
# ÉTAPE 3: VÉRIFIER/CRÉER LA BASE DE DONNÉES
# ===================================================================
Write-Host ""
Write-Host "═══════════════════════════════════════════════════════════════════════════" -ForegroundColor Yellow
Write-Host "ÉTAPE 3: VÉRIFICATION DE LA BASE DE DONNÉES" -ForegroundColor Yellow
Write-Host "═══════════════════════════════════════════════════════════════════════════" -ForegroundColor Yellow
Write-Host ""

Show-Step "Vérification de la base de données 'attijari_compass'..."

# Configuration PostgreSQL
$PG_HOST = "localhost"
$PG_PORT = "5432"
$PG_USER = "postgres"
$PG_PASSWORD = "Leaders2003"
$DB_NAME = "attijari_compass"

# Créer la base de données si elle n'existe pas
# Note: Ce script suppose que l'authentification par mot de passe est configurée
$psqlPath = "psql"

# Vérifier si la base de données existe
try {
    $env:PGPASSWORD = $PG_PASSWORD
    $dbList = & $psqlPath -h $PG_HOST -U $PG_USER -l 2>&1 | findstr $DB_NAME

    if ($dbList) {
        Show-Success "Base de données '$DB_NAME' existe"
    } else {
        Show-Warning "Base de données '$DB_NAME' n'existe pas, création..."
        & $psqlPath -h $PG_HOST -U $PG_USER -c "CREATE DATABASE $DB_NAME;" 2>&1
        Show-Success "Base de données '$DB_NAME' créée"
    }
} catch {
    Show-Error "Impossible de créer la base de données"
    Show-Info "Vous pouvez créer manuellement via pgAdmin 4"
    Write-Host "  Créez une database nommée: $DB_NAME" -ForegroundColor Yellow
}

# ===================================================================
# ÉTAPE 4: VÉRIFIER MAVEN
# ===================================================================
Write-Host ""
Write-Host "═══════════════════════════════════════════════════════════════════════════" -ForegroundColor Yellow
Write-Host "ÉTAPE 4: VÉRIFICATION DE MAVEN" -ForegroundColor Yellow
Write-Host "═══════════════════════════════════════════════════════════════════════════" -ForegroundColor Yellow
Write-Host ""

Show-Step "Vérification de Maven..."
$mvnVersion = mvn --version 2>&1 | Select-Object -First 1
if ($?) {
    Show-Success "Maven détecté: $mvnVersion"
} else {
    Show-Error "Maven n'est pas disponible"
    exit 1
}

Show-Step "Vérification de Java..."
$javaVersion = java --version 2>&1 | Select-Object -First 1
if ($?) {
    Show-Success "Java détecté: $javaVersion"
} else {
    Show-Error "Java n'est pas disponible"
    exit 1
}

# ===================================================================
# ÉTAPE 5: COMPILATION MAVEN
# ===================================================================
Write-Host ""
Write-Host "═══════════════════════════════════════════════════════════════════════════" -ForegroundColor Yellow
Write-Host "ÉTAPE 5: COMPILATION DU PROJET" -ForegroundColor Yellow
Write-Host "═══════════════════════════════════════════════════════════════════════════" -ForegroundColor Yellow
Write-Host ""

$projectPath = Get-Location
Show-Step "Compilation du projet dans: $projectPath"
Show-Info "Cette étape peut prendre 2-5 minutes..."
Write-Host ""

$compilation = mvn clean install -DskipTests 2>&1

if ($?) {
    Show-Success "Compilation réussie !"
} else {
    Show-Error "Erreur de compilation"
    Write-Host ""
    Write-Host "Détails de l'erreur :" -ForegroundColor Red
    $compilation | Tail -50 | ForEach-Object { Write-Host $_ -ForegroundColor Red }
    exit 1
}

# ===================================================================
# ÉTAPE 6: DÉMARRAGE DU BACKEND
# ===================================================================
Write-Host ""
Write-Host "═══════════════════════════════════════════════════════════════════════════" -ForegroundColor Yellow
Write-Host "ÉTAPE 6: DÉMARRAGE DU BACKEND" -ForegroundColor Yellow
Write-Host "═══════════════════════════════════════════════════════════════════════════" -ForegroundColor Yellow
Write-Host ""

Show-Info "Configuration du backend:"
Write-Host "  URL: http://localhost:8082" -ForegroundColor Cyan
Write-Host "  Swagger UI: http://localhost:8082/swagger-ui.html" -ForegroundColor Cyan
Write-Host "  Database: attijari_compass" -ForegroundColor Cyan
Write-Host "  Database Port: 5432" -ForegroundColor Cyan
Write-Host ""

Show-Step "Démarrage du backend Spring Boot..."
Write-Host "Cela peut prendre 30-45 secondes..." -ForegroundColor Yellow
Write-Host ""
Write-Host "Cherchez ce message d'activation :" -ForegroundColor Cyan
Write-Host "  'Started AttijariCompassApplication in X seconds'" -ForegroundColor Cyan
Write-Host ""

# Démarrer le backend
mvn spring-boot:run

