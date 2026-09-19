package za.co.qsnext.employeemanagement.document;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import za.co.qsnext.employeemanagement.document.dto.DocumentResponse;
import za.co.qsnext.employeemanagement.security.CustomUserDetails;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/documents")
public class DocumentController {

    private static final String MANAGE_AUTHORITY = "DOCUMENT_MANAGE";

    private final DocumentService documentService;

    public DocumentController(DocumentService documentService) {
        this.documentService = documentService;
    }

    @PreAuthorize("hasAuthority('DOCUMENT_MANAGE')")
    @PostMapping(value = "/employees/{employeeId}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<DocumentResponse> upload(
            Authentication authentication,
            @PathVariable UUID employeeId,
            @RequestParam String category,
            @RequestParam String title,
            @RequestParam(required = false) String description,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate expiryDate,
            @RequestParam("file") MultipartFile file
    ) {
        CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();

        DocumentResponse response = documentService.upload(
                employeeId, category, title, description, expiryDate, file, userDetails.getUserId()
        );

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PreAuthorize("hasAuthority('DOCUMENT_MANAGE')")
    @PostMapping(
            value = "/employees/{employeeId}/families/{documentFamilyId}/versions",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE
    )
    public ResponseEntity<DocumentResponse> uploadNewVersion(
            Authentication authentication,
            @PathVariable UUID employeeId,
            @PathVariable UUID documentFamilyId,
            @RequestParam String title,
            @RequestParam(required = false) String description,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate expiryDate,
            @RequestParam("file") MultipartFile file
    ) {
        CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();

        DocumentResponse response = documentService.uploadNewVersion(
                employeeId, documentFamilyId, title, description, expiryDate, file, userDetails.getUserId()
        );

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PreAuthorize("hasAuthority('DOCUMENT_READ')")
    @GetMapping("/employees/{employeeId}")
    public ResponseEntity<List<DocumentResponse>> getEmployeeDocuments(
            Authentication authentication,
            @PathVariable UUID employeeId
    ) {
        CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();

        return ResponseEntity.ok(documentService.getCurrentDocumentsForEmployee(
                employeeId, userDetails.getUserId(), canManage(authentication)
        ));
    }

    @PreAuthorize("hasAuthority('DOCUMENT_READ')")
    @GetMapping("/my-documents")
    public ResponseEntity<List<DocumentResponse>> getMyDocuments(Authentication authentication) {
        CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();

        return ResponseEntity.ok(documentService.getMyDocuments(userDetails.getUserId()));
    }

    @PreAuthorize("hasAuthority('DOCUMENT_READ')")
    @GetMapping("/employees/{employeeId}/families/{documentFamilyId}/versions")
    public ResponseEntity<List<DocumentResponse>> getVersionHistory(
            Authentication authentication,
            @PathVariable UUID employeeId,
            @PathVariable UUID documentFamilyId
    ) {
        CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();

        return ResponseEntity.ok(documentService.getVersionHistory(
                employeeId, documentFamilyId, userDetails.getUserId(), canManage(authentication)
        ));
    }

    @PreAuthorize("hasAuthority('DOCUMENT_READ')")
    @GetMapping("/{documentId}/download")
    public ResponseEntity<byte[]> download(
            Authentication authentication,
            @PathVariable UUID documentId
    ) {
        CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();

        DocumentDownload download = documentService.download(
                documentId, userDetails.getUserId(), canManage(authentication)
        );

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + download.filename() + "\"")
                .contentType(MediaType.parseMediaType(download.contentType()))
                .body(download.content());
    }

    @PreAuthorize("hasAuthority('DOCUMENT_MANAGE')")
    @PatchMapping("/{documentId}/archive")
    public ResponseEntity<DocumentResponse> archive(@PathVariable UUID documentId) {
        return ResponseEntity.ok(documentService.archive(documentId));
    }

    @PreAuthorize("hasAuthority('DOCUMENT_MANAGE')")
    @GetMapping("/expiring")
    public ResponseEntity<List<DocumentResponse>> getExpiringDocuments(
            @RequestParam(defaultValue = "30") int withinDays
    ) {
        return ResponseEntity.ok(documentService.getExpiringDocuments(withinDays));
    }

    private boolean canManage(Authentication authentication) {
        return authentication.getAuthorities().stream()
                .anyMatch(authority -> MANAGE_AUTHORITY.equals(authority.getAuthority()));
    }
}
