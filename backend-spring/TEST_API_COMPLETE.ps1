# ===================================================================
# SCRIPT DE TEST COMPLET DE L'API
# Teste tous les endpoints du backend Attijari Compass
# ===================================================================

Write-Host ""
Write-Host "╔════════════════════════════════════════════════════════════════════════╗" -ForegroundColor Cyan
Write-Host "║                    TEST COMPLET DE L'API                              ║" -ForegroundColor Cyan
Write-Host "║              Attijari Compass - Vérification des Endpoints            ║" -ForegroundColor Cyan
Write-Host "╚════════════════════════════════════════════════════════════════════════╝" -ForegroundColor Cyan
Write-Host ""

# Configuration
$BACKEND_URL = "http://localhost:8082"
$TEST_EMAIL = "test@example.com"
$TEST_PASSWORD = "Test1234!"

# Fonctions
function Show-Step { param([string]$msg); Write-Host "▶ $msg" -ForegroundColor Yellow }
function Show-Success { param([string]$msg); Write-Host "✓ $msg" -ForegroundColor Green }
function Show-Error { param([string]$msg); Write-Host "✗ $msg" -ForegroundColor Red }
function Show-Info { param([string]$msg); Write-Host "ℹ $msg" -ForegroundColor Cyan }

# ===================================================================
# TEST 1: VÉRIFIER LA CONNECTIVITÉ AU BACKEND
# ===================================================================
Write-Host ""
Write-Host "═══════════════════════════════════════════════════════════════════════════" -ForegroundColor Yellow
Write-Host "TEST 1: CONNECTIVITÉ AU BACKEND" -ForegroundColor Yellow
Write-Host "═══════════════════════════════════════════════════════════════════════════" -ForegroundColor Yellow
Write-Host ""

Show-Step "Vérification de la connexion à $BACKEND_URL..."
try {
    $response = Invoke-WebRequest -Uri "$BACKEND_URL/swagger-ui.html" -Method Get -TimeoutSec 5 -ErrorAction Stop
    if ($response.StatusCode -eq 200) {
        Show-Success "Backend accessible - Code: $($response.StatusCode)"
    }
} catch {
    Show-Error "Backend non accessible"
    Show-Info "Assurez-vous que le backend est démarré:"
    Write-Host "  Exécutez: .\START_BACKEND.ps1" -ForegroundColor Yellow
    exit 1
}

# ===================================================================
# TEST 2: ENREGISTREMENT D'UN UTILISATEUR
# ===================================================================
Write-Host ""
Write-Host "═══════════════════════════════════════════════════════════════════════════" -ForegroundColor Yellow
Write-Host "TEST 2: ENREGISTREMENT D'UN UTILISATEUR" -ForegroundColor Yellow
Write-Host "═══════════════════════════════════════════════════════════════════════════" -ForegroundColor Yellow
Write-Host ""

Show-Step "Enregistrement de l'utilisateur: $TEST_EMAIL"

$registerBody = @{
    email = $TEST_EMAIL
    password = $TEST_PASSWORD
} | ConvertTo-Json

try {
    $registerResponse = Invoke-WebRequest `
        -Uri "$BACKEND_URL/api/auth/register" `
        -Method Post `
        -Body $registerBody `
        -ContentType "application/json" `
        -TimeoutSec 10 `
        -ErrorAction Stop

    $registerData = $registerResponse.Content | ConvertFrom-Json
    Show-Success "Enregistrement réussi"
    Show-Info "Réponse serveur:"
    Write-Host "  Email: $($registerData.email)" -ForegroundColor Green
    Write-Host "  Token: $($registerData.token.Substring(0, 50))..." -ForegroundColor Green

} catch {
    if ($_.Exception.Response.StatusCode -eq 400) {
        Show-Info "Utilisateur déjà enregistré (ce qui est normal si vous avez déjà testé)"
    } else {
        Show-Error "Erreur d'enregistrement: $($_.Exception.Response.StatusCode)"
        Write-Host "Détails: $_" -ForegroundColor Red
    }
}

# ===================================================================
# TEST 3: CONNEXION DE L'UTILISATEUR
# ===================================================================
Write-Host ""
Write-Host "═══════════════════════════════════════════════════════════════════════════" -ForegroundColor Yellow
Write-Host "TEST 3: CONNEXION DE L'UTILISATEUR" -ForegroundColor Yellow
Write-Host "═══════════════════════════════════════════════════════════════════════════" -ForegroundColor Yellow
Write-Host ""

Show-Step "Connexion de l'utilisateur: $TEST_EMAIL"

$loginBody = @{
    email = $TEST_EMAIL
    password = $TEST_PASSWORD
} | ConvertTo-Json

try {
    $loginResponse = Invoke-WebRequest `
        -Uri "$BACKEND_URL/api/auth/login" `
        -Method Post `
        -Body $loginBody `
        -ContentType "application/json" `
        -TimeoutSec 10 `
        -ErrorAction Stop

    $loginData = $loginResponse.Content | ConvertFrom-Json
    $TOKEN = $loginData.token

    Show-Success "Connexion réussie"
    Show-Info "Token JWT obtenu: $($TOKEN.Substring(0, 50))..."

} catch {
    Show-Error "Erreur de connexion: $($_.Exception.Response.StatusCode)"
    Write-Host "Détails: $_" -ForegroundColor Red
    exit 1
}

