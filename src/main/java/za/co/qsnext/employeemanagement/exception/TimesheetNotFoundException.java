package za.co.qsnext.employeemanagement.exception;

public class TimesheetNotFoundException extends RuntimeException {

    public TimesheetNotFoundException(String message) {
        super(message);
    }
}