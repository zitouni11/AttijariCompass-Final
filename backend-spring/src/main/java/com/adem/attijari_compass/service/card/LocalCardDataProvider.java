package com.adem.attijari_compass.service.card;

import com.adem.attijari_compass.dto.card.CardCatalogDto;
import com.adem.attijari_compass.dto.card.CardDetailsDto;
import com.adem.attijari_compass.dto.card.CardSummaryDto;
import com.adem.attijari_compass.dto.card.CardTransactionDto;
import com.adem.attijari_compass.dto.card.LinkCardRequest;
import com.adem.attijari_compass.dto.card.UserCardLinkResponse;
import com.adem.attijari_compass.entity.CardCatalog;
import com.adem.attijari_compass.entity.CardPool;
import com.adem.attijari_compass.entity.CardSourceType;
import com.adem.attijari_compass.entity.CardStatus;
import com.adem.attijari_compass.entity.CardTransaction;
import com.adem.attijari_compass.entity.CardType;
import com.adem.attijari_compass.entity.User;
import com.adem.attijari_compass.entity.UserCard;
import com.adem.attijari_compass.exception.CardAlreadyLinkedException;
import com.adem.attijari_compass.exception.CardNotFoundException;
import com.adem.attijari_compass.exception.CardOwnershipException;
import com.adem.attijari_compass.exception.InvalidCardCatalogException;
import com.adem.attijari_compass.exception.ResourceNotFoundException;
import com.adem.attijari_compass.repository.CardCatalogRepository;
import com.adem.attijari_compass.repository.CardPoolRepository;
import com.adem.attijari_compass.repository.CardTransactionRepository;
import com.adem.attijari_compass.repository.UserCardRepository;
import com.adem.attijari_compass.repository.UserRepository;
import com.adem.attijari_compass.util.CardMaskingUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;

