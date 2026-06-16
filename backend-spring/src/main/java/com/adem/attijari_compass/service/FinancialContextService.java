package com.adem.attijari_compass.service;

import com.adem.attijari_compass.dto.storytelling.FinancialContextDto;
import com.adem.attijari_compass.entity.Transaction;
import com.adem.attijari_compass.entity.TransactionType;
import com.adem.attijari_compass.entity.User;
import com.adem.attijari_compass.model.storytelling.AssistantFinancialContext;
import com.adem.attijari_compass.repository.TransactionRepository;
import com.adem.attijari_compass.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class FinancialContextService {

    private final UserRepository userRepository;
    private final TransactionRepository transactionRepository;

    public AssistantFinancialContext buildFinancialContext(String userEmail, FinancialContextDto requestContext) {
        ContextSeed seed = seed(requestContext);

        BigDecimal actualIncome = null;
        BigDecimal actualExpenses = null;
        BigDecimal actualBalance = null;
        List<String> recentTransactions = Collections.emptyList();
        Map<String, BigDecimal> spendingByCategory = Collections.emptyMap();
        boolean realDataAvailable = false;

        Optional<User> userOptional = resolveUser(userEmail);
        if (userOptional.isPresent()) {
            User user = userOptional.get();
            LocalDate startOfMonth = LocalDate.now().withDayOfMonth(1);
            LocalDate today = LocalDate.now();

            actualIncome = toBigDecimal(transactionRepository.sumAmountByUserIdAndTypeAndDateBetween(
                    user.getId(), TransactionType.REVENU, startOfMonth, today));
            actualExpenses = toBigDecimal(transactionRepository.sumAmountByUserIdAndTypeAndDateBetween(
                    user.getId(), TransactionType.DEPENSE, startOfMonth, today));

            List<Transaction> transactions = transactionRepository.findAllByUserId(user.getId());
            if (!transactions.isEmpty()) {
                actualBalance = calculateNetBalance(transactions);
                recentTransactions = transactions.stream()
                        .limit(3)
                        .map(this::toTransactionSummary)
                        .toList();

                spendingByCategory = transactions.stream()
                        .filter(transaction -> transaction.getDate() != null && !transaction.getDate().isBefore(startOfMonth))
                        .filter(transaction -> transaction.getType() == TransactionType.DEPENSE)
                        .collect(Collectors.groupingBy(
                                transaction -> transaction.getCategory().name(),
                                LinkedHashMap::new,
                                Collectors.reducing(
                                        BigDecimal.ZERO,
                                        transaction -> BigDecimal.valueOf(transaction.getAmount()),
                                        BigDecimal::add
                                )
                        ));

                realDataAvailable = true;
            } else if (actualIncome != null || actualExpenses != null) {
                realDataAvailable = true;
            }
        }

        BigDecimal salary = seed.salary() != null ? seed.salary() : seed.income();
        BigDecimal income = seed.income() != null ? seed.income() : actualIncome;
        if (income == null) {
            income = salary;
        }

        BigDecimal expenses = seed.expenses() != null ? seed.expenses() : actualExpenses;
        BigDecimal balance = seed.balance() != null ? seed.balance() : actualBalance;

        return AssistantFinancialContext.builder()
                .salary(salary)
                .income(income)
                .expenses(expenses)
                .budget(seed.budget())
                .balance(balance)
                .savingsBalance(seed.savingsBalance())
                .currency(seed.currency())
                .realDataAvailable(realDataAvailable)
                .balanceAvailable(balance != null)
                .savingsBalanceAvailable(seed.savingsBalance() != null)
                .recentTransactions(recentTransactions)
                .spendingByCategory(spendingByCategory)
                .additionalData(seed.additionalData())
                .monthlySummary(buildMonthlySummary(salary, income, expenses))
                .build();
    }

    public AssistantFinancialContext buildAccountBalanceContext(String userEmail, FinancialContextDto requestContext) {
        ContextSeed seed = seed(requestContext);
        BigDecimal balance = seed.balance();
        boolean realDataAvailable = false;

        Optional<User> userOptional = resolveUser(userEmail);
        if (userOptional.isPresent()) {
            List<Transaction> transactions = transactionRepository.findAllByUserId(userOptional.get().getId());
            if (!transactions.isEmpty()) {
                balance = calculateNetBalance(transactions);
                realDataAvailable = true;
            }
        }

        return AssistantFinancialContext.builder()
                .balance(balance)
                .balanceAvailable(balance != null)
                .currency(seed.currency())
                .realDataAvailable(realDataAvailable)
                .additionalData(seed.additionalData())
                .build();
    }

    private Optional<User> resolveUser(String userEmail) {
        if (!StringUtils.hasText(userEmail)) {
            return Optional.empty();
        }
        return userRepository.findByEmail(userEmail);
    }

    private BigDecimal calculateNetBalance(List<Transaction> transactions) {
        return transactions.stream()
                .map(transaction -> transaction.getType() == TransactionType.REVENU
                        ? BigDecimal.valueOf(transaction.getAmount())
                        : BigDecimal.valueOf(transaction.getAmount()).negate())
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private BigDecimal toBigDecimal(Double value) {
        return value == null ? null : BigDecimal.valueOf(value);
    }

    private String toTransactionSummary(Transaction transaction) {
        return transaction.getDate() + " - "
                + transaction.getDescription() + " - "
                + BigDecimal.valueOf(transaction.getAmount()).toPlainString() + " DT";
    }

    private String buildMonthlySummary(BigDecimal salary, BigDecimal income, BigDecimal expenses) {
        if (salary == null && income == null && expenses == null) {
            return "No verified income or expense summary available.";
        }

        String salaryText = salary != null ? salary.toPlainString() : "Unavailable";
        String incomeText = income != null ? income.toPlainString() : "Unavailable";
        String expensesText = expenses != null ? expenses.toPlainString() : "Unavailable";
        return "This month, verified salary is " + salaryText + ", verified income is " + incomeText
                + " and verified expenses are " + expensesText + ".";
    }

    private ContextSeed seed(FinancialContextDto requestContext) {
        return new ContextSeed(
                requestContext != null ? requestContext.getSalary() : null,
                requestContext != null ? requestContext.getIncome() : null,
                requestContext != null ? requestContext.getExpenses() : null,
                requestContext != null ? requestContext.getBudget() : null,
                requestContext != null ? requestContext.getBalance() : null,
                requestContext != null ? requestContext.getSavingsBalance() : null,
                requestContext != null && StringUtils.hasText(requestContext.getCurrency())
                        ? requestContext.getCurrency().trim()
                        : "DT",
                requestContext != null ? requestContext.getAdditionalData() : null
        );
    }

    private record ContextSeed(
            BigDecimal salary,
            BigDecimal income,
            BigDecimal expenses,
            BigDecimal budget,
            BigDecimal balance,
            BigDecimal savingsBalance,
            String currency,
            Map<String, Object> additionalData
    ) {
    }
}
