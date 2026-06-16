# Diagrammes UML - Attijari Compass

Ce dossier contient les diagrammes PlantUML finaux pour la soutenance PFE.

- `rapport/` : versions plus detaillees pour le rapport.
- `slides/` : versions simplifiees pour la presentation.
- `rendered/rapport/` : rendus PNG et SVG des versions rapport.
- `rendered/slides/` : rendus PNG et SVG des versions slides.
- `tools/plantuml.jar` : jar PlantUML utilise pour generer les rendus.

Diagrammes disponibles :

1. Cas d'utilisation global
2. Diagramme de classes metier
3. Sequence d'authentification JWT
4. Sequence import CSV/Excel et categorisation ML
5. Sequence chatbot RAG avec Groq
6. Sequence synchronisation carte bancaire
7. Diagramme de composants
8. Diagramme de deploiement
9. Architecture globale

Commande de rendu avec le jar local :

```powershell
java -jar docs/uml/tools/plantuml.jar -tsvg -o ../rendered/rapport docs/uml/rapport/*.puml
java -jar docs/uml/tools/plantuml.jar -tpng -o ../rendered/rapport docs/uml/rapport/*.puml
java -jar docs/uml/tools/plantuml.jar -tsvg -o ../rendered/slides docs/uml/slides/*.puml
java -jar docs/uml/tools/plantuml.jar -tpng -o ../rendered/slides docs/uml/slides/*.puml
```

Verification syntaxique :

```powershell
java -jar docs/uml/tools/plantuml.jar -checkonly docs/uml/rapport/*.puml docs/uml/slides/*.puml
```
