# ⚡ QuickStart - 3 minutes

## 1️⃣ Préparer les images (1 min)

Créer le dossier `src/assets/` (s'il n'existe past)

Y placer les 2 images:
- `agent-eyes-open.jpg` (yeux ouverts)
- `agent-eyes-closed.jpg` (yeux fermés/clignement)

## 2️⃣ Lancer le Backend (1 min)

```powershell
# Dans PowerShell / CMD
cd attijari-compass-frontend

# Installer dépendances (première fois seulement)
npm install express cors dotenv axios

# Lancer le serveur
node backend.js
```

Vous devez voir:
```
🚀 Serveur chatbot démarré sur http://localhost:3000
```

## 3️⃣ Lancer le Frontend (1 min)

Ouvrir un **NOUVEAU terminal** et:

```powershell
cd attijari-compass-frontend
ng serve
```

Accès: **http://localhost:4200/storytelling**

---

## ✅ Vérifier que tout fonctionne

1. Aller à http://localhost:4200/storytelling
2. Voir l'image du conseiller
3. Taper "Bonjour" dans le chat
4. Entendre une réponse (Web Speech API par défaut)

## 🎤 Pour une voix ultra-réaliste

Sans API TTS: Utilise **Web Speech API** (gratuit, basique)

Avec **ElevenLabs** (voix réaliste):

1. S'inscrire: https://elevenlabs.io (gratuit 10k caractères/mois)
2. Copier API Key
3. Créer `.env` à la racine:
   ```
   ELEVEN_LABS_API_KEY=sk_xxxxx
   ```
4. Relancer: `node backend.js`

---

## 🚨 Problèmes courants?

| Problème | Solution |
|----------|----------|
| "Cannot POST /api/chat" | Backend pas lancé → `node backend.js` |
| Images noires | Vérifier `src/assets/agent-*.jpg` existent |
| Pas de son | Volume navigateur? Permissions audio? |
| "CORS Error" | Backend lancé sur 3000? |

---

## ✨ Prochaines étapes

Voir **AGENT_SETUP.md** pour:
- Configuration production
- Intégration autres services TTS
- Architecture complète
- Troubleshooting détaillé

---

**Prêt? Bon succès!** 🚀
