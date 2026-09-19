package za.co.qsnext.employeemanagement.esignature;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import za.co.qsnext.employeemanagement.audit.AuditService;
import za.co.qsnext.employeemanagement.document.DocumentRepository;
import za.co.qsnext.employeemanagement.esignature.dto.SignatureRequestResponse;
import za.co.qsnext.employeemanagement.esignature.dto.SignerResponse;
import za.co.qsnext.employeemanagement.exception.BusinessRuleException;
import za.co.qsnext.employeemanagement.exception.DocumentNotFoundException;
import za.co.qsnext.employeemanagement.exception.SignatureRequestNotFoundException;
import za.co.qsnext.employeemanagement.exception.UserNotFoundException;
import za.co.qsnext.employeemanagement.user.UserRepository;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
public class SignatureRequestService {

    private static final String ENTITY_TYPE = "SignatureRequest";

    private final SignatureRequestRepository signatureRequestRepository;
    private final SignatureRequestSignerRepository signerRepository;
    private final DocumentRepository documentRepository;
    private final UserRepository userRepository;
    private final ESignatureProvider eSignatureProvider;
    private final AuditService auditService;

    public SignatureRequestService(
            SignatureRequestRepository signatureRequestRepository,
            SignatureRequestSignerRepository signerRepository,
            DocumentRepository documentRepository,
            UserRepository userRepository,
            ESignatureProvider eSignatureProvider,
            AuditService auditService
    ) {
        this.signatureRequestRepository = signatureRequestRepository;
        this.signerRepository = signerRepository;
        this.documentRepository = documentRepository;
        this.userRepository = userRepository;
        this.eSignatureProvider = eSignatureProvider;
        this.auditService = auditService;
    }

    @Transactional
    public SignatureRequestResponse createRequest(
            UUID documentId,
            String title,
            List<UUID> signerUserIds,
            OffsetDateTime expiresAt,
            UUID requestedBy
    ) {
        if (!documentRepository.existsById(documentId)) {
            throw new DocumentNotFoundException("Document not found: " + documentId);
        }

        List<UUID> distinctSignerIds = signerUserIds.stream().distinct().toList();

        if (userRepository.findAllById(distinctSignerIds).size() != distinctSignerIds.size()) {
            throw new UserNotFoundException("One or more signers were not found");
        }

        SignatureRequest request = signatureRequestRepository.save(
                new SignatureRequest(documentId, title, requestedBy, expiresAt)
        );

        List<SignatureRequestSigner> signers = distinctSignerIds.stream()
                .map(signerId -> signerRepository.save(new SignatureRequestSigner(request.getId(), signerId)))
                .toList();

        eSignatureProvider.requestSignatures(request, signers);

        auditService.log("SIGNATURE_REQUEST_CREATED", ENTITY_TYPE, request.getId(), AuditService.RESULT_SUCCESS);

        return toResponse(request, signers);
    }

    public SignatureRequestResponse getById(UUID requestId, UUID requesterUserId, boolean requesterCanManage) {

        SignatureRequest request = findRequestOrThrow(requestId);
        List<SignatureRequestSigner> signers = signerRepository.findBySignatureRequestId(requestId);

        assertCanAccessRequest(request, signers, requesterUserId, requesterCanManage);

        return toResponse(request, signers);
    }

    public List<SignatureRequestResponse> getMyPendingSignatures(UUID userId) {

        List<SignatureRequestSigner> pendingSigners =
                signerRepository.findBySignerUserIdAndStatus(userId, SignatureRequestSigner.STATUS_PENDING);

        if (pendingSigners.isEmpty()) {
            return List.of();
        }

        List<UUID> requestIds = pendingSigners.stream()
                .map(SignatureRequestSigner::getSignatureRequestId)
                .distinct()
                .toList();

        Map<UUID, List<SignatureRequestSigner>> signersByRequest = signerRepository
                .findBySignatureRequestIdIn(requestIds).stream()
                .collect(Collectors.groupingBy(SignatureRequestSigner::getSignatureRequestId));

        return signatureRequestRepository.findAllById(requestIds).stream()
                .map(request -> toResponse(
                        request, signersByRequest.getOrDefault(request.getId(), List.of())
                ))
                .toList();
    }

