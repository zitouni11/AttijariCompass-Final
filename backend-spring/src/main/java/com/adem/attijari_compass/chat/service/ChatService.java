package com.adem.attijari_compass.chat.service;

import com.adem.attijari_compass.chat.config.GroqProperties;
import com.adem.attijari_compass.chat.dto.ChatResponseDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.OffsetDateTime;
import java.util.Locale;

@Service
@RequiredArgsConstructor
@Slf4j
public class ChatService {

    private final RagService ragService;
    private final GroqService groqService;
    private final GroqProperties groqProperties;
    private final ChatContextFormatter formatter;

    public ChatResponseDto chat(Long userId, String userMessage) {
        long startedAt = System.currentTimeMillis();

        String ragContext = ragService.buildAdaptiveContext(userId, userMessage);
        String selectedModel = shouldUseAdvancedModel(userMessage)
                ? groqProperties.getAdvancedModel()
                : groqProperties.getModel();

        String systemPrompt = """
                Vous etes Attijari Compass AI, un assistant financier bancaire premium.
                Repondez exclusivement en francais.
                Appuyez-vous uniquement sur le contexte fourni.
                N'inventez jamais une donnee absente.
                Si une information manque, dites-le clairement.
                Distinguez toujours:
                - les donnees utilisateur reelles
                - l'explication du projet ou des modules Attijari Compass
                N'affichez jamais les balises techniques du contexte comme [APP_CONTEXT], [TRANSACTION_CONTEXT], [MONTHLY_CONTEXT] ou toute autre etiquette interne.
                N'expliquez jamais que vous utilisez un RAG, un debug context, des sections internes ou des metadonnees techniques.
                Donnez des reponses claires, professionnelles, precises et orientees action.
                Si la question porte sur le projet, l'application, un module, un score ou un simulateur, priorisez [APP_CONTEXT] puis la section metier correspondante.
                Si la question porte sur des transactions globales, utilisez [TRANSACTION_CONTEXT].
                Si la question porte sur un mois, une comparaison de mois, "ce mois", "mois dernier" ou un mois nomme, utilisez [MONTHLY_CONTEXT].
                Si [MONTHLY_CONTEXT] contient des donnees pour la periode demandee, utilisez-les et ne dites pas que les donnees sont absentes.
                Pour le mois courant, les montants canoniques de [MONTHLY_CONTEXT] priment sur les extrapolations faites a partir d'exemples de transactions.
                Ne recalculez jamais librement les totaux a partir des lignes de transactions si une valeur canonique existe deja.
                Ne combinez jamais les montants historiques, les tendances, les recommandations ou les transactions de detail pour produire un nouveau total sans support explicite dans le contexte.
                Ne melangez jamais historique global et mois courant dans une meme reponse chiffree sans le dire clairement.
                Si la severite du mois courant est CRITICAL, dites clairement que le mois courant est critique.
                Ne qualifiez jamais la situation d'excellente, saine ou stable si les revenus du mois sont a 0 avec des depenses positives.
                Ne qualifiez jamais la situation d'excellente, saine ou stable si le solde net du mois est negatif.
                Quand c'est pertinent, structurez la reponse en:
                1. Constat
                2. Explication
                3. Action recommandee
                """;

        String userPrompt = "Contexte utilisateur:\n"
                + ragContext
                + "\n\nQuestion utilisateur:\n"
                + userMessage.trim();

        GroqService.GroqAnswer answer = groqService.askWithMetadata(selectedModel, systemPrompt, userPrompt);
        long durationMs = System.currentTimeMillis() - startedAt;
        log.info("Chat completed: userId={}, contextLength={}, model={}, durationMs={}",
                userId,
                ragContext.length(),
                answer.usedModel(),
                durationMs);

        return ChatResponseDto.builder()
                .answer(answer.content())
                .ragContextPreview(formatter.truncate(ragContext, 900))
                .usedModel(answer.usedModel())
                .timestamp(OffsetDateTime.now())
                .build();
    }

    private boolean shouldUseAdvancedModel(String userMessage) {
        if (!StringUtils.hasText(userMessage)) {
            return false;
        }

        String normalized = formatter.normalizeKeywordText(userMessage);
        return userMessage.length() > 220
                || containsAny(normalized, "analyse", "pourquoi", "plan", "complet", "detaille", "detaillee", "strategie", "realiste");
    }

    private boolean containsAny(String normalizedValue, String... tokens) {
        for (String token : tokens) {
            if (normalizedValue.contains(token.toLowerCase(Locale.ROOT))) {
                return true;
            }
        }
        return false;
    }
}
