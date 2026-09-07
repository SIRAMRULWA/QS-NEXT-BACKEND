package za.co.qsnext.employeemanagement.rabbitmq;

public final class RabbitMqConstants {

    private RabbitMqConstants() {
        // Utility class
    }

    /*
     * Email messaging
     */
    public static final String EMAIL_EXCHANGE =
            "qsnext.email.exchange";

    public static final String EMAIL_QUEUE =
            "qsnext.email.queue";

    public static final String EMAIL_ROUTING_KEY =
            "email.send";

    /*
     * Dead Letter Queue
     */
    public static final String EMAIL_DLX =
            "qsnext.email.dlx";

    public static final String EMAIL_DLQ =
            "qsnext.email.dlq";

    public static final String EMAIL_DLQ_ROUTING_KEY =
            "email.failed";
}
