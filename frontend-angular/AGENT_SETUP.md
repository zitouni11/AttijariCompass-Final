# 🚀 Guide Complet: Agent Bancaire avec Voix Naturelle

## Étape 1: Préparer les images

Vous avez 2 images du conseiller:
1. **Yeux ouverts** → `src/assets/agent-eyes-open.jpg`
2. **Yeux fermés** (clignement) → `src/assets/agent-eyes-closed.jpg`

Ces images doivent être placées dans le dossier `src/assets/`

Vous pouvez les sauvegarder directement depuis le navigateur en utilisant:
- Clic droit sur l'image → "Enregistrer sous"
- Nommer: `agent-eyes-open.jpg` et `agent-eyes-closed.jpg`

## Étape 2: Backend - Installer et Lancer

Le backend est fourni dans `backend.js`

### Installation des dépendances:

```bash
cd attijari-compass-frontend
npm install express cors dotenv axios
```

### Configuration (optionnel mais recommandé):

Créer `.env` à la racine du projet:

```env
PORT=3000
# Pour voix réaliste, ajouter une clé API:
# ELEVEN_LABS_API_KEY=votre_clé_ici
```

### Lancer le backend:

```bash
node backend.js
```

Vous devez voir:
```
🚀 Serveur chatbot démarré sur http://localhost:3000
```

## Étape 3: Mises à jour nécessaires

### A. environment.ts

Ouvrir `src/environments/environment.ts` et ajouter:

```typescript
export const environment = {
  production: false,
  apiUrl: 'http://localhost:3000'  // ← Ajouter cette ligne
};
```

### B. environment.prod.ts

Pour la production:

```typescript
export const environment = {
  production: true,
  apiUrl: 'https://votre-backend-api.com'  // ← URL de production
};
```

## Étape 4: Lancer l'application

### Terminal 1 - Frontend Angular:

```bash
cd attijari-compass-frontend
ng serve
```

Accès: `http://localhost:4200/storytelling`

### Terminal 2 - Backend Node.js:

```bash
node backend.js
```

Serveur: `http://localhost:3000`

## Étape 5: Test

Aller à `http://localhost:4200/storytelling`

### Tester les fonctionnalités:

1. **Chat basique**:
   - Cliquer dans le champ de saisie
   - Taper: "Bonjour"
   - Envoyer

2. **Analyse intelligente**:
   - Taper: "Quel est mon budget?"
   - Voir une réponse contextualisée + graphique

3. **Voix naturelle** (si API TTS configurée):
   - Vous devez entendre une voix humaine réaliste
   - Sans API: fallback Web Speech API (gratuit)

4. **Animation du conseiller**:
   - Les yeux clignent toutes les 4 secondes
   - La bouche s'anime quand il parle (avant/après audio)

5. **Actions prédéfinies**:
   - Cliquer les boutons (Budgets, Épargne, etc)
   - Les actions envoient automatiquement au chat

## Améliorer la voix (Optionnel)

### Option 1: ElevenLabs (Hautement recommandé)

1. S'inscrire: https://elevenlabs.io (gratuit 10k caractères/mois)
2. Copier votre API Key (Settings → API Keys)
3. Ajouter à `.env`:
   ```
   ELEVEN_LABS_API_KEY=sk_xxxxx
   ```
4. Redémarrer le backend

Résultat: **Voix ultra-réaliste** avec émotions et intonations naturelles ✨

### Option 2: Google Cloud Text-to-Speech

1. Créer projet Google Cloud
2. Activer "Text-to-Speech API"
3. Créer account service avec JSON key
4. Ajouter clé à `.env`:
   ```
   GOOGLE_CLOUD_TTS_KEY=clé_json
   ```

Résultat: Voix synthétisée très naturelle

### Option 3: Azure Cognitive Services

Voir documentation officielle Azure Speech Service

## Architecture

```
┌─────────────────────────────────────────┐
│   Angular Frontend (Port 4200)          │
│  - Chat Interface                       │
│  - Avatar avec animations               │
│  - WebAudio pour lecture son            │
└─────────────────────────────────────────┘
          ↓                          ↑
      HTTP POST                  Response JSON
          ↓                          ↑
┌─────────────────────────────────────────┐
│   Node.js Backend (Port 3000)           │
│  - POST /api/chat → Réponse intelligente│
│  - POST /api/tts/synthesize → Audio    │
│  - Intégration APIs TTS externes        │
└─────────────────────────────────────────┘
          ↓
    ┌─────────────────┐
    │ ElevenLabs API  │ (optionnel)
    │ Google Cloud    │
    │ Azure Speech    │
    └─────────────────┘
```

## Troubleshooting

### Erreur CORS?
→ Vérifier que le backend est bien lancé sur port 3000

### Pas de son?
→ Vérifier le volume et les permissions audio du navigateur

### Chat ne répond pas?
→ Vérifier que apiUrl pointe sur http://localhost:3000

### Images non affichées?
→ Vérifier que les fichiers .jpg sont dans `src/assets/`

### "Cannot POST /api/chat"?
→ Le backend n'est pas lancé
→ Faire: `node backend.js` en terminal séparé

## Architecture finale

✅ Frontend Angular 18+ avec Signals réactifs
✅ Backend Express.js avec endpoints chat/TTS
✅ Avatar avec animations yeux/bouche
✅ Chat intelligent et contextualisé
✅ TTS de qualité (ElevenLabs/Google/Azure)
✅ Graphiques financiers interactifs
✅ Responsive design (mobile/tablet/desktop)

## Prochaines étapes

1. ✅ Placer les images dans `src/assets/`
2. ✅ Installer dépendances backend: `npm install express cors dotenv axios`
3. ✅ Configurer `.env` avec API key (optionnel)
4. ✅ Lancer backend: `node backend.js`
5. ✅ Lancer frontend: `ng serve`
6. ✅ Accéder: `http://localhost:4200/storytelling`
7. ✅ Tester et profiter! 🎉

Questions? Consultez [BACKEND_SETUP.md](./BACKEND_SETUP.md)
