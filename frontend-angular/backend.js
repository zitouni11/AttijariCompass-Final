/**
 * Backend Chatbot pour Agent Bancaire
 * Configuration: npm install express cors dotenv axios
 * Lancement: node backend.js
 */

const express = require('express');
const cors = require('cors');
const axios = require('axios');
require('dotenv').config();

const app = express();
const PORT = process.env.PORT || 3000;

app.use(cors());
app.use(express.json());

// ===== ROUTE CHAT =====
app.post('/api/chat', async (req, res) => {
  try {
    const { message, userId } = req.body;

    if (!message) {
      return res.status(400).json({ error: 'Message requis' });
    }

    // Analyser le message et générer une réponse intelligente
    const response = generateSmartResponse(message);

    res.json({
      message: response.text,
      hasChart: response.hasChart,
      chart: response.chart || null
    });
  } catch (error) {
    console.error('Erreur chat:', error);
    res.status(500).json({ error: 'Erreur serveur' });
  }
});

// ===== ROUTE TTS (Text-to-Speech) =====
app.post('/api/tts/synthesize', async (req, res) => {
  try {
    const { text, language = 'fr-FR', voice = 'natural' } = req.body;

    if (!text) {
      return res.status(400).json({ error: 'Texte requis' });
    }

    // Option 1: Google Cloud TTS (nécessite une clé API)
    // const audioBuffer = await synthesizeWithGoogleCloud(text, language);

    // Option 2: Azure Cognitive Services
    // const audioBuffer = await synthesizeWithAzure(text, language);

    // Option 3: ElevenLabs
    const audioBuffer = await synthesizeWithElevenLabs(text, voice);

    res.set('Content-Type', 'audio/mpeg');
    res.send(audioBuffer);
  } catch (error) {
    console.error('Erreur TTS:', error);
    res.status(500).json({ error: 'Erreur TTS' });
  }
});

// ===== FONCTION: Générer réponse intelligente =====
function generateSmartResponse(message) {
  const msg = message.toLowerCase();

  // Analyse des mots-clés pour réponses contextuelles
  if (msg.includes('budget') || msg.includes('budg')) {
    return {
      text: `Excellent question sur le budget! Analysant vos dernières transactions... Vous avez dépensé 2,500 DT ce mois-ci sur un budget de 3,000 DT. Vous avez donc 500 DT disponibles. Voulez-vous que je vous propose des solutions d'épargne?`,
      hasChart: true,
      chart: {
        labels: ['Alimentation', 'Transport', 'Logement', 'Autres'],
        values: [600, 300, 1200, 400]
      }
    };
  }

  if (msg.includes('épargne') || msg.includes('epargne')) {
    return {
      text: `Votre épargne actuelle est de 15,000 DT. C'est une excellente base! Je recommande de viser un fonds d'urgence égal à 3 mois de revenus. Si vous gagnez 3,000 DT par mois, l'objectif est 9,000 DT. Vous êtes donc bien au-delà! Pensez à commencer des investissements pour faire croître votre patrimoine.`,
      hasChart: true,
      chart: {
        labels: ['Fonds urgence', 'Investissements', 'Autres'],
        values: [9000, 6000, 6000]
      }
    };
  }

  if (msg.includes('revenus') || msg.includes('salaire') || msg.includes('income')) {
    return {
      text: `Votre profil montre un revenu mensuel stable de 3,500 DT. C'est un bon point de départ pour bâtir votre stratégie financière. Avez-vous des sources de revenus supplémentaires que vous aimeriez explorer?`,
      hasChart: false
    };
  }

  if (msg.includes('conseil') || msg.includes('recommand')) {
    return {
      text: `Voici mes recommandations personnalisées pour vous: 
1. Augmentez votre fonds d'urgence à 9,000 DT (3 mois de revenus)
2. Diversifiez avec des investissements (actions, obligations)
3. Maîtrisez vos abonnements - j'ai détecté 150 DT d'abonnements inutilisés
4. Mettez en place un budget mensuel stricte
5. Automatisez votre épargne avec des virements réguliers
Lequel voulez-vous explorer en premier?`,
      hasChart: false
    };
  }

  if (msg.includes('abonnement') || msg.includes('subscription')) {
    return {
      text: `J'ai scanné vos transactions récentes. J'ai trouvé: Netflix (15 DT), Spotify Premium (10 DT), Amazon Prime (12 DT), et un magazine numérique (8 DT) non utilisé depuis 2 mois. Total: 45 DT/mois que vous pouvez économiser! Voulez-vous que je vous aide à optimiser cela?`,
      hasChart: true,
      chart: {
        labels: ['Netflix', 'Spotify', 'Amazon', 'Magazine'],
        values: [15, 10, 12, 8]
      }
    };
  }

  if (msg.includes('invest') || msg.includes('placement')) {
    return {
      text: `Les investissements sont cruciaux pour construire votre richesse. Vous pouvez commencer avec:
- Obligations gouvernementales: rendement ~4% par an
- Fonds d'actions: rendement moyen 8-10% par an
- Plan épargne retraite: avantages fiscaux
Avec votre capital de 15,000 DT, vous pouvez diversifier. Quel profil de risque vous intéresse?`,
      hasChart: false
    };
  }

  // Réponse par défaut générique
  return {
    text: `Merci pour votre question sur "${message.substring(0, 30)}..."! C'est un aspect important de votre gestion financière. En analysant votre profil, je pense que c'est directement lié à votre objectif d'optimiser votre budget. Laissez-moi vous proposer une analyse détaillée et un plan d'action. Quels sont vos objectifs principaux?`,
    hasChart: false
  };
}

