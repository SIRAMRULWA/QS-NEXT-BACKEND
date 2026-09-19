package za.co.qsnext.employeemanagement.document;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.access.AccessDeniedException;

import za.co.qsnext.employeemanagement.audit.AuditService;
import za.co.qsnext.employeemanagement.document.dto.DocumentResponse;
import za.co.qsnext.employeemanagement.employee.Employee;
import za.co.qsnext.employeemanagement.employee.EmployeeRepository;
import za.co.qsnext.employeemanagement.exception.BusinessRuleException;
import za.co.qsnext.employeemanagement.exception.DocumentNotFoundException;
import za.co.qsnext.employeemanagement.exception.EmployeeNotFoundException;

import java.lang.reflect.Field;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DocumentServiceTest {

    @Mock
    private DocumentRepository documentRepository;
    @Mock
    private EmployeeRepository employeeRepository;
    @Mock
    private DocumentStorageService documentStorageService;
    @Mock
    private AuditService auditService;

    private DocumentService documentService;

    @BeforeEach
    void setUp() {
        documentService = new DocumentService(
                documentRepository, employeeRepository, documentStorageService, auditService);
    }

    private Employee employeeWithId(UUID id, UUID userId) {
        Employee employee = new Employee(
                userId, UUID.randomUUID(), "EMP-" + id, "Jane", "Doe",
                "0123456789", "Engineer", LocalDate.of(2020, 1, 1));
        setId(employee, id);
        return employee;
    }

    @Test
    void upload_savesTheDocument_asVersionOne() {
        UUID employeeId = UUID.randomUUID();
        MockMultipartFile file = new MockMultipartFile("file", "id.pdf", "application/pdf", "content".getBytes());

        when(employeeRepository.findById(employeeId))
                .thenReturn(Optional.of(employeeWithId(employeeId, UUID.randomUUID())));
        when(documentStorageService.store(any(), any(), any(), any())).thenReturn("storage/key");
        when(documentRepository.save(any())).thenAnswer(invocation -> {
            Document document = invocation.getArgument(0);
            setId(document, UUID.randomUUID());
            return document;
        });

        DocumentResponse response = documentService.upload(
                employeeId, "ID_DOCUMENT", "ID Document", null, null, file, UUID.randomUUID());

        assertThat(response.version()).isEqualTo(1);
        assertThat(response.status()).isEqualTo(Document.STATUS_ACTIVE);
    }

    @Test
    void upload_rejectsAnEmptyFile() {
        UUID employeeId = UUID.randomUUID();
        MockMultipartFile emptyFile = new MockMultipartFile("file", "empty.pdf", "application/pdf", new byte[0]);

        when(employeeRepository.findById(employeeId))
                .thenReturn(Optional.of(employeeWithId(employeeId, UUID.randomUUID())));

        assertThatThrownBy(() -> documentService.upload(
                employeeId, "ID_DOCUMENT", "ID Document", null, null, emptyFile, UUID.randomUUID()))
                .isInstanceOf(BusinessRuleException.class);
    }

    @Test
    void upload_throws_whenEmployeeDoesNotExist() {
        UUID employeeId = UUID.randomUUID();
        MockMultipartFile file = new MockMultipartFile("file", "id.pdf", "application/pdf", "content".getBytes());

        when(employeeRepository.findById(employeeId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> documentService.upload(
                employeeId, "ID_DOCUMENT", "ID Document", null, null, file, UUID.randomUUID()))
                .isInstanceOf(EmployeeNotFoundException.class);
    }

    @Test
    void uploadNewVersion_supersedesTheCurrentActiveVersion() {
        UUID employeeId = UUID.randomUUID();
        UUID documentFamilyId = UUID.randomUUID();

        Document currentVersion = new Document(
                employeeId, "ID_DOCUMENT", "ID Document", null, documentFamilyId, 1,
                "storage/v1", "id-v1.pdf", "application/pdf", 100, null, UUID.randomUUID());
        setId(currentVersion, UUID.randomUUID());

        MockMultipartFile file = new MockMultipartFile("file", "id-v2.pdf", "application/pdf", "content".getBytes());

        when(documentRepository.findByEmployeeIdAndDocumentFamilyIdOrderByVersionDesc(employeeId, documentFamilyId))
                .thenReturn(List.of(currentVersion));
        when(documentStorageService.store(any(), any(), any(), any())).thenReturn("storage/v2");
        when(documentRepository.save(any())).thenAnswer(invocation -> {
            Document document = invocation.getArgument(0);
            setId(document, UUID.randomUUID());
            return document;
        });

        DocumentResponse response = documentService.uploadNewVersion(
                employeeId, documentFamilyId, "ID Document", null, null, file, UUID.randomUUID());

        assertThat(response.version()).isEqualTo(2);
        assertThat(currentVersion.getStatus()).isEqualTo(Document.STATUS_SUPERSEDED);
    }

    @Test
    void uploadNewVersion_throws_whenNoExistingVersionFound() {
        UUID employeeId = UUID.randomUUID();
        UUID documentFamilyId = UUID.randomUUID();
        MockMultipartFile file = new MockMultipartFile("file", "id.pdf", "application/pdf", "content".getBytes());

        when(documentRepository.findByEmployeeIdAndDocumentFamilyIdOrderByVersionDesc(employeeId, documentFamilyId))
                .thenReturn(List.of());

        assertThatThrownBy(() -> documentService.uploadNewVersion(
                employeeId, documentFamilyId, "ID Document", null, null, file, UUID.randomUUID()))
                .isInstanceOf(DocumentNotFoundException.class);
    }

    @Test
    void getCurrentDocumentsForEmployee_isAllowed_fortheOwningEmployee() {
        UUID employeeId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();

        when(employeeRepository.findById(employeeId))
                .thenReturn(Optional.of(employeeWithId(employeeId, userId)));
        when(documentRepository.findByEmployeeIdAndStatusOrderByCreatedAtDesc(employeeId, Document.STATUS_ACTIVE))
                .thenReturn(List.of());

        assertThat(documentService.getCurrentDocumentsForEmployee(employeeId, userId, false)).isEmpty();
    }

    @Test
    void getCurrentDocumentsForEmployee_isDenied_forANonOwnerWithoutManageAuthority() {
        UUID employeeId = UUID.randomUUID();

        when(employeeRepository.findById(employeeId))
                .thenReturn(Optional.of(employeeWithId(employeeId, UUID.randomUUID())));

        assertThatThrownBy(() -> documentService.getCurrentDocumentsForEmployee(
                employeeId, UUID.randomUUID(), false))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void getCurrentDocumentsForEmployee_isAllowed_forAManager() {
        UUID employeeId = UUID.randomUUID();

        when(documentRepository.findByEmployeeIdAndStatusOrderByCreatedAtDesc(employeeId, Document.STATUS_ACTIVE))
                .thenReturn(List.of());

        assertThat(documentService.getCurrentDocumentsForEmployee(employeeId, UUID.randomUUID(), true)).isEmpty();
    }

    @Test
    void download_returnsTheStoredBytes() {
        UUID documentId = UUID.randomUUID();
        UUID employeeId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();

        Document document = new Document(
                employeeId, "ID_DOCUMENT", "ID Document", null, UUID.randomUUID(), 1,
                "storage/key", "id.pdf", "application/pdf", 100, null, UUID.randomUUID());
        setId(document, documentId);

        when(documentRepository.findById(documentId)).thenReturn(Optional.of(document));
        when(employeeRepository.findById(employeeId))
                .thenReturn(Optional.of(employeeWithId(employeeId, userId)));
        when(documentStorageService.retrieve("storage/key")).thenReturn("content".getBytes());

        DocumentDownload download = documentService.download(documentId, userId, false);

        assertThat(download.filename()).isEqualTo("id.pdf");
        assertThat(download.content()).isEqualTo("content".getBytes());
    }

    @Test
    void download_throws_whenDocumentDoesNotExist() {
        UUID documentId = UUID.randomUUID();
        when(documentRepository.findById(documentId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> documentService.download(documentId, UUID.randomUUID(), true))
                .isInstanceOf(DocumentNotFoundException.class);
    }

    @Test
    void archive_setsStatusToArchived() {
        UUID documentId = UUID.randomUUID();
        Document document = new Document(
                UUID.randomUUID(), "ID_DOCUMENT", "ID Document", null, UUID.randomUUID(), 1,
                "storage/key", "id.pdf", "application/pdf", 100, null, UUID.randomUUID());
        setId(document, documentId);

        when(documentRepository.findById(documentId)).thenReturn(Optional.of(document));

        DocumentResponse response = documentService.archive(documentId);

        assertThat(response.status()).isEqualTo(Document.STATUS_ARCHIVED);
    }

    @Test
    void getExpiringDocuments_rejectsANegativeWindow() {
        assertThatThrownBy(() -> documentService.getExpiringDocuments(-1))
                .isInstanceOf(BusinessRuleException.class);
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
