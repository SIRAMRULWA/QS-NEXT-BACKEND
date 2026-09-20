package za.co.qsnext.employeemanagement.learning;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import za.co.qsnext.employeemanagement.audit.AuditService;
import za.co.qsnext.employeemanagement.compliance.ComplianceService;
import za.co.qsnext.employeemanagement.employee.Employee;
import za.co.qsnext.employeemanagement.employee.EmployeeRepository;
import za.co.qsnext.employeemanagement.exception.BusinessRuleException;
import za.co.qsnext.employeemanagement.exception.DuplicateResourceException;
import za.co.qsnext.employeemanagement.exception.EmployeeNotFoundException;
import za.co.qsnext.employeemanagement.exception.LearningNotFoundException;
import za.co.qsnext.employeemanagement.learning.dto.CourseEnrollmentResponse;
import za.co.qsnext.employeemanagement.learning.dto.CourseResponse;
import za.co.qsnext.employeemanagement.learning.dto.LearningPathResponse;
import za.co.qsnext.employeemanagement.notification.NotificationPublisher;
import za.co.qsnext.employeemanagement.notification.NotificationType;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
public class LearningService {

    private final CourseRepository courseRepository;
    private final LearningPathRepository learningPathRepository;
    private final LearningPathCourseRepository learningPathCourseRepository;
    private final CourseEnrollmentRepository enrollmentRepository;
    private final EmployeeRepository employeeRepository;
    private final NotificationPublisher notificationPublisher;
    private final ComplianceService complianceService;
    private final AuditService auditService;

    public LearningService(
            CourseRepository courseRepository,
            LearningPathRepository learningPathRepository,
            LearningPathCourseRepository learningPathCourseRepository,
            CourseEnrollmentRepository enrollmentRepository,
            EmployeeRepository employeeRepository,
            NotificationPublisher notificationPublisher,
            ComplianceService complianceService,
            AuditService auditService
    ) {
        this.courseRepository = courseRepository;
        this.learningPathRepository = learningPathRepository;
        this.learningPathCourseRepository = learningPathCourseRepository;
        this.enrollmentRepository = enrollmentRepository;
        this.employeeRepository = employeeRepository;
        this.notificationPublisher = notificationPublisher;
        this.complianceService = complianceService;
        this.auditService = auditService;
    }

    @Transactional
    public CourseResponse createCourse(
            String title,
            String description,
            String category,
            Integer durationMinutes,
            boolean mandatory,
            UUID linkedComplianceRequirementId
    ) {
        if (courseRepository.existsByTitle(title)) {
            throw new DuplicateResourceException("Course already exists: " + title);
        }

        Course course = courseRepository.save(new Course(
                title, description, category, durationMinutes, mandatory, linkedComplianceRequirementId
        ));

        auditService.log("COURSE_CREATED", "Course", course.getId(), AuditService.RESULT_SUCCESS);

        return CourseResponse.from(course);
    }

    public List<CourseResponse> getActiveCourses() {
        return courseRepository.findByActiveTrue().stream().map(CourseResponse::from).toList();
    }

    @Transactional
    public LearningPathResponse createLearningPath(String name, String description, List<UUID> courseIds) {

        if (learningPathRepository.existsByName(name)) {
            throw new DuplicateResourceException("Learning path already exists: " + name);
        }

        List<Course> courses = courseRepository.findAllById(courseIds);

        if (courses.size() != courseIds.stream().distinct().count()) {
            throw new LearningNotFoundException("One or more courses were not found");
        }

        LearningPath path = learningPathRepository.save(new LearningPath(name, description));

        for (int i = 0; i < courseIds.size(); i++) {
            learningPathCourseRepository.save(new LearningPathCourse(path.getId(), courseIds.get(i), i));
        }

        auditService.log("LEARNING_PATH_CREATED", "LearningPath", path.getId(), AuditService.RESULT_SUCCESS);

        return toLearningPathResponse(path);
    }

    public List<LearningPathResponse> getAllLearningPaths() {
        return learningPathRepository.findAll().stream().map(this::toLearningPathResponse).toList();
    }

    @Transactional
    public CourseEnrollmentResponse enroll(
            UUID employeeId,
            UUID courseId,
            UUID requesterUserId,
            boolean requesterCanManage
    ) {
        assertCanAccessEmployeeLearning(employeeId, requesterUserId, requesterCanManage);

        if (!employeeRepository.existsById(employeeId)) {
            throw new EmployeeNotFoundException("Employee not found: " + employeeId);
        }

        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new LearningNotFoundException("Course not found: " + courseId));

        if (!course.isActive()) {
            throw new BusinessRuleException("Course is no longer active: " + course.getTitle());
        }

        if (enrollmentRepository.existsByEmployeeIdAndCourseId(employeeId, courseId)) {
            throw new BusinessRuleException("Employee is already enrolled in this course");
        }

