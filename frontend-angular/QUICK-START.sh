#!/usr/bin/env bash
# AGENT BANCAIRE ANIMÉ - QUICK START SCRIPT
# Utilisation: Cet fichier montre les étapes exactes

# ============================================================================
# 🚀 ÉTAPE 1: CRÉER LES IMAGES (5 minutes)
# ============================================================================

# Les 2 images PNG (280×280px) doivent être créées manuellement:
# - agent-eyes-open.png
# - agent-eyes-closed.png

# Ou utiliser le placeholder emoji (voir GUIDE_IMAGES.md)

# Placer les fichiers ici:
mkdir -p src/assets

# Copier vos 2 PNG dans src/assets/
# cp ~/your-images/agent-eyes-open.png src/assets/
# cp ~/your-images/agent-eyes-closed.png src/assets/

echo "✅ Étape 1: Images (À faire manuellement)"
echo "   Placer:"
echo "   - src/assets/agent-eyes-open.png"
echo "   - src/assets/agent-eyes-closed.png"

# ============================================================================
# 🎯 ÉTAPE 2: AJOUTER LA ROUTE (2 minutes)
# ============================================================================

# Éditer: src/app/app.routes.ts
# et ajouter:

# import { ChatComponent } from './features/storytelling/chat.component';
#
# export const routes: Routes = [
#   // ... autres routes
#   { path: 'storytelling', component: ChatComponent }
# ];

echo "✅ Étape 2: Route (À faire manuellement ou via commande)"
echo "   Éditer src/app/app.routes.ts"

# ============================================================================
# 🎉 ÉTAPE 3: LANCER (1 minute)
# ============================================================================

# Démarrer le serveur de développement:
echo "✅ Étape 3: Lancer le serveur"
echo "   Exécuter:"
echo "   $ ng serve"
echo ""
echo "   Puis accéder à:"
echo "   http://localhost:4200/storytelling"

# ============================================================================
# VÉRIFICATION
# ============================================================================

# Vérifier que les 3 fichiers composant existent:
echo ""
echo "📋 Vérification des fichiers composant:"

if [ -f "src/app/features/storytelling/chat.component.ts" ]; then
    echo "✅ chat.component.ts"
else
    echo "❌ chat.component.ts - MANQUANT"
fi

if [ -f "src/app/features/storytelling/chat.component.html" ]; then
    echo "✅ chat.component.html"
else
    echo "❌ chat.component.html - MANQUANT"
fi

if [ -f "src/app/features/storytelling/chat.component.css" ]; then
    echo "✅ chat.component.css"
else
    echo "❌ chat.component.css - MANQUANT"
fi

# Vérifier que les images existent:
echo ""
echo "🖼️  Vérification des images:"

if [ -f "src/assets/agent-eyes-open.png" ]; then
    echo "✅ agent-eyes-open.png"
else
    echo "⏳ agent-eyes-open.png - À CRÉER"
fi

if [ -f "src/assets/agent-eyes-closed.png" ]; then
    echo "✅ agent-eyes-closed.png"
else
    echo "⏳ agent-eyes-closed.png - À CRÉER"
fi

# ============================================================================
# SUITE
# ============================================================================

echo ""
echo "═══════════════════════════════════════════════════════════════════"
echo "🎊 PROCHAINES ÉTAPES:"
echo "═══════════════════════════════════════════════════════════════════"
echo ""
echo "1️⃣  Créer 2 images PNG (280×280px)"
echo "   - agent-eyes-open.png"
echo "   - agent-eyes-closed.png"
echo ""
echo "2️⃣  Éditer src/app/app.routes.ts"
echo "   Ajouter: { path: 'storytelling', component: ChatComponent }"
echo ""
echo "3️⃣  Lancer le serveur"
echo "   $ ng serve"
echo ""
echo "4️⃣  Accéder au composant"
echo "   http://localhost:4200/storytelling"
echo ""
echo "═══════════════════════════════════════════════════════════════════"
echo "📚 Pour plus d'informations, lire:"
echo "═══════════════════════════════════════════════════════════════════"
echo ""
echo "  START.md              - Démarrage rapide"
echo "  BIENVENUE.md          - Introduction"
echo "  INTEGRATION_RAPIDE.md - Guide complet"
echo "  GUIDE_IMAGES.md       - Pour les images"
echo ""
echo "═══════════════════════════════════════════════════════════════════"
echo ""
echo "✅ Vous êtes prêt! Bon développement! 🚀"
echo ""

# ============================================================================
# COMMANDES SUPPLÉMENTAIRES (Optionnel)
# ============================================================================

# Pour tester le composant:
# ng test

# Pour voir les erreurs:
# ng serve --open

# Pour build en production:
# ng build --prod

# Pour vérifier les erreurs de compilation:
# ng build
