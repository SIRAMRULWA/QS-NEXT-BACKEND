package za.co.qsnext.employeemanagement.document;

import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public interface DocumentRepository extends JpaRepository<Document, UUID> {

    List<Document> findByEmployeeIdAndStatusOrderByCreatedAtDesc(UUID employeeId, String status);

    List<Document> findByEmployeeIdAndDocumentFamilyIdOrderByVersionDesc(
            UUID employeeId,
            UUID documentFamilyId
    );

    List<Document> findByStatusAndExpiryDateBetween(
            String status,
            LocalDate from,
            LocalDate to
    );
}
