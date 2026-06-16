# ✔️ CHECKLIST PRÉ-LANCEMENT

Avant de démarrer, assurez-vous que tout est en place:

## 📋 Fichiers & Configuration

- [ ] **Images placées**
  - `src/assets/agent-eyes-open.jpg` existe
  - `src/assets/agent-eyes-closed.jpg` existe
  - Taille convenable (minimum 280x280px)

- [ ] **environment.ts configuré**
  - `apiUrl: 'http://localhost:3000'` ✓
  - Pas d'erreurs TypeScript

- [ ] **Backend dépendances**
  - `npm install express cors dotenv axios` exécuté
  - `node_modules` créé

- [ ] **Ports disponibles**
  - Port 3000 libre (backend)
  - Port 4200 libre (frontend) ou port alternatif

## 🚀 Avant le lancement

- [ ] Fermer autres applications sur port 3000/4200
- [ ] Terminal 1 prêt pour `node backend.js`
- [ ] Terminal 2 prêt pour `ng serve`
- [ ] Navigateur web prêt

## ✅ Étapes de lancement (dans cet ordre)

### Terminal 1: Backend
```powershell
cd attijari-compass-frontend
node backend.js
```
- [ ] Voir "🚀 Serveur chatbot démarré sur http://localhost:3000"
- [ ] Pas d'erreurs

### Terminal 2: Frontend  
```powershell
cd attijari-compass-frontend
ng serve
```
- [ ] Voir "Application bundle generation complete"
- [ ] Voir "Watch mode enabled"
- [ ] Pas d'erreurs

## 🔍 Tests fonctionnels

Une fois les deux servers lancés:

- [ ] **Accès URL** 
  - Navigateur: `http://localhost:4200/storytelling` (ou port affiché)
  - Page charge sans erreurs

- [ ] **UI visible**
  - Image du conseiller affichée
  - Zone de chat visible
  - Boutons d'actions visibles

- [ ] **Chat fonctionne**
  - Taper: "Bonjour"
  - Clic "Envoyer"
  - Réponse apparaît dans chat

- [ ] **Son fonctionne**
  - Entendre audio (Web Speech API par défaut)
  - Peut être robotique si pas API TTS

- [ ] **Animations**
  - Yeux clignent toutes les 4 sec
  - Bouche s'anime pendant parole

- [ ] **Actions prédéfinies**
  - Cliquer un bouton (ex: Budgets)
  - Message envoyé automatiquement
  - Réponse reçue

## 🛠️ Debugging si problème

| Symptôme | Cause probable | Vérification |
|----------|----------------|--------------|
| "Cannot POST /api/chat" | Backend pas lancé | `node backend.js` running? |
| Images noires | Fichiers manquants | `src/assets/*.jpg` existe? |
| Erreur port 3000 | Port en utilisation | Tuer autres processus port 3000 |
| CORS Error | Backend pas accessible | localhost:3000 accessible? |
| Page vide | Frontend erreur | Vérifier console browser (F12) |
| Pas de son | Permissions/fallback | Vérifier volume, permissions |

## 📊 État serveurs

Vérifier en console:

```powershell
# Backend status
curl http://localhost:3000/api/chat -X POST -H "Content-Type: application/json" -d '{"message":"test"}'

# Frontend
http://localhost:4200 (ou port affiché)
```

## 🎤 Voix naturelle (Optionnel)

Pour améliorer la voix:

- [ ] Compte ElevenLabs crée (gratuit)
- [ ] API Key copié
- [ ] Fichier `.env` crée:
  ```
  ELEVEN_LABS_API_KEY=sk_xxxxx
  ```
- [ ] Backend redémarré

## 📈 Performance checks

- [ ] Compilation < 3 secondes
- [ ] Frontend charge < 2 secondes
- [ ] Chat répond < 1 seconde
- [ ] Son joue sans lag

## 🎯 Validation finale

```
✅ Backend: http://localhost:3000
   └── /api/chat répond
   └── /api/tts/synthesize accessible

✅ Frontend: http://localhost:4200/storytelling
   └── Image affichée
   └── Chat fonctionne
   └── Son joue
   └── Animations activées

✅ Configuration
   └── environment.ts correct
   └── Images en place
   └── Node modules installés

✅ Documentation
   └── QUICKSTART.md lu
   └── AGENT_SETUP.md consultable
   └── Notes de config prêtes
```

## 🚨 En cas de problème

1. **Lire QUICKSTART.md**
2. **Consulter AGENT_SETUP.md:**
   - Point 2.3 Troubleshooting
3. **Vérifier console browser (F12)**
   - Erreurs JS?
   - Erreurs réseau?
4. **Vérifier docker/processus**
   - Ports en utilisation?
   - Autres services bloquants?

## ✨ Everything looks good?

✅ Tous les points cochés?

Bravo! Vous êtes prêt à **lancer l'agent bancaire**! 🚀

Bon succès! 🎉
