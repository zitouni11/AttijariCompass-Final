package com.adem.attijari_compass.dto.admin;

public record AdminDashboardDto(
        long totalUsers,
        long activeUsers,
        long inactiveUsers,
        long totalTransactions,
        long totalManualTransactions,
        long totalImportedTransactions,
        long totalCardTransactions,
        long totalBudgets,
        long totalGoals,
        long totalRecommendations,
        long totalSupportTickets,
        long openSupportTickets,
        long resolvedSupportTickets,
        long totalNotifications,
        long totalAuditLogs,
        String backendStatus,
        String databaseStatus,
        String fastApiStatus,
        String chatbotStatus
) {
}
