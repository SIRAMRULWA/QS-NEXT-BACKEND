package za.co.qsnext.employeemanagement.notification;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import za.co.qsnext.employeemanagement.notification.dto.NotificationPreferenceResponse;
import za.co.qsnext.employeemanagement.user.UserRepository;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {

    @Mock
    private NotificationRepository notificationRepository;
    @Mock
    private NotificationPreferenceRepository notificationPreferenceRepository;
    @Mock
    private UserRepository userRepository;

    private NotificationService notificationService;

    @BeforeEach
    void setUp() {
        notificationService = new NotificationService(
                notificationRepository, notificationPreferenceRepository, userRepository);
    }

    @Test
    void getPreference_returnsDefaults_whenNoneIsStored() {
        UUID userId = UUID.randomUUID();
        when(notificationPreferenceRepository.findByUserId(userId)).thenReturn(Optional.empty());

        NotificationPreferenceResponse response = notificationService.getPreference(userId);

        assertThat(response).isEqualTo(NotificationPreferenceResponse.defaults());
    }

    @Test
    void getPreference_returnsStoredValues() {
        UUID userId = UUID.randomUUID();
        NotificationPreference preference = new NotificationPreference(userId);
        preference.update(false, true);

        when(notificationPreferenceRepository.findByUserId(userId)).thenReturn(Optional.of(preference));

        NotificationPreferenceResponse response = notificationService.getPreference(userId);

        assertThat(response.inAppEnabled()).isFalse();
        assertThat(response.emailEnabled()).isTrue();
    }

    @Test
    void updatePreference_createsANewRow_whenNoneExists() {
        UUID userId = UUID.randomUUID();
        when(notificationPreferenceRepository.findByUserId(userId)).thenReturn(Optional.empty());
        when(notificationPreferenceRepository.save(any()))
                .thenAnswer(invocation -> invocation.getArgument(0));

        NotificationPreferenceResponse response =
                notificationService.updatePreference(userId, false, false);

        assertThat(response.inAppEnabled()).isFalse();
        assertThat(response.emailEnabled()).isFalse();
        verify(notificationPreferenceRepository).save(any(NotificationPreference.class));
    }

    @Test
    void updatePreference_updatesTheExistingRow() {
        UUID userId = UUID.randomUUID();
        NotificationPreference existing = new NotificationPreference(userId);

        when(notificationPreferenceRepository.findByUserId(userId)).thenReturn(Optional.of(existing));
        when(notificationPreferenceRepository.save(any()))
                .thenAnswer(invocation -> invocation.getArgument(0));

        NotificationPreferenceResponse response =
                notificationService.updatePreference(userId, true, false);

        assertThat(response.inAppEnabled()).isTrue();
        assertThat(response.emailEnabled()).isFalse();
        assertThat(existing.isEmailEnabled()).isFalse();
    }
}