# ===================================================================
# TEST 4: RÉCUPÉRER LES TRANSACTIONS
# ===================================================================
Write-Host ""
Write-Host "═══════════════════════════════════════════════════════════════════════════" -ForegroundColor Yellow
Write-Host "TEST 4: RÉCUPÉRER LES TRANSACTIONS" -ForegroundColor Yellow
Write-Host "═══════════════════════════════════════════════════════════════════════════" -ForegroundColor Yellow
Write-Host ""

Show-Step "Récupération des transactions (GET /api/transactions)..."

$headers = @{
    "Authorization" = "Bearer $TOKEN"
    "Content-Type" = "application/json"
}

try {
    $transResponse = Invoke-WebRequest `
        -Uri "$BACKEND_URL/api/transactions" `
        -Method Get `
        -Headers $headers `
        -TimeoutSec 10 `
        -ErrorAction Stop

    $transData = $transResponse.Content | ConvertFrom-Json
    Show-Success "Requête GET /api/transactions réussie - Code: $($transResponse.StatusCode)"

    if ($transData -is [array]) {
        Show-Info "Nombre de transactions: $($transData.Count)"
        if ($transData.Count -gt 0) {
            Write-Host "Exemples de transactions:" -ForegroundColor Cyan
            $transData | Select-Object -First 3 | ForEach-Object {
                Write-Host "  - Montant: $($_.amount), Description: $($_.description)" -ForegroundColor Cyan
            }
        }
    } elseif ($transData -is [object]) {
        Write-Host "Réponse: $($transData | ConvertTo-Json)" -ForegroundColor Cyan
    }

} catch {
    Show-Error "Erreur lors de la récupération des transactions: $($_.Exception.Response.StatusCode)"
    Write-Host "Détails: $_" -ForegroundColor Red
}

# ===================================================================
# TEST 5: CRÉER UNE TRANSACTION
# ===================================================================
Write-Host ""
Write-Host "═══════════════════════════════════════════════════════════════════════════" -ForegroundColor Yellow
Write-Host "TEST 5: CRÉER UNE TRANSACTION" -ForegroundColor Yellow
Write-Host "═══════════════════════════════════════════════════════════════════════════" -ForegroundColor Yellow
Write-Host ""

Show-Step "Création d'une nouvelle transaction..."

$transactionBody = @{
    date = (Get-Date).ToString("yyyy-MM-dd")
    description = "Test Transaction - Achat épicerie"
    amount = 50.00
    type = "EXPENSE"
    category = "GROCERIES"
    paymentMethod = "DEBIT_CARD"
} | ConvertTo-Json

try {
    $createTransResponse = Invoke-WebRequest `
        -Uri "$BACKEND_URL/api/transactions" `
        -Method Post `
        -Headers $headers `
        -Body $transactionBody `
        -TimeoutSec 10 `
        -ErrorAction Stop

    $createdTrans = $createTransResponse.Content | ConvertFrom-Json
    Show-Success "Transaction créée - Code: $($createTransResponse.StatusCode)"
    Show-Info "ID de la transaction: $($createdTrans.id)"
    Write-Host "  Date: $($createdTrans.date)" -ForegroundColor Cyan
    Write-Host "  Montant: $($createdTrans.amount)" -ForegroundColor Cyan
    Write-Host "  Description: $($createdTrans.description)" -ForegroundColor Cyan

} catch {
    Show-Error "Erreur lors de la création de la transaction: $($_.Exception.Response.StatusCode)"
    Write-Host "Détails: $_" -ForegroundColor Red
}

# ===================================================================
# TEST 6: VÉRIFIER SWAGGER UI
# ===================================================================
Write-Host ""
Write-Host "═══════════════════════════════════════════════════════════════════════════" -ForegroundColor Yellow
Write-Host "TEST 6: DOCUMENTATION API SWAGGER" -ForegroundColor Yellow
Write-Host "═══════════════════════════════════════════════════════════════════════════" -ForegroundColor Yellow
Write-Host ""

Show-Success "Swagger UI disponible à:"
Write-Host "  $BACKEND_URL/swagger-ui.html" -ForegroundColor Cyan
Show-Info "Ouvrez ce lien dans votre navigateur pour tester d'autres endpoints"

# ===================================================================
# RÉSUMÉ
# ===================================================================
Write-Host ""
Write-Host "═══════════════════════════════════════════════════════════════════════════" -ForegroundColor Green
Write-Host "RÉSUMÉ DES TESTS" -ForegroundColor Green
Write-Host "═══════════════════════════════════════════════════════════════════════════" -ForegroundColor Green
Write-Host ""

Write-Host "✓ Configuration:" -ForegroundColor Green
Write-Host "  Backend URL: $BACKEND_URL" -ForegroundColor Cyan
Write-Host "  Utilisateur test: $TEST_EMAIL" -ForegroundColor Cyan
Write-Host ""

Write-Host "✓ Endpoints testés:" -ForegroundColor Green
Write-Host "  POST   /api/auth/register" -ForegroundColor Cyan
Write-Host "  POST   /api/auth/login" -ForegroundColor Cyan
Write-Host "  GET    /api/transactions" -ForegroundColor Cyan
Write-Host "  POST   /api/transactions" -ForegroundColor Cyan
Write-Host ""

Write-Host "✓ Ressources utiles:" -ForegroundColor Green
Write-Host "  Swagger UI: $BACKEND_URL/swagger-ui.html" -ForegroundColor Cyan
Write-Host "  API Docs: $BACKEND_URL/v3/api-docs" -ForegroundColor Cyan
Write-Host ""

pause