        CourseEnrollment enrollment = enrollmentRepository.save(new CourseEnrollment(employeeId, courseId));

        auditService.log("COURSE_ENROLLED", "CourseEnrollment", enrollment.getId(), AuditService.RESULT_SUCCESS);

        return CourseEnrollmentResponse.from(enrollment);
    }

    @Transactional
    public CourseEnrollmentResponse updateProgress(
            UUID enrollmentId,
            int progressPercent,
            UUID requesterUserId,
            boolean requesterCanManage
    ) {
        CourseEnrollment enrollment = findEnrollmentOrThrow(enrollmentId);
        assertCanAccessEmployeeLearning(enrollment.getEmployeeId(), requesterUserId, requesterCanManage);

        if (enrollment.isCompleted()) {
            return CourseEnrollmentResponse.from(enrollment);
        }

        enrollment.updateProgress(progressPercent);

        if (enrollment.isCompleted()) {
            onCourseCompleted(enrollment);
        }

        return CourseEnrollmentResponse.from(enrollment);
    }

    @Transactional
    public CourseEnrollmentResponse completeCourse(
            UUID enrollmentId,
            UUID requesterUserId,
            boolean requesterCanManage
    ) {
        CourseEnrollment enrollment = findEnrollmentOrThrow(enrollmentId);
        assertCanAccessEmployeeLearning(enrollment.getEmployeeId(), requesterUserId, requesterCanManage);

        if (enrollment.isCompleted()) {
            return CourseEnrollmentResponse.from(enrollment);
        }

        enrollment.markCompleted();
        onCourseCompleted(enrollment);

        return CourseEnrollmentResponse.from(enrollment);
    }

    public List<CourseEnrollmentResponse> getEnrollmentsForEmployee(
            UUID employeeId,
            UUID requesterUserId,
            boolean requesterCanManage
    ) {
        assertCanAccessEmployeeLearning(employeeId, requesterUserId, requesterCanManage);

        return enrollmentRepository.findByEmployeeId(employeeId).stream()
                .map(CourseEnrollmentResponse::from)
                .toList();
    }

    public List<CourseEnrollmentResponse> getMyEnrollments(UUID userId) {

        Employee employee = employeeRepository.findByUserId(userId)
                .orElseThrow(() -> new EmployeeNotFoundException("Employee profile not found"));

        return enrollmentRepository.findByEmployeeId(employee.getId()).stream()
                .map(CourseEnrollmentResponse::from)
                .toList();
    }

    private void onCourseCompleted(CourseEnrollment enrollment) {

        Course course = courseRepository.findById(enrollment.getCourseId())
                .orElseThrow(() -> new LearningNotFoundException("Course not found: " + enrollment.getCourseId()));

        Employee employee = employeeRepository.findById(enrollment.getEmployeeId())
                .orElseThrow(() -> new EmployeeNotFoundException("Employee not found: " + enrollment.getEmployeeId()));

        notificationPublisher.publish(
                employee.getUserId(),
                NotificationType.COURSE_COMPLETED,
                "Course completed",
                "You have completed \"" + course.getTitle() + "\"."
        );

        if (course.getLinkedComplianceRequirementId() != null) {
            complianceService.autoCompleteFromTraining(
                    enrollment.getEmployeeId(), course.getLinkedComplianceRequirementId()
            );
        }

        auditService.log("COURSE_COMPLETED", "CourseEnrollment", enrollment.getId(), AuditService.RESULT_SUCCESS);
    }

    private LearningPathResponse toLearningPathResponse(LearningPath path) {

        List<LearningPathCourse> pathCourses =
                learningPathCourseRepository.findByLearningPathIdOrderBySortOrderAsc(path.getId());

        Map<UUID, Course> coursesById = courseRepository
                .findAllById(pathCourses.stream().map(LearningPathCourse::getCourseId).toList())
                .stream()
                .collect(Collectors.toMap(Course::getId, course -> course));

        List<CourseResponse> courses = pathCourses.stream()
                .map(pathCourse -> coursesById.get(pathCourse.getCourseId()))
                .filter(java.util.Objects::nonNull)
                .map(CourseResponse::from)
                .toList();

        return LearningPathResponse.from(path, courses);
    }

    private CourseEnrollment findEnrollmentOrThrow(UUID enrollmentId) {
        return enrollmentRepository.findById(enrollmentId)
                .orElseThrow(() -> new LearningNotFoundException("Enrollment not found: " + enrollmentId));
    }

    private void assertCanAccessEmployeeLearning(
            UUID employeeId,
            UUID requesterUserId,
            boolean requesterCanManage
    ) {
        if (requesterCanManage) {
            return;
        }

        Employee employee = employeeRepository.findById(employeeId)
                .orElseThrow(() -> new EmployeeNotFoundException("Employee not found: " + employeeId));

        if (!employee.getUserId().equals(requesterUserId)) {
            throw new AccessDeniedException("You do not have permission to access this employee's learning records");
        }
    }
}
