# Script pour arrêter les processus Java et redémarrer le backend
Write-Host "========================================"
Write-Host "Arrêt des processus Java existants"
Write-Host "========================================"

# Arrêter tous les processus Java
Get-Process -Name java -ErrorAction SilentlyContinue | Stop-Process -Force
Write-Host "✓ Processus Java arrêtés"

# Attendre 2 secondes
Start-Sleep -Seconds 2

# Vérifier que les ports sont libres
Write-Host "Vérification des ports..."
$port8082 = netstat -ano | findstr ":8082"
$port5432 = netstat -ano | findstr ":5432"

if ($port8082) {
    Write-Host "⚠️  Port 8082 est toujours utilisé"
    Write-Host $port8082
} else {
    Write-Host "✓ Port 8082 libre"
}

if (!$port5432) {
    Write-Host "⚠️  Port 5432 (PostgreSQL) n'est pas accessible"
    Write-Host "   Assurez-vous que PostgreSQL est en exécution"
} else {
    Write-Host "✓ PostgreSQL accessible sur le port 5432"
}

Write-Host ""
Write-Host "========================================"
Write-Host "Démarrage du backend (port 8082)"
Write-Host "========================================"

# Lancer le backend
java -jar target/attijari-compass-0.0.1-SNAPSHOT.jar

Write-Host ""
Write-Host "Le serveur est lancé. Attendez le message 'Started AttijariCompassApplication'"
Write-Host "Swagger sera accessible sur http://localhost:8082/swagger-ui.html"

