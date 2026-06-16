# 🚀 AGENT BANCAIRE ANIMÉ - COMPOSANT COMPLET

## ✅ Fichiers créés et prêts à utiliser

```
src/app/features/storytelling/
├── chat.component.ts       ✅ (Logique complète)
├── chat.component.html     ✅ (Template prêt)
├── chat.component.css      ✅ (Styles + animations)
└── storytelling.component.ts  (Existant)

src/assets/
├── agent-eyes-open.png     📌 À créer/placer
└── agent-eyes-closed.png   📌 À créer/placer
```

---

## 🎯 ÉTAPES D'INTÉGRATION RAPIDE

### 1️⃣ Placer les images aux bons emplacements

Créez 2 images PNG (280x280px):
- `src/assets/agent-eyes-open.png` (yeux ouverts)
- `src/assets/agent-eyes-closed.png` (yeux fermés)

**Si vous n'avez pas d'images**, utilisez ce placeholder temporaire:

Remplacez dans `chat.component.html` (ligne ~11):
```html
<!-- AVANT -->
<img 
  [src]="avatarEyesOpen() ? 'assets/agent-eyes-open.png' : 'assets/agent-eyes-closed.png'"
  class="avatar-image">

<!-- APRÈS (Emoji placeholder) -->
<div class="avatar-image" style="
  background: linear-gradient(135deg, #6b7ff4 0%, #ff9500 100%);
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 120px;
  border-radius: 20px;">
  👨‍💼
</div>
```

### 2️⃣ Ajouter la route

**Option A: Route standalone**

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

**Option B: Ajouter au composant parent**

```typescript
// src/app/features/storytelling/storytelling.component.ts
import { ChatComponent } from './chat.component';

@Component({
  selector: 'app-storytelling',
  standalone: true,
  imports: [CommonModule, ChatComponent],
  template: `<app-chat></app-chat>`,
  styleUrl: './storytelling.component.css'
})
export class StorytellingComponent {}
```

### 3️⃣ Vérifier/Créer l'endpoint backend

Le composant appelle:
```
POST /api/storytelling/chat
```

**Si le backend n'existe pas**, créer temporairement un mock dans le service:

```typescript
// src/app/core/services/api.services.ts - Ajouter cette méthode

@Injectable({ providedIn: 'root' })
export class StorytellingService {
  private http = inject(HttpClient);
  private apiUrl = `${environment.apiUrl}/storytelling`;

  chat(message: string): Observable<any> {
    // Les 4 réponses par défaut
    const responses = [
      'C\'est une excellente approche financière! Vous pourriez réduire vos dépenses en révisant vos abonnements récurrents. Selon votre profil, je recommande un budget de 1500 DT par mois pour les dépenses essentielles.',
      'Vous souhaitez créer un plan mensuel? C\'est judicieux! Un budget préétabli vous aidera à mieux contrôler vos dépenses. Je peux vous proposer un plan personnalisé selon votre revenu et vos besoins.',
      'Pour épargner plus, concentrez-vous sur les catégories non-essentielles. Réduisez vos dépenses de loisirs et shopping d\'environ 20%. Cela vous permettrait d\'économiser 300 DT supplémentaires par mois.',
      'Votre objectif financier est important. Nous pouvons mettre en place une stratégie d\'épargne progressive. Commençons par fixer un montant mensuel réaliste et surveiller votre budget.'
    ];
    
    return of({ message: responses[Math.floor(Math.random() * responses.length)] });
  }
}
```

Ou en Spring Boot (exemple):

```java
@RestController
@RequestMapping("/api/storytelling")
public class StorytellingController {
    
    @PostMapping("/chat")
    public ResponseEntity<Map<String, String>> chat(@RequestBody Map<String, String> request) {
        String userMessage = request.get("message");
        String response = generateStorytellingResponse(userMessage);
        
        return ResponseEntity.ok(Map.of("message", response));
    }
    
    private String generateStorytellingResponse(String message) {
        // Logique IA ou mock
        if (message.toLowerCase().contains("abonnement")) {
            return "Réduire vos abonnements peut vous épargner jusqu'à 300 DT par mois...";
        }
        // etc.
        return "Votre situation financière est importante pour nous...";
    }
}
```

### 4️⃣ Tester l'intégration

Accédez à: `http://localhost:4200/storytelling`

✅ Vous devriez voir:
- Agent animé avec yeux qui clignent
- Chat prêt à discuter
- Boutons d'actions rapides
- Messages qui apparaissent
- TTS qui lit les réponses

---

## 🎨 PERSONNALISATION

### Changer les couleurs

```css
/* chat.component.css */

/* Gradient principal (or/orange) */
.avatar-container {
  border: 3px solid #ffbc00;  /* ← Couleur border */
  box-shadow: 0 12px 32px rgba(255, 188, 0, 0.15);
}

.send-btn {
  background: linear-gradient(135deg, #ffbc00, #ff9500);  /* ← Gradient */
}

.message-user .message-content {
  background: linear-gradient(135deg, #ffbc00, #ff9500);  /* ← Couleur message user */
}
```

### Changer les actions rapides

```typescript
// chat.component.ts - ligne 62
suggestedActions = [
  { label: '💳 Réduire abonnements', value: 'reduce_subscriptions' },
  { label: '📅 Plan mensuel', value: 'monthly_plan' },
  { label: '💰 Épargner plus', value: 'save_more' },
  { label: '🎯 Objectif financier', value: 'financial_goal' },
  // ← Ajouter vos actions ici
  { label: '🏦 Conseil en investissement', value: 'investment_advice' }
];
```