    @Transactional
    public SignatureRequestResponse accept(UUID signerId, UUID requesterUserId) {

        SignatureRequestSigner signer = findSignerOrThrow(signerId);
        assertIsThisSigner(signer, requesterUserId);

        SignatureRequest request = findRequestOrThrow(signer.getSignatureRequestId());

        if (!signer.isPending()) {
            return toResponse(request, signerRepository.findBySignatureRequestId(request.getId()));
        }

        assertActionable(request);

        signer.sign();

        boolean allSigned = !signerRepository.existsBySignatureRequestIdAndStatusNot(
                request.getId(), SignatureRequestSigner.STATUS_SIGNED
        );

        if (allSigned) {
            request.complete();
            eSignatureProvider.notifyOutcome(request);
            auditService.log(
                    "SIGNATURE_REQUEST_COMPLETED", ENTITY_TYPE, request.getId(), AuditService.RESULT_SUCCESS
            );
        }

        auditService.log("SIGNATURE_ACCEPTED", "SignatureRequestSigner", signer.getId(), AuditService.RESULT_SUCCESS);

        return toResponse(request, signerRepository.findBySignatureRequestId(request.getId()));
    }

    @Transactional
    public SignatureRequestResponse decline(UUID signerId, UUID requesterUserId, String reason) {

        SignatureRequestSigner signer = findSignerOrThrow(signerId);
        assertIsThisSigner(signer, requesterUserId);

        SignatureRequest request = findRequestOrThrow(signer.getSignatureRequestId());

        if (!signer.isPending()) {
            return toResponse(request, signerRepository.findBySignatureRequestId(request.getId()));
        }

        assertActionable(request);

        signer.decline(reason);
        request.decline();
        eSignatureProvider.notifyOutcome(request);

        auditService.log("SIGNATURE_DECLINED", "SignatureRequestSigner", signer.getId(), AuditService.RESULT_SUCCESS);

        return toResponse(request, signerRepository.findBySignatureRequestId(request.getId()));
    }

    @Transactional
    public SignatureRequestResponse cancel(UUID requestId) {

        SignatureRequest request = findRequestOrThrow(requestId);

        if (!request.isPending()) {
            throw new BusinessRuleException("Only a pending signature request can be cancelled");
        }

        request.cancel();

        auditService.log("SIGNATURE_REQUEST_CANCELLED", ENTITY_TYPE, requestId, AuditService.RESULT_SUCCESS);

        return toResponse(request, signerRepository.findBySignatureRequestId(requestId));
    }

    private void assertActionable(SignatureRequest request) {

        if (!request.isPending()) {
            throw new BusinessRuleException("Signature request is not pending");
        }

        if (OffsetDateTime.now().isAfter(request.getExpiresAt())) {
            request.expire();
            throw new BusinessRuleException("Signature request has expired");
        }
    }

    private void assertIsThisSigner(SignatureRequestSigner signer, UUID requesterUserId) {
        if (!signer.getSignerUserId().equals(requesterUserId)) {
            throw new AccessDeniedException("You are not a signer on this request");
        }
    }

    private void assertCanAccessRequest(
            SignatureRequest request,
            List<SignatureRequestSigner> signers,
            UUID requesterUserId,
            boolean requesterCanManage
    ) {
        if (requesterCanManage || request.getRequestedBy().equals(requesterUserId)) {
            return;
        }

        boolean isSigner = signers.stream()
                .anyMatch(signer -> signer.getSignerUserId().equals(requesterUserId));

        if (!isSigner) {
            throw new AccessDeniedException("You do not have permission to view this signature request");
        }
    }

    private SignatureRequest findRequestOrThrow(UUID requestId) {
        return signatureRequestRepository.findById(requestId)
                .orElseThrow(() -> new SignatureRequestNotFoundException("Signature request not found: " + requestId));
    }

    private SignatureRequestSigner findSignerOrThrow(UUID signerId) {
        return signerRepository.findById(signerId)
                .orElseThrow(() -> new SignatureRequestNotFoundException("Signer not found: " + signerId));
    }

    private SignatureRequestResponse toResponse(SignatureRequest request, List<SignatureRequestSigner> signers) {
        return SignatureRequestResponse.from(
                request, signers.stream().map(SignerResponse::from).toList()
        );
    }
}
