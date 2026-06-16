#!/bin/bash

# Script de démarrage complet du projet attijari-compass
# Usage: ./start-full.sh (ou start-full.bat sur Windows)

echo "🚀 Démarrage d'Attijari Compass (Backend + Frontend)"
echo "======================================================"
echo ""

# Couleurs pour le texte
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# Configuration
BACKEND_DIR="C:\Users\ademz\OneDrive\Bureau\attijari-compass\attijari-compass"
FRONTEND_DIR="C:\Users\ademz\Downloads\attijari-compass-frontend-FINAL\attijari-compass-frontend"
BACKEND_PORT=8082
FRONTEND_PORT=4200

echo -e "${YELLOW}[1/4]${NC} Vérification des prérequis..."

# Vérifier PostgreSQL
echo -n "   → PostgreSQL... "
if netstat -ano | findstr :5432 > nul; then
  echo -e "${GREEN}OK${NC}"
else
  echo -e "${RED}ERREUR - PostgreSQL n'est pas en cours d'exécution${NC}"
  echo "   Démarrez PostgreSQL avant de continuer!"
  exit 1
fi

# Vérifier les ports
echo -n "   → Port 8082... "
if netstat -ano | findstr :8082 > nul; then
  echo -e "${YELLOW}OCCUPÉ${NC} (peut être le backend précédent)"
  echo "   Arrêt du processus existant..."
  # Trouver et arrêter le processus
else
  echo -e "${GREEN}LIBRE${NC}"
fi

echo ""
echo -e "${YELLOW}[2/4]${NC} Compilation du backend..."
cd "$BACKEND_DIR"

# Vérifier si Maven wrapper existe
if [ ! -f "mvnw" ]; then
  echo -e "${RED}Erreur: mvnw non trouvé dans $BACKEND_DIR${NC}"
  exit 1
fi

echo "   Compilation en cours (cela peut prendre quelques minutes)..."
./mvnw clean package -DskipTests -q

if [ $? -eq 0 ]; then
  echo -e "${GREEN}✓ Backend compilé avec succès${NC}"
else
  echo -e "${RED}✗ Erreur lors de la compilation du backend${NC}"
  exit 1
fi

echo ""
echo -e "${YELLOW}[3/4]${NC} Lancement du backend (port $BACKEND_PORT)..."

# Lancer le backend en arrière-plan
java -jar "target/attijari-compass-0.0.1-SNAPSHOT.jar" &
BACKEND_PID=$!

# Attendre que le backend soit prêt
echo "   Attendre le démarrage du serveur..."
sleep 5

# Vérifier que le serveur est prêt
if ! netstat -ano | findstr :$BACKEND_PORT > /dev/null; then
  echo -e "${RED}✗ Le backend n'a pas démarré correctement${NC}"
  kill $BACKEND_PID 2>/dev/null
  exit 1
fi

echo -e "${GREEN}✓ Backend démarré sur http://localhost:$BACKEND_PORT${NC}"

echo ""
echo -e "${YELLOW}[4/4]${NC} Lancement du frontend..."

if [ ! -d "$FRONTEND_DIR" ]; then
  echo -e "${RED}Erreur: Dossier frontend non trouvé: $FRONTEND_DIR${NC}"
  kill $BACKEND_PID
  exit 1
fi

cd "$FRONTEND_DIR"

# Vérifier les dépendances
if [ ! -d "node_modules" ]; then
  echo "   Installation des dépendances npm..."
  npm install
fi

echo -e "${GREEN}✓ Démarrage du serveur de développement Angular...${NC}"
echo ""
echo "======================================================"
echo -e "${GREEN}🎉 Projet démarré avec succès!${NC}"
echo ""
echo "   Backend:  http://localhost:$BACKEND_PORT/api"
echo "   Frontend: http://localhost:$FRONTEND_PORT"
echo ""
echo "   Appuyez sur Ctrl+C pour arrêter les serveurs"
echo "======================================================"
echo ""

# Lancer le frontend au premier plan
ng serve --open

# Nettoyage lors de l'arrêt
trap "echo ''; echo 'Arrêt des serveurs...'; kill $BACKEND_PID; exit 0" INT

