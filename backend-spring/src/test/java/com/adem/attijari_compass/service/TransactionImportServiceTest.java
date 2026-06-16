package com.adem.attijari_compass.service;

import com.adem.attijari_compass.dto.transaction.ImportTransactionsResponse;
import com.adem.attijari_compass.dto.transaction.TransactionResponse;
import com.adem.attijari_compass.entity.PaymentMethod;
import com.adem.attijari_compass.entity.Role;
import com.adem.attijari_compass.entity.Transaction;
import com.adem.attijari_compass.entity.TransactionCategory;
import com.adem.attijari_compass.entity.TransactionSource;
import com.adem.attijari_compass.entity.TransactionType;
import com.adem.attijari_compass.entity.User;
import com.adem.attijari_compass.model.categorization.CategorizationResult;
import com.adem.attijari_compass.model.categorization.CategorizationSources;
import com.adem.attijari_compass.repository.TransactionRepository;
import com.adem.attijari_compass.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Row;
import org.springframework.mock.web.MockMultipartFile;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TransactionImportServiceTest {

    @Mock
    private TransactionRepository transactionRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private SmartCategorizationService smartCategorizationService;

    @Mock
    private TransactionService transactionService;

    private TransactionImportService transactionImportService;
    private User user;

    @BeforeEach
    void setUp() {
        transactionImportService = new TransactionImportService(
                transactionRepository,
                userRepository,
                smartCategorizationService,
                transactionService
        );

        user = User.builder()
                .id(7L)
                .email("user@test.com")
                .password("secret")
                .role(Role.USER)
                .createdAt(LocalDateTime.now())
                .build();

        when(userRepository.findByEmail(user.getEmail())).thenReturn(Optional.of(user));
        mockSavedTransactions();
        mockTransactionResponses();
        mockCategorization();
    }

    @Test
    void shouldImportSemicolonSeparatedCsv() {
        MockMultipartFile file = csvFile("""
                date;description;amount;type;merchantName
                21/04/2026;Abonnement Netflix;29,90;DEPENSE;Netflix
                20/04/2026;Salaire avril;5000,00;REVENU;Employeur
                """);

        ImportTransactionsResponse response = transactionImportService.importTransactions(file, user.getEmail());

        assertEquals(2, response.getImportedCount());
        assertEquals(0, response.getErrorCount());
        assertEquals(2, response.getTransactions().size());

        ArgumentCaptor<Transaction> captor = ArgumentCaptor.forClass(Transaction.class);
        verify(transactionRepository, times(2)).save(captor.capture());

        List<Transaction> savedTransactions = captor.getAllValues();
        assertEquals(TransactionCategory.DIVERTISSEMENT, savedTransactions.get(0).getCategory());
        assertEquals(TransactionCategory.SALAIRE, savedTransactions.get(1).getCategory());
    }

    @Test
    void shouldCollectRowErrorsWithoutStoppingBatch() {
        MockMultipartFile file = csvFile("""
                date;description;amount;type;merchantName
                21/04/2026;Abonnement Netflix;;DEPENSE;Netflix
                20/04/2026;McDo Lac 1;18,50;DEPENSE;McDo
                """);

        ImportTransactionsResponse response = transactionImportService.importTransactions(file, user.getEmail());

        assertEquals(1, response.getImportedCount());
        assertEquals(1, response.getErrorCount());
        assertEquals(1, response.getTransactions().size());
        assertEquals(2, response.getErrors().get(0).getRowNumber());
        assertTrue(response.getErrors().get(0).getMessage().contains("Montant"));
    }

    @Test
    void shouldInferTypeFromDebitAndCreditColumns() {
        MockMultipartFile file = csvFile("""
                date;description;debit;credit;merchantName
                19/04/2026;Essence BP;120,50;;BP
                18/04/2026;Salaire mai;;3000,00;Employeur
                """);

        ImportTransactionsResponse response = transactionImportService.importTransactions(file, user.getEmail());

        assertEquals(2, response.getImportedCount());
        assertEquals(0, response.getErrorCount());

        ArgumentCaptor<Transaction> captor = ArgumentCaptor.forClass(Transaction.class);
        verify(transactionRepository, times(2)).save(captor.capture());

        List<Transaction> savedTransactions = captor.getAllValues();
        assertEquals(TransactionType.DEPENSE, savedTransactions.get(0).getType());
        assertEquals(TransactionType.REVENU, savedTransactions.get(1).getType());
        assertEquals(120.50d, savedTransactions.get(0).getAmount());
        assertEquals(3000.00d, savedTransactions.get(1).getAmount());
    }

    @Test
    void shouldImportExcelWithMixedStringAndNumericCells() throws Exception {
        MockMultipartFile file = excelFile();

        ImportTransactionsResponse response = transactionImportService.importTransactions(file, user.getEmail());

        assertEquals(2, response.getImportedCount());
        assertEquals(0, response.getErrorCount());
        assertEquals(2, response.getTransactions().size());

        ArgumentCaptor<Transaction> captor = ArgumentCaptor.forClass(Transaction.class);
        verify(transactionRepository, times(2)).save(captor.capture());

        List<Transaction> savedTransactions = captor.getAllValues();
        assertEquals(LocalDate.of(2026, 4, 21), savedTransactions.get(0).getDate());
        assertEquals(LocalDate.of(2026, 4, 20), savedTransactions.get(1).getDate());
        assertEquals(29.90d, savedTransactions.get(0).getAmount());
        assertEquals(87.50d, savedTransactions.get(1).getAmount());
        assertEquals(TransactionCategory.ALIMENTATION, savedTransactions.get(1).getCategory());
        assertEquals(PaymentMethod.CARD, savedTransactions.get(1).getPaymentMethod());
    }

    private MockMultipartFile csvFile(String content) {
        return new MockMultipartFile(
                "file",
                "transactions.csv",
                "text/csv",
                content.getBytes(StandardCharsets.UTF_8)
        );
    }

    private MockMultipartFile excelFile() throws Exception {
        try (XSSFWorkbook workbook = new XSSFWorkbook();
             ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet("transactions");
            Row header = sheet.createRow(0);
            header.createCell(0).setCellValue("date");
            header.createCell(1).setCellValue("description");
            header.createCell(2).setCellValue("amount");
            header.createCell(3).setCellValue("type");
            header.createCell(4).setCellValue("merchantName");

            Row numericRow = sheet.createRow(1);
            numericRow.createCell(0).setCellValue(java.sql.Date.valueOf(LocalDate.of(2026, 4, 21)));
            numericRow.createCell(1).setCellValue("Abonnement Netflix");
            numericRow.createCell(2).setCellValue(29.90d);
            numericRow.createCell(3).setCellValue("DEPENSE");
            numericRow.createCell(4).setCellValue("Netflix");

            Row stringRow = sheet.createRow(2);
            stringRow.createCell(0).setCellValue("20/04/2026");
            stringRow.createCell(1).setCellValue("Achat alimentaire");
            stringRow.createCell(2).setCellValue("87,500 TND");
            stringRow.createCell(3).setCellValue("Alimentation");
            stringRow.createCell(4).setCellValue("Carte");

            workbook.write(outputStream);

            return new MockMultipartFile(
                    "file",
                    "transactions.xlsx",
                    "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                    outputStream.toByteArray()
            );
        }
    }

    private void mockSavedTransactions() {
        long[] nextId = {1L};
        when(transactionRepository.save(any(Transaction.class))).thenAnswer(invocation -> {
            Transaction transaction = invocation.getArgument(0);
            transaction.setId(nextId[0]++);
            return transaction;
        });
    }

    private void mockTransactionResponses() {
        when(transactionService.mapToResponse(any(Transaction.class))).thenAnswer(invocation -> {
            Transaction transaction = invocation.getArgument(0);
            return TransactionResponse.builder()
                    .id(transaction.getId())
                    .description(transaction.getDescription())
                    .amount(transaction.getAmount())
                    .date(transaction.getDate())
                    .category(transaction.getCategory())
                    .type(transaction.getType())
                    .userId(transaction.getUser().getId())
                    .merchantName(transaction.getMerchantName())
                    .paymentMethod(transaction.getPaymentMethod())
                    .source(TransactionSource.IMPORTED_FILE)
                    .createdAt(transaction.getCreatedAt())
                    .categorizationSource(transaction.getCategorizationSource())
                    .categorizationConfidence(transaction.getCategorizationConfidence())
                    .categorizationNormalizedText(transaction.getCategorizationNormalizedText())
                    .build();
        });
    }

    private void mockCategorization() {
        when(smartCategorizationService.categorize(anyString(), anyString(), eq(user.getId())))
                .thenAnswer(invocation -> {
                    String merchant = invocation.getArgument(0, String.class);
                    String description = invocation.getArgument(1, String.class);
                    String text = (merchant + " " + description).toLowerCase();

                    TransactionCategory category = TransactionCategory.AUTRES;
                    if (text.contains("netflix")) {
                        category = TransactionCategory.DIVERTISSEMENT;
                    } else if (text.contains("salaire")) {
                        category = TransactionCategory.SALAIRE;
                    } else if (text.contains("mcdo")) {
                        category = TransactionCategory.RESTAURANT;
                    } else if (text.contains("bp") || text.contains("essence")) {
                        category = TransactionCategory.STATION_SERVICES;
                    }

                    return CategorizationResult.builder()
                            .category(category)
                            .confidence(0.98d)
                            .source(CategorizationSources.RULE_ENGINE)
                            .reason("test_rule")
                            .normalizedText(text.trim())
                            .build();
                });
    }
}
