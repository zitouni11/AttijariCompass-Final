# 🚀 Script de démarrage COMPLET du projet Attijari Compass
# Pour Windows PowerShell

Write-Host "`n════════════════════════════════════════════════════════════════" -ForegroundColor Cyan
Write-Host "   🎯 DÉMARRAGE DU PROJET ATTIJARI COMPASS" -ForegroundColor Green
Write-Host "════════════════════════════════════════════════════════════════`n" -ForegroundColor Cyan

# Configuration
$BACKEND_PATH = "C:\Users\ademz\OneDrive\Bureau\attijari-compass\attijari-compass"
$FRONTEND_PATH = "C:\Users\ademz\Downloads\attijari-compass-frontend-FINAL\attijari-compass-frontend"
$BACKEND_PORT = 8082
$FRONTEND_PORT = 4200

# Vérifications préalables
Write-Host "📦 Vérification des prérequis..." -ForegroundColor Yellow
if (-Not (Test-Path $BACKEND_PATH)) {
    Write-Host "❌ Dossier backend non trouvé: $BACKEND_PATH" -ForegroundColor Red
    exit 1
}
Write-Host "✅ Backend trouvé" -ForegroundColor Green

if (-Not (Test-Path "$BACKEND_PATH\pom.xml")) {
    Write-Host "❌ pom.xml non trouvé" -ForegroundColor Red
    exit 1
}
Write-Host "✅ Configuration Maven trouvée" -ForegroundColor Green

if (-Not (Test-Path "$BACKEND_PATH\target\attijari-compass-0.0.1-SNAPSHOT.jar")) {
    Write-Host "`n⚠️  JAR pas encore compilé, compilation en cours..." -ForegroundColor Yellow

    Push-Location $BACKEND_PATH
    Write-Host "Compilation... (cela peut prendre 15-20 secondes)"

    # Utiliser mvnw.cmd pour éviter les problèmes de PATH
    & ".\mvnw.cmd" clean package -DskipTests | Out-Null

    if ($LASTEXITCODE -ne 0) {
        Write-Host "❌ Compilation échouée" -ForegroundColor Red
        Pop-Location
        exit 1
    }
    Pop-Location
    Write-Host "✅ Backend compilé avec succès" -ForegroundColor Green
} else {
    Write-Host "✅ JAR déjà compilé" -ForegroundColor Green
}

# Vérifier les ports
Write-Host "`n🔍 Vérification des ports..." -ForegroundColor Yellow

$port8082InUse = netstat -ano 2>$null | Select-String ":$BACKEND_PORT " -ErrorAction SilentlyContinue
if ($port8082InUse) {
    Write-Host "⚠️  Port $BACKEND_PORT peut être occupé" -ForegroundColor Yellow
    $response = Read-Host "Continuer? (y/n)"
    if ($response -ne 'y' -and $response -ne 'yes') {
        Write-Host "Annulé" -ForegroundColor Red
        exit 1
    }
}

# Démarrer le backend
Write-Host "`n════════════════════════════════════════════════════════════════" -ForegroundColor Cyan
Write-Host "ÉTAPE 1: DÉMARRAGE DU BACKEND (port $BACKEND_PORT)" -ForegroundColor Cyan
Write-Host "════════════════════════════════════════════════════════════════`n" -ForegroundColor Cyan

Push-Location $BACKEND_PATH
Write-Host "Démarrage de java -jar target/attijari-compass-0.0.1-SNAPSHOT.jar`n" -ForegroundColor Cyan

# Lancer le JAR en arrière-plan
$backendProcess = Start-Process -FilePath "java" -ArgumentList @(
    "-jar",
    "target/attijari-compass-0.0.1-SNAPSHOT.jar"
) -NoNewWindow -PassThru

Write-Host "✅ Backend démarré (PID: $($backendProcess.Id))" -ForegroundColor Green
Write-Host "⏳ Attente du démarrage du serveur..." -ForegroundColor Yellow

# Attendre que le serveur soit prêt
$waitTime = 0
$maxWait = 60
while ($waitTime -lt $maxWait) {
    try {
        $response = Invoke-WebRequest -Uri "http://localhost:$BACKEND_PORT/actuator/health" -ErrorAction SilentlyContinue
        if ($response.StatusCode -eq 200) {
            Write-Host "`n✅ Serveur prêt! (après $waitTime secondes)" -ForegroundColor Green
            break
        }
    } catch {
        # Serveur pas encore prêt
    }
    Write-Host -NoNewline "." -ForegroundColor Cyan
    Start-Sleep -Seconds 1
    $waitTime++
}

