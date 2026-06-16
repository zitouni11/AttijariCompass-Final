# Script PowerShell pour tester l'API Attijari Compass
# Utilisation: .\test-api.ps1

$API_URL = "http://localhost:8082/api"
$EMAIL = "test@example.com"
$PASSWORD = "Test1234!"

Write-Host "========================================" -ForegroundColor Cyan
Write-Host "Attijari Compass - API Test" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""

# Test 1: Enregistrement
Write-Host "[1] Test d'enregistrement..." -ForegroundColor Yellow
$registerResponse = Invoke-WebRequest -Uri "$API_URL/auth/register" `
    -Method POST `
    -Headers @{"Content-Type" = "application/json"} `
    -Body (ConvertTo-Json @{
        email = $EMAIL
        password = $PASSWORD
    })

$registerData = $registerResponse.Content | ConvertFrom-Json
$TOKEN = $registerData.token

Write-Host "✓ Enregistrement réussi!" -ForegroundColor Green
Write-Host "Token: $TOKEN" -ForegroundColor Gray
Write-Host ""

# Test 2: Connexion
Write-Host "[2] Test de connexion..." -ForegroundColor Yellow
$loginResponse = Invoke-WebRequest -Uri "$API_URL/auth/login" `
    -Method POST `
    -Headers @{"Content-Type" = "application/json"} `
    -Body (ConvertTo-Json @{
        email = $EMAIL
        password = $PASSWORD
    })

Write-Host "✓ Connexion réussie!" -ForegroundColor Green
Write-Host ""

# Test 3: Obtenir l'utilisateur courant
Write-Host "[3] Test obtenir utilisateur courant..." -ForegroundColor Yellow
$userResponse = Invoke-WebRequest -Uri "$API_URL/users/me" `
    -Method GET `
    -Headers @{"Authorization" = "Bearer $TOKEN"}

$userData = $userResponse.Content | ConvertFrom-Json
Write-Host "✓ Utilisateur récupéré!" -ForegroundColor Green
Write-Host "Email: $($userData.email)" -ForegroundColor Gray
Write-Host ""

# Test 4: Créer une transaction
Write-Host "[4] Test création transaction..." -ForegroundColor Yellow
$transactionResponse = Invoke-WebRequest -Uri "$API_URL/transactions/card-payment" `
    -Method POST `
    -Headers @{
        "Authorization" = "Bearer $TOKEN"
        "Content-Type" = "application/json"
    } `
    -Body (ConvertTo-Json @{
        merchantName = "Restaurant Test"
        description = "Dîner en famille"
        amount = 50.00
        date = (Get-Date -Format "yyyy-MM-dd")
        cardLast4 = "1234"
    })

$transactionData = $transactionResponse.Content | ConvertFrom-Json
Write-Host "✓ Transaction créée!" -ForegroundColor Green
Write-Host "ID: $($transactionData.id)" -ForegroundColor Gray
Write-Host "Montant: $($transactionData.amount)" -ForegroundColor Gray
Write-Host "Catégorie: $($transactionData.category)" -ForegroundColor Gray
Write-Host ""

# Test 5: Lister les transactions
Write-Host "[5] Test lister les transactions..." -ForegroundColor Yellow
$listResponse = Invoke-WebRequest -Uri "$API_URL/transactions" `
    -Method GET `
    -Headers @{"Authorization" = "Bearer $TOKEN"}

$transactions = $listResponse.Content | ConvertFrom-Json
Write-Host "✓ Transactions récupérées!" -ForegroundColor Green
Write-Host "Total: $($transactions.Count) transactions" -ForegroundColor Gray
foreach ($t in $transactions) {
    Write-Host "  - $($t.description): $($t.amount) DT (Catégorie: $($t.category))" -ForegroundColor Gray
}
Write-Host ""

# Test 6: Obtenir le dashboard
Write-Host "[6] Test obtenir le dashboard..." -ForegroundColor Yellow
try {
    $dashboardResponse = Invoke-WebRequest -Uri "$API_URL/dashboard" `
        -Method GET `
        -Headers @{"Authorization" = "Bearer $TOKEN"}

    $dashboard = $dashboardResponse.Content | ConvertFrom-Json
    Write-Host "✓ Dashboard récupéré!" -ForegroundColor Green
    Write-Host "Solde total: $($dashboard.totalBalance) DT" -ForegroundColor Gray
    Write-Host "Revenus: $($dashboard.totalIncome) DT" -ForegroundColor Gray
    Write-Host "Dépenses: $($dashboard.totalExpenses) DT" -ForegroundColor Gray
    Write-Host "Taux d'épargne: $($dashboard.savingsRate)%" -ForegroundColor Gray
}
catch {
    Write-Host "✗ Erreur lors de la récupération du dashboard" -ForegroundColor Red
    Write-Host $_.Exception.Message -ForegroundColor Red
}
Write-Host ""

# Test 7: Importer des transactions
Write-Host "[7] Test import de transactions..." -ForegroundColor Yellow
if (Test-Path "sample-transactions.csv") {
    $filePath = (Get-Item "sample-transactions.csv").FullName

    $fileContent = [System.IO.File]::ReadAllBytes($filePath)
    $fileName = "sample-transactions.csv"

    $boundary = [System.Guid]::NewGuid().ToString()
    $bodyLines = @()

    $bodyLines += "--$boundary"
    $bodyLines += 'Content-Disposition: form-data; name="file"; filename="' + $fileName + '"'
    $bodyLines += "Content-Type: application/octet-stream"
    $bodyLines += ""

    $body = $bodyLines -join "`r`n"
    $body = [System.Text.Encoding]::UTF8.GetBytes($body)
    $body += $fileContent
    $body += [System.Text.Encoding]::UTF8.GetBytes("`r`n--$boundary--`r`n")

    try {
        $importResponse = Invoke-WebRequest -Uri "$API_URL/transactions/import" `
            -Method POST `
            -Headers @{
                "Authorization" = "Bearer $TOKEN"
                "Content-Type" = "multipart/form-data; boundary=$boundary"
            } `
            -Body $body

        $importData = $importResponse.Content | ConvertFrom-Json
        Write-Host "✓ Import réussi!" -ForegroundColor Green
        Write-Host "Total: $($importData.totalProcessed)" -ForegroundColor Gray
        Write-Host "Succès: $($importData.successCount)" -ForegroundColor Green
        Write-Host "Erreurs: $($importData.errorCount)" -ForegroundColor Yellow
        if ($importData.errorCount -gt 0) {
            foreach ($error in $importData.errors) {
                Write-Host "  - $error" -ForegroundColor Red
            }
        }
    }
    catch {
        Write-Host "✗ Erreur lors de l'import" -ForegroundColor Red
        Write-Host $_.Exception.Message -ForegroundColor Red
    }
}
else {
    Write-Host "✗ Fichier sample-transactions.csv non trouvé" -ForegroundColor Red
}
Write-Host ""

Write-Host "========================================" -ForegroundColor Cyan
Write-Host "Tests terminés!" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan

