# =========================================
# DIAGNOSTIC COMPLET ET CORRECTION
# Attijari Compass - Tous les Problèmes
# =========================================

Write-Host ""
Write-Host "╔═════════════════════════════════════════════════════════════════════════╗" -ForegroundColor Cyan
Write-Host "║   DIAGNOSTIC COMPLET - ATTIJARI COMPASS                               ║" -ForegroundColor Cyan
Write-Host "║   Ce script va : 1) Diagnostiquer 2) Corriger 3) Tester                ║" -ForegroundColor Cyan
Write-Host "╚═════════════════════════════════════════════════════════════════════════╝" -ForegroundColor Cyan
Write-Host ""

# ==================== FONCTIONS ====================
function Show-Step { param([string]$msg); Write-Host "▶ $msg" -ForegroundColor Yellow }
function Show-Success { param([string]$msg); Write-Host "✓ $msg" -ForegroundColor Green }
function Show-Error { param([string]$msg); Write-Host "✗ $msg" -ForegroundColor Red }
function Show-Info { param([string]$msg); Write-Host "ℹ $msg" -ForegroundColor Cyan }
function Show-Section { param([string]$msg); Write-Host ""; Write-Host "═══════════════════════════════════════════════════════════════" -ForegroundColor Cyan; Write-Host $msg -ForegroundColor Cyan; Write-Host "═══════════════════════════════════════════════════════════════" -ForegroundColor Cyan; Write-Host "" }

# ==================== DIAGNOSTIC ====================
Show-Section "PHASE 1: DIAGNOSTIC SYSTÈME"

# 1. Vérifier les processus Java
Show-Step "Vérification des processus Java..."
$javaProcesses = Get-Process java -ErrorAction SilentlyContinue
if ($javaProcesses) {
    Show-Error "Processus Java en cours d'exécution :"
    $javaProcesses | ForEach-Object { Write-Host "  - PID: $($_.Id), Memory: $([math]::Round($_.WorkingSet / 1MB))MB" -ForegroundColor Red }
    Write-Host ""
    Show-Step "Arrêt des processus Java..."
    Get-Process java | Stop-Process -Force -ErrorAction SilentlyContinue
    Start-Sleep -Seconds 2
    Show-Success "Processus Java arrêtés"
} else {
    Show-Success "Aucun processus Java trouvé"
}

# 2. Vérifier les ports
Show-Step "Vérification des ports..."
$ports = @(
    @{Port = 8082; Service = "Backend Spring Boot"},
    @{Port = 5432; Service = "PostgreSQL"},
    @{Port = 4200; Service = "Frontend Angular"}
)

$occupiedPorts = @()
foreach ($portInfo in $ports) {
    $port = $portInfo.Port
    $service = $portInfo.Service
    $check = netstat -ano 2>$null | findstr ":$port"

    if ($check) {
        Show-Error "Port $port ($service) : EN UTILISATION"
        $occupiedPorts += $port
    } else {
        Show-Success "Port $port ($service) : LIBRE"
    }
}

# 3. Vérifier Maven
Show-Step "Vérification de Maven..."
$mvnVersion = mvn --version 2>&1 | Select-Object -First 1
if ($?) {
    Show-Success "Maven installé : $mvnVersion"
} else {
    Show-Error "Maven n'est pas trouvé ou non configuré"
}

# 4. Vérifier Java
Show-Step "Vérification de Java..."
$javaVersion = java --version 2>&1 | Select-Object -First 1
if ($?) {
    Show-Success "Java installé : $javaVersion"
} else {
    Show-Error "Java n'est pas trouvé ou non configuré"
}

# 5. Vérifier PostgreSQL
Show-Step "Vérification de PostgreSQL..."
$pgConnection = Test-NetConnection localhost -Port 5432 -WarningAction SilentlyContinue
if ($pgConnection.TcpTestSucceeded) {
    Show-Success "PostgreSQL accessible sur le port 5432"
} else {
    Show-Error "PostgreSQL n'est pas accessible sur le port 5432"
    Show-Info "PostgreSQL doit être démarré manuellement"
}

# ==================== NETTOYAGE ====================
Show-Section "PHASE 2: NETTOYAGE"

Show-Step "Suppression du dossier target (ancienne compilation)..."
if (Test-Path "target") {
    Remove-Item -Recurse -Force "target" -ErrorAction SilentlyContinue
    Show-Success "Dossier target supprimé"
} else {
    Show-Info "Dossier target inexistant"
}

# ==================== COMPILATION ====================
Show-Section "PHASE 3: COMPILATION MAVEN"

Show-Step "Compilation du projet Maven..."
cd (Get-Location)

# Faire un clean install
$compilationOutput = mvn clean install -DskipTests 2>&1
if ($?) {
    Show-Success "Compilation terminée avec succès"
} else {
    Show-Error "La compilation a échoué"
    Write-Host "Erreur de compilation :" -ForegroundColor Red
    Write-Host $compilationOutput -ForegroundColor Red
    exit 1
}

# ==================== VÉRIFICATION DE LA CONFIGURATION ====================
Show-Section "PHASE 4: VÉRIFICATION DE LA CONFIGURATION"

# Vérifier application.yml
Show-Step "Vérification de application.yml..."
$appYmlPath = "src/main/resources/application.yml"
if (Test-Path $appYmlPath) {
    Show-Success "Fichier application.yml trouvé"
    $appYmlContent = Get-Content $appYmlPath -Raw

    if ($appYmlContent -match "port: 8082") {
        Show-Success "Port 8082 configuré dans application.yml"
    } else {
        Show-Error "Port 8082 NON configuré dans application.yml"
    }

    if ($appYmlContent -match "attijari_compass") {
        Show-Success "Base de données 'attijari_compass' configurée"
    }
} else {
    Show-Error "Fichier application.yml non trouvé"
}

# Vérifier proxy.conf.json
Show-Step "Vérification de proxy.conf.json..."
if (Test-Path "proxy.conf.json") {
    Show-Success "Fichier proxy.conf.json trouvé"
} else {
    Show-Info "Fichier proxy.conf.json non trouvé (nécessaire si frontend Angular présent)"
}

# ==================== DÉMARRAGE DU BACKEND ====================
Show-Section "PHASE 5: DÉMARRAGE DU BACKEND"

Show-Info "Le backend va démarrer sur http://localhost:8082"
Show-Info "Swagger UI sera accessible sur http://localhost:8082/swagger-ui.html"
Write-Host ""

Show-Step "Démarrage du backend Spring Boot..."
Write-Host "Attendez le message 'Started AttijariCompassApplication'" -ForegroundColor Yellow
Write-Host ""

# Démarrer le backend dans une nouvelle fenêtre (optionnel)
$startBackendChoice = Read-Host "Démarrer le backend dans une nouvelle fenêtre PowerShell? (y/n)"
if ($startBackendChoice -eq 'y') {
    Start-Process powershell -ArgumentList "-NoExit", "-Command", "cd '$((Get-Location).Path)'; mvn spring-boot:run"
    Write-Host ""
    Show-Info "Backend lancé dans une nouvelle fenêtre..."
    Write-Host "Veuillez attendre 30-45 secondes pour le démarrage complet" -ForegroundColor Yellow
    Write-Host ""
    Write-Host "Une fois prêt, vous verrez :" -ForegroundColor Cyan
    Write-Host "  'Started AttijariCompassApplication in X seconds'" -ForegroundColor Cyan
    Write-Host ""
    pause
} else {
    Show-Step "Démarrage du backend (mode console)..."
    mvn spring-boot:run
}

