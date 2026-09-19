package za.co.qsnext.employeemanagement.recognition;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import za.co.qsnext.employeemanagement.audit.AuditService;
import za.co.qsnext.employeemanagement.employee.Employee;
import za.co.qsnext.employeemanagement.employee.EmployeeRepository;
import za.co.qsnext.employeemanagement.exception.BusinessRuleException;
import za.co.qsnext.employeemanagement.exception.DuplicateResourceException;
import za.co.qsnext.employeemanagement.notification.NotificationPublisher;
import za.co.qsnext.employeemanagement.notification.NotificationType;
import za.co.qsnext.employeemanagement.recognition.dto.LeaderboardEntryResponse;
import za.co.qsnext.employeemanagement.recognition.dto.RecognitionResponse;
import za.co.qsnext.employeemanagement.recognition.dto.RecognitionTypeResponse;

import java.lang.reflect.Field;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RecognitionServiceTest {

    @Mock
    private RecognitionTypeRepository typeRepository;
    @Mock
    private RecognitionRepository recognitionRepository;
    @Mock
    private EmployeeRepository employeeRepository;
    @Mock
    private NotificationPublisher notificationPublisher;
    @Mock
    private AuditService auditService;

    private RecognitionService recognitionService;

    @BeforeEach
    void setUp() {
        recognitionService = new RecognitionService(
                typeRepository, recognitionRepository, employeeRepository, notificationPublisher, auditService);
    }

    private Employee employeeWithId(UUID id, UUID userId) {
        Employee employee = new Employee(
                userId, UUID.randomUUID(), "EMP-" + id, "Jane", "Doe",
                "0123456789", "Engineer", LocalDate.of(2020, 1, 1));
        setId(employee, id);
        return employee;
    }

    private RecognitionType typeWithId(UUID id, int pointValue) {
        RecognitionType type = new RecognitionType("Above and Beyond", "Desc", pointValue);
        setId(type, id);
        return type;
    }

    @Test
    void createType_savesTheType() {
        when(typeRepository.existsByName("Above and Beyond")).thenReturn(false);
        when(typeRepository.save(any())).thenAnswer(invocation -> {
            RecognitionType type = invocation.getArgument(0);
            setId(type, UUID.randomUUID());
            return type;
        });

        RecognitionTypeResponse response = recognitionService.createType("Above and Beyond", "Desc", 10);

        assertThat(response.pointValue()).isEqualTo(10);
    }

    @Test
    void createType_rejectsADuplicateName() {
        when(typeRepository.existsByName("Above and Beyond")).thenReturn(true);

        assertThatThrownBy(() -> recognitionService.createType("Above and Beyond", "Desc", 10))
                .isInstanceOf(DuplicateResourceException.class);
    }

    @Test
    void giveRecognition_savesAndNotifiesTheRecipient_withPointsSnapshotFromTheType() {
        UUID typeId = UUID.randomUUID();
        UUID employeeId = UUID.randomUUID();
        UUID recipientUserId = UUID.randomUUID();
        UUID giverUserId = UUID.randomUUID();

        when(typeRepository.findById(typeId)).thenReturn(Optional.of(typeWithId(typeId, 25)));
        when(employeeRepository.findById(employeeId))
                .thenReturn(Optional.of(employeeWithId(employeeId, recipientUserId)));
        when(recognitionRepository.save(any())).thenAnswer(invocation -> {
            Recognition recognition = invocation.getArgument(0);
            setId(recognition, UUID.randomUUID());
            return recognition;
        });

        RecognitionResponse response = recognitionService.giveRecognition(
                typeId, giverUserId, employeeId, "Great job!", null);

        assertThat(response.points()).isEqualTo(25);
        assertThat(response.visibility()).isEqualTo(Recognition.VISIBILITY_PUBLIC);
        verify(notificationPublisher).publish(
                eq(recipientUserId), eq(NotificationType.RECOGNITION_RECEIVED), any(), any());
    }

    @Test
    void giveRecognition_throws_whenTypeIsInactive() {
        UUID typeId = UUID.randomUUID();
        RecognitionType type = typeWithId(typeId, 10);
        type.deactivate();

        when(typeRepository.findById(typeId)).thenReturn(Optional.of(type));

        assertThatThrownBy(() -> recognitionService.giveRecognition(
                typeId, UUID.randomUUID(), UUID.randomUUID(), "Great job!", null))
                .isInstanceOf(BusinessRuleException.class);
    }

    @Test
    void getReceivedByEmployee_hidesPrivateRecognitions_fromANonOwnerNonManager() {
        UUID employeeId = UUID.randomUUID();
        Recognition publicOne = new Recognition(
                UUID.randomUUID(), UUID.randomUUID(), employeeId, "msg", 10, Recognition.VISIBILITY_PUBLIC);
        Recognition privateOne = new Recognition(
                UUID.randomUUID(), UUID.randomUUID(), employeeId, "msg", 10, Recognition.VISIBILITY_PRIVATE);

        when(employeeRepository.findById(employeeId))
                .thenReturn(Optional.of(employeeWithId(employeeId, UUID.randomUUID())));
        when(recognitionRepository.findByGivenToEmployeeIdOrderByCreatedAtDesc(employeeId))
                .thenReturn(List.of(publicOne, privateOne));

        List<RecognitionResponse> results =
                recognitionService.getReceivedByEmployee(employeeId, UUID.randomUUID(), false);

        assertThat(results).hasSize(1);
        assertThat(results.getFirst().visibility()).isEqualTo(Recognition.VISIBILITY_PUBLIC);
    }

    @Test
    void getReceivedByEmployee_showsPrivateRecognitions_toTheOwner() {
        UUID employeeId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        Recognition privateOne = new Recognition(
                UUID.randomUUID(), UUID.randomUUID(), employeeId, "msg", 10, Recognition.VISIBILITY_PRIVATE);

        when(employeeRepository.findById(employeeId))
                .thenReturn(Optional.of(employeeWithId(employeeId, userId)));
        when(recognitionRepository.findByGivenToEmployeeIdOrderByCreatedAtDesc(employeeId))
                .thenReturn(List.of(privateOne));

        List<RecognitionResponse> results = recognitionService.getReceivedByEmployee(employeeId, userId, false);

        assertThat(results).hasSize(1);
    }

    @Test
    void getPointsTotalForEmployee_delegatesToRepositorySum() {
        UUID employeeId = UUID.randomUUID();
        when(recognitionRepository.sumPointsByGivenToEmployeeId(employeeId)).thenReturn(45L);

        assertThat(recognitionService.getPointsTotalForEmployee(employeeId)).isEqualTo(45L);
    }

    @Test
    void getLeaderboard_resolvesEmployeeNames() {
        UUID employeeId = UUID.randomUUID();

        RecognitionLeaderboardEntry entry = mock(RecognitionLeaderboardEntry.class);
        when(entry.getEmployeeId()).thenReturn(employeeId);
        when(entry.getTotalPoints()).thenReturn(100L);

        when(recognitionRepository.findLeaderboard(any())).thenReturn(List.of(entry));
        when(employeeRepository.findAllById(List.of(employeeId)))
                .thenReturn(List.of(employeeWithId(employeeId, UUID.randomUUID())));

        List<LeaderboardEntryResponse> leaderboard = recognitionService.getLeaderboard(10);

        assertThat(leaderboard).hasSize(1);
        assertThat(leaderboard.getFirst().employeeName()).isEqualTo("Jane Doe");
        assertThat(leaderboard.getFirst().totalPoints()).isEqualTo(100L);
    }

    private static void setId(Object entity, UUID id) {
        try {
            Field field = entity.getClass().getDeclaredField("id");
            field.setAccessible(true);
            field.set(entity, id);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException(e);
        }
    }
}
