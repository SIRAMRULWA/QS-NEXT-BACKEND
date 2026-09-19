package za.co.qsnext.employeemanagement.compliance.dto;

import jakarta.validation.constraints.Size;

import java.util.UUID;

public record CompleteComplianceRecordRequest(

        UUID evidenceDocumentId,

        @Size(max = 1000, message = "Notes must not exceed 1000 characters")
        String notes
) {
}
