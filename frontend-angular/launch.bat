@echo off
REM Script de lancement pour Windows
REM Double-cliquez pour lancer le système

echo.
echo ========================================
echo   Agent Bancaire - Lancement Rapide
echo ========================================
echo.

REM Vérifier Node.js
node --version >nul 2>&1
if errorlevel 1 (
    echo ERREUR: Node.js n'est pas installe
    echo Telecharger: https://nodejs.org
    pause
    exit /b 1
)

echo [OK] Node.js detectable

REM Vérifier les images
if not exist "src\assets\agent-eyes-open.jpg" (
    echo [ATTENTION] Image agent-eyes-open.jpg manquante
    echo voir AGENT_SETUP.md pour instructions
)
if not exist "src\assets\agent-eyes-closed.jpg" (
    echo [ATTENTION] Image agent-eyes-closed.jpg manquante
)

REM Installer dépendances si nécessaire
if not exist "node_modules" (
    echo.
    echo [INSTALLATION] Dépendances backend...
    call npm install express cors dotenv axios
)

echo.
echo ========================================
echo   Instructions de lancement
echo ========================================
echo.
echo TERMINAL 1 - Backend (ce terminal):
echo   Appuyer sur Enter pour continuer...
pause >nul

echo Lancement du serveur backend...
echo Port: 3000
echo.
start "" node backend.js

timeout /t 3

echo.
echo ========================================
echo   TERMINAL 2 - Frontend Angular
echo ========================================
echo.
echo Ouvrir un NOUVEAU terminal (PowerShell/CMD) et executer:
echo.
echo   cd attijari-compass-frontend
echo   ng serve
echo.
echo Puis accedez a: http://localhost:4200/storytelling
echo.
pause
