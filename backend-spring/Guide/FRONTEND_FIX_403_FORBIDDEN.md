# 🔒 ERREUR 403 FORBIDDEN - GUIDE DE CORRECTION

## 🔴 PROBLÈME

```
POST http://localhost:59947/api/auth/login 403 (Forbidden)
```

**Causes possibles:**
1. ❌ Email/Password incorrects
2. ❌ L'utilisateur n'existe pas
3. ❌ Données mal formatées
4. ❌ Configuration CORS

---

## ✅ SOLUTION

### **Étape 1: Vérifier/Créer un utilisateur de test**

**Sur le backend**, utilise Swagger pour créer un compte d'abord :

```
1. Aller à: http://localhost:8081/swagger-ui/index.html
2. Chercher: POST /api/auth/register
3. Cliquer: Try it out
4. Remplir:
   {
     "email": "test@example.com",
     "password": "Password123",
     "role": "USER"
   }
5. Execute
```

**Résultat attendu:** Status 201 Created avec token

---

### **Étape 2: Utiliser les credentials dans le frontend**

Utilise exactement les mêmes credentials pour login :

```
Email: test@example.com
Password: Password123
```

---

### **Étape 3: Remplacer tes composants login/register**

Copie les fichiers corrigés :
- **`FRONTEND_FIX_login.component.ts`** → `src/app/components/login/login.component.ts`
- **`FRONTEND_FIX_login.component.html`** → `src/app/components/login/login.component.html`
- **`FRONTEND_FIX_register.component.ts`** → `src/app/components/register/register.component.ts`
- **`FRONTEND_FIX_register.component.html`** → `src/app/components/register/register.component.html`

---

### **Étape 4: Vérifier l'AuthService**

Assure-toi que `src/app/services/auth.service.ts` contient :

```typescript
import { environment } from '../../../environments/environment';

export class AuthService {
  private apiUrl = environment.apiUrl;

  login(email: string, password: string): Observable<any> {
    return this.http.post(
      `${this.apiUrl}/auth/login`,
      { email, password },
      { headers: new HttpHeaders({ 'Content-Type': 'application/json' }) }
    ).pipe(
      tap((response: any) => {
        if (response.token) {
          localStorage.setItem('token', response.token);
          localStorage.setItem('refreshToken', response.refreshToken);
        }
      })
    );
  }
}
```

---

### **Étape 5: Redémarrer et tester**

```bash
# Arrête Angular (Ctrl+C)
ng serve
```

Puis :
1. Va à http://localhost:4200/register
2. Crée un nouveau compte
3. Essaie de te connecter
4. Vérifie dans DevTools (F12 → Network) que:
   - ✅ Status 200 OK (pas 403)
   - ✅ Response contient `token` et `refreshToken`

---

## 🧪 TEST ÉTAPE PAR ÉTAPE

### Test 1: Register via Swagger (Backend)
```
✅ POST http://localhost:8081/api/auth/register
✅ Status: 201 Created
✅ Response: { token, refreshToken, email, role }
```

### Test 2: Login via Swagger (Backend)
```
✅ POST http://localhost:8081/api/auth/login
✅ Status: 200 OK
✅ Response: { token, refreshToken, email, role }
```

### Test 3: Register via Frontend
```
✅ POST http://localhost:8081/api/auth/register
✅ Token sauvegardé dans localStorage
✅ Redirection à /dashboard
```

### Test 4: Login via Frontend
```
✅ POST http://localhost:8081/api/auth/login
✅ Token sauvegardé dans localStorage
✅ Redirection à /dashboard
```

---

## 🎯 Point critique

Tu DOIS utiliser `environment.apiUrl` dans tous les services :

❌ MAUVAIS:
```typescript
private apiUrl = 'http://localhost:8081/api';
```

✅ BON:
```typescript
import { environment } from '../../../environments/environment';
private apiUrl = environment.apiUrl;
```

---

## 📋 CHECKLIST

- [ ] `src/environments/environment.ts` créé avec `apiUrl: 'http://localhost:8081/api'`
- [ ] `AuthService` utilise `environment.apiUrl`
- [ ] `login.component.ts` remplacé par `FRONTEND_FIX_login.component.ts`
- [ ] `login.component.html` remplacé par `FRONTEND_FIX_login.component.html`
- [ ] `register.component.ts` remplacé par `FRONTEND_FIX_register.component.ts`
- [ ] `register.component.html` remplacé par `FRONTEND_FIX_register.component.html`
- [ ] Angular redémarré (`ng serve`)
- [ ] Backend tourne (`mvn spring-boot:run`)

---

## 🔐 Si tu obtiens toujours 403

**Debugger:**

1. Ouvre F12 → Network → Cherche la requête login
2. Clique dessus
3. Regarde le **Request Body**:
   ```json
   {
     "email": "test@example.com",
     "password": "Password123"
   }
   ```

4. Regarde le **Response**:
   - Si `{ "message": "User not found" }` → Email n'existe pas
   - Si `{ "message": "Bad credentials" }` → Password incorrect
   - Si `{ "message": "... " }` → Autre erreur

5. **Solution**: Va d'abord à `/register` et crée un compte avec le même email/password

---

**Fais ces changements et dis-moi le résultat! 🚀**

