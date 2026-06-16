package com.adem.attijari_compass.recommendation.storytelling;

import com.adem.attijari_compass.recommendation.dto.RecommendationDto;
import com.adem.attijari_compass.recommendation.dto.RecommendationResponseDto;
import com.adem.attijari_compass.recommendation.dto.RecommendationSummaryDto;
import com.adem.attijari_compass.recommendation.enums.CurrentMonthSeverity;
import com.adem.attijari_compass.service.storytelling.OllamaClient;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.StringJoiner;

@Service
@RequiredArgsConstructor
@Slf4j
public class RecommendationStorytellingService {

    private static final String SYSTEM_PROMPT = """
            Tu es un conseiller bancaire digital haut de gamme.
            Tu analyses des recommandations deja calculees par un moteur financier.
            Ta mission est de produire une narration premium, fluide, naturelle et utile.

            STYLE OBLIGATOIRE :
            - Ecris en francais naturel, clair et professionnel.
            - Adresse-toi toujours au client avec "vous".
            - Adopte le ton d'un conseiller financier rassurant, precis et sobre.
            - Fais des phrases courtes, elegantes et faciles a lire.
            - Evite toute repetition.

            INTERDICTIONS :
            - N'utilise jamais "l'utilisateur".
            - N'invente jamais de chiffres, de categories ou de recommandations.
            - N'utilise pas d'anglicismes inutiles.
            - N'emploie pas de formulations maladroites ou mecaniques.
            - Ne recopie pas simplement les recommandations.

            GARDE-FOUS METIER OBLIGATOIRES :
            - Priorise toujours le mois courant avant l'historique.
            - Si currentMonthSeverity = CRITICAL, dis clairement que le mois courant est critique ou sous forte pression.
            - Si les revenus du mois sont a 0 et les depenses du mois sont > 0, n'utilise jamais les mots "excellent", "saine", "sain" ou "stable".
            - Si le solde net du mois est negatif, n'utilise jamais les mots "excellent", "saine", "sain" ou "stable" pour qualifier la situation.
            - Ne minimise jamais une situation critique avec un ton trop positif.

            ATTENTES PAR CHAMP :
            - summary : une seule phrase, vue d'ensemble claire et synthetique.
            - mainConcern : une seule phrase, le probleme principal formule naturellement.
            - opportunity : une seule phrase, ton positif, mentionner le gain potentiel si disponible.
            - action : une seule phrase, concrete, simple et orientee decision.

            FORMAT :
            - Reponds STRICTEMENT en JSON valide avec exactement ces champs :
              summary, mainConcern, opportunity, action
            - Ne mets pas de markdown.
            """;

    private final OllamaClient ollamaClient;
    private final ObjectMapper objectMapper;

    public RecommendationStorytellingDto generateStory(RecommendationResponseDto response) {
        if (response == null) {
            log.warn("Recommendation storytelling fallback used: response is null");
            return buildFallback(null);
        }

        try {
            String prompt = buildPrompt(response);
            String llmResponse = callLlm(prompt);
            RecommendationStorytellingDto parsedStorytelling = parseResponse(llmResponse);

            if (parsedStorytelling == null) {
                log.warn("Recommendation storytelling fallback used: LLM response could not be parsed");
                return buildFallback(response);
            }

            RecommendationStorytellingDto storytelling = completeMissingFields(parsedStorytelling, response);
            log.info("Recommendation storytelling generated successfully");
            return storytelling;
        } catch (Exception ex) {
            log.warn("Recommendation storytelling fallback used: {}", ex.getMessage());
            return buildFallback(response);
        }
    }

