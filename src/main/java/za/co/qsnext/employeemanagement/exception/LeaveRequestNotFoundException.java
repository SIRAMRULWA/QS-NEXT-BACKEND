package za.co.qsnext.employeemanagement.exception;

public class LeaveRequestNotFoundException extends RuntimeException {

    public LeaveRequestNotFoundException(String message) {
        super(message);
    }
}