if ($waitTime -ge $maxWait) {
    Write-Host "`n⚠️  Le serveur a pris plus de $maxWait secondes pour démarrer" -ForegroundColor Yellow
    Write-Host "Vérifiez les logs en haut de cette fenêtre" -ForegroundColor Yellow
}

Pop-Location

Write-Host "`n📊 Backend actif:" -ForegroundColor Cyan
Write-Host "  URL API: http://localhost:$BACKEND_PORT/api" -ForegroundColor Green
Write-Host "  Health: http://localhost:$BACKEND_PORT/actuator/health" -ForegroundColor Green

# Frontend (optionnel)
if (Test-Path $FRONTEND_PATH) {
    $startFrontend = Read-Host "`n✨ Démarrer aussi le frontend? (y/n)"

    if ($startFrontend -eq 'y' -or $startFrontend -eq 'yes') {
        Write-Host "`n════════════════════════════════════════════════════════════════" -ForegroundColor Cyan
        Write-Host "ÉTAPE 2: DÉMARRAGE DU FRONTEND (port $FRONTEND_PORT)" -ForegroundColor Cyan
        Write-Host "════════════════════════════════════════════════════════════════`n" -ForegroundColor Cyan

        Push-Location $FRONTEND_PATH

        if (-Not (Test-Path "node_modules")) {
            Write-Host "Installation des dépendances npm..." -ForegroundColor Yellow
            npm install | Out-Null
        }

        Write-Host "Démarrage du serveur Angular..." -ForegroundColor Cyan
        $frontendProcess = Start-Process -FilePath "ng" -ArgumentList @(
            "serve",
            "--open"
        ) -NoNewWindow -PassThru

        Write-Host "✅ Frontend démarré (PID: $($frontendProcess.Id))" -ForegroundColor Green
        Write-Host "   URL: http://localhost:$FRONTEND_PORT" -ForegroundColor Green

        Pop-Location
    }
}

# Résumé
Write-Host "`n════════════════════════════════════════════════════════════════" -ForegroundColor Cyan
Write-Host "🎉 DÉMARRAGE COMPLET!" -ForegroundColor Green
Write-Host "════════════════════════════════════════════════════════════════`n" -ForegroundColor Cyan

Write-Host "📊 Services actifs:" -ForegroundColor Cyan
Write-Host "  🔹 Backend (API):       http://localhost:$BACKEND_PORT" -ForegroundColor Green
Write-Host "  🔹 Frontend (optionnel): http://localhost:$FRONTEND_PORT" -ForegroundColor Green
Write-Host "  🔹 Database:            PostgreSQL (localhost:5432)" -ForegroundColor Green

Write-Host "`n📝 INTÉGRATION FRONTEND:" -ForegroundColor Yellow
Write-Host "  1. Ouvrir: $FRONTEND_PATH" -ForegroundColor White
Write-Host "  2. Modifier 4 fichiers:" -ForegroundColor White
Write-Host "     - src/environments/environment.ts (port 8082)" -ForegroundColor Cyan
Write-Host "     - src/app/services/transaction.service.ts" -ForegroundColor Cyan
Write-Host "     - src/app/components/transactions-list/transactions-list.component.ts" -ForegroundColor Cyan
Write-Host "     - src/app/components/transactions-list/transactions-list.component.html" -ForegroundColor Cyan
Write-Host "  3. Fichiers de référence dans:" -ForegroundColor White
Write-Host "     $BACKEND_PATH" -ForegroundColor Cyan

Write-Host "`n🔑 Identifiants de test:" -ForegroundColor Yellow
Write-Host "  Email: test@example.com" -ForegroundColor White
Write-Host "  Mot de passe: password123" -ForegroundColor White

Write-Host "`n📞 DÉPANNAGE:" -ForegroundColor Yellow
Write-Host "  Backend logs: Consultez la fenêtre noire du backend" -ForegroundColor White
Write-Host "  Port occupé:  taskkill /PID {PID} /F" -ForegroundColor White
Write-Host "  Test API:     Invoke-WebRequest http://localhost:$BACKEND_PORT/api/transactions" -ForegroundColor White

Write-Host "`n════════════════════════════════════════════════════════════════`n" -ForegroundColor Cyan
Write-Host "⏳ Le serveur reste actif. Fermez ces fenêtres pour arrêter." -ForegroundColor Yellow
Write-Host "════════════════════════════════════════════════════════════════`n" -ForegroundColor Cyan

# Garder le script ouvert
while ($true) {
    Start-Sleep -Seconds 10

    # Vérifier que les processus sont toujours actifs
    if (-Not (Get-Process -Id $backendProcess.Id -ErrorAction SilentlyContinue)) {
        Write-Host "`n⚠️  Le backend s'est arrêté!" -ForegroundColor Red
        break
    }
}

