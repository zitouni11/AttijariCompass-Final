package com.adem.attijari_compass.controller.admin;

import com.adem.attijari_compass.dto.admin.decision.TransactionSourceDecisionDto;
import com.adem.attijari_compass.service.admin.AdminTransactionSourceDecisionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/decision")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class AdminDecisionController {

    private final AdminTransactionSourceDecisionService decisionService;

    @GetMapping("/transaction-sources")
    public ResponseEntity<TransactionSourceDecisionDto> transactionSources() {
        return ResponseEntity.ok()
                .header("Cache-Control", "no-store, no-cache, must-revalidate")
                .header("Pragma", "no-cache")
                .header("Expires", "0")
                .body(decisionService.analyzeTransactionSources());
    }
}
