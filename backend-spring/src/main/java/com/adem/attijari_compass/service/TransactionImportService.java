package com.adem.attijari_compass.service;

import com.adem.attijari_compass.dto.transaction.ImportTransactionErrorRow;
import com.adem.attijari_compass.dto.transaction.ImportTransactionsResponse;
import com.adem.attijari_compass.dto.transaction.ImportTransactionsSummary;
import com.adem.attijari_compass.dto.transaction.TransactionResponse;
import com.adem.attijari_compass.entity.PaymentMethod;
import com.adem.attijari_compass.entity.Transaction;
import com.adem.attijari_compass.entity.TransactionCategory;
import com.adem.attijari_compass.entity.TransactionSource;
import com.adem.attijari_compass.entity.TransactionType;
import com.adem.attijari_compass.entity.User;
import com.adem.attijari_compass.exception.ResourceNotFoundException;
import com.adem.attijari_compass.model.categorization.CategorizationResult;
import com.adem.attijari_compass.repository.TransactionRepository;
import com.adem.attijari_compass.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.DateUtil;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.text.Normalizer;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class TransactionImportService {

    private static final DataFormatter EXCEL_DATA_FORMATTER = new DataFormatter(Locale.FRANCE);
    private static final List<DateTimeFormatter> DATE_FORMATTERS = List.of(
            DateTimeFormatter.ofPattern("dd/MM/yyyy"),
            DateTimeFormatter.ofPattern("dd-MM-yyyy"),
            DateTimeFormatter.ofPattern("yyyy-MM-dd"),
            DateTimeFormatter.ofPattern("yyyy/MM/dd"),
            DateTimeFormatter.ofPattern("dd/MM/yy"),
            DateTimeFormatter.ofPattern("dd-MM-yy")
    );

    private final TransactionRepository transactionRepository;
    private final UserRepository userRepository;
    private final SmartCategorizationService smartCategorizationService;
    private final TransactionService transactionService;

    public ImportTransactionsResponse importTransactions(MultipartFile file, String userEmail) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with email: " + userEmail));

        List<ImportTransactionErrorRow> errors = new ArrayList<>();
        List<Transaction> savedTransactions = new ArrayList<>();

        try {
            String filename = file.getOriginalFilename();
            if (filename == null || filename.isBlank()) {
                throw new IOException("Nom de fichier non valide");
            }

            byte[] fileBytes = file.getBytes();
            List<TransactionData> transactions = parseFile(filename, fileBytes, errors);

            for (TransactionData transactionData : transactions) {
                int rowNumber = transactionData.getRowNumber();
                log.info("Processing row {}: {}", rowNumber, transactionData);

                try {
                    Transaction transaction = createTransactionFromData(transactionData, user);
                    Transaction savedTransaction = transactionRepository.save(transaction);
                    savedTransactions.add(savedTransaction);
                } catch (Exception e) {
                    errors.add(buildErrorRow(rowNumber, e.getMessage()));
                    log.error("Error at row {}: {}", rowNumber, e.getMessage(), e);
                }
            }
        } catch (IOException e) {
            log.error("Erreur lors de la lecture du fichier: {}", e.getMessage(), e);
            errors.add(buildErrorRow(0, "Erreur lors de la lecture du fichier: " + e.getMessage()));
        }

        int successCount = savedTransactions.size();
        int errorCount = errors.size();
        List<TransactionResponse> importedTransactions = savedTransactions.stream()
                .map(transactionService::mapToResponse)
                .collect(Collectors.toList());

        String message = errorCount > 0
                ? String.format(
                "%d transactions importees et categorisees avec succes, %d erreur(s) detectee(s)",
                successCount,
                errorCount
        )
                : String.format("%d transactions importees et categorisees avec succes", successCount);

        return ImportTransactionsResponse.builder()
                .totalProcessed(successCount + errorCount)
                .successCount(successCount)
                .importedCount(successCount)
                .errorCount(errorCount)
                .errors(errors)
                .message(message)
                .transactions(importedTransactions)
                .summary(buildSummary(savedTransactions))
                .build();
    }

    private List<TransactionData> parseFile(
            String filename,
            byte[] fileBytes,
            List<ImportTransactionErrorRow> errors
    ) throws IOException {
        String normalizedFilename = filename.toLowerCase(Locale.ROOT);

        if (normalizedFilename.endsWith(".csv")) {
            return parseCsv(new ByteArrayInputStream(fileBytes), errors);
        }
        if (normalizedFilename.endsWith(".xlsx") || normalizedFilename.endsWith(".xls")) {
            return parseExcel(new ByteArrayInputStream(fileBytes), errors);
        }

        throw new IOException("Format de fichier non supporte. Veuillez utiliser CSV ou Excel.");
    }

    private ImportTransactionsSummary buildSummary(List<Transaction> transactions) {
        double totalExpenses = 0.0d;
        double totalIncome = 0.0d;
        int categorizedCount = 0;
        int expenseCount = 0;
        int incomeCount = 0;

        for (Transaction transaction : transactions) {
            if (transaction.getCategory() != null) {
                categorizedCount++;
            }

            if (transaction.getType() == TransactionType.REVENU) {
                incomeCount++;
                totalIncome += Math.abs(transaction.getAmount());
            } else {
                expenseCount++;
                totalExpenses += Math.abs(transaction.getAmount());
            }
        }

        return ImportTransactionsSummary.builder()
                .categorizedCount(categorizedCount)
                .expenseCount(expenseCount)
                .incomeCount(incomeCount)
                .totalExpenses(totalExpenses)
                .totalIncome(totalIncome)
                .netAmount(totalIncome - totalExpenses)
                .build();
    }

    private List<TransactionData> parseCsv(
            InputStream inputStream,
            List<ImportTransactionErrorRow> errors
    ) throws IOException {
        List<TransactionData> transactions = new ArrayList<>();
        String content = new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
        if (content.startsWith("\ufeff")) {
            content = content.substring(1);
        }

        char delimiter = detectCsvDelimiter(findFirstNonBlankLine(content));
        log.info("Detected CSV delimiter '{}' for import batch", delimiter);

        CSVFormat format = CSVFormat.DEFAULT.builder()
                .setDelimiter(delimiter)
                .setHeader()
                .setSkipHeaderRecord(true)
                .setIgnoreSurroundingSpaces(true)
                .setTrim(true)
                .build();

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(
                new ByteArrayInputStream(content.getBytes(StandardCharsets.UTF_8)),
                StandardCharsets.UTF_8
        ));
             CSVParser csvParser = new CSVParser(reader, format)) {
            Map<String, Integer> headerMap = csvParser.getHeaderMap() != null ? csvParser.getHeaderMap() : Map.of();

            for (CSVRecord record : csvParser) {
                int rowNumber = Math.toIntExact(record.getRecordNumber()) + 1;

                try {
                    TransactionData data = TransactionData.builder()
                            .rowNumber(rowNumber)
                            .date(readCsvValue(record, headerMap, "date", "transactionDate", "bookingDate", "operationDate", "postedDate"))
                            .description(firstNonBlank(
                                    readCsvValue(record, headerMap, "description", "libelle", "label", "operation", "transaction", "details", "narrative"),
                                    readCsvValue(record, headerMap, "merchantName", "merchant", "merchant_name", "affil name", "affil_name", "beneficiary")
                            ))
                            .amount(readCsvValue(record, headerMap, "montant", "amount", "value", "transactionAmount", "net amount", "montant net"))
                            .debitAmount(readCsvValue(record, headerMap, "debit", "debitAmount", "withdrawal", "outflow"))
                            .creditAmount(readCsvValue(record, headerMap, "credit", "creditAmount", "deposit", "inflow"))
                            .category(readCsvValue(record, headerMap, "categorie", "category"))
                            .type(readCsvValue(record, headerMap, "type", "direction", "flow", "sens"))
                            .paymentMethod(firstNonBlank(
                                    readCsvValue(record, headerMap, "methode", "paymentMethod", "payment_method", "method", "channel"),
                                    "BANK_TRANSFER"
                            ))
                            .merchantName(firstNonBlank(
                                    readCsvValue(record, headerMap, "merchantName", "merchant", "merchant_name", "affil name", "affil_name", "beneficiary"),
                                    readCsvValue(record, headerMap, "description", "libelle", "label")
                            ))
                            .rawValues(extractCsvRowData(record, headerMap))
                            .build();

                    if (isBlankRow(data)) {
                        continue;
                    }

                    transactions.add(data);
                } catch (Exception e) {
                    errors.add(buildErrorRow(rowNumber, "Erreur de parsing CSV: " + e.getMessage()));
                    log.error("Error at row {}: {}", rowNumber, e.getMessage(), e);
                }
            }
        }

        return transactions;
    }

    private List<TransactionData> parseExcel(
            InputStream inputStream,
            List<ImportTransactionErrorRow> errors
    ) throws IOException {
        List<TransactionData> transactions = new ArrayList<>();

        try (Workbook workbook = WorkbookFactory.create(inputStream)) {
            Sheet sheet = workbook.getSheetAt(0);
            Row headerRow = sheet.getPhysicalNumberOfRows() > 0 ? sheet.getRow(sheet.getFirstRowNum()) : null;
            Map<String, Integer> headerIndexes = buildExcelHeaderIndex(headerRow);

            for (int rowIndex = sheet.getFirstRowNum() + 1; rowIndex <= sheet.getLastRowNum(); rowIndex++) {
                Row row = sheet.getRow(rowIndex);
                if (row == null) {
                    continue;
                }

                int rowNumber = row.getRowNum() + 1;

                try {
                    TransactionData data = TransactionData.builder()
                            .rowNumber(rowNumber)
                            .date(readExcelValue(row, headerIndexes, 0, "date", "transactionDate", "bookingDate", "operationDate", "postedDate"))
                            .description(firstNonBlank(
                                    readExcelValue(row, headerIndexes, 1, "description", "libelle", "label", "operation", "transaction", "details", "narrative"),
                                    readExcelValue(row, headerIndexes, 1, "merchantName", "merchant", "merchant_name", "affil name", "affil_name", "beneficiary")
                            ))
                            .amount(readExcelAmount(row, headerIndexes, 2, "montant", "amount", "value", "transactionAmount", "net amount", "montant net", "montanttnd"))
                            .debitAmount(readExcelValue(row, headerIndexes, -1, "debit", "debitAmount", "withdrawal", "outflow"))
                            .creditAmount(readExcelValue(row, headerIndexes, -1, "credit", "creditAmount", "deposit", "inflow"))
                            .category(readExcelValue(row, headerIndexes, 3, "categorie", "category", "type"))
                            .type(readExcelValue(row, headerIndexes, -1, "transactionType", "direction", "flow", "sens"))
                            .paymentMethod(firstNonBlank(
                                    readExcelValue(row, headerIndexes, 4, "methode", "paymentMethod", "payment_method", "method", "channel"),
                                    "BANK_TRANSFER"
                            ))
                            .merchantName(firstNonBlank(
                                    readExcelValue(row, headerIndexes, 1, "merchantName", "merchant", "merchant_name", "affil name", "affil_name", "beneficiary"),
                                    readExcelValue(row, headerIndexes, 1, "description", "libelle", "label")
                            ))
                            .rawValues(extractExcelRowData(row, headerIndexes))
                            .build();

                    if (isBlankRow(data)) {
                        continue;
                    }

                    transactions.add(data);
                } catch (Exception e) {
                    errors.add(buildErrorRow(rowNumber, "Erreur de parsing Excel: " + e.getMessage()));
                    log.error("Error at row {}: {}", rowNumber, e.getMessage(), e);
                }
            }
        } catch (Exception e) {
            throw new IOException("Lecture Excel impossible: " + e.getMessage(), e);
        }

        return transactions;
    }

    private String getCellValueAsString(Cell cell) {
        if (cell == null) {
            return "";
        }

        return switch (cell.getCellType()) {
            case STRING -> cell.getStringCellValue();
            case NUMERIC -> formatNumericCell(cell);
            case BOOLEAN -> String.valueOf(cell.getBooleanCellValue());
            case FORMULA -> formatFormulaCell(cell);
            default -> "";
        };
    }

    private String formatNumericCell(Cell cell) {
        if (DateUtil.isCellDateFormatted(cell)) {
            return DateUtil.getJavaDate(cell.getNumericCellValue())
                    .toInstant()
                    .atZone(ZoneId.systemDefault())
                    .toLocalDate()
                    .toString();
        }

        return EXCEL_DATA_FORMATTER.formatCellValue(cell);
    }

    private String formatFormulaCell(Cell cell) {
        return switch (cell.getCachedFormulaResultType()) {
            case STRING -> cell.getStringCellValue();
            case NUMERIC -> formatNumericCell(cell);
            case BOOLEAN -> String.valueOf(cell.getBooleanCellValue());
            default -> EXCEL_DATA_FORMATTER.formatCellValue(cell);
        };
    }

    private Transaction createTransactionFromData(TransactionData data, User user) {
        String description = firstNonBlank(data.getDescription(), data.getMerchantName());
        if (description == null || description.isBlank()) {
            throw new IllegalArgumentException("Description ou marchand introuvable dans la ligne importee.");
        }

        ParsedAmount parsedAmount = resolveAmount(data);
        LocalDate transactionDate = parseDate(data.getDate());

        CategorizationResult categorizationResult = null;
        TransactionCategory category = null;

        if (firstNonBlank(data.getCategory()) != null) {
            category = TransactionCategory.fromValue(data.getCategory());
            if (category == TransactionCategory.fallback()) {
                categorizationResult = smartCategorizationService.categorize(
                        data.getMerchantName(),
                        data.getDescription(),
                        user.getId()
                );
                category = categorizationResult.getCategory();
            }
        } else {
            categorizationResult = smartCategorizationService.categorize(
                    data.getMerchantName(),
                    data.getDescription(),
                    user.getId()
            );
            category = categorizationResult.getCategory();
        }

        if (category == null) {
            category = TransactionCategory.fallback();
        }

        TransactionType type = resolveTransactionType(data.getType(), parsedAmount.signedAmount(), category);
        PaymentMethod paymentMethod = resolvePaymentMethod(data.getPaymentMethod());
        double amount = Math.abs(parsedAmount.signedAmount());

        if (amount <= 0.0d) {
            throw new IllegalArgumentException("Le montant doit etre strictement positif.");
        }

        Transaction transaction = Transaction.builder()
                .description(description)
                .merchantName(firstNonBlank(data.getMerchantName(), description))
                .amount(amount)
                .date(transactionDate)
                .category(category)
                .type(type)
                .paymentMethod(paymentMethod)
                .source(TransactionSource.IMPORTED_FILE)
                .createdAt(LocalDateTime.now())
                .user(user)
                .build();

        applyCategorizationMetadata(transaction, categorizationResult);
        return transaction;
    }

    private LocalDate parseDate(String dateStr) {
        if (dateStr == null || dateStr.isBlank()) {
            throw new IllegalArgumentException("Date missing");
        }

        String normalized = dateStr.trim();
        for (DateTimeFormatter formatter : DATE_FORMATTERS) {
            try {
                return LocalDate.parse(normalized, formatter);
            } catch (Exception ignored) {
                // Try next formatter
            }
        }

        try {
            double excelDate = Double.parseDouble(normalized.replace(',', '.'));
            return DateUtil.getJavaDate(excelDate)
                    .toInstant()
                    .atZone(ZoneId.systemDefault())
                    .toLocalDate();
        } catch (Exception ignored) {
            throw new IllegalArgumentException("Date invalide: " + dateStr);
        }
    }

    private void applyCategorizationMetadata(Transaction transaction, CategorizationResult result) {
        if (result == null) {
            transaction.setCategorizationConfidence(null);
            transaction.setCategorizationSource(null);
            transaction.setCategorizationNormalizedText(null);
            return;
        }

        transaction.setCategorizationConfidence(result.getConfidence());
        transaction.setCategorizationSource(result.getSource());
        transaction.setCategorizationNormalizedText(result.getNormalizedText());
    }

    private String readCsvValue(CSVRecord record, Map<String, Integer> headerMap, String... aliases) {
        for (String alias : aliases) {
            if (alias == null) {
                continue;
            }

            String direct = readCsvValue(record, alias);
            if (direct != null) {
                return direct;
            }

            String normalizedAlias = normalizeHeader(alias);
            for (String header : headerMap.keySet()) {
                if (normalizeHeader(header).equals(normalizedAlias)) {
                    String candidate = readCsvValue(record, header);
                    if (candidate != null) {
                        return candidate;
                    }
                }
            }
        }

        return null;
    }

    private String readCsvValue(CSVRecord record, String header) {
        try {
            if (!record.isMapped(header)) {
                return null;
            }

            String value = record.get(header);
            return value != null && !value.isBlank() ? value.trim() : null;
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }

    private Map<String, String> extractCsvRowData(CSVRecord record, Map<String, Integer> headerMap) {
        Map<String, String> rawValues = new LinkedHashMap<>();
        for (String header : headerMap.keySet()) {
            String value = readCsvValue(record, header);
            rawValues.put(header, value != null ? value : "");
        }
        return rawValues;
    }

    private Map<String, Integer> buildExcelHeaderIndex(Row headerRow) {
        Map<String, Integer> indexes = new LinkedHashMap<>();
        if (headerRow == null) {
            return indexes;
        }

        for (Cell cell : headerRow) {
            String header = getCellValueAsString(cell);
            if (header == null || header.isBlank()) {
                continue;
            }

            indexes.put(normalizeHeader(header), cell.getColumnIndex());
        }

        return indexes;
    }

    private String readExcelValue(Row row, Map<String, Integer> headerIndexes, int fallbackIndex, String... aliases) {
        Cell cell = findExcelCell(row, headerIndexes, fallbackIndex, aliases);
        if (cell == null) {
            return null;
        }

        String value = getCellValueAsString(cell);
        return value.isBlank() ? null : value.trim();
    }

    private String readExcelAmount(Row row, Map<String, Integer> headerIndexes, int fallbackIndex, String... aliases) {
        Cell cell = findExcelCell(row, headerIndexes, fallbackIndex, aliases);
        if (cell == null) {
            return null;
        }

        Double parsedAmount = parseAmount(cell);
        return parsedAmount == null ? null : Double.toString(parsedAmount);
    }

    private Cell findExcelCell(Row row, Map<String, Integer> headerIndexes, int fallbackIndex, String... aliases) {
        for (String alias : aliases) {
            Integer index = headerIndexes.get(normalizeHeader(alias));
            if (index != null) {
                Cell cell = row.getCell(index);
                String value = getCellValueAsString(cell);
                if (!value.isBlank()) {
                    return cell;
                }
            }
        }

        if (fallbackIndex >= 0 && row.getLastCellNum() > fallbackIndex) {
            Cell fallbackCell = row.getCell(fallbackIndex);
            String fallback = getCellValueAsString(fallbackCell);
            return fallback.isBlank() ? null : fallbackCell;
        }

        return null;
    }

    private Map<String, String> extractExcelRowData(Row row, Map<String, Integer> headerIndexes) {
        Map<String, String> rawValues = new LinkedHashMap<>();
        for (Map.Entry<String, Integer> entry : headerIndexes.entrySet()) {
            rawValues.put(entry.getKey(), getCellValueAsString(row.getCell(entry.getValue())).trim());
        }
        return rawValues;
    }

    private String firstNonBlank(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value.trim();
            }
        }
        return null;
    }

    private String normalizeHeader(String value) {
        return value == null
                ? ""
                : value.trim()
                .toLowerCase(Locale.ROOT)
                .replace('\u00a0', ' ')
                .replace('\ufeff', ' ')
                .replaceAll("[^a-z0-9]+", "");
    }

    private double parseAmount(String rawAmount) {
        if (rawAmount == null || rawAmount.isBlank()) {
            throw new IllegalArgumentException("Amount missing");
        }

        String normalized = rawAmount.trim()
                .replace('\u00a0', ' ')
                .replace("TND", "")
                .replace("tnd", "")
                .replace(" ", "")
                .replaceAll("[^0-9,.-]", "");

        if (normalized.isBlank()) {
            throw new IllegalArgumentException("Montant invalide: " + rawAmount);
        }

        try {
            return Double.parseDouble(normalizeDecimalSeparators(normalized));
        } catch (NumberFormatException ex) {
            throw new IllegalArgumentException("Montant invalide: " + rawAmount);
        }
    }

    private Double parseAmount(Cell cell) {
        if (cell == null) {
            return null;
        }

        return switch (cell.getCellType()) {
            case NUMERIC -> cell.getNumericCellValue();
            case FORMULA -> switch (cell.getCachedFormulaResultType()) {
                case NUMERIC -> cell.getNumericCellValue();
                case STRING -> parseAmount(EXCEL_DATA_FORMATTER.formatCellValue(cell));
                default -> parseAmount(EXCEL_DATA_FORMATTER.formatCellValue(cell));
            };
            case STRING -> parseAmount(cell.getStringCellValue());
            default -> {
                String raw = EXCEL_DATA_FORMATTER.formatCellValue(cell);
                yield raw == null || raw.trim().isEmpty() ? null : parseAmount(raw);
            }
        };
    }

    private String normalizeDecimalSeparators(String value) {
        String normalized = value;

        if (normalized.contains(",") && normalized.contains(".")) {
            if (normalized.lastIndexOf(',') > normalized.lastIndexOf('.')) {
                normalized = normalized.replace(".", "").replace(',', '.');
            } else {
                normalized = normalized.replace(",", "");
            }
        } else if (normalized.contains(",")) {
            normalized = normalized.replace(',', '.');
        }

        return normalized;
    }

    private ParsedAmount resolveAmount(TransactionData data) {
        String rawAmount = firstNonBlank(data.getAmount());
        if (rawAmount != null) {
            return new ParsedAmount(parseAmount(rawAmount), "amount");
        }

        String debitAmount = firstNonBlank(data.getDebitAmount());
        if (debitAmount != null) {
            return new ParsedAmount(-Math.abs(parseAmount(debitAmount)), "debit");
        }

        String creditAmount = firstNonBlank(data.getCreditAmount());
        if (creditAmount != null) {
            return new ParsedAmount(Math.abs(parseAmount(creditAmount)), "credit");
        }

        throw new IllegalArgumentException("Montant introuvable dans la ligne importee.");
    }

    private TransactionType resolveTransactionType(String rawType, double signedAmount, TransactionCategory category) {
        String normalized = normalizeValue(rawType);

        if (!normalized.isBlank()) {
            if (containsOneOf(normalized, "REVENU", "INCOME", "CREDIT", "ENTREE", "INFLOW", "DEPOSIT")) {
                return TransactionType.REVENU;
            }
            if (containsOneOf(normalized, "DEPENSE", "EXPENSE", "DEBIT", "SORTIE", "OUTFLOW", "WITHDRAWAL", "PAIEMENT")) {
                return TransactionType.DEPENSE;
            }
            try {
                return TransactionType.valueOf(normalized);
            } catch (IllegalArgumentException ignored) {
                // Fall back to inference
            }
        }

        if (signedAmount < 0.0d) {
            return TransactionType.DEPENSE;
        }
        if (category == TransactionCategory.SALAIRE) {
            return TransactionType.REVENU;
        }
        return TransactionType.DEPENSE;
    }

    private PaymentMethod resolvePaymentMethod(String rawPaymentMethod) {
        String normalized = normalizeValue(rawPaymentMethod);

        if (normalized.isBlank()) {
            return PaymentMethod.BANK_TRANSFER;
        }
        if (containsOneOf(normalized, "CARD", "CARTE")) {
            return PaymentMethod.CARD;
        }
        if (containsOneOf(normalized, "BANK_TRANSFER", "TRANSFER", "VIREMENT", "WIRE")) {
            return PaymentMethod.BANK_TRANSFER;
        }
        if (containsOneOf(normalized, "CASH", "ESPECES")) {
            return PaymentMethod.CASH;
        }
        if (containsOneOf(normalized, "DIGITAL_WALLET", "WALLET", "MOBILE_MONEY")) {
            return PaymentMethod.DIGITAL_WALLET;
        }

        try {
            return PaymentMethod.valueOf(normalized);
        } catch (IllegalArgumentException e) {
            return PaymentMethod.BANK_TRANSFER;
        }
    }

    private String normalizeValue(String value) {
        if (value == null) {
            return "";
        }

        return Normalizer.normalize(value.trim(), Normalizer.Form.NFD)
                .replaceAll("\\p{M}+", "")
                .replace('\u00a0', ' ')
                .toUpperCase(Locale.ROOT)
                .replace('-', '_')
                .replace(' ', '_');
    }

    private boolean containsOneOf(String value, String... candidates) {
        return Arrays.stream(candidates).anyMatch(value::contains);
    }

    private boolean isBlankRow(TransactionData data) {
        return firstNonBlank(
                data.getDate(),
                data.getDescription(),
                data.getAmount(),
                data.getDebitAmount(),
                data.getCreditAmount(),
                data.getCategory(),
                data.getType(),
                data.getPaymentMethod(),
                data.getMerchantName()
        ) == null;
    }

    private ImportTransactionErrorRow buildErrorRow(int rowNumber, String message) {
        return ImportTransactionErrorRow.builder()
                .rowNumber(rowNumber)
                .message(message)
                .build();
    }

    private String findFirstNonBlankLine(String content) {
        return content.lines()
                .map(String::trim)
                .filter(line -> !line.isBlank())
                .findFirst()
                .orElse("");
    }

    private char detectCsvDelimiter(String headerLine) {
        long semicolonCount = headerLine.chars().filter(ch -> ch == ';').count();
        long commaCount = headerLine.chars().filter(ch -> ch == ',').count();
        return semicolonCount > commaCount ? ';' : ',';
    }

    @lombok.Data
    @lombok.Builder
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    private static class TransactionData {
        private int rowNumber;
        private String date;
        private String description;
        private String amount;
        private String debitAmount;
        private String creditAmount;
        private String category;
        private String type;
        private String paymentMethod;
        private String merchantName;
        @lombok.Builder.Default
        private Map<String, String> rawValues = Collections.emptyMap();
    }

    private record ParsedAmount(double signedAmount, String source) {
    }
}
