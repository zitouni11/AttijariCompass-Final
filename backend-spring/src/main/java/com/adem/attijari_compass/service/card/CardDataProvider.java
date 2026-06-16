package com.adem.attijari_compass.service.card;

import com.adem.attijari_compass.dto.card.CardCatalogDto;
import com.adem.attijari_compass.dto.card.CardDetailsDto;
import com.adem.attijari_compass.dto.card.CardSummaryDto;
import com.adem.attijari_compass.dto.card.CardTransactionDto;
import com.adem.attijari_compass.dto.card.LinkCardRequest;
import com.adem.attijari_compass.dto.card.UserCardLinkResponse;

import java.util.List;

public interface CardDataProvider {

    List<CardCatalogDto> getCardCatalog();

    List<CardSummaryDto> getUserCards(Long userId);

    CardDetailsDto getCardDetails(Long userId, Long cardId);

    List<CardTransactionDto> getCardTransactions(Long userId, Long cardId);

    UserCardLinkResponse linkCard(Long userId, LinkCardRequest request);

    void unlinkCard(Long userId, Long cardId);
}
