package com.adem.attijari_compass.recommendation.service;

import com.adem.attijari_compass.chat.config.GroqProperties;
import com.adem.attijari_compass.chat.service.GroqService;
import com.adem.attijari_compass.chat.service.RagService;
import com.adem.attijari_compass.recommendation.dto.RecommendationExplanationRequest;
import com.adem.attijari_compass.recommendation.dto.RecommendationExplanationResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.Locale;

@Service
@RequiredArgsConstructor
@Slf4j
public class RecommendationExplanationService {

    private static final int MAX_EXPLANATION_LENGTH = 420;

    private final RagService ragService;
    private final GroqService groqService;
    private final GroqProperties groqProperties;

    public RecommendationExplanationResponse generateRecommendationExplanation(
            Long userId,
            RecommendationExplanationRequest request
    ) {
        String fallback = buildFallback(request);

        try {
            String context = ragService.buildAdaptiveContext(userId, buildContextQuestion(request));
            String explanation = askModel(context, request);

            if (StringUtils.hasText(explanation)) {
                return RecommendationExplanationResponse.builder()
                        .explanation(limit(cleanExplanation(explanation)))
                        .source("RAG")
                        .fallbackUsed(false)
                        .build();
            }
        } catch (Exception ex) {
            log.warn("Recommendation explanation fallback used: userId={}, title={}, reason={}",
                    userId,
                    request.getTitle(),
                    ex.getMessage());
        }

        return RecommendationExplanationResponse.builder()
                .explanation(fallback)
                .source("FALLBACK")
                .fallbackUsed(true)
                .build();
    }

    public String buildFallback(RecommendationExplanationRequest request) {
        String normalized = normalize(String.join(" ",
                valueOrEmpty(request.getCategory()),
                valueOrEmpty(request.getType()),
                valueOrEmpty(request.getTitle()),
                valueOrEmpty(request.getMessage()),
                valueOrEmpty(request.getSuggestedAction())
        ));
        String amount = formatEstimatedImpact(request);
        String potential = formatPotential(request);
        String targetedTotal = formatTargetedTransactions(request);

        if (containsAny(normalized, "cafe", "cafes", "coffee")) {
            return "Cette recommandation est prioritaire car vos dépenses cafés semblent récurrentes"
                    + targetedTotal
                    + amount
                    + " et peuvent être ajustées progressivement sans impact majeur sur votre quotidien"
                    + potential
                    + ".";
        }

        if (containsAny(normalized, "shopping", "achat", "achats", "mode")) {
            return "Cette recommandation est prioritaire car cette catégorie contient probablement des achats reportables"
                    + targetedTotal
                    + amount
                    + " pouvant améliorer rapidement votre capacité d’épargne"
                    + potential
                    + ".";
        }

        if (containsAny(normalized, "epargne", "saving", "savings", "objectif")) {
            return "Cette recommandation est prioritaire car une légère hausse de votre effort d’épargne peut accélérer l’atteinte de votre objectif"
                    + formatSavingsRate(request)
                    + amount
                    + potential
                    + ".";
        }

        if (containsAny(normalized, "restaurant", "sortie", "repas", "restauration")) {
            return "Cette recommandation est prioritaire car vos sorties et repas non planifiés peuvent peser sur le budget mensuel"
                    + targetedTotal
                    + amount
                    + " et offrir une marge d’ajustement rapide"
                    + potential
                    + ".";
        }

        if (containsAny(normalized, "budget", "plafond")) {
            return "Cette recommandation est prioritaire car un budget cible peut encadrer cette catégorie et rendre l’effort plus facile à suivre"
                    + amount
                    + potential
                    + ".";
        }

        return "Cette recommandation est prioritaire car elle relie vos signaux financiers récents à une action concrète"
                + targetedTotal
                + amount
                + " susceptible d’améliorer votre trajectoire mensuelle"
                + potential
                + ".";
    }

