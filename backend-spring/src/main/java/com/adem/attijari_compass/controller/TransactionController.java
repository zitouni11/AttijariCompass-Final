package com.adem.attijari_compass.controller;

import com.adem.attijari_compass.dto.transaction.*;
import com.adem.attijari_compass.exception.AuthenticationRequiredException;
import com.adem.attijari_compass.service.TransactionCashBreakdownService;
import com.adem.attijari_compass.service.TransactionService;
import com.adem.attijari_compass.service.TransactionImportService;
import com.adem.attijari_compass.service.admin.AppSettingService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;



@RestController
@RequestMapping("/api/transactions")
@RequiredArgsConstructor
public class TransactionController {

    private final TransactionService transactionService;
    private final TransactionImportService transactionImportService;
    private final TransactionCashBreakdownService transactionCashBreakdownService;
    private final AppSettingService appSettingService;

    /**
     * Importer des transactions depuis un fichier CSV ou Excel
     * Le fichier doit contenir les colonnes : date, description, amount, category (opt), type (opt), paymentMethod (opt)
     */
    @PostMapping("/import")
    public ResponseEntity<ImportTransactionsResponse> importTransactions(
            @RequestParam("file") MultipartFile file,
            @AuthenticationPrincipal UserDetails userDetails) {
        if (userDetails == null) {
            throw new AuthenticationRequiredException("Authentication is required to import transactions");
        }

        if (appSettingService.isMaintenanceMode()) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "L application est temporairement en maintenance.");
        }

        if (!appSettingService.isImportsEnabled()) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "L import de transactions est temporairement desactive par l administrateur.");
        }

        long maxBytes = appSettingService.getMaxImportFileSizeMb() * 1024L * 1024L;
        if (file != null && file.getSize() > maxBytes) {
            throw new ResponseStatusException(HttpStatus.PAYLOAD_TOO_LARGE,
                    "La taille maximale autorisee est de " + appSettingService.getMaxImportFileSizeMb() + " Mo.");
        }

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(transactionImportService.importTransactions(file, userDetails.getUsername()));
    }

    /**
     * Enregistrer un paiement par carte
     * La catégorisation se fait automatiquement basée sur le nom du commerçant
     */
    @PostMapping("/card-payment")
    public ResponseEntity<TransactionResponse> createCardPayment(
            @Valid @RequestBody CardPaymentRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(transactionService.createCardPayment(request, userDetails.getUsername()));
    }

    /**
     * Endpoint classique - ancien format (mantenu pour compatibilité)
     */
    @PostMapping
    public ResponseEntity<TransactionResponse> createTransaction(
            @Valid @RequestBody TransactionRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(transactionService.createTransaction(request, userDetails.getUsername()));
    }

    /**
     * Endpoint paginé pour récupérer les transactions avec pagination
     * Paramètres: page (0-indexed), size (par défaut 25)
     * NOTE: Cet endpoint remplace getAllTransactions pour toutes les requêtes
     */
    @GetMapping
    public ResponseEntity<PaginatedTransactionResponse> getAllTransactions(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "25") int size,
            @AuthenticationPrincipal UserDetails userDetails) {
        Pageable pageable = PageRequest.of(page, size);
        return ResponseEntity.ok(transactionService.getAllTransactionsPaginated(userDetails.getUsername(), pageable));
    }

    /**
     * Endpoint paginé - alias pour compatibilité
     * Paramètres: page (0-indexed), size (par défaut 25)
     */
    @GetMapping("/paginated")
    public ResponseEntity<PaginatedTransactionResponse> getAllTransactionsPaginated(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "25") int size,
            @AuthenticationPrincipal UserDetails userDetails) {
        Pageable pageable = PageRequest.of(page, size);
        return ResponseEntity.ok(transactionService.getAllTransactionsPaginated(userDetails.getUsername(), pageable));
    }

    @GetMapping("/{id}")
    public ResponseEntity<TransactionResponse> getTransactionById(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(transactionService.getTransactionById(id, userDetails.getUsername()));
    }

    /**
     * Mettre à jour une transaction (ancien endpoint)
     */
    @PutMapping("/{id}")
    public ResponseEntity<TransactionResponse> updateTransaction(
            @PathVariable Long id,
            @Valid @RequestBody TransactionRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(transactionService.updateTransaction(id, request, userDetails.getUsername()));
    }

    /**
     * Corriger manuellement la catégorie d'une transaction
     * Permet à l'utilisateur d'ajuster la catégorisation automatique
     */
    @PatchMapping("/{id}/category")
    public ResponseEntity<TransactionResponse> updateCategory(
            @PathVariable Long id,
            @Valid @RequestBody UpdateCategoryRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(transactionService.updateCategory(id, request, userDetails.getUsername()));
    }

    @GetMapping("/{id}/cash-breakdown")
    public ResponseEntity<TransactionCashBreakdownResponse> getCashBreakdown(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(transactionCashBreakdownService.getBreakdown(id, userDetails.getUsername()));
    }

    @PutMapping("/{id}/cash-breakdown")
    public ResponseEntity<TransactionCashBreakdownResponse> saveCashBreakdown(
            @PathVariable Long id,
            @Valid @RequestBody TransactionCashBreakdownRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(transactionCashBreakdownService.saveBreakdown(id, request, userDetails.getUsername()));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteTransaction(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails userDetails) {
        transactionService.deleteTransaction(id, userDetails.getUsername());
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/all")
    public ResponseEntity<TransactionBulkDeleteResponse> deleteAllTransactions(
            @AuthenticationPrincipal UserDetails userDetails) {
        if (userDetails == null) {
            throw new AuthenticationRequiredException("Authentication is required to delete transactions");
        }

        return ResponseEntity.ok(transactionService.deleteAllCurrentUserTransactions(userDetails.getUsername()));
    }
}

