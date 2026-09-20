package za.co.qsnext.employeemanagement.esignature;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface SignatureRequestSignerRepository extends JpaRepository<SignatureRequestSigner, UUID> {

    List<SignatureRequestSigner> findBySignatureRequestId(UUID signatureRequestId);

    List<SignatureRequestSigner> findBySignatureRequestIdIn(List<UUID> signatureRequestIds);

    List<SignatureRequestSigner> findBySignerUserIdAndStatus(UUID signerUserId, String status);

    boolean existsBySignatureRequestIdAndStatusNot(UUID signatureRequestId, String status);
}