    private String buildPrompt(RecommendationResponseDto response) {
        RecommendationSummaryDto summary = response.getSummary();
        List<RecommendationDto> topRecommendations = safeRecommendations(response).stream()
                .limit(3)
                .toList();

        String globalStatus = defaultText(summary != null ? summary.getGlobalStatus() : null, "INCONNU");
        int totalRecommendations = summary != null && summary.getTotalRecommendations() != null
                ? summary.getTotalRecommendations()
                : 0;
        int highCount = summary != null && summary.getHighCount() != null
                ? summary.getHighCount()
                : 0;
        String financialScore = summary != null && summary.getFinancialScore() != null
                ? String.valueOf(summary.getFinancialScore())
                : "0";
        String financialScoreLabel = defaultText(summary != null ? summary.getFinancialScoreLabel() : null, "Inconnu");
        String currentMonthSeverity = summary != null && summary.getCurrentMonthSeverity() != null
                ? summary.getCurrentMonthSeverity().name()
                : CurrentMonthSeverity.NORMAL.name();
        String currentMonthStatusLabel = defaultText(summary != null ? summary.getCurrentMonthStatusLabel() : null, "Normale");
        String currentMonthIncome = defaultDouble(summary != null ? summary.getCurrentMonthIncome() : null);
        String currentMonthExpenses = defaultDouble(summary != null ? summary.getCurrentMonthExpenses() : null);
        String currentMonthNetBalance = defaultDouble(summary != null ? summary.getCurrentMonthNetBalance() : null);
        String currentMonthSavingsRate = defaultDouble(summary != null ? summary.getCurrentMonthSavingsRate() : null);
        String potentialGain = defaultDouble(summary != null ? summary.getTotalEstimatedMonthlyGain() : null);
        String aiSummary = defaultText(summary != null ? summary.getAiSummary() : null, "Aucune synthese complementaire disponible");

        StringJoiner recommendationsBlock = new StringJoiner("\n");
        if (topRecommendations.isEmpty()) {
            recommendationsBlock.add("1. Titre: Aucune recommandation prioritaire disponible");
            recommendationsBlock.add("   Message: Le moteur n'a pas remonte de recommandation actionnable");
            recommendationsBlock.add("   Priorite: UNKNOWN");
        } else {
            for (int i = 0; i < topRecommendations.size(); i++) {
                RecommendationDto recommendation = topRecommendations.get(i);
                recommendationsBlock.add((i + 1) + ". Titre: " + defaultText(recommendation.getTitle(), "Sans titre"));
                recommendationsBlock.add("   Message: " + defaultText(recommendation.getMessage(), "Aucun message"));
                recommendationsBlock.add("   Priorite: " + (recommendation.getPriority() != null ? recommendation.getPriority().name() : "UNKNOWN"));
            }
        }

        return """
                Voici la situation financiere a interpreter :

                Statut global : %s
                Score financier : %s/100 (%s)
                currentMonthSeverity : %s
                Statut du mois courant : %s
                Revenus du mois courant : %s DT
                Depenses du mois courant : %s DT
                Solde net du mois courant : %s DT
                Taux d'epargne du mois courant : %s%%
                Nombre de recommandations : %d
                Recommandations prioritaires : %d
                Gain mensuel potentiel : %s DT
                Synthese moteur : %s

                Recommandations principales :
                %s

                Analyse la situation et genere une narration premium.

                Contraintes :
                - utiliser "vous"
                - francais naturel et fluide
                - ton conseiller bancaire premium
                - pas d'invention de chiffres
                - pas de repetition
                - zero formulation robotique
                - zero mot maladroit comme "shoppings" ou "principal preoccupant"
                - chaque champ doit tenir en une phrase
                - expliquer la logique, pas seulement reformuler
                - si le gain potentiel est disponible, l'utiliser naturellement dans opportunity
                - ne jamais dire "l'utilisateur"
                - toujours prioriser le mois courant avant l'historique
                - si currentMonthSeverity = CRITICAL, le dire clairement dans summary et mainConcern
                - ne jamais qualifier la situation d'excellente, saine ou stable si revenus du mois = 0 et depenses > 0
                - ne jamais qualifier la situation d'excellente, saine ou stable si le solde net du mois est negatif

                Repond STRICTEMENT en JSON :
                {
                  "summary": "...",
                  "mainConcern": "...",
                  "opportunity": "...",
                  "action": "..."
                }
                """.formatted(
                globalStatus,
                financialScore,
                financialScoreLabel,
                currentMonthSeverity,
                currentMonthStatusLabel,
                currentMonthIncome,
                currentMonthExpenses,
                currentMonthNetBalance,
                currentMonthSavingsRate,
                totalRecommendations,
                highCount,
                potentialGain,
                aiSummary,
                recommendationsBlock
        );
    }

