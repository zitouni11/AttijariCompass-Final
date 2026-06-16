# 🎯 GUIDE IMAGES & ASSETS

## 📍 Placement des fichiers

```
c:\Users\ademz\Downloads\attijari-compass-frontend-FINAL\
attijari-compass-frontend\
├── src\
│   ├── app\
│   │   └── features\
│   │       └── storytelling\
│   │           ├── chat.component.ts ✅
│   │           ├── chat.component.html ✅
│   │           └── chat.component.css ✅
│   └── assets\          ← 📌 ICI
│       ├── agent-eyes-open.png   ← À créer
│       └── agent-eyes-closed.png ← À créer
└── INTEGRATION_RAPIDE.md
```

---

## 🖼️ IMAGES REQUISES

### Spécifications techniques

| Propriété | Valeur |
|-----------|--------|
| Dimensions | 280 × 280 pixels |
| Format | PNG |
| Transparence | Optionnelle (mais recommandée) |
| Couleur fond | Transparent ou solide |
| Type | Photo réaliste ou avatar |

### Exemple: agent-eyes-open.png
```
- Photo d'un homme en costume bancaire
- Yeux OUVERTS et naturels
- Sourire professionnel
- Arrière-plan simple ou transparent
- Bien éclairé
```

### Exemple: agent-eyes-closed.png
```
- MÊME photo que eyes-open.png
- Mais avec yeux FERMÉS (clignotement)
- Tous les autres détails identiques
- Arrière-plan identique
```

---

## 🎨 CRÉER LES IMAGES RAPIDEMENT

### Option 1: Utiliser des photos existantes

1. Trouver 2 photos d'un banquier/conseiller
2. Ouvrir dans Photoshop/GIMP:
   ```
   Fichier → Ouvrir → image.jpg
   Image → Redimensionner → 280x280
   Fichier → Exporter → agent-eyes-open.png
   ```

### Option 2: Générer avec une IA (Gratuit)

