# 🤖 Agent Bancaire Animé - Guide d'intégration

## 📋 Vue d'ensemble du composant

Le composant `ChatComponent` crée un agent bancaire IA réaliste avec:
- ✅ **Animation des yeux** (clignement automatique toutes les 4 secondes)
- ✅ **Animation de la bouche** (zoom lors du Text-to-Speech)
- ✅ **Chat interactif** avec backend
- ✅ **Boutons d'actions rapides**
- ✅ **Graphiques animés** (détection automatique de mots-clés)
- ✅ **Text-to-Speech** en français (Web Speech API)

---

## 🎨 Configuration des assets (TRÈS IMPORTANT)

Le composant a besoin de **2 images** pour l'animation des yeux:

### 1. Créer le dossier (s'il n'existe pas)
```bash
mkdir -p src/assets
```

### 2. Créer les 2 images PNG

#### Image 1: `agent-eyes-open.png` (280x280 px)
- Photo d'un conseiller bancaire avec **yeux ouverts**
- Format: PNG avec transparence (optionnel)
- Placement: `src/assets/agent-eyes-open.png`

#### Image 2: `agent-eyes-closed.png` (280x280 px)
- **Même photo** que la première, mais avec yeux fermés
- Format: PNG avec transparence (optionnel)
- Placement: `src/assets/agent-eyes-closed.png`

### 3. Alternative - Générer les images rapidement

Si vous n'avez pas d'images:

**Option A: Utiliser des images existantes**
- Téléchargez 2 photos d'un agent bancaire
- Redimensionnez à 280x280 px
- Sauvegardez dans `src/assets/`

**Option B: Utiliser un placeholder temporaire**
```html
<!-- Dans le HTML, remplacer les img par: -->
<div class="avatar-image" style="background: linear-gradient(135deg, #6b7ff4, #ff9500); display: flex; align-items: center; justify-content: center; font-size: 80px;">
  👨‍💼
</div>
```

### 4. Mettre à jour les chemins si différents

Si vos images sont ailleurs, éditer dans `chat.component.html`:
```html
<!-- Ligne 11 -->
[src]="avatarEyesOpen() ? 'assets/agent-eyes-open.png' : 'assets/agent-eyes-closed.png'"
```

---

## ⚙️ Intégration dans votre projet

### 1. Ajouter le composant au module/routing

**Option A: Route directe**
```typescript
// src/app/app.routes.ts
import { ChatComponent } from './features/storytelling/chat.component';

export const routes: Routes = [
  // ... autres routes
  {
    path: 'storytelling',
    component: ChatComponent
  }
];
```

**Option B: Dans un composant parent**
```typescript
// Dans votre composant parent (ex: storytelling.component.ts)
import { ChatComponent } from './chat.component';

@Component({
  selector: 'app-storytelling',
  standalone: true,
  imports: [ChatComponent],
  template: `<app-chat></app-chat>`
})
export class StorytellingComponent {}
```

### 2. Configuration du backend (API)

Le composant appelle l'endpoint:
```
POST /api/storytelling/chat
Body: { message: "texte utilisateur" }
Response: { message: "réponse agent" }
```

**Si vous n'avez pas cet endpoint**, créer temporairement un mock:

```typescript
// Ajouter dans ApiService (api.services.ts)
post(endpoint: string, data: any): Observable<any> {
  // Mock pour le storytelling
  if (endpoint === '/api/storytelling/chat') {
    const responses = [
      'C\'est une excellente approche pour optimiser votre budget.',
      'Vous pouvez réduire vos dépenses en révisant vos abonnements.',
      'Selon votre profil, un plan mensuel serait idéal pour vous.',
      'Vos revenus peuvent être mieux gérés avec un budget préétabli.'
    ];
    return of({ message: responses[Math.floor(Math.random() * responses.length)] });
  }
  
  // Vrai appel API sinon
  return this.http.post(endpoint, data);
}
```

### 3. Vérifier les imports

Assurez-vous que le composant importe:
```typescript
import { ChatComponent } from './features/storytelling/chat.component';
```

---

