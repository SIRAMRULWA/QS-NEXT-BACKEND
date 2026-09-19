package za.co.qsnext.employeemanagement.document.dto;

import za.co.qsnext.employeemanagement.document.Document;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

public record DocumentResponse(
        UUID id,
        UUID employeeId,
        String category,
        String title,
        String description,
        UUID documentFamilyId,
        int version,
        String status,
        String originalFilename,
        String contentType,
        long fileSizeBytes,
        LocalDate expiryDate,
        UUID uploadedBy,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {

    public static DocumentResponse from(Document document) {
        return new DocumentResponse(
                document.getId(),
                document.getEmployeeId(),
                document.getCategory(),
                document.getTitle(),
                document.getDescription(),
                document.getDocumentFamilyId(),
                document.getVersion(),
                document.getStatus(),
                document.getOriginalFilename(),
                document.getContentType(),
                document.getFileSizeBytes(),
                document.getExpiryDate(),
                document.getUploadedBy(),
                document.getCreatedAt(),
                document.getUpdatedAt()
        );
    }
}
