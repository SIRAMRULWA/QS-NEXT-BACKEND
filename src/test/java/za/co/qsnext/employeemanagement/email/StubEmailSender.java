package za.co.qsnext.employeemanagement.email;

import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Test double for {@link EmailSender}, registered in place of
 * {@link SmtpEmailSender} for integration tests (see
 * {@code TestEmailConfiguration}) so tests don't depend on a real SMTP
 * server and can deterministically simulate delivery failures to exercise
 * the retry/DLQ path.
 */
public class StubEmailSender implements EmailSender {

    private final List<Email> sentEmails = new CopyOnWriteArrayList<>();
    private final Set<String> recipientsToFail = ConcurrentHashMap.newKeySet();

    @Override
    public void send(Email email) throws EmailDeliveryException {

        if (recipientsToFail.contains(email.getRecipient())) {
            throw new EmailDeliveryException(
                    "Simulated delivery failure for " + email.getRecipient(),
                    null
            );
        }

        sentEmails.add(email);
    }

    public void alwaysFailDeliveryTo(String recipient) {
        recipientsToFail.add(recipient);
    }

    public void stopFailingDeliveryTo(String recipient) {
        recipientsToFail.remove(recipient);
    }

    public List<Email> sentEmails() {
        return List.copyOf(sentEmails);
    }

    public long countSentTo(String recipient) {
        return sentEmails.stream()
                .filter(email -> email.getRecipient().equals(recipient))
                .count();
    }

    public void reset() {
        sentEmails.clear();
        recipientsToFail.clear();
    }
}
