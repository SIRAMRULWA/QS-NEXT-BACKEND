package za.co.qsnext.employeemanagement.document;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import za.co.qsnext.employeemanagement.audit.AuditService;
import za.co.qsnext.employeemanagement.document.dto.DocumentResponse;
import za.co.qsnext.employeemanagement.employee.Employee;
import za.co.qsnext.employeemanagement.employee.EmployeeRepository;
import za.co.qsnext.employeemanagement.exception.BusinessRuleException;
import za.co.qsnext.employeemanagement.exception.DocumentNotFoundException;
import za.co.qsnext.employeemanagement.exception.EmployeeNotFoundException;

import java.io.IOException;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class DocumentService {

    private static final String ENTITY_TYPE = "Document";

    private final DocumentRepository documentRepository;
    private final EmployeeRepository employeeRepository;
    private final DocumentStorageService documentStorageService;
    private final AuditService auditService;

    public DocumentService(
            DocumentRepository documentRepository,
            EmployeeRepository employeeRepository,
            DocumentStorageService documentStorageService,
            AuditService auditService
    ) {
        this.documentRepository = documentRepository;
        this.employeeRepository = employeeRepository;
        this.documentStorageService = documentStorageService;
        this.auditService = auditService;
    }

    @Transactional
    public DocumentResponse upload(
            UUID employeeId,
            String category,
            String title,
            String description,
            LocalDate expiryDate,
            MultipartFile file,
            UUID uploadedBy
    ) {
        Employee employee = employeeRepository.findById(employeeId)
                .orElseThrow(() -> new EmployeeNotFoundException("Employee not found: " + employeeId));

        byte[] content = readContent(file);

        UUID documentFamilyId = UUID.randomUUID();
        String storageKey = documentStorageService.store(
                employee.getId(), file.getOriginalFilename(), file.getContentType(), content
        );

        Document document = documentRepository.save(new Document(
                employee.getId(), category, title, description, documentFamilyId, 1,
                storageKey, safeFilename(file), safeContentType(file), content.length,
                expiryDate, uploadedBy
        ));

        auditService.log("DOCUMENT_UPLOADED", ENTITY_TYPE, document.getId(), AuditService.RESULT_SUCCESS);

        return DocumentResponse.from(document);
    }

    @Transactional
    public DocumentResponse uploadNewVersion(
            UUID employeeId,
            UUID documentFamilyId,
            String title,
            String description,
            LocalDate expiryDate,
            MultipartFile file,
            UUID uploadedBy
    ) {
        List<Document> versions = documentRepository
                .findByEmployeeIdAndDocumentFamilyIdOrderByVersionDesc(employeeId, documentFamilyId);

        if (versions.isEmpty()) {
            throw new DocumentNotFoundException("Document not found: " + documentFamilyId);
        }

        Document currentVersion = versions.getFirst();

        if (Document.STATUS_ACTIVE.equals(currentVersion.getStatus())) {
            currentVersion.supersede();
        }

        byte[] content = readContent(file);
        String storageKey = documentStorageService.store(
                employeeId, file.getOriginalFilename(), file.getContentType(), content
        );

        Document newVersion = documentRepository.save(new Document(
                employeeId, currentVersion.getCategory(), title, description, documentFamilyId,
                currentVersion.getVersion() + 1, storageKey, safeFilename(file), safeContentType(file),
                content.length, expiryDate, uploadedBy
        ));

        auditService.log("DOCUMENT_VERSION_UPLOADED", ENTITY_TYPE, newVersion.getId(), AuditService.RESULT_SUCCESS);

        return DocumentResponse.from(newVersion);
    }

    public List<DocumentResponse> getCurrentDocumentsForEmployee(
            UUID employeeId,
            UUID requesterUserId,
            boolean requesterCanManage
    ) {
        assertCanAccessEmployeeDocuments(employeeId, requesterUserId, requesterCanManage);

        return documentRepository
                .findByEmployeeIdAndStatusOrderByCreatedAtDesc(employeeId, Document.STATUS_ACTIVE)
                .stream()
                .map(DocumentResponse::from)
                .toList();
    }

    public List<DocumentResponse> getMyDocuments(UUID userId) {

        Employee employee = employeeRepository.findByUserId(userId)
                .orElseThrow(() -> new EmployeeNotFoundException("Employee profile not found"));

        return documentRepository
                .findByEmployeeIdAndStatusOrderByCreatedAtDesc(employee.getId(), Document.STATUS_ACTIVE)
                .stream()
                .map(DocumentResponse::from)
                .toList();
    }

    public List<DocumentResponse> getVersionHistory(
            UUID employeeId,
            UUID documentFamilyId,
            UUID requesterUserId,
            boolean requesterCanManage
    ) {
        assertCanAccessEmployeeDocuments(employeeId, requesterUserId, requesterCanManage);

        return documentRepository
                .findByEmployeeIdAndDocumentFamilyIdOrderByVersionDesc(employeeId, documentFamilyId)
                .stream()
                .map(DocumentResponse::from)
                .toList();
    }

    public DocumentDownload download(UUID documentId, UUID requesterUserId, boolean requesterCanManage) {

        Document document = documentRepository.findById(documentId)
                .orElseThrow(() -> new DocumentNotFoundException("Document not found: " + documentId));

        assertCanAccessEmployeeDocuments(document.getEmployeeId(), requesterUserId, requesterCanManage);

        byte[] content = documentStorageService.retrieve(document.getStorageKey());

        auditService.log("DOCUMENT_DOWNLOADED", ENTITY_TYPE, documentId, AuditService.RESULT_SUCCESS);

        return new DocumentDownload(document.getOriginalFilename(), document.getContentType(), content);
    }

    @Transactional
    public DocumentResponse archive(UUID documentId) {

        Document document = documentRepository.findById(documentId)
                .orElseThrow(() -> new DocumentNotFoundException("Document not found: " + documentId));

        document.archive();

        auditService.log("DOCUMENT_ARCHIVED", ENTITY_TYPE, documentId, AuditService.RESULT_SUCCESS);

        return DocumentResponse.from(document);
    }

    public List<DocumentResponse> getExpiringDocuments(int withinDays) {

        if (withinDays < 0) {
            throw new BusinessRuleException("withinDays cannot be negative");
        }

        LocalDate today = LocalDate.now();

        return documentRepository
                .findByStatusAndExpiryDateBetween(Document.STATUS_ACTIVE, today, today.plusDays(withinDays))
                .stream()
                .map(DocumentResponse::from)
                .toList();
    }

    private void assertCanAccessEmployeeDocuments(
            UUID employeeId,
            UUID requesterUserId,
            boolean requesterCanManage
    ) {
        if (requesterCanManage) {
            return;
        }

        Employee employee = employeeRepository.findById(employeeId)
                .orElseThrow(() -> new EmployeeNotFoundException("Employee not found: " + employeeId));

        if (!employee.getUserId().equals(requesterUserId)) {
            throw new AccessDeniedException("You do not have permission to access these documents");
        }
    }

    private byte[] readContent(MultipartFile file) {

        if (file == null || file.isEmpty()) {
            throw new BusinessRuleException("A file is required");
        }

        try {
            return file.getBytes();
        } catch (IOException e) {
            throw new DocumentStorageException("Failed to read uploaded file", e);
        }
    }

    private String safeFilename(MultipartFile file) {
        String filename = file.getOriginalFilename();
        return (filename == null || filename.isBlank()) ? "document" : filename;
    }

    private String safeContentType(MultipartFile file) {
        String contentType = file.getContentType();
        return (contentType == null || contentType.isBlank()) ? "application/octet-stream" : contentType;
    }
}
