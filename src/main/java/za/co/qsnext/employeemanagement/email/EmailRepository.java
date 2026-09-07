package za.co.qsnext.employeemanagement.email;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface EmailRepository
        extends JpaRepository<Email, UUID> {

    Page<Email> findByStatus(
            EmailStatus status,
            Pageable pageable
    );
}