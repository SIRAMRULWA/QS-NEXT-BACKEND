package za.co.qsnext.employeemanagement.document;

/**
 * Carries file bytes back to the controller for a download response.
 * Deliberately not a DTO (DTOs in this codebase are JSON response
 * shapes; this is a raw-bytes transfer object between service and
 * controller only).
 */
public record DocumentDownload(String filename, String contentType, byte[] content) {
}
