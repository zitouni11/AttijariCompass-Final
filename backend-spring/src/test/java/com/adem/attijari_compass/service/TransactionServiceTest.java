package com.adem.attijari_compass.service;

import com.adem.attijari_compass.dto.transaction.TransactionRequest;
import com.adem.attijari_compass.dto.transaction.TransactionResponse;
import com.adem.attijari_compass.entity.CardStatus;
import com.adem.attijari_compass.entity.CardType;
import com.adem.attijari_compass.entity.PaymentMethod;
import com.adem.attijari_compass.entity.Role;
import com.adem.attijari_compass.entity.TestCardCatalog;
import com.adem.attijari_compass.entity.Transaction;
import com.adem.attijari_compass.entity.TransactionCategory;
import com.adem.attijari_compass.entity.TransactionSource;
import com.adem.attijari_compass.entity.TransactionType;
import com.adem.attijari_compass.entity.User;
import com.adem.attijari_compass.entity.UserCard;
import com.adem.attijari_compass.exception.ResourceNotFoundException;
import com.adem.attijari_compass.repository.TransactionRepository;
import com.adem.attijari_compass.repository.UserCardRepository;
import com.adem.attijari_compass.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TransactionServiceTest {

    @Mock
    private TransactionRepository transactionRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private UserCardRepository userCardRepository;

    @Mock
    private SmartCategorizationService smartCategorizationService;

    @Mock
    private TransactionCategoryFeedbackService transactionCategoryFeedbackService;

    @InjectMocks
    private TransactionService transactionService;

    @Test
    void shouldAttachUserCardWhenCreatingCardTransaction() {
        User user = buildUser(7L, "owner@test.com");
        UserCard userCard = buildUserCard(21L, user, "**** **** **** 4242");
        TransactionRequest request = buildRequest(PaymentMethod.CARD, userCard.getId());

        when(userRepository.findByEmail(user.getEmail())).thenReturn(Optional.of(user));
        when(userCardRepository.findById(userCard.getId())).thenReturn(Optional.of(userCard));
        when(transactionRepository.save(any(Transaction.class))).thenAnswer(invocation -> {
            Transaction transaction = invocation.getArgument(0);
            transaction.setId(100L);
            return transaction;
        });

        TransactionResponse response = transactionService.createTransaction(request, user.getEmail());

        assertNotNull(response);
        assertEquals(PaymentMethod.CARD, response.getPaymentMethod());
        assertEquals(TransactionSource.MANUAL_CARD, response.getSource());
        assertEquals(userCard.getId(), response.getUserCardId());
        assertEquals(userCard.getMaskedCardNumber(), response.getMaskedCardNumber());
        assertEquals("4242", response.getCardLast4());

        ArgumentCaptor<Transaction> transactionCaptor = ArgumentCaptor.forClass(Transaction.class);
        verify(transactionRepository).save(transactionCaptor.capture());
        Transaction savedTransaction = transactionCaptor.getValue();
        assertEquals(userCard, savedTransaction.getUserCard());
        assertEquals(TransactionSource.MANUAL_CARD, savedTransaction.getSource());
        assertEquals("4242", savedTransaction.getCardLast4());
    }

    @Test
    void shouldKeepUserCardNullWhenCreatingNonCardTransaction() {
        User user = buildUser(7L, "owner@test.com");
        TransactionRequest request = buildRequest(PaymentMethod.CASH, 21L);

        when(userRepository.findByEmail(user.getEmail())).thenReturn(Optional.of(user));
        when(transactionRepository.save(any(Transaction.class))).thenAnswer(invocation -> {
            Transaction transaction = invocation.getArgument(0);
            transaction.setId(101L);
            return transaction;
        });

        TransactionResponse response = transactionService.createTransaction(request, user.getEmail());

        assertNotNull(response);
        assertEquals(PaymentMethod.CASH, response.getPaymentMethod());
        assertEquals(TransactionSource.MANUAL_ENTRY, response.getSource());
        assertNull(response.getUserCardId());
        verify(userCardRepository, never()).findById(any());
    }

    @Test
    void shouldFailWhenSelectedUserCardDoesNotExist() {
        User user = buildUser(7L, "owner@test.com");
        TransactionRequest request = buildRequest(PaymentMethod.CARD, 999L);

        when(userRepository.findByEmail(user.getEmail())).thenReturn(Optional.of(user));
        when(userCardRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> transactionService.createTransaction(request, user.getEmail()));

        verify(transactionRepository, never()).save(any(Transaction.class));
    }

    @Test
    void shouldFailWhenSelectedUserCardBelongsToAnotherUser() {
        User owner = buildUser(7L, "owner@test.com");
        User anotherUser = buildUser(8L, "other@test.com");
        UserCard userCard = buildUserCard(22L, anotherUser, "**** **** **** 1111");
        TransactionRequest request = buildRequest(PaymentMethod.CARD, userCard.getId());

        when(userRepository.findByEmail(owner.getEmail())).thenReturn(Optional.of(owner));
        when(userCardRepository.findById(userCard.getId())).thenReturn(Optional.of(userCard));

        assertThrows(IllegalArgumentException.class, () -> transactionService.createTransaction(request, owner.getEmail()));

        verify(transactionRepository, never()).save(any(Transaction.class));
    }

    private TransactionRequest buildRequest(PaymentMethod paymentMethod, Long userCardId) {
        TransactionRequest request = new TransactionRequest();
        request.setDescription("Transaction manuelle");
        request.setAmount(125.0);
        request.setDate(LocalDate.of(2026, 4, 3));
        request.setCategory(TransactionCategory.ALIMENTATION);
        request.setType(TransactionType.DEPENSE);
        request.setPaymentMethod(paymentMethod);
        request.setUserCardId(userCardId);
        return request;
    }

    private User buildUser(Long id, String email) {
        return User.builder()
                .id(id)
                .email(email)
                .password("secret")
                .role(Role.USER)
                .build();
    }

    private UserCard buildUserCard(Long id, User user, String maskedCardNumber) {
        return UserCard.builder()
                .id(id)
                .user(user)
                .linkedTestCard(TestCardCatalog.builder()
                        .id(30L)
                        .holderName("Sandbox User")
                        .maskedCardNumber(maskedCardNumber)
                        .testCardNumber("4242424242424242")
                        .expiryMonth(12)
                        .expiryYear(2099)
                        .cvv("123")
                        .cardType(CardType.VISA)
                        .status(CardStatus.ACTIVE)
                        .initialBalance(1_000.0)
                        .build())
                .maskedCardNumber(maskedCardNumber)
                .holderName("Sandbox User")
                .cardType(CardType.VISA)
                .bankName("Attijari Bank Tunisie")
                .status(CardStatus.ACTIVE)
                .connectedAt(LocalDateTime.now().minusDays(5))
                .isActive(true)
                .build();
    }
}
