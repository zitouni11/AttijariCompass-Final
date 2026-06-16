#!/bin/bash
# Script de lancement rapide (systèmes Unix/Linux/Mac)
# Usage: bash launch.sh

echo "🚀 Lancement du système Agent Bancaire..."
echo ""

# Vérifier les images
if [ ! -f "src/assets/agent-eyes-open.jpg" ] || [ ! -f "src/assets/agent-eyes-closed.jpg" ]; then
  echo "⚠️  Images manquantes dans src/assets/"
  echo "Voir AGENT_SETUP.md pour instructions"
fi

# Vérifier Node.js
if ! command -v node &> /dev/null; then
    echo "❌ Node.js non installé"
    echo "Télécharger: https://nodejs.org"
    exit 1
fi

echo "✓ Node.js $(node --version) détecté"

# Vérifier Angular CLI
if ! command -v ng &> /dev/null; then
    echo "Installer Angular CLI globalement: npm install -g @angular/cli"
fi

# Installer dépendances backend
if [ ! -d "node_modules" ]; then
    echo "📦 Installation des dépendances..."
    npm install express cors dotenv axios
fi

echo ""
echo "🔧 Configuration"
echo "─────────────────────────"

# Vérifier environment.ts
if grep -q "apiUrl:" src/environments/environment.ts; then
    echo "✓ environment.ts configuré"
else
    echo "⚠️  Ajouter à environment.ts: apiUrl: 'http://localhost:3000'"
fi

echo ""
echo "🎯 Prêt à lancer!"
echo "─────────────────────────"
echo ""
echo "Terminal 1 - Lancer le backend:"
echo "  node backend.js"
echo ""
echo "Terminal 2 - Lancer le frontend:"
echo "  ng serve"
echo ""
echo "Puis accédez à: http://localhost:4200/storytelling"
echo ""