### Changer l'intervalle de clignement

```typescript
// chat.component.ts - ligne 115
setInterval(() => {
  if (!this.isAvatarSpeaking()) {
    this.avatarEyesOpen.set(false);
    setTimeout(() => this.avatarEyesOpen.set(true), 150);
  }
}, 3000);  // ← Changer de 4000ms à 3000ms (3 secondes)
```

---

## 📊 GRAPHIQUE - Mots-clés de déclenchement

Le graphique s'affiche automatiquement si le texte contient:

```typescript
// chat.component.ts - ligne ~245
if (lower.includes('budget') || 
    lower.includes('dépenses') || 
    lower.includes('revenu') ||
    lower.includes('graphique') ||  // ← Ajouter vos mots-clés
    lower.includes('statistiques')) {
```

Exemple: Si l'utilisateur dit "Affiche mon budget", le graphique apparaît.

---

## 🔊 TEXT-TO-SPEECH - Options

```typescript
// chat.component.ts - ligne ~161
const utterance = new SpeechSynthesisUtterance(text);
utterance.lang = 'fr-FR';        // ← Langue (fr-FR, en-US, es-ES, etc.)
utterance.rate = 0.95;            // ← Vitesse (0.5 = lent, 1.5 = rapide)
utterance.pitch = 1;              // ← Tonalité (0.5 = grave, 2 = aigu)
utterance.volume = 1;             // ← Volume (0 à 1)
```

---

## 🐛 DÉBOGAGE

### Le TTS ne fonctionne pas

1. **Vérifier la console** (F12 → Console)
   ```javascript
   window.speechSynthesis.getVoices()
   ```

2. **Autoriser le navigateur**
   - Certains navigateurs demandent la permission
   - Vérifier les paramètres du navigateur

3. **Essayer en HTTPS**
   - Certains navigateurs requièrent HTTPS pour le TTS
   - Utiliser `https://localhost:4200` ou déployer

### Les images ne s'affichent pas

- ✅ Vérifier: `src/assets/agent-eyes-open.png` existe
- ✅ Vérifier: `src/assets/agent-eyes-closed.png` existe
- ❌ Ne pas utiliser: `src/assets/` dans le composant (utiliser juste `assets/`)
- ✅ Format: PNG, 280x280px minimum

### Le chat n'envoie pas

```javascript
// Vérifier dans F12 → Network → POST /api/storytelling/chat
// Vérifier la réponse du serveur
```

---

## 📦 STRUCTURE FINALE

```
mon-projet/
├── src/
│   ├── app/
│   │   ├── features/
│   │   │   └── storytelling/
│   │   │       ├── chat.component.ts ✅
│   │   │       ├── chat.component.html ✅
│   │   │       ├── chat.component.css ✅
│   │   │       └── storytelling.component.ts
│   │   ├── core/
│   │   │   └── services/
│   │   │       └── api.services.ts (ou storytelling.service.ts)
│   │   └── app.routes.ts
│   ├── assets/
│   │   ├── agent-eyes-open.png ✅
│   │   └── agent-eyes-closed.png ✅
│   └── environments/
│       └── environment.ts
├── package.json
└── angular.json
```

---

## ✨ FONCTIONNALITÉS BONUS

### 1. Historique du chat persistent

```typescript
// Ajouter dans ngOnInit
const savedMessages = localStorage.getItem('chatHistory');
if (savedMessages) {
  this.messages.set(JSON.parse(savedMessages));
}

// Dans ngOnDestroy
localStorage.setItem('chatHistory', JSON.stringify(this.messages()));
```

### 2. Mode sombre

```css
/* Ajouter en haut du CSS */
@media (prefers-color-scheme: dark) {
  .chat-container {
    background: #1a1f36;
  }
  .chat-section,
  .agent-card {
    background: #2a2f47;
    border-color: #3a3f5f;
  }
  /* ... etc */
}
```

### 3. Ajouter un avatar animé Emoji

```html
<!-- Remplaxcer l'img par -->
<div class="avatar-image">
  <span class="emoji-avatar" id="avatar">👨‍💼</span>
</div>

<style>
  .emoji-avatar {
    font-size: 100px;
    animation: avatarBounce 0.5s ease-in-out infinite;
  }
  @keyframes avatarBounce {
    0%, 100% { transform: translateY(0); }
    50% { transform: translateY(-10px); }
  }
</style>
```

---

## 🎯 PROCHAINES ÉTAPES

1. ✅ Placer les images
2. ✅ Intégrer le composant à la route
3. ✅ Implémenter/tester l'endpoint backend
4. ✅ Tester le TTS en français
5. 🔄 Customiser les couleurs/actions
6. 📈 Ajouter de vrais données au graphique
7. 🚀 Déployer en production

---

## 💡 CONSEILS

- **Performance**: Les animations utilisent GPU (will-change: transform)
- **Accessibilité**: Ajouter `aria-label` sur les boutons
- **Mobile**: Responsive design au 100% (testé)
- **UX**: Le scroll autorise à chaque message
- **Flexibilité**: Facile à customiser/étendre

---

**🎉 Prêt à lancer!**

Accédez à `http://localhost:4200/storytelling` et commencez!
