package com.adem.attijari_compass.service.admin;

import com.adem.attijari_compass.dto.admin.AdminDashboardDto;
import com.adem.attijari_compass.dto.admin.TechnicalStatusDto;
import com.adem.attijari_compass.entity.SupportTicketStatus;
import com.adem.attijari_compass.entity.TransactionSource;
import com.adem.attijari_compass.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AdminDashboardService {
    private final UserRepository userRepository;
    private final TransactionRepository transactionRepository;
    private final CardTransactionRepository cardTransactionRepository;
    private final BudgetTargetRepository budgetTargetRepository;
    private final FinancialGoalRepository financialGoalRepository;
    private final SupportTicketRepository supportTicketRepository;
    private final GeneralNotificationRepository generalNotificationRepository;
    private final AuditLogRepository auditLogRepository;
    private final TechnicalStatusService technicalStatusService;

    public AdminDashboardDto getDashboard() {
        TechnicalStatusDto status = technicalStatusService.getStatusSilently();
        long cardTransactionCount = cardTransactionRepository.count();
        long transactionCardSources = transactionRepository.countBySourceIn(List.of(
                TransactionSource.BANK_API,
                TransactionSource.CARD_SYNC,
                TransactionSource.CARD_SANDBOX,
                TransactionSource.MANUAL_CARD,
                TransactionSource.TEST_CARD
        ));
        return new AdminDashboardDto(
                userRepository.countByDeletedFalse(),
                userRepository.countByActiveTrueAndDeletedFalse(),
                userRepository.countByActiveFalseAndDeletedFalse(),
                transactionRepository.count(),
                transactionRepository.countBySourceIn(List.of(TransactionSource.MANUAL_ENTRY)),
                transactionRepository.countBySourceIn(List.of(TransactionSource.IMPORTED_FILE)),
                cardTransactionCount + transactionCardSources,
                budgetTargetRepository.count(),
                financialGoalRepository.count(),
                0,
                supportTicketRepository.count(),
                supportTicketRepository.countByStatus(SupportTicketStatus.NEW)
                        + supportTicketRepository.countByStatus(SupportTicketStatus.IN_PROGRESS),
                supportTicketRepository.countByStatus(SupportTicketStatus.RESOLVED),
                generalNotificationRepository.count(),
                auditLogRepository.count(),
                status.backendStatus(),
                status.databaseStatus(),
                status.fastApiStatus(),
                status.chatbotStatus()
        );
    }
}