@Service("local")
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class LocalCardDataProvider implements CardDataProvider {

    private static final String BANK_NAME = "Attijari Bank Tunisie";

    private final UserRepository userRepository;
    private final UserCardRepository userCardRepository;
    private final CardCatalogRepository cardCatalogRepository;
    private final CardPoolRepository cardPoolRepository;
    private final CardTransactionRepository cardTransactionRepository;
    private final CardTransactionSyncService cardTransactionSyncService;

    @Override
    public List<CardCatalogDto> getCardCatalog() {
        return cardCatalogRepository.findAllByActiveTrueOrderByNameAsc().stream()
                .map(this::toCatalogDto)
                .toList();
    }

    @Override
    public List<CardSummaryDto> getUserCards(Long userId) {
        ensureUserExists(userId);
        return userCardRepository.findActiveManagedCardsByUserId(userId).stream()
                .map(this::toSummary)
                .toList();
    }

    @Override
    public CardDetailsDto getCardDetails(Long userId, Long cardId) {
        return toDetails(getManagedCardForUser(userId, cardId));
    }

    @Override
    public List<CardTransactionDto> getCardTransactions(Long userId, Long cardId) {
        getManagedCardForUser(userId, cardId);
        return cardTransactionRepository.findAllByUserIdAndCardIdOrderByTransactionDateDesc(userId, cardId).stream()
                .map(this::toTransaction)
                .toList();
    }

    @Override
    @Transactional
    public UserCardLinkResponse linkCard(Long userId, LinkCardRequest request) {
        User user = getRequiredUser(userId);
        CardCatalog cardCatalog = cardCatalogRepository.findByIdAndActiveTrue(request.getCardCatalogId())
                .orElseThrow(() -> new InvalidCardCatalogException(
                        "Catalogue de carte introuvable ou inactif pour id: " + request.getCardCatalogId()
                ));

        String normalizedHolderName = resolveConnectedUserHolderName(user);
        String normalizedCardNumber = normalizeCardNumber(request.getCardNumber());

        CardPool cardPool = cardPoolRepository.findFirstByCardCatalog_IdAndCardNumberAndExpiryMonthAndExpiryYearAndCardHolderNameIgnoreCase(
                        request.getCardCatalogId(),
                        normalizedCardNumber,
                        request.getExpiryMonth(),
                        request.getExpiryYear(),
                        normalizedHolderName
                )
                .orElseThrow(() -> new CardNotFoundException("Carte introuvable ou non autorisée pour votre compte."));

        if (cardPool.isAssigned()) {
            if (userId.equals(cardPool.getAssignedUserId())) {
                throw new CardAlreadyLinkedException("Cette carte est deja associee a votre compte");
            }
            throw new CardAlreadyLinkedException("Cette carte est deja associee");
        }

        UserCard userCard = resolveUserCardForLink(userId, user, cardPool, cardCatalog);
        cardTransactionSyncService.syncPoolTransactions(userCard, cardPool);

        cardPool.setAssigned(true);
        cardPool.setAssignedUserId(userId);
        cardPoolRepository.save(cardPool);

        return toLinkResponse(userCard);
    }

    @Override
    @Transactional
    public void unlinkCard(Long userId, Long cardId) {
        UserCard userCard = getManagedCardForUser(userId, cardId);
        boolean wasPrimary = userCard.isPrimaryCard();

        userCard.setActive(false);
        userCard.setPrimaryCard(false);
        userCard.setCardStatus(CardStatus.INACTIVE);
        userCard.setStatus(CardStatus.INACTIVE);
        userCardRepository.save(userCard);

        if (userCard.getCardPool() != null) {
            CardPool cardPool = userCard.getCardPool();
            cardPool.setAssigned(false);
            cardPool.setAssignedUserId(null);
            cardPoolRepository.save(cardPool);
        }

        if (wasPrimary) {
            userCardRepository.findActiveManagedCardsByUserId(userId).stream()
                    .findFirst()
                    .ifPresent(nextPrimary -> {
                        nextPrimary.setPrimaryCard(true);
                        userCardRepository.save(nextPrimary);
                    });
        }
    }

    private UserCard resolveUserCardForLink(Long userId, User user, CardPool cardPool, CardCatalog cardCatalog) {
        List<UserCard> existingCards = userCardRepository.findAllByUserIdAndCardPoolIdOrderByIdDesc(userId, cardPool.getId());

        if (existingCards.stream().anyMatch(UserCard::isActive)) {
            throw new CardAlreadyLinkedException("Cette carte est deja associee a votre compte");
        }

        boolean firstCard = userCardRepository.findActiveManagedCardsByUserId(userId).isEmpty();
        LocalDateTime now = LocalDateTime.now();

        UserCard userCard = existingCards.isEmpty() ? new UserCard() : existingCards.getFirst();
        userCard.setUser(user);
        userCard.setCardCatalog(cardCatalog);
        userCard.setCardPool(cardPool);
        userCard.setCardHolderName(cardPool.getCardHolderName());
        userCard.setHolderName(cardPool.getCardHolderName());
        userCard.setCardNumber(cardPool.getCardNumber());
        userCard.setMaskedCardNumber(cardPool.getMaskedCardNumber());
        userCard.setLast4(cardPool.getLast4());
        userCard.setExpiryMonth(cardPool.getExpiryMonth());
        userCard.setExpiryYear(cardPool.getExpiryYear());
        userCard.setCardCode(cardPool.getCardCode());
        userCard.setCardStatus(CardStatus.ACTIVE);
        userCard.setStatus(CardStatus.ACTIVE);
        userCard.setLinkedAt(now);
        userCard.setConnectedAt(now);
        userCard.setPrimaryCard(firstCard);
        userCard.setSourceType(CardSourceType.DEMO_POOL);
        userCard.setBankName(BANK_NAME);
        userCard.setCardType(resolveLegacyCardType(cardCatalog));
        userCard.setActive(true);

        return userCardRepository.save(userCard);
    }

    private UserCard getManagedCardForUser(Long userId, Long cardId) {
        ensureUserExists(userId);
        return userCardRepository.findManagedCardByIdAndUserId(cardId, userId)
                .orElseGet(() -> {
                    userCardRepository.findManagedCardById(cardId)
                            .orElseThrow(() -> new CardNotFoundException("Carte introuvable"));
                    throw new CardOwnershipException("Cette carte n'appartient pas a l'utilisateur connecte");
                });
    }

    private User getRequiredUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));
    }

    private void ensureUserExists(Long userId) {
        if (!userRepository.existsById(userId)) {
            throw new ResourceNotFoundException("User not found with id: " + userId);
        }
    }

    private String normalizeHolderName(String value) {
        return value == null
                ? ""
                : value.trim().replaceAll("\\s+", " ").toUpperCase(Locale.ROOT);
    }

    private String resolveConnectedUserHolderName(User user) {
        String fullName = normalizeHolderName(user.getFullName());

        if (!fullName.isBlank()) {
            return fullName;
        }

        String email = user.getEmail() == null ? "" : user.getEmail().trim();
        int atIndex = email.indexOf('@');
        String localPart = atIndex > 0 ? email.substring(0, atIndex) : email;
        String fallback = localPart
                .replaceAll("[._-]+", " ")
                .replaceAll("\\s+", " ")
                .trim();

        return normalizeHolderName(fallback);
    }

    private String normalizeCardNumber(String value) {
        String normalized = CardMaskingUtil.normalizeCardNumber(value);
        if (normalized.length() < 13 || normalized.length() > 19) {
            throw new IllegalArgumentException("cardNumber must contain between 13 and 19 digits");
        }
        return normalized;
    }

    private CardType resolveLegacyCardType(CardCatalog cardCatalog) {
        String brand = cardCatalog.getBrand() == null ? "" : cardCatalog.getBrand().trim().toUpperCase(Locale.ROOT);
        return brand.contains("MASTER") ? CardType.MASTERCARD : CardType.VISA;
    }

    private CardSummaryDto toSummary(UserCard userCard) {
        CardCatalog cardCatalog = userCard.getCardCatalog();
        return CardSummaryDto.builder()
                .id(userCard.getId())
                .cardCatalogId(cardCatalog.getId())
                .cardCatalogCode(cardCatalog.getCode())
                .cardCatalogName(cardCatalog.getName())
                .brand(cardCatalog.getBrand())
                .scope(cardCatalog.getScope())
                .cardHolderName(userCard.getCardHolderName())
                .maskedCardNumber(userCard.getMaskedCardNumber())
                .last4(userCard.getLast4())
                .expiryMonth(userCard.getExpiryMonth())
                .expiryYear(userCard.getExpiryYear())
                .cardCode(userCard.getCardCode())
                .cardStatus(resolveStatus(userCard))
                .primaryCard(userCard.isPrimaryCard())
                .sourceType(userCard.getSourceType())
                .imageUrl(cardCatalog.getImageUrl())
                .linkedAt(userCard.getLinkedAt())
                .build();
    }

    private CardDetailsDto toDetails(UserCard userCard) {
        CardCatalog cardCatalog = userCard.getCardCatalog();
        return CardDetailsDto.builder()
                .id(userCard.getId())
                .cardCatalogId(cardCatalog.getId())
                .cardCatalogCode(cardCatalog.getCode())
                .cardCatalogName(cardCatalog.getName())
                .description(cardCatalog.getDescription())
                .brand(cardCatalog.getBrand())
                .scope(cardCatalog.getScope())
                .cardHolderName(userCard.getCardHolderName())
                .maskedCardNumber(userCard.getMaskedCardNumber())
                .last4(userCard.getLast4())
                .expiryMonth(userCard.getExpiryMonth())
                .expiryYear(userCard.getExpiryYear())
                .cardCode(userCard.getCardCode())
                .cardStatus(resolveStatus(userCard))
                .primaryCard(userCard.isPrimaryCard())
                .active(userCard.isActive())
                .sourceType(userCard.getSourceType())
                .maxPaymentLimit(cardCatalog.getMaxPaymentLimit())
                .maxWithdrawalLimit(cardCatalog.getMaxWithdrawalLimit())
                .allowsOnlinePayment(cardCatalog.isAllowsOnlinePayment())
                .allowsInternationalPayment(cardCatalog.isAllowsInternationalPayment())
                .allowsInstallments(cardCatalog.isAllowsInstallments())
                .installmentMonthsMax(cardCatalog.getInstallmentMonthsMax())
                .imageUrl(cardCatalog.getImageUrl())
                .linkedAt(userCard.getLinkedAt())
                .createdAt(userCard.getCreatedAt())
                .updatedAt(userCard.getUpdatedAt())
                .build();
    }

    private UserCardLinkResponse toLinkResponse(UserCard userCard) {
        CardCatalog cardCatalog = userCard.getCardCatalog();
        return UserCardLinkResponse.builder()
                .id(userCard.getId())
                .cardCatalogId(cardCatalog.getId())
                .cardCatalogCode(cardCatalog.getCode())
                .cardCatalogName(cardCatalog.getName())
                .cardHolderName(userCard.getCardHolderName())
                .maskedCardNumber(userCard.getMaskedCardNumber())
                .last4(userCard.getLast4())
                .expiryMonth(userCard.getExpiryMonth())
                .expiryYear(userCard.getExpiryYear())
                .cardCode(userCard.getCardCode())
                .cardStatus(resolveStatus(userCard))
                .primaryCard(userCard.isPrimaryCard())
                .sourceType(userCard.getSourceType())
                .linkedAt(userCard.getLinkedAt())
                .build();
    }

    private CardTransactionDto toTransaction(CardTransaction transaction) {
        return CardTransactionDto.builder()
                .date(transaction.getTransactionDate())
                .merchantName(transaction.getMerchantName())
                .category(transaction.getCategory())
                .amount(transaction.getAmount())
                .build();
    }

    private CardStatus resolveStatus(UserCard userCard) {
        return userCard.getCardStatus() != null ? userCard.getCardStatus() : userCard.getStatus();
    }

    private CardCatalogDto toCatalogDto(CardCatalog cardCatalog) {
        return CardCatalogDto.builder()
                .id(cardCatalog.getId())
                .code(cardCatalog.getCode())
                .name(cardCatalog.getName())
                .description(cardCatalog.getDescription())
                .brand(cardCatalog.getBrand())
                .scope(cardCatalog.getScope())
                .maxPaymentLimit(cardCatalog.getMaxPaymentLimit())
                .maxWithdrawalLimit(cardCatalog.getMaxWithdrawalLimit())
                .allowsOnlinePayment(cardCatalog.isAllowsOnlinePayment())
                .allowsInternationalPayment(cardCatalog.isAllowsInternationalPayment())
                .allowsInstallments(cardCatalog.isAllowsInstallments())
                .installmentMonthsMax(cardCatalog.getInstallmentMonthsMax())
                .imageUrl(cardCatalog.getImageUrl())
                .active(cardCatalog.isActive())
                .build();
    }
}
