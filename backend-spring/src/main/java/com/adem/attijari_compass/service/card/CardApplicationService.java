package com.adem.attijari_compass.service.card;

import com.adem.attijari_compass.dto.card.CardCatalogDto;
import com.adem.attijari_compass.dto.card.CardDetailsDto;
import com.adem.attijari_compass.dto.card.CardSummaryDto;
import com.adem.attijari_compass.dto.card.CardTransactionDto;
import com.adem.attijari_compass.dto.card.LinkCardRequest;
import com.adem.attijari_compass.dto.card.UserCardLinkResponse;
import com.adem.attijari_compass.exception.ResourceNotFoundException;
import com.adem.attijari_compass.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CardApplicationService {

    private final CardDataProviderFactory cardDataProviderFactory;
    private final UserRepository userRepository;

    public List<CardSummaryDto> getCurrentUserCards(String email) {
        return getUserCards(resolveUserId(email));
    }

    public List<CardCatalogDto> getCardCatalog() {
        return cardDataProviderFactory.getActiveProvider().getCardCatalog();
    }

    public CardDetailsDto getCurrentUserCardDetails(String email, Long cardId) {
        return getCardDetails(resolveUserId(email), cardId);
    }

    public List<CardTransactionDto> getCurrentUserCardTransactions(String email, Long cardId) {
        return getCardTransactions(resolveUserId(email), cardId);
    }

    @Transactional
    public UserCardLinkResponse linkCurrentUserCard(String email, LinkCardRequest request) {
        return linkCard(resolveUserId(email), request);
    }

    @Transactional
    public void unlinkCurrentUserCard(String email, Long cardId) {
        unlinkCard(resolveUserId(email), cardId);
    }

    public List<CardSummaryDto> getUserCards(Long userId) {
        return cardDataProviderFactory.getActiveProvider().getUserCards(userId);
    }

    public CardDetailsDto getCardDetails(Long userId, Long cardId) {
        return cardDataProviderFactory.getActiveProvider().getCardDetails(userId, cardId);
    }

    public List<CardTransactionDto> getCardTransactions(Long userId, Long cardId) {
        return cardDataProviderFactory.getActiveProvider().getCardTransactions(userId, cardId);
    }

    @Transactional
    public UserCardLinkResponse linkCard(Long userId, LinkCardRequest request) {
        return cardDataProviderFactory.getActiveProvider().linkCard(userId, request);
    }

    @Transactional
    public void unlinkCard(Long userId, Long cardId) {
        cardDataProviderFactory.getActiveProvider().unlinkCard(userId, cardId);
    }

    private Long resolveUserId(String email) {
        return userRepository.findByEmail(email)
                .map(user -> user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("User not found with email: " + email));
    }
}
