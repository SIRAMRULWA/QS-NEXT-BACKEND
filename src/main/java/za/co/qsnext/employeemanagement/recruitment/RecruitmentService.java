package za.co.qsnext.employeemanagement.recruitment;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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

import java.util.List;
import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class RecruitmentService {

    private final JobRequisitionRepository requisitionRepository;
    private final JobPostingRepository postingRepository;
    private final CandidateRepository candidateRepository;
    private final ApplicationRepository applicationRepository;
    private final DepartmentRepository departmentRepository;
    private final AuditService auditService;

    public RecruitmentService(
            JobRequisitionRepository requisitionRepository,
            JobPostingRepository postingRepository,
            CandidateRepository candidateRepository,
            ApplicationRepository applicationRepository,
            DepartmentRepository departmentRepository,
            AuditService auditService
    ) {
        this.requisitionRepository = requisitionRepository;
        this.postingRepository = postingRepository;
        this.candidateRepository = candidateRepository;
        this.applicationRepository = applicationRepository;
        this.departmentRepository = departmentRepository;
        this.auditService = auditService;
    }

    @Transactional
    public JobRequisitionResponse createRequisition(
            String title,
            UUID departmentId,
            String description,
            int numberOfOpenings,
            UUID requestedBy
    ) {
        if (!departmentRepository.existsById(departmentId)) {
            throw new DepartmentNotFoundException("Department not found: " + departmentId);
        }

        JobRequisition requisition = requisitionRepository.save(
                new JobRequisition(title, departmentId, description, numberOfOpenings, requestedBy)
        );

        auditService.log(
                "JOB_REQUISITION_CREATED", "JobRequisition", requisition.getId(), AuditService.RESULT_SUCCESS
        );

        return JobRequisitionResponse.from(requisition);
    }

    public Page<JobRequisitionResponse> getAllRequisitions(Pageable pageable) {
        return requisitionRepository.findAll(pageable).map(JobRequisitionResponse::from);
    }

    @Transactional
    public JobRequisitionResponse closeRequisition(UUID requisitionId) {
        JobRequisition requisition = findRequisitionOrThrow(requisitionId);
        requisition.close();
        return JobRequisitionResponse.from(requisition);
    }

    @Transactional
    public JobPostingResponse createPosting(
            UUID requisitionId,
            String title,
            String description,
            String location,
            String employmentType
    ) {
        JobRequisition requisition = findRequisitionOrThrow(requisitionId);

        if (!requisition.isOpen()) {
            throw new BusinessRuleException("Cannot post against a requisition that is not open");
        }

        JobPosting posting = postingRepository.save(
                new JobPosting(requisitionId, title, description, location, employmentType)
        );

        auditService.log("JOB_POSTING_CREATED", "JobPosting", posting.getId(), AuditService.RESULT_SUCCESS);

        return JobPostingResponse.from(posting);
    }

    public List<JobPostingResponse> getOpenPostings() {
        return postingRepository.findByStatus(JobPosting.STATUS_OPEN).stream()
                .map(JobPostingResponse::from)
                .toList();
    }

    @Transactional
    public JobPostingResponse closePosting(UUID postingId) {
        JobPosting posting = findPostingOrThrow(postingId);
        posting.close();
        return JobPostingResponse.from(posting);
    }

    @Transactional
    public CandidateResponse createCandidate(
            String firstName,
            String lastName,
            String email,
            String phone,
            UUID resumeDocumentId,
            String source
    ) {
        if (candidateRepository.existsByEmail(email)) {
            throw new DuplicateResourceException("A candidate with this email already exists: " + email);
        }

        Candidate candidate = candidateRepository.save(
                new Candidate(firstName, lastName, email, phone, resumeDocumentId, source)
        );

        auditService.log("CANDIDATE_CREATED", "Candidate", candidate.getId(), AuditService.RESULT_SUCCESS);

        return CandidateResponse.from(candidate);
    }

    public CandidateResponse getCandidate(UUID candidateId) {
        return CandidateResponse.from(findCandidateOrThrow(candidateId));
    }

    @Transactional
    public ApplicationResponse createApplication(UUID candidateId, UUID jobPostingId) {

        if (!candidateRepository.existsById(candidateId)) {
            throw new RecruitmentNotFoundException("Candidate not found: " + candidateId);
        }

        JobPosting posting = findPostingOrThrow(jobPostingId);

        if (!posting.isOpen()) {
            throw new BusinessRuleException("Cannot apply to a job posting that is closed");
        }

        if (applicationRepository.existsByCandidateIdAndJobPostingId(candidateId, jobPostingId)) {
            throw new BusinessRuleException("This candidate has already applied to this job posting");
        }

        Application application = applicationRepository.save(new Application(candidateId, jobPostingId));

        auditService.log("APPLICATION_CREATED", "Application", application.getId(), AuditService.RESULT_SUCCESS);

        return ApplicationResponse.from(application);
    }

    public ApplicationResponse getApplication(UUID applicationId) {
        return ApplicationResponse.from(findApplicationOrThrow(applicationId));
    }

    public List<ApplicationResponse> getApplicationsForPosting(UUID jobPostingId) {
        return applicationRepository.findByJobPostingIdOrderByAppliedAtAsc(jobPostingId).stream()
                .map(ApplicationResponse::from)
                .toList();
    }

    public List<ApplicationResponse> getApplicationsForCandidate(UUID candidateId) {
        return applicationRepository.findByCandidateId(candidateId).stream()
                .map(ApplicationResponse::from)
                .toList();
    }

    @Transactional
    public ApplicationResponse advanceApplication(UUID applicationId, String targetStatus) {

        Application application = findApplicationOrThrow(applicationId);
        assertApplicationActive(application);

        switch (targetStatus) {
            case Application.STATUS_SCREENING -> application.advanceToScreening();
            case Application.STATUS_INTERVIEWING -> application.advanceToInterviewing();
            case Application.STATUS_OFFER -> application.advanceToOffer();
            default -> throw new BusinessRuleException("Unsupported pipeline stage: " + targetStatus);
        }

        auditService.log("APPLICATION_ADVANCED", "Application", applicationId, AuditService.RESULT_SUCCESS);

        return ApplicationResponse.from(application);
    }

    @Transactional
    public ApplicationResponse rejectApplication(UUID applicationId, String reason) {

        Application application = findApplicationOrThrow(applicationId);
        assertApplicationActive(application);

        application.reject(reason);

        auditService.log("APPLICATION_REJECTED", "Application", applicationId, AuditService.RESULT_SUCCESS);

        return ApplicationResponse.from(application);
    }

    @Transactional
    public ApplicationResponse withdrawApplication(UUID applicationId) {

        Application application = findApplicationOrThrow(applicationId);
        assertApplicationActive(application);

        application.withdraw();

        return ApplicationResponse.from(application);
    }

    private void assertApplicationActive(Application application) {
        if (!application.isActive()) {
            throw new BusinessRuleException("Application is no longer active: " + application.getStatus());
        }
    }

    private JobRequisition findRequisitionOrThrow(UUID requisitionId) {
        return requisitionRepository.findById(requisitionId)
                .orElseThrow(() -> new RecruitmentNotFoundException("Job requisition not found: " + requisitionId));
    }

    private JobPosting findPostingOrThrow(UUID postingId) {
        return postingRepository.findById(postingId)
                .orElseThrow(() -> new RecruitmentNotFoundException("Job posting not found: " + postingId));
    }

    private Candidate findCandidateOrThrow(UUID candidateId) {
        return candidateRepository.findById(candidateId)
                .orElseThrow(() -> new RecruitmentNotFoundException("Candidate not found: " + candidateId));
    }

    private Application findApplicationOrThrow(UUID applicationId) {
        return applicationRepository.findById(applicationId)
                .orElseThrow(() -> new RecruitmentNotFoundException("Application not found: " + applicationId));
    }
}
