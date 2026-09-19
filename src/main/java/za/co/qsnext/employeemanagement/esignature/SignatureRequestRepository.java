package za.co.qsnext.employeemanagement.esignature;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface SignatureRequestRepository extends JpaRepository<SignatureRequest, UUID> {
}
