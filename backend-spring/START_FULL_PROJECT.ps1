# ✅ Script PowerShell pour démarrer le projet complet
# Usage: .\START_FULL_PROJECT.ps1

param(
    [switch]$BackendOnly,
    [switch]$FrontendOnly,
    [switch]$NoOpen
)

Write-Host "🚀 Démarrage d'Attijari Compass" -ForegroundColor Cyan
Write-Host "=================================" -ForegroundColor Cyan
Write-Host ""

# Configuration
$BackendDir = "C:\Users\ademz\OneDrive\Bureau\attijari-compass\attijari-compass"
$FrontendDir = "C:\Users\ademz\Downloads\attijari-compass-frontend-FINAL\attijari-compass-frontend"
$BackendPort = 8082
$FrontendPort = 4200

# Fonctions utiles
function Check-Port {
    param([int]$Port)
    $Connection = Test-NetConnection -ComputerName localhost -Port $Port -ErrorAction SilentlyContinue
    return $Connection.TcpTestSucceeded
}

function Check-Process {
    param([string]$ProcessName)
    return (Get-Process $ProcessName -ErrorAction SilentlyContinue) -ne $null
}

# Vérifications préalables
Write-Host "[1/3] Vérification des prérequis..." -ForegroundColor Yellow

# PostgreSQL
Write-Host "   → PostgreSQL... " -NoNewline
if (Check-Port 5432) {
    Write-Host "✓" -ForegroundColor Green
} else {
    Write-Host "✗" -ForegroundColor Red
    Write-Host "   PostgreSQL n'est pas en cours d'exécution!" -ForegroundColor Red
    exit 1
}

# Chemins
Write-Host "   → Chemins... " -NoNewline
if ((Test-Path $BackendDir) -and (Test-Path $FrontendDir)) {
    Write-Host "✓" -ForegroundColor Green
} else {
    Write-Host "✗" -ForegroundColor Red
    if (-not (Test-Path $BackendDir)) {
        Write-Host "   Backend: $BackendDir - NON TROUVÉ" -ForegroundColor Red
    }
    if (-not (Test-Path $FrontendDir)) {
        Write-Host "   Frontend: $FrontendDir - NON TROUVÉ" -ForegroundColor Red
    }
    exit 1
}

Write-Host ""

# BACKEND
if (-not $FrontendOnly) {
    Write-Host "[2/3] Compilation et démarrage du backend..." -ForegroundColor Yellow

    # Arrêter le backend existant
    if (Check-Port $BackendPort) {
        Write-Host "   Arrêt du backend existant..." -ForegroundColor DarkYellow
        $Process = Get-NetTCPConnection -LocalPort $BackendPort -ErrorAction SilentlyContinue
        if ($Process) {
            Stop-Process -Id $Process.OwningProcess -Force -ErrorAction SilentlyContinue
            Start-Sleep -Seconds 2
        }
    }

    # Compiler
    Write-Host "   Compilation en cours..." -NoNewline
    Push-Location $BackendDir
    & .\mvnw clean package -DskipTests -q 2>&1 | Out-Null

    if ($LASTEXITCODE -eq 0) {
        Write-Host " ✓" -ForegroundColor Green
    } else {
        Write-Host " ✗" -ForegroundColor Red
        Write-Host "   Erreur lors de la compilation du backend" -ForegroundColor Red
        Pop-Location
        exit 1
    }

    # Démarrer
    Write-Host "   Démarrage du serveur (port $BackendPort)..." -NoNewline
    Start-Process java -ArgumentList "-jar", "target/attijari-compass-0.0.1-SNAPSHOT.jar" -NoNewWindow

    Start-Sleep -Seconds 5

    if (Check-Port $BackendPort) {
        Write-Host " ✓" -ForegroundColor Green
        Write-Host "   http://localhost:$BackendPort/api" -ForegroundColor Green
    } else {
        Write-Host " ✗" -ForegroundColor Red
        Write-Host "   Le backend n'a pas démarré correctement" -ForegroundColor Red
        Pop-Location
        exit 1
    }

    Pop-Location
}

Write-Host ""

# FRONTEND
if (-not $BackendOnly) {
    Write-Host "[3/3] Préparation du frontend..." -ForegroundColor Yellow

    Push-Location $FrontendDir

    # Vérifier node_modules
    if (-not (Test-Path "node_modules")) {
        Write-Host "   Installation des dépendances npm..." -NoNewline
        npm install -q 2>&1 | Out-Null
        Write-Host " ✓" -ForegroundColor Green
    }

    # Lancer le frontend
    Write-Host "   Démarrage du serveur Angular..." -NoNewline

    if ($NoOpen) {
        & ng serve --open:false --port $FrontendPort 2>&1 | ForEach-Object { Write-Host $_ }
    } else {
        Start-Process -FilePath "ng" -ArgumentList "serve", "--open", "--port", $FrontendPort -NoNewWindow
        Write-Host " ✓" -ForegroundColor Green
        Write-Host "   http://localhost:$FrontendPort" -ForegroundColor Green
    }

    Pop-Location
}

Write-Host ""
Write-Host "=================================" -ForegroundColor Cyan
Write-Host "✅ Projet démarré avec succès!" -ForegroundColor Green
Write-Host "=================================" -ForegroundColor Cyan
Write-Host ""

if (-not $FrontendOnly) {
    Write-Host "Backend:  http://localhost:$BackendPort/api" -ForegroundColor Cyan
}

if (-not $BackendOnly) {
    Write-Host "Frontend: http://localhost:$FrontendPort" -ForegroundColor Cyan
}

Write-Host ""
Write-Host "Appuyez sur Ctrl+C pour arrêter" -ForegroundColor Yellow