1. Aller sur [Remove.bg](https://remove.bg)
2. Uploader une photo
3. Supprimer le fond
4. Redimensionner à 280x280
5. Exporter en PNG
6. Dupliquer et appliquer un effet "yeux fermés"

### Option 3: Placer un placeholder Emoji (Temporaire)

Si vous voulez tester IMMÉDIATEMENT sans images réelles:

**Modifier `chat.component.html` (ligne ~11):**

```html
<!-- AVANT (utilise les images) -->
<img 
  [src]="avatarEyesOpen() ? 'assets/agent-eyes-open.png' : 'assets/agent-eyes-closed.png'"
  alt="Agent Bancaire"
  class="avatar-image">

<!-- APRÈS (utilise emoji) -->
<div class="avatar-placeholder">
  <span class="emoji" [class.blink]="!avatarEyesOpen()">👨‍💼</span>
</div>
```

**Ajouter le CSS dans `chat.component.css`:**

```css
.avatar-placeholder {
  width: 280px;
  height: 280px;
  background: linear-gradient(135deg, #6b7ff4 0%, #ff9500 100%);
  border-radius: 20px;
  display: flex;
  align-items: center;
  justify-content: center;
  border: 3px solid #ffbc00;
  box-shadow: 0 12px 32px rgba(255, 188, 0, 0.15);
}

.emoji {
  font-size: 120px;
  transition: opacity 0.15s ease;
  display: block;
}

.emoji.blink {
  opacity: 0.3;  /* Simuler les yeux fermés */
}
```

### Option 4: Utiliser des APIs de génération (Temps réel)

```javascript
// Générer un avatar aléatoire avec DiceBear
const avatarUrl = `https://api.dicebear.com/7.x/avataaars/svg?seed=${Math.random()}`;

// Dans chat.component.html:
<img [src]="'https://api.dicebear.com/7.x/avataaars/svg?seed=banker'" alt="Agent">
```

---

## ✅ VÉRIFIER L'INTÉGRATION

### Après avoir placé les images

1. **Copier les fichiers PNG dans `src/assets/`**
   ```bash
   # Windows
   copy "agent-eyes-open.png" "src\assets\"
   copy "agent-eyes-closed.png" "src\assets\"
   ```

2. **Démarrer le serveur dev**
   ```bash
   ng serve
   ```

3. **Accéder à la page**
   ```
   http://localhost:4200/storytelling
   ```

4. **Vérifier dans le navigateur (F12)**
   - Onglet "Network"
   - Chercher les requêtes "agent-eyes-open.png"
   - Status 200 = ✅ En succès

---

## 🐛 DÉBOGAGE IMAGES

### Les images ne s'affichent pas?

**Checklist:**
- [ ] Le fichier existe: `src/assets/agent-eyes-open.png` ?
- [ ] Nom exact (minuscules, pas d'espaces) ?
- [ ] Format: PNG, JPG, ou WebP ?
- [ ] Taille: au moins 280x280px ?
- [ ] Console (F12): Erreur 404 ?

**Solution rapide:**
```html
<!-- Déboguer le chemin dans HTML -->
<img src="assets/agent-eyes-open.png" alt="Debug">

<!-- Si ça fonctionne ici, le chemin est bon -->
```

### La couleur/taille est bizarr?

```css
/* Forcer les dimensions exactes */
.avatar-image {
  width: 280px !important;
  height: 280px !important;
  object-fit: cover;  /* Recadrer l'image si besoin */
  object-position: center;  /* Centrer au lieu de crop -->
}
```

---

## 📸 EXEMPLE COMPLET IMAGE

### Scénario: Vous avez une photo `.jpg` d'un banquier

**Étape 1: Préparer l'image**
```bash
# Sur Windows, utiliser GIMP ou Paint
1. Ouvrir Paint
2. Fichier → Ouvrir → banker.jpg
3. Image → Redimensionner → 280x280
4. Fichier → Exporter → agent-eyes-open.png
```

**Étape 2: Créer la version yeux fermés**
```bash
# GIMP:
1. Filtres → Distorsion → Yeux
2. Ou: Outils → Sélectionner → Élipse (sélectionner les yeux)
3. Édition → Remplir → couleur de peau
4. Fichier → Exporter → agent-eyes-closed.png
```

**Étape 3: Placer dans le projet**
```bash
agent-eyes-open.png   → src/assets/agent-eyes-open.png
agent-eyes-closed.png → src/assets/agent-eyes-closed.png
```

**Étape 4: Tester**
```bash
ng serve
# Accéder à http://localhost:4200/storytelling
```

---

## 🎬 ANIMATION DE L'ŒIL

### Comment ça fonctionne?

```typescript
// chat.component.ts - Toutes les 4 secondes:
setInterval(() => {
  // 1. Afficher les yeux fermés
  this.avatarEyesOpen.set(false);
  
  // 2. Attendre 150ms
  setTimeout(() => {
    // 3. Afficher les yeux ouverts
    this.avatarEyesOpen.set(true);
  }, 150);
}, 4000);
```

### Résultat:
```
Sec 0-4:    Yeux ouverts  👀
Sec 4:      CLIGNEMENT    ↓
  - 150ms:  Yeux fermés   😴
  + 150ms:  Yeux ouverts  👀
Sec 4-8:    Yeux ouverts  👀
Sec 8:      CLIGNEMENT    ↓ (répète)
```

---

## 🔧 CUSTOMISER L'ASPECT

### Changer la taille de l'avatar

```css
/* Rendre plus grand */
.avatar-container {
  width: 350px;   /* ← Augmenter */
  height: 350px;  /* ← Augmenter */
}
```

### Ajouter un contour/effet

```css
.avatar-image {
  border: 5px solid #ffbc00;
  border-radius: 20px;
  box-shadow: 0 20px 40px rgba(0, 0, 0, 0.3);
  filter: drop-shadow(0 4px 8px rgba(255, 188, 0, 0.3));
}
```

### Animation de l'avatar (bonus)

```css
.avatar-image {
  animation: avatarFloat 3s ease-in-out infinite;
}

@keyframes avatarFloat {
  0%, 100% { transform: translateY(0); }
  50% { transform: translateY(-10px); }
}
```

---

## 📦 ASSETS ADDITIONNELS (Optionnel)

Si vous voulez enrichir le projet:

```
src/assets/
├── agent-eyes-open.png       ✅ Requiss
├── agent-eyes-closed.png     ✅ Requis
├── logo-bank.png             (optionnel)
├── icon-chart.svg            (optionnel)
├── background-pattern.svg    (optionnel)
└── sounds/
    └── notification.mp3      (optionnel)
```

---

## 🎯 RÉSUMÉ RAPIDE

1. **Créer 2 images PNG** (280x280):
   - `agent-eyes-open.png`
   - `agent-eyes-closed.png`

2. **Placer dans** `src/assets/`

3. **Vérifier** http://localhost:4200/storytelling

4. **Prêt!** L'animation fonctionnera 🚀

---

**Besoin d'aide pour créer les images? Consultez [cette doc](https://remove.bg/docs) ou utilisez le placeholder Emoji temporaire.**
