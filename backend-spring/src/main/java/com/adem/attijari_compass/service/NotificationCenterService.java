package com.adem.attijari_compass.service;

import com.adem.attijari_compass.dto.budget.BudgetAlertResponse;
import com.adem.attijari_compass.dto.notification.NotificationResponse;
import com.adem.attijari_compass.entity.BudgetAlertSeverity;
import com.adem.attijari_compass.entity.BudgetAlertType;
import com.adem.attijari_compass.entity.NotificationSeverity;
import com.adem.attijari_compass.entity.NotificationType;
import com.adem.attijari_compass.entity.Transaction;
import com.adem.attijari_compass.entity.User;
import com.adem.attijari_compass.entity.UserNotificationState;
import com.adem.attijari_compass.exception.ResourceNotFoundException;
import com.adem.attijari_compass.repository.UserNotificationStateRepository;
import com.adem.attijari_compass.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationCenterService {

    private static final String DEFAULT_CURRENCY = "DT";

    private final BudgetTargetAlertService budgetTargetAlertService;
    private final UserNotificationStateRepository userNotificationStateRepository;
    private final UserRepository userRepository;

    @Transactional
    public List<NotificationResponse> getNotificationsForCurrentUser(String email) {
        User user = getRequiredUser(email);
        return buildAndSyncNotifications(user);
    }

    @Transactional
    public long getUnreadCountForCurrentUser(String email) {
        return getNotificationsForCurrentUser(email).stream()
                .filter(notification -> !notification.isRead())
                .count();
    }

    @Transactional
    public void markAsRead(String email, String notificationId) {
        User user = getRequiredUser(email);
        List<NotificationResponse> activeNotifications = buildAndSyncNotifications(user);
        ensureNotificationExists(activeNotifications, notificationId);

        UserNotificationState state = getOrCreateState(user, notificationId);
        state.setRead(true);
        state.setDeleted(false);
        userNotificationStateRepository.save(state);
    }

    @Transactional
    public void markAllAsRead(String email) {
        User user = getRequiredUser(email);
        List<NotificationResponse> activeNotifications = buildAndSyncNotifications(user);

        for (NotificationResponse notification : activeNotifications) {
            UserNotificationState state = getOrCreateState(user, notification.getId());
            state.setRead(true);
            state.setDeleted(false);
            userNotificationStateRepository.save(state);
        }
    }

    @Transactional
    public void deleteNotification(String email, String notificationId) {
        User user = getRequiredUser(email);
        List<NotificationResponse> activeNotifications = buildAndSyncNotifications(user);
        ensureNotificationExists(activeNotifications, notificationId);

        UserNotificationState state = getOrCreateState(user, notificationId);
        state.setDeleted(true);
        userNotificationStateRepository.save(state);
    }

    @Transactional
    public void synchronizeBudgetNotificationsAfterExpense(User user, Transaction transaction) {
        if (user == null || transaction == null) {
            return;
        }

        log.info(
                "Budget check started: userId={}, category={}, amount={}, transactionId={}",
                user.getId(),
                transaction.getCategory(),
                transaction.getAmount(),
                transaction.getId()
        );

        List<BudgetAlertResponse> budgetAlerts = budgetTargetAlertService.getAlertsForCurrentUser(user.getEmail());
        buildAndSyncNotifications(user);
        boolean matched = false;

        for (BudgetAlertResponse budgetAlert : budgetAlerts) {
            if (budgetAlert.getCategory() == null || transaction.getCategory() == null) {
                continue;
            }

            if (budgetAlert.getCategory() == transaction.getCategory()) {
                matched = true;
                log.info(
                        "Budget matched: userId={}, budgetCategory={}, transactionCategory={}, usagePercent={}, title={}",
                        user.getId(),
                        budgetAlert.getCategory(),
                        transaction.getCategory(),
                        budgetAlert.getUsagePercent(),
                        budgetAlert.getTitle()
                );
            }
        }

        if (!matched) {
            log.info(
                    "No budget notification matched after transaction: userId={}, category={}, amount={}",
                    user.getId(),
                    transaction.getCategory(),
                    transaction.getAmount()
            );
        }
    }

    private List<NotificationResponse> buildAndSyncNotifications(User user) {
        List<BudgetAlertResponse> budgetAlerts = budgetTargetAlertService.getAlertsForCurrentUser(user.getEmail());
        Map<String, UserNotificationState> stateByKey = loadStates(user);
        List<NotificationResponse> notifications = new ArrayList<>();

        for (BudgetAlertResponse budgetAlert : budgetAlerts) {
            String notificationKey = buildBudgetNotificationKey(budgetAlert);
            UserNotificationState state = stateByKey.get(notificationKey);

            if (state == null) {
                state = userNotificationStateRepository.save(UserNotificationState.builder()
                        .user(user)
                        .notificationKey(notificationKey)
                        .read(false)
                        .deleted(false)
                        .build());
                stateByKey.put(notificationKey, state);

                log.info(
                        "Notification created: userId={}, notificationId={}, type={}, severity={}, usagePercent={}",
                        user.getId(),
                        notificationKey,
                        NotificationType.BUDGET,
                        mapSeverity(budgetAlert.getSeverity()),
                        budgetAlert.getUsagePercent()
                );
            }

            if (state.isDeleted()) {
                continue;
            }

            notifications.add(mapBudgetAlert(notificationKey, budgetAlert, state));
        }

        notifications.sort(
                Comparator.comparing(NotificationResponse::getTimestamp, Comparator.nullsLast(Comparator.reverseOrder()))
                        .thenComparing(NotificationResponse::getSeverity, Comparator.nullsLast(Comparator.reverseOrder()))
        );

        return notifications;
    }

    private Map<String, UserNotificationState> loadStates(User user) {
        Map<String, UserNotificationState> states = new LinkedHashMap<>();
        userNotificationStateRepository.findAllByUserId(user.getId())
                .forEach(state -> states.put(state.getNotificationKey(), state));
        return states;
    }

    private NotificationResponse mapBudgetAlert(String notificationKey,
                                                BudgetAlertResponse alert,
                                                UserNotificationState state) {
        return NotificationResponse.builder()
                .id(notificationKey)
                .type(NotificationType.BUDGET)
                .severity(mapSeverity(alert.getSeverity()))
                .title(alert.getTitle())
                .message(enrichBudgetMessage(alert))
                .amount(extractBudgetAmount(alert))
                .currency(DEFAULT_CURRENCY)
                .timestamp(alert.getGeneratedAt())
                .read(state != null && state.isRead())
                .actionLabel("Voir les budgets")
                .actionRoute("/budgets")
                .build();
    }

    private String enrichBudgetMessage(BudgetAlertResponse alert) {
        String categoryLabel = alert.getCategoryLabel() != null && !alert.getCategoryLabel().isBlank()
                ? alert.getCategoryLabel()
                : null;

        if (categoryLabel == null) {
            return alert.getMessage();
        }

        return String.format(Locale.ROOT, "%s Categorie concernee : %s.", alert.getMessage(), categoryLabel);
    }

    private BigDecimal extractBudgetAmount(BudgetAlertResponse alert) {
        if (alert == null) {
            return null;
        }

        if (alert.getRemainingAmount() != null && alert.getRemainingAmount().signum() < 0) {
            return alert.getRemainingAmount().abs().setScale(2, RoundingMode.HALF_UP);
        }

        if (alert.getSpentThisMonth() != null) {
            return alert.getSpentThisMonth().setScale(2, RoundingMode.HALF_UP);
        }

        return null;
    }

    private NotificationSeverity mapSeverity(BudgetAlertSeverity severity) {
        if (severity == null) {
            return NotificationSeverity.INFO;
        }

        return switch (severity) {
            case CRITICAL -> NotificationSeverity.CRITICAL;
            case WARNING -> NotificationSeverity.WARNING;
            case INFO -> NotificationSeverity.INFO;
        };
    }

    private String buildBudgetNotificationKey(BudgetAlertResponse alert) {
        String categoryPart = alert.getCategory() != null
                ? alert.getCategory().name().toLowerCase(Locale.ROOT)
                : "global";
        String budgetPart = alert.getBudgetTargetId() != null
                ? alert.getBudgetTargetId().toString()
                : "global";
        String typePart = alert.getAlertType() != null
                ? alert.getAlertType().name().toLowerCase(Locale.ROOT)
                : BudgetAlertType.BUDGET_SOUS_CONTROLE.name().toLowerCase(Locale.ROOT);
        String monthPart = YearMonth.from(alert.getGeneratedAt() != null ? alert.getGeneratedAt() : java.time.LocalDateTime.now())
                .toString()
                .toLowerCase(Locale.ROOT)
                .replace('-', '_');

        return "budget-" + monthPart + "-" + budgetPart + "-" + categoryPart + "-" + typePart;
    }

    private UserNotificationState getOrCreateState(User user, String notificationKey) {
        return userNotificationStateRepository.findByUserIdAndNotificationKey(user.getId(), notificationKey)
                .orElseGet(() -> UserNotificationState.builder()
                        .user(user)
                        .notificationKey(notificationKey)
                        .read(false)
                        .deleted(false)
                        .build());
    }

    private void ensureNotificationExists(List<NotificationResponse> notifications, String notificationId) {
        boolean exists = notifications.stream()
                .map(NotificationResponse::getId)
                .filter(Objects::nonNull)
                .anyMatch(notificationId::equals);

        if (!exists) {
            throw new ResourceNotFoundException("Notification not found with id: " + notificationId);
        }
    }

    private User getRequiredUser(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with email: " + email));
    }
}
