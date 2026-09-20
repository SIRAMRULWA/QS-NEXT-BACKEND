package za.co.qsnext.employeemanagement.document;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unlike the rest of this phase's integration coverage, this test needs
 * no database, message broker or Testcontainers - just a real local
 * directory - so it actually runs in this environment despite the
 * Docker-pull restriction that blocks Testcontainers-based tests here.
 */
class LocalFileSystemDocumentStorageServiceTest {

    @TempDir
    Path tempDir;

    @Test
    void storeAndRetrieve_roundTripsTheBytes() {
        LocalFileSystemDocumentStorageService storage =
                new LocalFileSystemDocumentStorageService(tempDir.toString());

        UUID employeeId = UUID.randomUUID();
        byte[] content = "hello document".getBytes();

        String storageKey = storage.store(employeeId, "id.pdf", "application/pdf", content);

        assertThat(storageKey).startsWith(employeeId.toString() + "/");
        assertThat(storage.retrieve(storageKey)).isEqualTo(content);
    }

    @Test
    void store_sanitizesAPathTraversalFilename() {
        LocalFileSystemDocumentStorageService storage =
                new LocalFileSystemDocumentStorageService(tempDir.toString());

        String storageKey = storage.store(
                UUID.randomUUID(), "../../etc/passwd", "text/plain", "x".getBytes());

        assertThat(storageKey).doesNotContain("..");
        assertThat(Path.of(tempDir.toString()).resolve(storageKey).normalize())
                .startsWith(tempDir);
    }

    @Test
    void delete_removesTheStoredFile() {
        LocalFileSystemDocumentStorageService storage =
                new LocalFileSystemDocumentStorageService(tempDir.toString());

        String storageKey = storage.store(UUID.randomUUID(), "id.pdf", "application/pdf", "x".getBytes());

        storage.delete(storageKey);

        assertThatThrownBy(() -> storage.retrieve(storageKey))
                .isInstanceOf(DocumentStorageException.class);
    }

    @Test
    void retrieve_rejectsAKeyThatWouldEscapeTheBasePath() {
        LocalFileSystemDocumentStorageService storage =
                new LocalFileSystemDocumentStorageService(tempDir.toString());

        assertThatThrownBy(() -> storage.retrieve("../../../etc/passwd"))
                .isInstanceOf(DocumentStorageException.class);
    }
}
