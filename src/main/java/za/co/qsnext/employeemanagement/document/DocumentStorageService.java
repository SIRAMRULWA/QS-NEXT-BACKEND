package za.co.qsnext.employeemanagement.document;

import java.util.UUID;

/**
 * Abstraction over where document bytes actually live, so the domain
 * layer never depends on a specific provider. {@link LocalFileSystemDocumentStorageService}
 * is the only implementation today (suitable for local development and
 * single-node deployments); a future provider (S3, Azure Blob Storage)
 * plugs in here without {@link DocumentService} needing to change.
 */
public interface DocumentStorageService {

    /**
     * Persists the given bytes and returns an opaque key that
     * {@link #retrieve(String)} and {@link #delete(String)} can later use
     * to address them. Callers must not assume anything about the key's
     * structure - it is meaningful only to the storage implementation.
     */
    String store(UUID employeeId, String originalFilename, String contentType, byte[] content);

    byte[] retrieve(String storageKey);

    void delete(String storageKey);
}
