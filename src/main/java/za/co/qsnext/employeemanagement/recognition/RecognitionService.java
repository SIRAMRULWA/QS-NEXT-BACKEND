package za.co.qsnext.employeemanagement.recognition;

import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import za.co.qsnext.employeemanagement.audit.AuditService;
import za.co.qsnext.employeemanagement.employee.Employee;
import za.co.qsnext.employeemanagement.employee.EmployeeRepository;
import za.co.qsnext.employeemanagement.exception.BusinessRuleException;
import za.co.qsnext.employeemanagement.exception.DuplicateResourceException;
import za.co.qsnext.employeemanagement.exception.EmployeeNotFoundException;
import za.co.qsnext.employeemanagement.exception.RecognitionNotFoundException;
import za.co.qsnext.employeemanagement.notification.NotificationPublisher;
import za.co.qsnext.employeemanagement.notification.NotificationType;
import za.co.qsnext.employeemanagement.recognition.dto.LeaderboardEntryResponse;
import za.co.qsnext.employeemanagement.recognition.dto.RecognitionResponse;
import za.co.qsnext.employeemanagement.recognition.dto.RecognitionTypeResponse;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
public class RecognitionService {

    private final RecognitionTypeRepository typeRepository;
    private final RecognitionRepository recognitionRepository;
    private final EmployeeRepository employeeRepository;
    private final NotificationPublisher notificationPublisher;
    private final AuditService auditService;

    public RecognitionService(
            RecognitionTypeRepository typeRepository,
            RecognitionRepository recognitionRepository,
            EmployeeRepository employeeRepository,
            NotificationPublisher notificationPublisher,
            AuditService auditService
    ) {
        this.typeRepository = typeRepository;
        this.recognitionRepository = recognitionRepository;
        this.employeeRepository = employeeRepository;
        this.notificationPublisher = notificationPublisher;
        this.auditService = auditService;
    }

    @Transactional
    public RecognitionTypeResponse createType(String name, String description, int pointValue) {

        if (typeRepository.existsByName(name)) {
            throw new DuplicateResourceException("Recognition type already exists: " + name);
        }

        return RecognitionTypeResponse.from(
                typeRepository.save(new RecognitionType(name, description, pointValue))
        );
    }

    public List<RecognitionTypeResponse> getActiveTypes() {
        return typeRepository.findByActiveTrue().stream().map(RecognitionTypeResponse::from).toList();
    }

    @Transactional
    public RecognitionResponse giveRecognition(
            UUID typeId,
            UUID givenByUserId,
            UUID givenToEmployeeId,
            String message,
            String visibility
    ) {
        RecognitionType type = typeRepository.findById(typeId)
                .orElseThrow(() -> new RecognitionNotFoundException("Recognition type not found: " + typeId));

        if (!type.isActive()) {
            throw new BusinessRuleException("Recognition type is no longer active: " + type.getName());
        }

        Employee recipient = employeeRepository.findById(givenToEmployeeId)
                .orElseThrow(() -> new EmployeeNotFoundException("Employee not found: " + givenToEmployeeId));

        String resolvedVisibility = (visibility == null || visibility.isBlank())
                ? Recognition.VISIBILITY_PUBLIC
                : visibility;

        Recognition recognition = recognitionRepository.save(new Recognition(
                typeId, givenByUserId, givenToEmployeeId, message, type.getPointValue(), resolvedVisibility
        ));

        notificationPublisher.publish(
                recipient.getUserId(),
                NotificationType.RECOGNITION_RECEIVED,
                "You've been recognized!",
                "You received \"" + type.getName() + "\" (" + type.getPointValue() + " points)."
        );

        auditService.log("RECOGNITION_GIVEN", "Recognition", recognition.getId(), AuditService.RESULT_SUCCESS);

        return RecognitionResponse.from(recognition);
    }

    /**
     * PRIVATE-visibility recognitions are only shown to the recipient
     * themself or to RECOGNITION_MANAGE holders - anyone else sees only
     * the PUBLIC ones.
     */
    public List<RecognitionResponse> getReceivedByEmployee(
            UUID employeeId,
            UUID requesterUserId,
            boolean requesterCanManage
    ) {
        Employee employee = employeeRepository.findById(employeeId)
                .orElseThrow(() -> new EmployeeNotFoundException("Employee not found: " + employeeId));

        boolean isSelf = employee.getUserId().equals(requesterUserId);

        return recognitionRepository.findByGivenToEmployeeIdOrderByCreatedAtDesc(employeeId).stream()
                .filter(recognition -> requesterCanManage || isSelf
                        || Recognition.VISIBILITY_PUBLIC.equals(recognition.getVisibility()))
                .map(RecognitionResponse::from)
                .toList();
    }

    public List<RecognitionResponse> getGivenByUser(UUID userId) {
        return recognitionRepository.findByGivenByUserIdOrderByCreatedAtDesc(userId).stream()
                .map(RecognitionResponse::from)
                .toList();
    }

    public long getPointsTotalForEmployee(UUID employeeId) {
        return recognitionRepository.sumPointsByGivenToEmployeeId(employeeId);
    }

    public List<LeaderboardEntryResponse> getLeaderboard(int limit) {

        List<RecognitionLeaderboardEntry> entries =
                recognitionRepository.findLeaderboard(PageRequest.of(0, Math.max(limit, 1)));

        Map<UUID, String> namesById = employeeRepository
                .findAllById(entries.stream().map(RecognitionLeaderboardEntry::getEmployeeId).toList())
                .stream()
                .collect(Collectors.toMap(Employee::getId, e -> e.getFirstName() + " " + e.getLastName()));

        return entries.stream()
                .map(entry -> new LeaderboardEntryResponse(
                        entry.getEmployeeId(),
                        namesById.get(entry.getEmployeeId()),
                        entry.getTotalPoints()
                ))
                .toList();
    }
}
