@echo off
REM Script de test API pour Attijari Compass
REM Utilisation: test-api.bat

setlocal enabledelayedexpansion

set API_URL=http://localhost:8081/api
set EMAIL=test@example.com
set PASSWORD=Test1234!

REM Couleurs pour affichage
cls
echo ========================================
echo Attijari Compass - API Test Script
echo ========================================
echo.

REM Test 1: Enregistrement
echo.
echo [1] Testing User Registration...
echo.
curl -X POST %API_URL%/auth/register ^
  -H "Content-Type: application/json" ^
  -d "{\"email\":\"%EMAIL%\",\"password\":\"%PASSWORD%\"}"
echo.
echo.
pause

REM Test 2: Connexion
echo.
echo [2] Testing User Login...
echo.
for /f "tokens=*" %%i in ('curl -s -X POST %API_URL%/auth/login ^
  -H "Content-Type: application/json" ^
  -d "{\"email\":\"%EMAIL%\",\"password\":\"%PASSWORD%\"}" ^
  ^| findstr /i token') do set TOKEN=%%i

curl -X POST %API_URL%/auth/login ^
  -H "Content-Type: application/json" ^
  -d "{\"email\":\"%EMAIL%\",\"password\":\"%PASSWORD%\"}"
echo.
echo Copy the token from above and set it in this script
pause

REM Utiliser manuellement le token
set /p TOKEN="Enter your JWT token: "

REM Test 3: Get Current User
echo.
echo [3] Testing Get Current User...
echo.
curl -X GET %API_URL%/users/me ^
  -H "Authorization: Bearer %TOKEN%"
echo.
pause

REM Test 4: Create Card Payment Transaction
echo.
echo [4] Testing Create Card Payment...
echo.
curl -X POST %API_URL%/transactions/card-payment ^
  -H "Authorization: Bearer %TOKEN%" ^
  -H "Content-Type: application/json" ^
  -d "{\"merchantName\":\"Restaurant Test\",\"description\":\"Dinner\",\"amount\":50.00,\"date\":\"2026-03-27\",\"cardLast4\":\"1234\"}"
echo.
pause

REM Test 5: Get All Transactions
echo.
echo [5] Testing Get All Transactions...
echo.
curl -X GET %API_URL%/transactions ^
  -H "Authorization: Bearer %TOKEN%"
echo.
pause

REM Test 6: Import Transactions
echo.
echo [6] Testing Import Transactions...
echo.
if exist sample-transactions.csv (
  curl -X POST %API_URL%/transactions/import ^
    -H "Authorization: Bearer %TOKEN%" ^
    -F "file=@sample-transactions.csv"
) else (
  echo File sample-transactions.csv not found!
)
echo.
pause

REM Test 7: Get Dashboard
echo.
echo [7] Testing Get Dashboard...
echo.
curl -X GET %API_URL%/dashboard ^
  -H "Authorization: Bearer %TOKEN%"
echo.
pause

echo.
echo All tests completed!
pause

