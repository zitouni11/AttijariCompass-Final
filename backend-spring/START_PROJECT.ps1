# =========================================
# Script de démarrage complet du projet
# Attijari Compass (Backend + Frontend)
# =========================================

Write-Host "╔════════════════════════════════════════════════════════════════╗" -ForegroundColor Cyan
Write-Host "║        ATTIJARI COMPASS - DÉMARRAGE COMPLET DU PROJET         ║" -ForegroundColor Cyan
Write-Host "╚════════════════════════════════════════════════════════════════╝" -ForegroundColor Cyan
Write-Host ""

# Variables de configuration
$BACKEND_PORT = 8082
$FRONTEND_PORT = 4200
$DB_PORT = 5432
$CURRENT_PATH = Get-Location

# Fonction pour afficher les étapes
function Show-Step {
    param([string]$message)
    Write-Host "▶ $message" -ForegroundColor Yellow
}

function Show-Success {
    param([string]$message)
    Write-Host "✓ $message" -ForegroundColor Green
}

function Show-Error {
    param([string]$message)
    Write-Host "✗ $message" -ForegroundColor Red
}

function Show-Info {
    param([string]$message)
    Write-Host "ℹ $message" -ForegroundColor Cyan
}

# Vérifications préalables
Write-Host ""
Write-Host "═══════════════════════════════════════════════════════════════" -ForegroundColor Cyan
Write-Host "ÉTAPE 1: VÉRIFICATIONS PRÉALABLES" -ForegroundColor Cyan
Write-Host "═══════════════════════════════════════════════════════════════" -ForegroundColor Cyan
Write-Host ""

Show-Step "Vérification de PostgreSQL..."
$pgCheck = netstat -ano | findstr ":$DB_PORT"
if ($pgCheck) {
    Show-Success "PostgreSQL est en cours d'exécution sur le port $DB_PORT"
} else {
    Show-Error "PostgreSQL n'est pas accessible sur le port $DB_PORT"
    Show-Info "Veuillez démarrer PostgreSQL avant de continuer"
    exit 1
}

Show-Step "Vérification du port $BACKEND_PORT..."
$portCheck = netstat -ano | findstr ":$BACKEND_PORT"
if ($portCheck) {
    Show-Error "Le port $BACKEND_PORT est déjà utilisé"
    Show-Info "Veuillez arrêter le processus qui utilise ce port"
    exit 1
} else {
    Show-Success "Le port $BACKEND_PORT est libre"
}

Show-Step "Vérification du port $FRONTEND_PORT..."
$portCheck = netstat -ano | findstr ":$FRONTEND_PORT"
if ($portCheck) {
    Show-Error "Le port $FRONTEND_PORT est déjà utilisé"
    Show-Info "Veuillez arrêter le processus qui utilise ce port"
} else {
    Show-Success "Le port $FRONTEND_PORT est libre"
}

# Vérification de Maven
Show-Step "Vérification de Maven..."
try {
    $mvnVersion = mvn --version 2>&1 | Select-Object -First 1
    Show-Success "Maven trouvé: $mvnVersion"
} catch {
    Show-Error "Maven n'est pas installé ou non accessible"
    exit 1
}

# Compilation du projet
Write-Host ""
Write-Host "═══════════════════════════════════════════════════════════════" -ForegroundColor Cyan
Write-Host "ÉTAPE 2: COMPILATION DU PROJET" -ForegroundColor Cyan
Write-Host "═══════════════════════════════════════════════════════════════" -ForegroundColor Cyan
Write-Host ""

Show-Step "Nettoyage du projet..."
mvn clean -q
Show-Success "Nettoyage terminé"

Show-Step "Compilation en cours (cela peut prendre quelques minutes)..."
mvn compile -q
if ($LASTEXITCODE -eq 0) {
    Show-Success "Compilation réussie"
} else {
    Show-Error "Erreur lors de la compilation"
    exit 1
}

# Démarrage du backend
Write-Host ""
Write-Host "═══════════════════════════════════════════════════════════════" -ForegroundColor Cyan
Write-Host "ÉTAPE 3: DÉMARRAGE DU BACKEND" -ForegroundColor Cyan
Write-Host "═══════════════════════════════════════════════════════════════" -ForegroundColor Cyan
Write-Host ""

Show-Step "Démarrage de l'application Spring Boot sur le port $BACKEND_PORT..."
Show-Info "Attendez le message 'Started AttijariCompassApplication' pour continuer"
Show-Info "Le backend écoutera sur: http://localhost:$BACKEND_PORT"
Write-Host ""

# Démarrer le backend en arrière-plan
Start-Process -FilePath "mvn" -ArgumentList "spring-boot:run" -NoNewWindow -RedirectStandardOutput "$CURRENT_PATH\backend.log" -RedirectStandardError "$CURRENT_PATH\backend-error.log"
Start-Sleep -Seconds 15

# Vérifier si le backend a démarré correctement
$backendCheck = netstat -ano | findstr ":$BACKEND_PORT"
if ($backendCheck) {
    Show-Success "Backend démarré avec succès sur le port $BACKEND_PORT"
} else {
    Show-Error "Impossible de démarrer le backend"
    Show-Info "Vérifiez les logs: $CURRENT_PATH\backend.log"
    exit 1
}

# Attendre et afficher un résumé
Write-Host ""
Write-Host "═══════════════════════════════════════════════════════════════" -ForegroundColor Cyan
Write-Host "✓ DÉMARRAGE RÉUSSI" -ForegroundColor Green
Write-Host "═══════════════════════════════════════════════════════════════" -ForegroundColor Cyan
Write-Host ""

Write-Host "📊 ENDPOINTS DISPONIBLES:" -ForegroundColor Cyan
Write-Host ""
Write-Host "🔧 API Backend:" -ForegroundColor Yellow
Write-Host "   • API: http://localhost:$BACKEND_PORT/api" -ForegroundColor White
Write-Host "   • Swagger UI: http://localhost:$BACKEND_PORT/swagger-ui.html" -ForegroundColor White
Write-Host "   • OpenAPI Docs: http://localhost:$BACKEND_PORT/v3/api-docs" -ForegroundColor White
Write-Host ""

Write-Host "📝 ACTIONS SUIVANTES:" -ForegroundColor Cyan
Write-Host ""
Write-Host "1️⃣  TESTER L'API DIRECTEMENT:" -ForegroundColor Yellow
Write-Host "   .\test-api.ps1" -ForegroundColor White
Write-Host ""
Write-Host "2️⃣  ACCÉDER À LA DOCUMENTATION SWAGGER:" -ForegroundColor Yellow
Write-Host "   http://localhost:$BACKEND_PORT/swagger-ui.html" -ForegroundColor White
Write-Host ""
Write-Host "3️⃣  VOIR LES LOGS:" -ForegroundColor Yellow
Write-Host "   tail -f $CURRENT_PATH\backend.log" -ForegroundColor White
Write-Host ""

Write-Host "═══════════════════════════════════════════════════════════════" -ForegroundColor Cyan
Write-Host "ℹ Le processus continue de s'exécuter en arrière-plan" -ForegroundColor Cyan
Write-Host "═══════════════════════════════════════════════════════════════" -ForegroundColor Cyan
Write-Host ""
Write-Host "Appuyez sur Ctrl+C pour arrêter le processus" -ForegroundColor Yellow
Write-Host ""

# Maintenir le script actif
while ($true) {
    Start-Sleep -Seconds 10
    $backendCheck = netstat -ano | findstr ":$BACKEND_PORT"
    if (-not $backendCheck) {
        Show-Error "Le backend s'est arrêté!"
        exit 1
    }
}

