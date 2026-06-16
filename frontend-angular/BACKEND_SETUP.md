# Backend Chatbot Agent Bancaire

## Installation

```bash
npm install express cors dotenv axios
```

## Configuration

Créer un fichier `.env`:

```env
PORT=3000

# TTS Services (optionnel - fallback auto si non configuré)
# ELEVEN_LABS_API_KEY=your_key_here
# GOOGLE_CLOUD_TTS_KEY=your_key_here
# AZURE_SPEECH_KEY=your_key_here
```

## Lancement

```bash
node backend.js
```

Le serveur démarre sur `http://localhost:3000`

## API Endpoints

### 1. Chat Intelligence (POST /api/chat)

```json
Request:
{
  "message": "Quel est mon budget?",
  "userId": "user123"
}

Response:
{
  "message": "Réponse du conseiller...",
  "hasChart": true,
  "chart": {
    "labels": ["Alimentation", "Transport"],
    "values": [600, 300]
  }
}
```

### 2. Text-to-Speech (POST /api/tts/synthesize)

```json
Request:
{
  "text": "Bonjour, comment allez-vous?",
  "language": "fr-FR",
  "voice": "natural"
}

Response:
[Binary MP3 Audio Stream]
```

## Configuration pour la voix réaliste

### Option 1: ElevenLabs (Recommandé)

1. Créer compte: https://elevenlabs.io
2. Copier votre API Key
3. Ajouter à `.env`:
   ```
   ELEVEN_LABS_API_KEY=sk-xxx...
   ```

### Option 2: Google Cloud Text-to-Speech

1. Setup: https://cloud.google.com/text-to-speech/docs/quickstart-client-libraries
2. Obtenir credentials JSON
3. Ajouter à `.env`:
   ```
   GOOGLE_CLOUD_TTS_KEY=AIza...
   ```

### Option 3: Azure Cognitive Services

1. Setup: https://learn.microsoft.com/fr-fr/azure/cognitive-services/speech-service/get-started-text-to-speech
2. Configurer dans backend.js
3. Ajouter à `.env`:
   ```
   AZURE_SPEECH_KEY=xxx...
   ```

## Integration Frontend

Dans `environment.ts`:

```typescript
export const environment = {
  apiUrl: 'http://localhost:3000'
};
```

## Architecture

```
Backend:
- Message Chat → NLP → Réponse contextualisée
- Texte → TTS API → Stream audio MP3
- Support multi-langues (Fr, En, Es, etc)

Frontend (Angular):
- Chat UI avec avatar
- WebAudio API pour écoute
- Animations yeux/bouche pendant parole
```

## Notes

- Sans API TTS configurée: utilise fallback Web Speech API (gratuit, moins naturel)
- Avec ElevenLabs: voix ultra-réaliste avec émotions
- Avec Google Cloud: voix synthétisée neural très naturelle
- Réponses mock intégrées pour démo immédiate

## Troubleshooting

**CORS Error?**
- Vérifier que cors() est activé dans backend.js ✓

**Erreur API chat?**
- Backend lancé sur port 3000? ✓
- Frontend attend sur environment.apiUrl correct? ✓

**Pas de son?**
- Vérifier que speakers/volume activés
- Vérifier permissions navigateur pour audio
- Fallback sur Web Speech API active ✓

## Lancer les deux en parallèle

Terminal 1 (Frontend):
```bash
cd attijari-compass-frontend
ng serve
```

Terminal 2 (Backend):
```bash
node backend.js
```

Accès: `http://localhost:4200/storytelling`
