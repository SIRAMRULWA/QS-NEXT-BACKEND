package za.co.qsnext.employeemanagement.exception;

/**
 * Covers a missing MobileDevice.
 */
public class MobileDeviceNotFoundException extends RuntimeException {

    public MobileDeviceNotFoundException(String message) {
        super(message);
    }
}
