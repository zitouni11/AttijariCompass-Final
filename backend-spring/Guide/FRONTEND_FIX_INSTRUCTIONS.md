# 🔧 FRONTEND FIX - ERREUR 500 CORRIGÉE

## 📋 PROBLÈME
```
POST http://localhost:4200/api/transactions 500 (Internal Server Error)
```

**Cause:** Les requêtes HTTP vont vers `localhost:4200` (Angular) au lieu de `localhost:8081` (Backend Spring Boot).

---

## ✅ SOLUTION EN 4 ÉTAPES

### **ÉTAPE 1: Créer le fichier environment**

**Crée:** `src/environments/environment.ts`

Copie le contenu de: `FRONTEND_FIX_environment.ts`

---

### **ÉTAPE 2: Remplacer le TransactionService**

**Remplace complètement:** `src/app/services/transaction.service.ts`

Copie le contenu de: `FRONTEND_FIX_transaction.service.ts`

---

### **ÉTAPE 3: Remplacer le composant transaction-form-dialog**

**Remplace complètement:** `src/app/components/transaction-form-dialog/transaction-form-dialog.component.ts`

Copie le contenu de: `FRONTEND_FIX_transaction-form-dialog.component.ts`

---

### **ÉTAPE 4: Remplacer le template**

**Remplace complètement:** `src/app/components/transaction-form-dialog/transaction-form-dialog.component.html`

Copie le contenu de: `FRONTEND_FIX_transaction-form-dialog.component.html`

---

## 🧪 TESTER

1. ✅ **Redémarre Angular:**
   ```bash
   # Arrête le serveur (Ctrl+C)
   # Relance
   ng serve
   ```

2. ✅ **Ouvre les DevTools (F12 → Network)**

3. ✅ **Essaie de créer une transaction**

4. ✅ **Vérifie que les requêtes vont à:**
   ```
   ✅ http://localhost:8081/api/transactions/card-payment
   ✅ http://localhost:8081/api/transactions
   ```
   
   **Et NON à:**
   ```
   ❌ http://localhost:4200/api/transactions
   ```

---

## ✨ RÉSULTAT ATTENDU

Après ces changements :
- ✅ Les requêtes vont au bon URL (8081)
- ✅ L'auto-catégorisation fonctionne
- ✅ Les transactions s'enregistrent correctement
- ✅ Les nouveaux champs s'affichent (merchantName, cardLast4, etc.)

---

## 📝 FICHIERS MODIFIÉS

| Fichier | Action |
|---------|--------|
| `src/environments/environment.ts` | ✅ Créer |
| `src/app/services/transaction.service.ts` | ✏️ Remplacer |
| `src/app/components/transaction-form-dialog/transaction-form-dialog.component.ts` | ✏️ Remplacer |
| `src/app/components/transaction-form-dialog/transaction-form-dialog.component.html` | ✏️ Remplacer |

---

## 🎯 CAS PARTICULIERS

### Si tu utilises un autre service API

Cherche TOUS les fichiers `.service.ts` dans `src/app/services/` et modifie chacun pour :

```typescript
import { environment } from '../../../environments/environment';

export class XyzService {
  private apiUrl = environment.apiUrl;  // ← UTILISE ENVIRONMENT
  
  // Au lieu de hardcoder:
  // private apiUrl = 'http://localhost:8081/api';  ❌ MAUVAIS
}
```

---

## 💡 ASTUCE: Vérifier toutes les URLs

Dans VSCode, fais une recherche globale :
```
Ctrl+Shift+F
Cherche: "localhost:4200"
```

Et remplace TOUTES les occurrences par:
```
${environment.apiUrl}
```

---

## 📞 SI ÇA NE MARCHE TOUJOURS PAS

1. ✅ Vérifie que le backend tourne: `http://localhost:8081/swagger-ui/index.html`
2. ✅ Regarde la console Angular (F12 → Console) pour les erreurs CORS
3. ✅ Vérifie que le token JWT est sauvegardé dans `localStorage`
4. ✅ Redémarre complètement Angular (`Ctrl+C` puis `ng serve`)

---

**Fait ces changements et dis-moi si l'erreur 500 disparaît! 🚀**

