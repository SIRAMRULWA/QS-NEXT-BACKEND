package za.co.qsnext.employeemanagement.recruitment;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import za.co.qsnext.employeemanagement.audit.AuditService;
import za.co.qsnext.employeemanagement.department.DepartmentRepository;
import za.co.qsnext.employeemanagement.exception.BusinessRuleException;
import za.co.qsnext.employeemanagement.exception.DepartmentNotFoundException;
import za.co.qsnext.employeemanagement.exception.DuplicateResourceException;
import za.co.qsnext.employeemanagement.exception.RecruitmentNotFoundException;
import za.co.qsnext.employeemanagement.recruitment.dto.ApplicationResponse;
import za.co.qsnext.employeemanagement.recruitment.dto.CandidateResponse;
import za.co.qsnext.employeemanagement.recruitment.dto.JobPostingResponse;
import za.co.qsnext.employeemanagement.recruitment.dto.JobRequisitionResponse;

import java.lang.reflect.Field;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RecruitmentServiceTest {

    @Mock
    private JobRequisitionRepository requisitionRepository;
    @Mock
    private JobPostingRepository postingRepository;
    @Mock
    private CandidateRepository candidateRepository;
    @Mock
    private ApplicationRepository applicationRepository;
    @Mock
    private DepartmentRepository departmentRepository;
    @Mock
    private AuditService auditService;

    private RecruitmentService recruitmentService;

    @BeforeEach
    void setUp() {
        recruitmentService = new RecruitmentService(
                requisitionRepository, postingRepository, candidateRepository,
                applicationRepository, departmentRepository, auditService);
    }

    private JobRequisition requisitionWithId(UUID id) {
        JobRequisition requisition = new JobRequisition(
                "Backend Engineer", UUID.randomUUID(), "Desc", 2, UUID.randomUUID());
        setId(requisition, id);
        return requisition;
    }

    private JobPosting postingWithId(UUID id, UUID requisitionId) {
        JobPosting posting = new JobPosting(requisitionId, "Backend Engineer", "Desc", "Remote", "FULL_TIME");
        setId(posting, id);
        return posting;
    }

    private Application applicationWithId(UUID id, UUID candidateId, UUID postingId) {
        Application application = new Application(candidateId, postingId);
        setId(application, id);
        return application;
    }

    @Test
    void createRequisition_throws_whenDepartmentDoesNotExist() {
        UUID departmentId = UUID.randomUUID();
        when(departmentRepository.existsById(departmentId)).thenReturn(false);

        assertThatThrownBy(() -> recruitmentService.createRequisition(
                "Backend Engineer", departmentId, "Desc", 2, UUID.randomUUID()))
                .isInstanceOf(DepartmentNotFoundException.class);
    }

    @Test
    void createRequisition_savesTheRequisition() {
        UUID departmentId = UUID.randomUUID();
        when(departmentRepository.existsById(departmentId)).thenReturn(true);
        when(requisitionRepository.save(any())).thenAnswer(invocation -> {
            JobRequisition requisition = invocation.getArgument(0);
            setId(requisition, UUID.randomUUID());
            return requisition;
        });

        JobRequisitionResponse response = recruitmentService.createRequisition(
                "Backend Engineer", departmentId, "Desc", 2, UUID.randomUUID());

        assertThat(response.status()).isEqualTo(JobRequisition.STATUS_OPEN);
    }

    @Test
    void createPosting_throws_whenRequisitionIsNotOpen() {
        UUID requisitionId = UUID.randomUUID();
        JobRequisition requisition = requisitionWithId(requisitionId);
        requisition.close();

        when(requisitionRepository.findById(requisitionId)).thenReturn(Optional.of(requisition));

        assertThatThrownBy(() -> recruitmentService.createPosting(
                requisitionId, "Backend Engineer", "Desc", "Remote", "FULL_TIME"))
                .isInstanceOf(BusinessRuleException.class);
    }

    @Test
    void createCandidate_rejectsADuplicateEmail() {
        when(candidateRepository.existsByEmail("jane@example.com")).thenReturn(true);

        assertThatThrownBy(() -> recruitmentService.createCandidate(
                "Jane", "Doe", "jane@example.com", "0123456789", null, "referral"))
                .isInstanceOf(DuplicateResourceException.class);
    }

    @Test
    void createApplication_rejectsWhenPostingIsClosed() {
        UUID candidateId = UUID.randomUUID();
        UUID postingId = UUID.randomUUID();
        JobPosting posting = postingWithId(postingId, UUID.randomUUID());
        posting.close();

        when(candidateRepository.existsById(candidateId)).thenReturn(true);
        when(postingRepository.findById(postingId)).thenReturn(Optional.of(posting));

        assertThatThrownBy(() -> recruitmentService.createApplication(candidateId, postingId))
                .isInstanceOf(BusinessRuleException.class);
    }

    @Test
    void createApplication_rejectsADuplicateApplication() {
        UUID candidateId = UUID.randomUUID();
        UUID postingId = UUID.randomUUID();

        when(candidateRepository.existsById(candidateId)).thenReturn(true);
        when(postingRepository.findById(postingId)).thenReturn(Optional.of(postingWithId(postingId, UUID.randomUUID())));
        when(applicationRepository.existsByCandidateIdAndJobPostingId(candidateId, postingId)).thenReturn(true);

        assertThatThrownBy(() -> recruitmentService.createApplication(candidateId, postingId))
                .isInstanceOf(BusinessRuleException.class);
    }

    @Test
    void createApplication_savesANewApplication() {
        UUID candidateId = UUID.randomUUID();
        UUID postingId = UUID.randomUUID();

        when(candidateRepository.existsById(candidateId)).thenReturn(true);
        when(postingRepository.findById(postingId)).thenReturn(Optional.of(postingWithId(postingId, UUID.randomUUID())));
        when(applicationRepository.existsByCandidateIdAndJobPostingId(candidateId, postingId)).thenReturn(false);
        when(applicationRepository.save(any())).thenAnswer(invocation -> {
            Application application = invocation.getArgument(0);
            setId(application, UUID.randomUUID());
            return application;
        });

        ApplicationResponse response = recruitmentService.createApplication(candidateId, postingId);

        assertThat(response.status()).isEqualTo(Application.STATUS_APPLIED);
    }

    @Test
    void advanceApplication_movesThroughValidStages() {
        UUID applicationId = UUID.randomUUID();
        Application application = applicationWithId(applicationId, UUID.randomUUID(), UUID.randomUUID());

        when(applicationRepository.findById(applicationId)).thenReturn(Optional.of(application));

        ApplicationResponse response =
                recruitmentService.advanceApplication(applicationId, Application.STATUS_SCREENING);

        assertThat(response.status()).isEqualTo(Application.STATUS_SCREENING);
    }

    @Test
    void advanceApplication_rejectsAnUnsupportedStage() {
        UUID applicationId = UUID.randomUUID();
        Application application = applicationWithId(applicationId, UUID.randomUUID(), UUID.randomUUID());

        when(applicationRepository.findById(applicationId)).thenReturn(Optional.of(application));

        assertThatThrownBy(() -> recruitmentService.advanceApplication(applicationId, Application.STATUS_HIRED))
                .isInstanceOf(BusinessRuleException.class);
    }

    @Test
    void advanceApplication_throws_whenApplicationIsNoLongerActive() {
        UUID applicationId = UUID.randomUUID();
        Application application = applicationWithId(applicationId, UUID.randomUUID(), UUID.randomUUID());
        application.withdraw();

        when(applicationRepository.findById(applicationId)).thenReturn(Optional.of(application));

        assertThatThrownBy(() -> recruitmentService.advanceApplication(applicationId, Application.STATUS_SCREENING))
                .isInstanceOf(BusinessRuleException.class);
    }

    @Test
    void rejectApplication_recordsTheReason() {
        UUID applicationId = UUID.randomUUID();
        Application application = applicationWithId(applicationId, UUID.randomUUID(), UUID.randomUUID());

        when(applicationRepository.findById(applicationId)).thenReturn(Optional.of(application));

        ApplicationResponse response = recruitmentService.rejectApplication(applicationId, "Not enough experience");

        assertThat(response.status()).isEqualTo(Application.STATUS_REJECTED);
        assertThat(response.rejectionReason()).isEqualTo("Not enough experience");
    }

    @Test
    void getApplication_throws_whenApplicationDoesNotExist() {
        UUID applicationId = UUID.randomUUID();
        when(applicationRepository.findById(applicationId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> recruitmentService.getApplication(applicationId))
                .isInstanceOf(RecruitmentNotFoundException.class);
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
