package com.adem.attijari_compass.service.admin;

import com.adem.attijari_compass.dto.admin.TechnicalStatusDto;
import com.adem.attijari_compass.entity.AuditStatus;
import com.adem.attijari_compass.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import javax.sql.DataSource;
import java.lang.management.ManagementFactory;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.sql.Connection;
import java.time.Duration;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class TechnicalStatusService {
    private final DataSource dataSource;
    private final AuditLogService auditLogService;

    @Value("${app.categorization.ml.base-url:}")
    private String fastApiBaseUrl;

    @Value("${groq.api.key:}")
    private String groqApiKey;

    @Value("${app.powerbi.iframe-url:}")
    private String powerBiIframeUrl;

    private final long startedAt = System.currentTimeMillis();

    public TechnicalStatusDto getStatus(User actor) {
        long start = System.currentTimeMillis();
        String databaseStatus = checkDatabase();
        String fastApiStatus = checkHttpHealth(fastApiBaseUrl);
        String chatbotStatus = StringUtils.hasText(groqApiKey) ? "UP" : "UNKNOWN";
        String powerBiStatus = StringUtils.hasText(powerBiIframeUrl) ? "UP" : "UNKNOWN";
        long duration = System.currentTimeMillis() - start;
        if (actor != null) {
            auditLogService.log(actor, "TECHNICAL_CHECK", "TECHNICAL", AuditStatus.SUCCESS,
                    "Verification technique executee");
        }
        return new TechnicalStatusDto(
                "UP",
                databaseStatus,
                fastApiStatus,
                chatbotStatus,
                powerBiStatus,
                formatUptime(System.currentTimeMillis() - startedAt),
                duration,
                LocalDateTime.now()
        );
    }

    public TechnicalStatusDto getStatusSilently() {
        return getStatus(null);
    }

    private String checkDatabase() {
        try (Connection connection = dataSource.getConnection()) {
            return connection.isValid(2) ? "UP" : "DOWN";
        } catch (Exception ex) {
            return "DOWN";
        }
    }

    private String checkHttpHealth(String baseUrl) {
        if (!StringUtils.hasText(baseUrl)) {
            return "UNKNOWN";
        }
        try {
            String healthUrl = baseUrl.endsWith("/") ? baseUrl + "health" : baseUrl + "/health";
            HttpRequest request = HttpRequest.newBuilder(URI.create(healthUrl))
                    .timeout(Duration.ofSeconds(2))
                    .GET()
                    .build();
            int statusCode = HttpClient.newBuilder()
                    .connectTimeout(Duration.ofSeconds(2))
                    .build()
                    .send(request, HttpResponse.BodyHandlers.discarding())
                    .statusCode();
            return statusCode >= 200 && statusCode < 500 ? "UP" : "DOWN";
        } catch (Exception ex) {
            return "DOWN";
        }
    }

    private String formatUptime(long millis) {
        Duration duration = Duration.ofMillis(Math.max(millis, ManagementFactory.getRuntimeMXBean().getUptime()));
        long days = duration.toDays();
        long hours = duration.minusDays(days).toHours();
        long minutes = duration.minusDays(days).minusHours(hours).toMinutes();
        return days + "j " + hours + "h " + minutes + "m";
    }
}
