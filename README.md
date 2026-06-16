# AttijariCompass Final

AttijariCompass Final est une application de gestion financiere composee d'un backend Spring Boot, d'un frontend Angular et d'une documentation UML.

## Structure du projet

- `backend-spring/` : API backend Spring Boot, securite JWT, services metier, recommandations, cartes, transactions, budgets, objectifs et module ML.
- `frontend-angular/` : application web Angular pour l'interface utilisateur.
- `docs/` : diagrammes UML et ressources de documentation.
- `maven-settings.xml` : configuration Maven locale du projet.
- `START_HERE_INTEGRATED.txt` : guide de demarrage rapide.

## Prerequis

- Java 17+
- Maven ou Maven Wrapper
- Node.js 18+
- npm
- PostgreSQL

## Demarrage du backend

```powershell
cd backend-spring
.\mvnw.cmd spring-boot:run
```

Configuration principale :

- `SPRING_DATASOURCE_URL`
- `SPRING_DATASOURCE_USERNAME`
- `SPRING_DATASOURCE_PASSWORD`
- `JWT_SECRET`
- `GROQ_API_KEY`

## Demarrage du frontend

```powershell
cd frontend-angular
npm install
npm start
```

Par defaut, le frontend Angular demarre sur `http://localhost:4200`.

## Build

Backend :

```powershell
cd backend-spring
.\mvnw.cmd -DskipTests compile
```

Frontend :

```powershell
cd frontend-angular
npm run build
```

## Branch principale

Tout le projet final est publie sur une seule branche : `main`.
