# ⚡ QUICK FIX - ERREUR 403 FORBIDDEN

## 📌 LA CAUSE PRINCIPALE

Tu essaies de te connecter avec des credentials qui **n'existent pas** dans la base de données.

---

## ✅ FIX IMMÉDIAT (5 minutes)

### **Étape 1: Créer un compte via Swagger**

1. Ouvre: http://localhost:8081/swagger-ui/index.html
2. Cherche: **POST /api/auth/register**
3. Clique: **Try it out**
4. Remplis:
   ```json
   {
     "email": "test@example.com",
     "password": "Password123",
     "role": "USER"
   }
   ```
5. Clique: **Execute**
6. Tu dois voir: **Status 201 Created**

---

### **Étape 2: Utiliser ces credentials dans Angular**

Maintenant dans ton app Angular (http://localhost:4200):

1. Va à `/login`
2. Entre:
   ```
   Email: test@example.com
   Password: Password123
   ```
3. Clique: **Connexion**

**Résultat attendu:** ✅ Connexion réussie, redirection à /dashboard

---

## 🎯 SI TU AS TOUJOURS 403

**Vérifier:**

1. ✅ Le backend tourne? (`mvn spring-boot:run`)
2. ✅ Angular tourne? (`ng serve`)
3. ✅ L'utilisateur a été créé via Swagger?
4. ✅ Tu utilises exactement les mêmes credentials?

---

## 📁 FICHIERS À COPIER (optionnel mais recommandé)

Ces fichiers améliorent l'UI et la gestion d'erreurs:

```
✅ FRONTEND_FIX_login.component.ts
   → src/app/components/login/login.component.ts

✅ FRONTEND_FIX_login.component.html
   → src/app/components/login/login.component.html

✅ FRONTEND_FIX_register.component.ts
   → src/app/components/register/register.component.ts

✅ FRONTEND_FIX_register.component.html
   → src/app/components/register/register.component.html
```

---

## 🚀 C'EST FAIT!

Une fois que tu es connecté:
- ✅ Crée une transaction
- ✅ Enregistre un paiement
- ✅ Teste l'auto-catégorisation

---

**Dis-moi quand c'est connecté! 🎉**

