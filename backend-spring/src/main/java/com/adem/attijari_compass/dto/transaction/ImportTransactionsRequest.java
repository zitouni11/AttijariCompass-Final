package com.adem.attijari_compass.dto.transaction;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.web.multipart.MultipartFile;

/**
 * DTO pour importer des transactions depuis un fichier CSV ou Excel
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ImportTransactionsRequest {

    @NotNull(message = "Le fichier ne peut pas être vide")
    private MultipartFile file;

    // Type de fichier : "csv" ou "excel"
    private String fileType;
}