    private String askModel(String context, RecommendationExplanationRequest request) {
        String systemPrompt = """
                Vous êtes Attijari Compass AI, un assistant financier bancaire premium.
                Répondez exclusivement en français.
                Utilisez uniquement le contexte fourni.
                Rédigez une seule explication courte de 2 phrases maximum.
                Ne mentionnez pas de détails techniques internes.
                N'inventez jamais de chiffres.
                Si le total réel et l'impact mensuel estimé diffèrent, présentez clairement le total comme un total filtré sur la période et l'impact comme une estimation.
                Si un chiffre est absent ou incohérent, évitez les montants exacts.
                Expliquez pourquoi la recommandation est prioritaire selon la catégorie, la période, la tendance, l'objectif ou l'impact potentiel.
                """;

        String userPrompt = """
                Contexte financier utilisateur:
                %s

                Recommandation à expliquer:
                - Titre: %s
                - Catégorie: %s
                - Type: %s
                - Période: %s
                - Total réel des transactions ciblées: %s
                - Impact mensuel estimé: %s
                - Potentiel visuel: %s
                - Taux d'épargne: %s
                - Objectif lié: %s
                - Score financier: %s
                - Statut global: %s
                - Message: %s
                - Action: %s
                """.formatted(
                context,
                valueOrEmpty(request.getTitle()),
                valueOrEmpty(request.getCategory()),
                valueOrEmpty(request.getType()),
                valueOrDefault(request.getPeriod(), "non fournie"),
                formatPromptAmount(request.getTargetedTransactionsTotal()),
                formatPromptAmount(firstNonNull(request.getMonthlyImpactEstimated(), request.getAmount())),
                request.getPotentialPercent() == null ? "non fourni" : request.getPotentialPercent() + " %",
                request.getSavingsRate() == null ? "non fourni" : request.getSavingsRate() + " %",
                valueOrEmpty(request.getGoalTitle()),
                request.getFinancialScore() == null ? "non fourni" : request.getFinancialScore() + "/100",
                valueOrEmpty(request.getGlobalStatus()),
                valueOrEmpty(request.getMessage()),
                valueOrEmpty(request.getSuggestedAction())
        );

        return groqService.ask(groqProperties.getModel(), systemPrompt, userPrompt);
    }

    private String buildContextQuestion(RecommendationExplanationRequest request) {
        return "Explique pourquoi cette recommandation financière est prioritaire: "
                + valueOrEmpty(request.getTitle())
                + " "
                + valueOrEmpty(request.getCategory());
    }

    private String cleanExplanation(String value) {
        String cleaned = value
                .replace('\n', ' ')
                .replaceAll("\\s+", " ")
                .trim();

        if ((cleaned.startsWith("\"") && cleaned.endsWith("\""))
                || (cleaned.startsWith("'") && cleaned.endsWith("'"))) {
            cleaned = cleaned.substring(1, cleaned.length() - 1).trim();
        }

        return cleaned;
    }

    private String limit(String value) {
        if (value.length() <= MAX_EXPLANATION_LENGTH) {
            return value;
        }

        return value.substring(0, MAX_EXPLANATION_LENGTH - 1).trim() + ".";
    }

    private String formatEstimatedImpact(RecommendationExplanationRequest request) {
        Double amount = firstNonNull(request.getMonthlyImpactEstimated(), request.getAmount());

        if (amount == null || !Double.isFinite(amount) || amount <= 0) {
            return "";
        }

        return " avec un impact mensuel estimé autour de " + Math.round(amount) + " DT";
    }

    private String formatTargetedTransactions(RecommendationExplanationRequest request) {
        Double total = request.getTargetedTransactionsTotal();

        if (total == null || !Double.isFinite(total) || total <= 0) {
            return "";
        }

        String period = StringUtils.hasText(request.getPeriod()) ? " sur " + request.getPeriod() : "";
        return " dont le total ciblé" + period + " atteint " + Math.round(total) + " DT";
    }

    private String formatSavingsRate(RecommendationExplanationRequest request) {
        Double savingsRate = request.getSavingsRate();

        if (savingsRate == null || !Double.isFinite(savingsRate)) {
            return "";
        }

        return " alors que votre taux d’épargne est proche de " + Math.round(savingsRate) + " %";
    }

    private String formatPotential(RecommendationExplanationRequest request) {
        Integer potentialPercent = request.getPotentialPercent();

        if (potentialPercent == null || potentialPercent <= 0) {
            return "";
        }

        String normalized = normalize(String.join(" ",
                valueOrEmpty(request.getCategory()),
                valueOrEmpty(request.getType()),
                valueOrEmpty(request.getTitle())
        ));

        if (containsAny(normalized, "epargne", "saving", "savings")) {
            return " avec un effort supplémentaire estimé d’environ " + potentialPercent + " %";
        }

        if (containsAny(normalized, "objectif", "goal", "target")) {
            return " avec une progression estimée d’environ " + potentialPercent + " % vers l’objectif";
        }

        if (containsAny(normalized, "budget")) {
            return " avec une marge d’optimisation estimée d’environ " + potentialPercent + " %";
        }

        return " avec un potentiel de réduction estimé d’environ " + potentialPercent + " %";
    }

    private String formatPromptAmount(Double amount) {
        if (amount == null || !Double.isFinite(amount)) {
            return "non fourni";
        }

        return Math.round(amount) + " DT";
    }

    private Double firstNonNull(Double first, Double second) {
        return first != null ? first : second;
    }

    private String valueOrDefault(String value, String fallback) {
        return StringUtils.hasText(value) ? value.trim() : fallback;
    }

    private boolean containsAny(String value, String... tokens) {
        for (String token : tokens) {
            if (value.contains(token)) {
                return true;
            }
        }
        return false;
    }

    private String normalize(String value) {
        return java.text.Normalizer.normalize(value, java.text.Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .toLowerCase(Locale.ROOT);
    }

    private String valueOrEmpty(String value) {
        return value == null ? "" : value.trim();
    }
}
