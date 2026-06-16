package com.adem.attijari_compass.service;

import com.adem.attijari_compass.entity.Transaction;
import com.adem.attijari_compass.entity.TransactionCategory;
import com.adem.attijari_compass.entity.TransactionCategoryFeedback;
import com.adem.attijari_compass.repository.TransactionCategoryFeedbackRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class TransactionCategoryFeedbackServiceTest {

    @Mock
    private TransactionCategoryFeedbackRepository feedbackRepository;

    @InjectMocks
    private TransactionCategoryFeedbackService feedbackService;

    @Test
    void shouldSaveFeedbackWhenUserCorrectsPredictedCategory() {
        Transaction transaction = Transaction.builder()
                .id(12L)
                .merchantName("foody")
                .description("payment card foody tunis")
                .category(TransactionCategory.CAFES)
                .categorizationConfidence(0.91d)
                .categorizationSource("ML_MODEL")
                .categorizationNormalizedText("foody payment card foody tunis")
                .build();

        feedbackService.recordCorrection(transaction, TransactionCategory.ALIMENTATION, 5L);

        ArgumentCaptor<TransactionCategoryFeedback> captor = ArgumentCaptor.forClass(TransactionCategoryFeedback.class);
        verify(feedbackRepository).save(captor.capture());

        TransactionCategoryFeedback feedback = captor.getValue();
        assertEquals(12L, feedback.getTransactionId());
        assertEquals(TransactionCategory.CAFES, feedback.getPredictedCategory());
        assertEquals(TransactionCategory.ALIMENTATION, feedback.getCorrectedCategory());
        assertEquals("ML_MODEL", feedback.getSource());
        assertEquals("foody payment card foody tunis", feedback.getOriginalText());
        assertEquals(5L, feedback.getUserId());
    }

    @Test
    void shouldSkipFeedbackWhenPredictionMetadataIsMissing() {
        Transaction transaction = Transaction.builder()
                .id(77L)
                .description("manual transaction")
                .category(TransactionCategory.AUTRES)
                .build();

        feedbackService.recordCorrection(transaction, TransactionCategory.SHOPPING, 4L);

        verify(feedbackRepository, never()).save(org.mockito.ArgumentMatchers.any());
    }
}
