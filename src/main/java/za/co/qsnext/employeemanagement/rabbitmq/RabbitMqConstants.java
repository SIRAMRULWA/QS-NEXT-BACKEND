package za.co.qsnext.employeemanagement.rabbitmq;

public final class RabbitMqConstants {

    private RabbitMqConstants() {
    }

    public static final String EMAIL_EXCHANGE =
            "qsnext.email.exchange";

    public static final String EMAIL_QUEUE =
            "qsnext.email.queue";

    public static final String EMAIL_ROUTING_KEY =
            "email.send";

    public static final String EMAIL_DEAD_LETTER_EXCHANGE =
            "qsnext.email.dlx";

    public static final String EMAIL_DEAD_LETTER_QUEUE =
            "qsnext.email.dlq";

    public static final String EMAIL_DEAD_LETTER_ROUTING_KEY =
            "email.failed";
}