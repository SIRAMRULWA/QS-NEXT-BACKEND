package za.co.qsnext.employeemanagement.document;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

/**
 * Development/single-node implementation of {@link DocumentStorageService}
 * that writes to a local directory. Suitable as-is for local development
 * and small deployments; a clustered/production deployment would swap
 * this for an S3/Azure Blob adapter behind the same interface.
 */
@Component
public class LocalFileSystemDocumentStorageService implements DocumentStorageService {

    private final Path basePath;

    public LocalFileSystemDocumentStorageService(
            @Value("${document.storage.local.base-path:./data/documents}") String basePath
    ) {
        this.basePath = Path.of(basePath).toAbsolutePath().normalize();
    }

    @Override
    public String store(UUID employeeId, String originalFilename, String contentType, byte[] content) {

        String storageKey = employeeId + "/" + UUID.randomUUID() + "-" + sanitize(originalFilename);
        Path target = resolveWithinBasePath(storageKey);

        try {
            Files.createDirectories(target.getParent());
            Files.write(target, content);
        } catch (IOException e) {
            throw new DocumentStorageException("Failed to store document at " + storageKey, e);
        }

        return storageKey;
    }

    @Override
    public byte[] retrieve(String storageKey) {

        Path source = resolveWithinBasePath(storageKey);

        try {
            return Files.readAllBytes(source);
        } catch (IOException e) {
            throw new DocumentStorageException("Failed to read document at " + storageKey, e);
        }
    }

    @Override
    public void delete(String storageKey) {

        Path target = resolveWithinBasePath(storageKey);

        try {
            Files.deleteIfExists(target);
        } catch (IOException e) {
            throw new DocumentStorageException("Failed to delete document at " + storageKey, e);
        }
    }

    /**
     * Resolves a storage key against the base path and rejects anything
     * that would escape it (e.g. a key containing {@code ..}), since the
     * key ultimately derives from a user-supplied filename.
     */
    private Path resolveWithinBasePath(String storageKey) {

        Path resolved = basePath.resolve(storageKey).normalize();

        if (!resolved.startsWith(basePath)) {
            throw new DocumentStorageException("Invalid storage key: " + storageKey, null);
        }

        return resolved;
    }

    private String sanitize(String originalFilename) {

        String name = (originalFilename == null || originalFilename.isBlank())
                ? "document"
                : Path.of(originalFilename).getFileName().toString();

        return name.replaceAll("[^a-zA-Z0-9._-]", "_");
    }
}
