package za.co.qsnext.employeemanagement.rabbitmq;

public record EmailEvent(
        String recipient,
        String subject,
        String body
) {
}