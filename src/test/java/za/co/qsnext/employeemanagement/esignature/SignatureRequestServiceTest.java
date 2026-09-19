package za.co.qsnext.employeemanagement.esignature;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import org.springframework.security.access.AccessDeniedException;

import za.co.qsnext.employeemanagement.audit.AuditService;
import za.co.qsnext.employeemanagement.document.DocumentRepository;
import za.co.qsnext.employeemanagement.esignature.dto.SignatureRequestResponse;
import za.co.qsnext.employeemanagement.exception.BusinessRuleException;
import za.co.qsnext.employeemanagement.exception.DocumentNotFoundException;
import za.co.qsnext.employeemanagement.exception.SignatureRequestNotFoundException;
import za.co.qsnext.employeemanagement.exception.UserNotFoundException;
import za.co.qsnext.employeemanagement.user.User;
import za.co.qsnext.employeemanagement.user.UserRepository;

import java.lang.reflect.Field;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SignatureRequestServiceTest {

    @Mock
    private SignatureRequestRepository signatureRequestRepository;
    @Mock
    private SignatureRequestSignerRepository signerRepository;
    @Mock
    private DocumentRepository documentRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private ESignatureProvider eSignatureProvider;
    @Mock
    private AuditService auditService;

    private SignatureRequestService signatureRequestService;

    @BeforeEach
    void setUp() {
        signatureRequestService = new SignatureRequestService(
                signatureRequestRepository, signerRepository, documentRepository,
                userRepository, eSignatureProvider, auditService);
    }

    private SignatureRequest requestWithId(UUID id, UUID requestedBy, OffsetDateTime expiresAt) {
        SignatureRequest request = new SignatureRequest(UUID.randomUUID(), "Contract", requestedBy, expiresAt);
        setId(request, id);
        return request;
    }

    private SignatureRequestSigner signerWithId(UUID id, UUID requestId, UUID signerUserId) {
        SignatureRequestSigner signer = new SignatureRequestSigner(requestId, signerUserId);
        setId(signer, id);
        return signer;
    }

    @Test
    void createRequest_savesRequestAndSignersAndDispatchesProvider() {
        UUID documentId = UUID.randomUUID();
        UUID signerId = UUID.randomUUID();
        OffsetDateTime expiresAt = OffsetDateTime.now().plusDays(7);

        when(documentRepository.existsById(documentId)).thenReturn(true);
        when(userRepository.findAllById(List.of(signerId))).thenReturn(List.of(
                new User("signer", "signer@qsnext.co.za", "hash")
        ));
        when(signatureRequestRepository.save(any())).thenAnswer(invocation -> {
            SignatureRequest request = invocation.getArgument(0);
            setId(request, UUID.randomUUID());
            return request;
        });
        when(signerRepository.save(any())).thenAnswer(invocation -> {
            SignatureRequestSigner signer = invocation.getArgument(0);
            setId(signer, UUID.randomUUID());
            return signer;
        });

        SignatureRequestResponse response = signatureRequestService.createRequest(
                documentId, "Contract", List.of(signerId), expiresAt, UUID.randomUUID());

        assertThat(response.status()).isEqualTo(SignatureRequest.STATUS_PENDING);
        assertThat(response.signers()).hasSize(1);
        verify(eSignatureProvider).requestSignatures(any(), any());
    }

    @Test
    void createRequest_throws_whenDocumentDoesNotExist() {
        UUID documentId = UUID.randomUUID();
        when(documentRepository.existsById(documentId)).thenReturn(false);

        assertThatThrownBy(() -> signatureRequestService.createRequest(
                documentId, "Contract", List.of(UUID.randomUUID()), OffsetDateTime.now().plusDays(1), UUID.randomUUID()))
                .isInstanceOf(DocumentNotFoundException.class);
    }

    @Test
    void createRequest_throws_whenASignerDoesNotExist() {
        UUID documentId = UUID.randomUUID();
        UUID signerId = UUID.randomUUID();

        when(documentRepository.existsById(documentId)).thenReturn(true);
        when(userRepository.findAllById(List.of(signerId))).thenReturn(List.of());

        assertThatThrownBy(() -> signatureRequestService.createRequest(
                documentId, "Contract", List.of(signerId), OffsetDateTime.now().plusDays(1), UUID.randomUUID()))
                .isInstanceOf(UserNotFoundException.class);
    }

    @Test
    void accept_completesTheRequest_whenItWasTheLastPendingSigner() {
        UUID requestId = UUID.randomUUID();
        UUID signerId = UUID.randomUUID();
        UUID signerUserId = UUID.randomUUID();

        SignatureRequest request = requestWithId(requestId, UUID.randomUUID(), OffsetDateTime.now().plusDays(1));
        SignatureRequestSigner signer = signerWithId(signerId, requestId, signerUserId);

        when(signerRepository.findById(signerId)).thenReturn(Optional.of(signer));
        when(signatureRequestRepository.findById(requestId)).thenReturn(Optional.of(request));
        when(signerRepository.existsBySignatureRequestIdAndStatusNot(requestId, SignatureRequestSigner.STATUS_SIGNED))
                .thenReturn(false);
        when(signerRepository.findBySignatureRequestId(requestId)).thenReturn(List.of(signer));

        SignatureRequestResponse response = signatureRequestService.accept(signerId, signerUserId);

        assertThat(response.status()).isEqualTo(SignatureRequest.STATUS_COMPLETED);
        assertThat(signer.getStatus()).isEqualTo(SignatureRequestSigner.STATUS_SIGNED);
        verify(eSignatureProvider).notifyOutcome(request);
    }

    @Test
    void accept_leavesRequestPending_whenOtherSignersRemain() {
        UUID requestId = UUID.randomUUID();
        UUID signerId = UUID.randomUUID();
        UUID signerUserId = UUID.randomUUID();

        SignatureRequest request = requestWithId(requestId, UUID.randomUUID(), OffsetDateTime.now().plusDays(1));
        SignatureRequestSigner signer = signerWithId(signerId, requestId, signerUserId);

        when(signerRepository.findById(signerId)).thenReturn(Optional.of(signer));
        when(signatureRequestRepository.findById(requestId)).thenReturn(Optional.of(request));
        when(signerRepository.existsBySignatureRequestIdAndStatusNot(requestId, SignatureRequestSigner.STATUS_SIGNED))
                .thenReturn(true);
        when(signerRepository.findBySignatureRequestId(requestId)).thenReturn(List.of(signer));

        SignatureRequestResponse response = signatureRequestService.accept(signerId, signerUserId);

        assertThat(response.status()).isEqualTo(SignatureRequest.STATUS_PENDING);
        verify(eSignatureProvider, never()).notifyOutcome(any());
    }

    @Test
    void accept_isDenied_forANonSigner() {
        UUID signerId = UUID.randomUUID();
        SignatureRequestSigner signer = signerWithId(signerId, UUID.randomUUID(), UUID.randomUUID());

        when(signerRepository.findById(signerId)).thenReturn(Optional.of(signer));

        assertThatThrownBy(() -> signatureRequestService.accept(signerId, UUID.randomUUID()))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void accept_throwsAndExpiresTheRequest_whenPastExpiry() {
        UUID requestId = UUID.randomUUID();
        UUID signerId = UUID.randomUUID();
        UUID signerUserId = UUID.randomUUID();

        SignatureRequest request = requestWithId(requestId, UUID.randomUUID(), OffsetDateTime.now().minusMinutes(1));
        SignatureRequestSigner signer = signerWithId(signerId, requestId, signerUserId);

        when(signerRepository.findById(signerId)).thenReturn(Optional.of(signer));
        when(signatureRequestRepository.findById(requestId)).thenReturn(Optional.of(request));

        assertThatThrownBy(() -> signatureRequestService.accept(signerId, signerUserId))
                .isInstanceOf(BusinessRuleException.class);

        assertThat(request.getStatus()).isEqualTo(SignatureRequest.STATUS_EXPIRED);
    }

    @Test
    void decline_declinesBothSignerAndRequest() {
        UUID requestId = UUID.randomUUID();
        UUID signerId = UUID.randomUUID();
        UUID signerUserId = UUID.randomUUID();

        SignatureRequest request = requestWithId(requestId, UUID.randomUUID(), OffsetDateTime.now().plusDays(1));
        SignatureRequestSigner signer = signerWithId(signerId, requestId, signerUserId);

        when(signerRepository.findById(signerId)).thenReturn(Optional.of(signer));
        when(signatureRequestRepository.findById(requestId)).thenReturn(Optional.of(request));
        when(signerRepository.findBySignatureRequestId(requestId)).thenReturn(List.of(signer));

        SignatureRequestResponse response =
                signatureRequestService.decline(signerId, signerUserId, "Disagree with terms");

        assertThat(response.status()).isEqualTo(SignatureRequest.STATUS_DECLINED);
        assertThat(signer.getStatus()).isEqualTo(SignatureRequestSigner.STATUS_DECLINED);
        verify(eSignatureProvider).notifyOutcome(request);
    }

    @Test
    void cancel_cancelsAPendingRequest() {
        UUID requestId = UUID.randomUUID();
        SignatureRequest request = requestWithId(requestId, UUID.randomUUID(), OffsetDateTime.now().plusDays(1));

        when(signatureRequestRepository.findById(requestId)).thenReturn(Optional.of(request));
        when(signerRepository.findBySignatureRequestId(requestId)).thenReturn(List.of());

        SignatureRequestResponse response = signatureRequestService.cancel(requestId);

        assertThat(response.status()).isEqualTo(SignatureRequest.STATUS_CANCELLED);
    }

    @Test
    void cancel_throws_whenRequestIsNotPending() {
        UUID requestId = UUID.randomUUID();
        SignatureRequest request = requestWithId(requestId, UUID.randomUUID(), OffsetDateTime.now().plusDays(1));
        request.cancel();

        when(signatureRequestRepository.findById(requestId)).thenReturn(Optional.of(request));

        assertThatThrownBy(() -> signatureRequestService.cancel(requestId))
                .isInstanceOf(BusinessRuleException.class);
    }

    @Test
    void getById_isDenied_forAStranger() {
        UUID requestId = UUID.randomUUID();
        SignatureRequest request = requestWithId(requestId, UUID.randomUUID(), OffsetDateTime.now().plusDays(1));

        when(signatureRequestRepository.findById(requestId)).thenReturn(Optional.of(request));
        when(signerRepository.findBySignatureRequestId(requestId)).thenReturn(List.of());

        assertThatThrownBy(() -> signatureRequestService.getById(requestId, UUID.randomUUID(), false))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void getById_isAllowed_fortheRequester() {
        UUID requestId = UUID.randomUUID();
        UUID requestedBy = UUID.randomUUID();
        SignatureRequest request = requestWithId(requestId, requestedBy, OffsetDateTime.now().plusDays(1));

        when(signatureRequestRepository.findById(requestId)).thenReturn(Optional.of(request));
        when(signerRepository.findBySignatureRequestId(requestId)).thenReturn(List.of());

        SignatureRequestResponse response = signatureRequestService.getById(requestId, requestedBy, false);

        assertThat(response.id()).isEqualTo(requestId);
    }

    @Test
    void getById_throws_whenRequestDoesNotExist() {
        UUID requestId = UUID.randomUUID();
        when(signatureRequestRepository.findById(requestId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> signatureRequestService.getById(requestId, UUID.randomUUID(), true))
                .isInstanceOf(SignatureRequestNotFoundException.class);
    }

    @Test
    void getMyPendingSignatures_groupsSignersByRequest() {
        UUID userId = UUID.randomUUID();
        UUID requestId = UUID.randomUUID();
        SignatureRequest request = requestWithId(requestId, UUID.randomUUID(), OffsetDateTime.now().plusDays(1));
        SignatureRequestSigner signer = signerWithId(UUID.randomUUID(), requestId, userId);

        when(signerRepository.findBySignerUserIdAndStatus(userId, SignatureRequestSigner.STATUS_PENDING))
                .thenReturn(List.of(signer));
        when(signerRepository.findBySignatureRequestIdIn(List.of(requestId))).thenReturn(List.of(signer));
        when(signatureRequestRepository.findAllById(List.of(requestId))).thenReturn(List.of(request));

        List<SignatureRequestResponse> results = signatureRequestService.getMyPendingSignatures(userId);

        assertThat(results).hasSize(1);
        assertThat(results.getFirst().signers()).hasSize(1);
    }

    @Test
    void getMyPendingSignatures_isEmpty_whenNoneArePending() {
        UUID userId = UUID.randomUUID();
        when(signerRepository.findBySignerUserIdAndStatus(userId, SignatureRequestSigner.STATUS_PENDING))
                .thenReturn(List.of());

        assertThat(signatureRequestService.getMyPendingSignatures(userId)).isEmpty();
    }

    private static void setId(Object entity, UUID id) {
        try {
            Field field = entity.getClass().getDeclaredField("id");
            field.setAccessible(true);
            field.set(entity, id);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException(e);
        }
    }
}
