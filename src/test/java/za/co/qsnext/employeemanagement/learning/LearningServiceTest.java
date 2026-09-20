package za.co.qsnext.employeemanagement.learning;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import org.springframework.security.access.AccessDeniedException;

import za.co.qsnext.employeemanagement.audit.AuditService;
import za.co.qsnext.employeemanagement.compliance.ComplianceService;
import za.co.qsnext.employeemanagement.employee.Employee;
import za.co.qsnext.employeemanagement.employee.EmployeeRepository;
import za.co.qsnext.employeemanagement.exception.BusinessRuleException;
import za.co.qsnext.employeemanagement.exception.LearningNotFoundException;
import za.co.qsnext.employeemanagement.learning.dto.CourseEnrollmentResponse;
import za.co.qsnext.employeemanagement.learning.dto.LearningPathResponse;
import za.co.qsnext.employeemanagement.notification.NotificationPublisher;
import za.co.qsnext.employeemanagement.notification.NotificationType;

import java.lang.reflect.Field;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LearningServiceTest {

    @Mock
    private CourseRepository courseRepository;
    @Mock
    private LearningPathRepository learningPathRepository;
    @Mock
    private LearningPathCourseRepository learningPathCourseRepository;
    @Mock
    private CourseEnrollmentRepository enrollmentRepository;
    @Mock
    private EmployeeRepository employeeRepository;
    @Mock
    private NotificationPublisher notificationPublisher;
    @Mock
    private ComplianceService complianceService;
    @Mock
    private AuditService auditService;

    private LearningService learningService;

    @BeforeEach
    void setUp() {
        learningService = new LearningService(
                courseRepository, learningPathRepository, learningPathCourseRepository,
                enrollmentRepository, employeeRepository, notificationPublisher,
                complianceService, auditService);
    }

    private Employee employeeWithId(UUID id, UUID userId) {
        Employee employee = new Employee(
                userId, UUID.randomUUID(), "EMP-" + id, "Jane", "Doe",
                "0123456789", "Engineer", LocalDate.of(2020, 1, 1));
        setId(employee, id);
        return employee;
    }

    private Course courseWithId(UUID id, UUID linkedRequirementId) {
        Course course = new Course("Onboarding 101", "Desc", "General", 30, false, linkedRequirementId);
        setId(course, id);
        return course;
    }

    @Test
    void createCourse_savesTheCourse() {
        when(courseRepository.existsByTitle("Onboarding 101")).thenReturn(false);
        when(courseRepository.save(any())).thenAnswer(invocation -> {
            Course course = invocation.getArgument(0);
            setId(course, UUID.randomUUID());
            return course;
        });

        var response = learningService.createCourse("Onboarding 101", "Desc", "General", 30, true, null);

        assertThat(response.title()).isEqualTo("Onboarding 101");
        assertThat(response.mandatory()).isTrue();
    }

    @Test
    void createLearningPath_savesPathAndOrderedCourses() {
        UUID courseId1 = UUID.randomUUID();
        UUID courseId2 = UUID.randomUUID();

        when(learningPathRepository.existsByName("New Hire Path")).thenReturn(false);
        when(courseRepository.findAllById(List.of(courseId1, courseId2)))
                .thenReturn(List.of(courseWithId(courseId1, null), courseWithId(courseId2, null)));
        when(learningPathRepository.save(any())).thenAnswer(invocation -> {
            LearningPath path = invocation.getArgument(0);
            setId(path, UUID.randomUUID());
            return path;
        });
        when(learningPathCourseRepository.findByLearningPathIdOrderBySortOrderAsc(any())).thenReturn(List.of());

        LearningPathResponse response = learningService.createLearningPath(
                "New Hire Path", "Desc", List.of(courseId1, courseId2));

        assertThat(response.name()).isEqualTo("New Hire Path");
        verify(learningPathCourseRepository, org.mockito.Mockito.times(2)).save(any());
    }

    @Test
    void createLearningPath_throws_whenACourseDoesNotExist() {
        UUID courseId = UUID.randomUUID();
        when(learningPathRepository.existsByName("Path")).thenReturn(false);
        when(courseRepository.findAllById(List.of(courseId))).thenReturn(List.of());

        assertThatThrownBy(() -> learningService.createLearningPath("Path", "Desc", List.of(courseId)))
                .isInstanceOf(LearningNotFoundException.class);
    }

    @Test
    void enroll_createsAnEnrollment() {
        UUID employeeId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        UUID courseId = UUID.randomUUID();

        when(employeeRepository.findById(employeeId))
                .thenReturn(Optional.of(employeeWithId(employeeId, userId)));
        when(employeeRepository.existsById(employeeId)).thenReturn(true);
        when(courseRepository.findById(courseId)).thenReturn(Optional.of(courseWithId(courseId, null)));
        when(enrollmentRepository.existsByEmployeeIdAndCourseId(employeeId, courseId)).thenReturn(false);
        when(enrollmentRepository.save(any())).thenAnswer(invocation -> {
            CourseEnrollment enrollment = invocation.getArgument(0);
            setId(enrollment, UUID.randomUUID());
            return enrollment;
        });

        CourseEnrollmentResponse response = learningService.enroll(employeeId, courseId, userId, false);

        assertThat(response.status()).isEqualTo(CourseEnrollment.STATUS_ENROLLED);
    }

    @Test
    void enroll_rejectsADuplicateEnrollment() {
        UUID employeeId = UUID.randomUUID();
        UUID courseId = UUID.randomUUID();

        when(employeeRepository.existsById(employeeId)).thenReturn(true);
        when(courseRepository.findById(courseId)).thenReturn(Optional.of(courseWithId(courseId, null)));
        when(enrollmentRepository.existsByEmployeeIdAndCourseId(employeeId, courseId)).thenReturn(true);

        assertThatThrownBy(() -> learningService.enroll(employeeId, courseId, UUID.randomUUID(), true))
                .isInstanceOf(BusinessRuleException.class);
    }

    @Test
    void updateProgress_completesEnrollment_notifiesAndTriggersComplianceHook() {
        UUID employeeId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        UUID courseId = UUID.randomUUID();
        UUID requirementId = UUID.randomUUID();
        UUID enrollmentId = UUID.randomUUID();

        CourseEnrollment enrollment = new CourseEnrollment(employeeId, courseId);
        setId(enrollment, enrollmentId);

        when(enrollmentRepository.findById(enrollmentId)).thenReturn(Optional.of(enrollment));
        when(employeeRepository.findById(employeeId))
                .thenReturn(Optional.of(employeeWithId(employeeId, userId)));
        when(courseRepository.findById(courseId)).thenReturn(Optional.of(courseWithId(courseId, requirementId)));

        CourseEnrollmentResponse response = learningService.updateProgress(enrollmentId, 100, userId, false);

        assertThat(response.status()).isEqualTo(CourseEnrollment.STATUS_COMPLETED);
        verify(notificationPublisher).publish(eq(userId), eq(NotificationType.COURSE_COMPLETED), any(), any());
        verify(complianceService).autoCompleteFromTraining(employeeId, requirementId);
    }

    @Test
    void updateProgress_isIdempotent_whenAlreadyCompleted() {
        UUID employeeId = UUID.randomUUID();
        UUID courseId = UUID.randomUUID();
        UUID enrollmentId = UUID.randomUUID();

        CourseEnrollment enrollment = new CourseEnrollment(employeeId, courseId);
        enrollment.markCompleted();
        setId(enrollment, enrollmentId);

        when(enrollmentRepository.findById(enrollmentId)).thenReturn(Optional.of(enrollment));

        CourseEnrollmentResponse response = learningService.updateProgress(enrollmentId, 50, UUID.randomUUID(), true);

        assertThat(response.progressPercent()).isEqualTo(100);
        verify(notificationPublisher, never()).publish(any(), any(), any(), any());
    }

    @Test
    void completeCourse_doesNotTriggerComplianceHook_whenCourseHasNoLinkedRequirement() {
        UUID employeeId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        UUID courseId = UUID.randomUUID();
        UUID enrollmentId = UUID.randomUUID();

        CourseEnrollment enrollment = new CourseEnrollment(employeeId, courseId);
        setId(enrollment, enrollmentId);

        when(enrollmentRepository.findById(enrollmentId)).thenReturn(Optional.of(enrollment));
        when(employeeRepository.findById(employeeId))
                .thenReturn(Optional.of(employeeWithId(employeeId, userId)));
        when(courseRepository.findById(courseId)).thenReturn(Optional.of(courseWithId(courseId, null)));

        learningService.completeCourse(enrollmentId, userId, false);

        verify(complianceService, never()).autoCompleteFromTraining(any(), any());
    }

    @Test
    void getEnrollmentsForEmployee_isDenied_forANonOwnerWithoutManageAuthority() {
        UUID employeeId = UUID.randomUUID();

        when(employeeRepository.findById(employeeId))
                .thenReturn(Optional.of(employeeWithId(employeeId, UUID.randomUUID())));

        assertThatThrownBy(() -> learningService.getEnrollmentsForEmployee(employeeId, UUID.randomUUID(), false))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void getMyEnrollments_resolvesEmployeeByUserId() {
        UUID userId = UUID.randomUUID();
        UUID employeeId = UUID.randomUUID();

        when(employeeRepository.findByUserId(userId))
                .thenReturn(Optional.of(employeeWithId(employeeId, userId)));
        when(enrollmentRepository.findByEmployeeId(employeeId)).thenReturn(List.of());

        assertThat(learningService.getMyEnrollments(userId)).isEmpty();
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