    private String callLlm(String prompt) {
        log.info("Calling Ollama for recommendation storytelling");
        log.debug("Recommendation storytelling system prompt: {}", SYSTEM_PROMPT);
        log.debug("Recommendation storytelling user prompt: {}", prompt);
        String response = ollamaClient.generateStructuredJson(SYSTEM_PROMPT, prompt);
        log.debug("Recommendation storytelling raw LLM response: {}", response);
        return response;
    }

    private RecommendationStorytellingDto parseResponse(String response) {
        if (!StringUtils.hasText(response)) {
            log.warn("Recommendation storytelling parsing skipped: empty LLM response");
            return null;
        }

        try {
            String json = extractJson(response);
            log.debug("Recommendation storytelling extracted JSON: {}", json);
            RecommendationStorytellingDto parsed = objectMapper.readValue(json, RecommendationStorytellingDto.class);
            if (!hasAnyText(parsed)) {
                log.warn("Recommendation storytelling parsing produced an empty DTO");
                return null;
            }
            log.info("Recommendation storytelling JSON parsed successfully");
            return parsed;
        } catch (Exception ex) {
            log.error("Recommendation storytelling parsing failed: {}", ex.getMessage());
            return null;
        }
    }

    private RecommendationStorytellingDto buildFallback(RecommendationResponseDto response) {
        RecommendationSummaryDto summary = response != null ? response.getSummary() : null;
        List<RecommendationDto> recommendations = safeRecommendations(response);
        RecommendationDto topRecommendation = recommendations.isEmpty() ? null : recommendations.get(0);

        String summaryText = buildFallbackSummary(summary);
        String mainConcern = buildFallbackMainConcern(summary, topRecommendation);
        String opportunity = buildFallbackOpportunity(summary, recommendations);
        String action = buildFallbackAction(topRecommendation);

        return RecommendationStorytellingDto.builder()
                .summary(summaryText)
                .mainConcern(mainConcern)
                .opportunity(opportunity)
                .action(action)
                .build();
    }

    private String buildFallbackSummary(RecommendationSummaryDto summary) {
        if (summary != null && summary.getCurrentMonthSeverity() == CurrentMonthSeverity.CRITICAL) {
            return "Le mois courant est critique et demande une action rapide sur vos priorites budgetaires.";
        }

        if (summary == null || !StringUtils.hasText(summary.getGlobalStatus())) {
            return "Votre situation financiere necessite une attention particuliere.";
        }

        return switch (summary.getGlobalStatus().trim().toUpperCase(Locale.ROOT)) {
            case "CRITIQUE" -> "Votre situation financiere demande une action rapide sur plusieurs priorites.";
            case "A SURVEILLER" -> "Votre situation financiere merite une attention rapprochee sur les principaux postes identifies.";
            case "STABLE" -> "Votre situation financiere reste globalement stable, avec quelques leviers d'optimisation.";
            default -> "Votre situation financiere necessite une attention particuliere.";
        };
    }

    private String buildFallbackMainConcern(RecommendationSummaryDto summary, RecommendationDto topRecommendation) {
        if (summary != null && summary.getCurrentMonthSeverity() == CurrentMonthSeverity.CRITICAL) {
            return String.format(
                    Locale.ROOT,
                    "Le mois courant montre un desequilibre net avec %.2f DT de revenus, %.2f DT de depenses et un solde de %.2f DT.",
                    summary.getCurrentMonthIncome() != null ? summary.getCurrentMonthIncome() : 0.0d,
                    summary.getCurrentMonthExpenses() != null ? summary.getCurrentMonthExpenses() : 0.0d,
                    summary.getCurrentMonthNetBalance() != null ? summary.getCurrentMonthNetBalance() : 0.0d
            );
        }

        if (topRecommendation != null && StringUtils.hasText(topRecommendation.getMessage())) {
            return topRecommendation.getMessage();
        }
        if (summary != null && Objects.equals(summary.getHighCount(), 0)) {
            return "La priorite immediate reste limitee, mais certains ajustements peuvent encore renforcer votre equilibre.";
        }
        return "La priorite concerne vos depenses et votre equilibre budgetaire.";
    }

