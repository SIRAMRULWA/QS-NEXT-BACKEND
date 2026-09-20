package za.co.qsnext.employeemanagement.document;

/**
 * Unchecked - a storage failure (disk full, unreachable object store) is
 * an infrastructure fault, not an expected business outcome, so callers
 * aren't forced to handle it explicitly.
 */
public class DocumentStorageException extends RuntimeException {

    public DocumentStorageException(String message, Throwable cause) {
        super(message, cause);
    }
}