// ===== FONCTION: TTS avec ElevenLabs =====
async function synthesizeWithElevenLabs(text, voice = 'natural') {
  const apiKey = process.env.ELEVEN_LABS_API_KEY;
  
  if (!apiKey) {
    console.warn('ElevenLabs API key not configured. Fallback to mock.');
    return getMockAudio(); // Mock pour démo
  }

  try {
    const voiceId = voice === 'natural' ? 'onwK4e9ZjU2zsmTz5XQP' : '9BWtsMINqrJLrRacOk9x';
    
    const response = await axios.post(
      `https://api.elevenlabs.io/v1/text-to-speech/${voiceId}`,
      {
        text,
        model_id: 'eleven_monolingual_v1',
        voice_settings: {
          stability: 0.5,
          similarity_boost: 0.75
        }
      },
      {
        headers: {
          'xi-api-key': apiKey,
          'Content-Type': 'application/json'
        },
        responseType: 'arraybuffer'
      }
    );

    return response.data;
  } catch (error) {
    console.error('ElevenLabs error:', error.message);
    // Fallback
    return getMockAudio();
  }
}

// ===== FONCTION: TTS avec Google Cloud =====
async function synthesizeWithGoogleCloud(text, language = 'fr-FR') {
  const apiKey = process.env.GOOGLE_CLOUD_TTS_KEY;
  
  if (!apiKey) {
    return getMockAudio();
  }

  try {
    const response = await axios.post(
      `https://texttospeech.googleapis.com/v1/text:synthesize?key=${apiKey}`,
      {
        input: { text },
        voice: {
          languageCode: language,
          name: `${language}-Neural2-A`
        },
        audioConfig: {
          audioEncoding: 'MP3'
        }
      }
    );

    return Buffer.from(response.data.audioContent, 'base64');
  } catch (error) {
    console.error('Google Cloud TTS error:', error.message);
    return getMockAudio();
  }
}

// ===== FONCTION: Audio Mock =====
function getMockAudio() {
  // Retourner un fichier audio vide/silence pour démo
  // En production, utiliser une vraie API TTS
  return Buffer.from([
    // Minimal MP3 header (silence)
    0xFF, 0xFB, 0x10, 0x00, 0x00, 0x00, 0x00, 0x00,
    0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00
  ]);
}

// ===== DÉMARRAGE SERVEUR =====
app.listen(PORT, () => {
  console.log(`\n🚀 Serveur chatbot démarré sur http://localhost:${PORT}`);
  console.log(`
📝 Endpoints disponibles:
- POST /api/chat - Envoyer un message
- POST /api/tts/synthesize - Synthèse vocale

🔑 Variables d'environnement à configurer:
- ELEVEN_LABS_API_KEY (optionnel)
- GOOGLE_CLOUD_TTS_KEY (optionnel)
- AZURE_SPEECH_KEY (optionnel)

💡 Pour une qualité TTS réaliste, configurer une API:
  1. ElevenLabs: https://elevenlabs.io
  2. Google Cloud: https://cloud.google.com/text-to-speech
  3. Azure: https://azure.microsoft.com/fr-fr/services/cognitive-services/text-to-speech/
  `);
});

module.exports = app;