    private String buildFallbackOpportunity(RecommendationSummaryDto summary, List<RecommendationDto> recommendations) {
        if (summary != null && summary.getCurrentMonthSeverity() == CurrentMonthSeverity.CRITICAL) {
            Double gain = summary.getTotalEstimatedMonthlyGain();
            if (gain != null && gain > 0.0d) {
                return "Une fois l'equilibre du mois retabli, les leviers identifies peuvent encore liberer environ " + round(gain) + " DT par mois.";
            }
            return "La premiere opportunite consiste a stopper la derive du mois avant de chercher une optimisation plus large.";
        }

        Double gain = summary != null ? summary.getTotalEstimatedMonthlyGain() : null;
        if (gain != null && gain > 0.0d) {
            return "Plusieurs leviers peuvent ameliorer votre marge mensuelle, avec un potentiel estime a " + round(gain) + " DT.";
        }
        if (!recommendations.isEmpty()) {
            return "Plusieurs leviers d'optimisation existent pour renforcer votre situation financiere.";
        }
        return "Plusieurs leviers peuvent ameliorer votre marge mensuelle.";
    }

    private String buildFallbackAction(RecommendationDto topRecommendation) {
        if (topRecommendation != null && StringUtils.hasText(topRecommendation.getSuggestedAction())) {
            return topRecommendation.getSuggestedAction();
        }
        return "Commencez par agir sur vos depenses les plus variables.";
    }

    private String extractJson(String rawResponse) {
        String cleanedResponse = rawResponse == null ? "" : rawResponse
                .replace("```json", "")
                .replace("```", "")
                .trim();

        int start = cleanedResponse.indexOf('{');
        if (start < 0) {
            return cleanedResponse;
        }

        boolean inQuotes = false;
        boolean escaped = false;
        int depth = 0;

        for (int i = start; i < cleanedResponse.length(); i++) {
            char current = cleanedResponse.charAt(i);

            if (escaped) {
                escaped = false;
                continue;
            }

            if (current == '\\') {
                escaped = true;
                continue;
            }

            if (current == '"') {
                inQuotes = !inQuotes;
                continue;
            }

            if (inQuotes) {
                continue;
            }

            if (current == '{') {
                depth++;
            } else if (current == '}') {
                depth--;
                if (depth == 0) {
                    return cleanedResponse.substring(start, i + 1);
                }
            }
        }

        return cleanedResponse.substring(start);
    }

    private RecommendationStorytellingDto completeMissingFields(RecommendationStorytellingDto parsed,
                                                                RecommendationResponseDto response) {
        RecommendationStorytellingDto fallback = buildFallback(response);
        return RecommendationStorytellingDto.builder()
                .summary(defaultText(parsed.getSummary(), fallback.getSummary()))
                .mainConcern(defaultText(parsed.getMainConcern(), fallback.getMainConcern()))
                .opportunity(defaultText(parsed.getOpportunity(), fallback.getOpportunity()))
                .action(defaultText(parsed.getAction(), fallback.getAction()))
                .build();
    }

    private boolean hasAnyText(RecommendationStorytellingDto dto) {
        return dto != null && (StringUtils.hasText(dto.getSummary())
                || StringUtils.hasText(dto.getMainConcern())
                || StringUtils.hasText(dto.getOpportunity())
                || StringUtils.hasText(dto.getAction()));
    }

    private List<RecommendationDto> safeRecommendations(RecommendationResponseDto response) {
        if (response == null || response.getRecommendations() == null) {
            return List.of();
        }
        return response.getRecommendations().stream()
                .filter(Objects::nonNull)
                .toList();
    }

    private String defaultText(String value, String fallback) {
        return StringUtils.hasText(value) ? value.trim() : fallback;
    }

    private String defaultDouble(Double value) {
        return value != null ? round(value) : "0.0";
    }

    private String round(Double value) {
        return String.format(Locale.ROOT, "%.2f", value);
    }
}