## 🎯 Utilisation du composant

### URL pour accéder
```
http://localhost:4200/storytelling
```

### Fonctionnalités principales

| Fonctionnalité | Déclencheur | Résultat |
|---|---|---|
| **Animation yeux** | Automatique | Clignement toutes les 4 sec |
| **Animation bouche** | TTS actif | Zoom léger durant la parole |
| **Chat** | Utilisateur envoie texte | Réponse du backend lue |
| **Actions rapides** | Click bouton | Message prédéfini envoyé |
| **Graphique** | Texte contient "budget/dépenses/revenu" | Affichage 5 sec + auto-hide |
| **Indicateur parole** | TTS actif | Pulsion animée en bas à droite |

---

## 🔧 Customisation

### 1. Changer l'intervalle de clignement

```typescript
// chat.component.ts, ligne 115
setInterval(() => {
  // Changer 4000 en millisecondes
}, 4000);  // ← Ici (4000ms = 4 secondes)
```

### 2. Modifier les actions rapides

```typescript
// chat.component.ts, ligne 62
suggestedActions = [
  { label: '💳 Votre texte', value: 'your_key' },
  // ...
];
```

### 3. Changer la couleur de l'indicateur "actif"

```css
/* chat.component.css, ligne 160 */
.status-dot.active {
  background: #10b981;  /* ← Changer cette couleur */
}
```

### 4. Ajouter des mots-clés pour déclencher le graphique

```typescript
// chat.component.ts, ligne 218
if (lower.includes('budget') || lower.includes('votre_mot')) {
  // ...
}
```

---

## 🐛 Troubleshooting

### Le TTS ne fonctionne pas
- **Solution 1**: Vérifier que la langue est `fr-FR` (ligne 161)
- **Solution 2**: Vérifier la console (Ctrl+Shift+I → Console)
- **Solution 3**: Certains navigateurs nécessitent HTTPS pour le TTS

### Les images ne s'affichent pas
- Vérifier que les fichiers existent: `src/assets/agent-eyes-open.png`
- Vérifier que le chemin commence par `assets/` (pas `src/assets/`)
- Vérifier les permissions de fichier

### Le chat n'envoie pas de messages
- Vérifier l'endpoint backend: `/api/storytelling/chat`
- Vérifier dans la console du navigateur (F12) → Network
- Vérifier que le backend renvoie `{ message: "..." }`

### La pagination/scroll ne fonctionne pas
- Fonction `ngAfterViewChecked()` à la fin du composant assure le scroll

---

## 📦 Fichiers fournis

```
src/app/features/storytelling/
├── chat.component.ts      ← Logique (animations, TTS, API)
├── chat.component.html    ← Template (UI)
├── chat.component.css     ← Styles (design, animations)
└── storytelling.component.ts  ← Composant parent (existant)

src/assets/
├── agent-eyes-open.png    ← À créer
└── agent-eyes-closed.png  ← À créer
```

---

## 📱 Responsive

Le composant est entièrement responsive:
- **Desktop** (>1024px): 2 colonnes (agent + chat)
- **Tablet** (640-1024px): 1 colonne avec agent sticky
- **Mobile** (<640px): Full responsive avec actions empilées

---

## 🚀 Performance

- ✅ Utilise **Signals Angular** pour la réactivité optimale
- ✅ Animations GPU-accélérées (`will-change: transform`)
- ✅ Lazy loading des graphiques
- ✅ Scroll virtuel pour les messages (auto-optimisé)

---

## ✨ Prochaines améliorations (optionnelles)

1. **Ajouter plusieurs agents** (différentes personnalités)
2. **Historique du chat persistant** (localStorage)
3. **Voice input** (reconnaissance vocale)
4. **Animoji** (plus d'expressions faciales)
5. **Backend intégration réelle** avec IA (GPT, Claude, etc.)

---

## 📞 Support

En cas de problème:
1. Vérifier la console F12
2. Vérifier les chemins des assets
3. Vérifier l'endpoint du backend
4. Vérifier que tous les fichiers sont créés

✅ **Tout est prêt à l'emploi!**
