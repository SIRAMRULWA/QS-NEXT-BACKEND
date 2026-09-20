package za.co.qsnext.employeemanagement.notification;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import org.springframework.context.ApplicationEventPublisher;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class NotificationPublisherTest {

    @Mock
    private ApplicationEventPublisher eventPublisher;

    private NotificationPublisher notificationPublisher;

    @BeforeEach
    void setUp() {
        notificationPublisher = new NotificationPublisher(eventPublisher);
    }

    @Test
    void publish_publishesANotificationEvent() {
        UUID userId = UUID.randomUUID();

        notificationPublisher.publish(
                userId, NotificationType.LEAVE_REQUEST_APPROVED, "Title", "Message");

        ArgumentCaptor<NotificationEvent> captor = ArgumentCaptor.forClass(NotificationEvent.class);
        verify(eventPublisher).publishEvent(captor.capture());

        NotificationEvent event = captor.getValue();
        assertThat(event.recipientUserId()).isEqualTo(userId);
        assertThat(event.type()).isEqualTo(NotificationType.LEAVE_REQUEST_APPROVED);
        assertThat(event.title()).isEqualTo("Title");
        assertThat(event.message()).isEqualTo("Message");
    }
}
