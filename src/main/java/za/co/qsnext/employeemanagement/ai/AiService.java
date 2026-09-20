package za.co.qsnext.employeemanagement.ai;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import za.co.qsnext.employeemanagement.ai.dto.AiSuggestionResponse;
import za.co.qsnext.employeemanagement.analytics.AnalyticsService;
import za.co.qsnext.employeemanagement.audit.AuditService;
import za.co.qsnext.employeemanagement.document.Document;
import za.co.qsnext.employeemanagement.document.DocumentRepository;
import za.co.qsnext.employeemanagement.document.DocumentStorageService;
import za.co.qsnext.employeemanagement.employee.Employee;
import za.co.qsnext.employeemanagement.employee.EmployeeRepository;
import za.co.qsnext.employeemanagement.exception.AiNotFoundException;
import za.co.qsnext.employeemanagement.exception.BusinessRuleException;
import za.co.qsnext.employeemanagement.exception.DocumentNotFoundException;
import za.co.qsnext.employeemanagement.exception.EmployeeNotFoundException;
import za.co.qsnext.employeemanagement.exception.RecruitmentNotFoundException;
import za.co.qsnext.employeemanagement.integration.IntegrationConfig;
import za.co.qsnext.employeemanagement.integration.IntegrationConfigRepository;
import za.co.qsnext.employeemanagement.learning.Course;
import za.co.qsnext.employeemanagement.learning.CourseRepository;
import za.co.qsnext.employeemanagement.learning.EmployeeSkill;
import za.co.qsnext.employeemanagement.learning.EmployeeSkillRepository;
import za.co.qsnext.employeemanagement.learning.Skill;
import za.co.qsnext.employeemanagement.learning.SkillRepository;
import za.co.qsnext.employeemanagement.recruitment.Application;
import za.co.qsnext.employeemanagement.recruitment.ApplicationRepository;
import za.co.qsnext.employeemanagement.recruitment.Candidate;
import za.co.qsnext.employeemanagement.recruitment.CandidateRepository;
import za.co.qsnext.employeemanagement.recruitment.JobPosting;
import za.co.qsnext.employeemanagement.recruitment.JobPostingRepository;
import za.co.qsnext.employeemanagement.recruitment.JobRequisition;
import za.co.qsnext.employeemanagement.recruitment.JobRequisitionRepository;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * The one place every AI-powered HR capability in QSNext is implemented.
 * Every method here only ever reads domain data, calls {@link
 * AiProvider#complete}, and persists the result as an {@link
 * AiSuggestion} - never writes to Employee, Application, Offer or any
 * other domain entity. That is deliberate: per the project's AI policy,
 * AI must never automatically make a hiring, firing, promotion,
 * disciplinary or salary decision. Every capability here produces a
 * recommendation an authorized human reads and acts on themselves
 * through the normal (non-AI) module endpoints.
 */
@Service
@Transactional(readOnly = true)
public class AiService {

    private static final String HR_ASSISTANT_SYSTEM_PROMPT = """
            You are QSNext's HR assistant. Answer general HR questions and
            policy/process questions helpfully and concisely. You are not
            a lawyer and this is not legal advice - say so if a question
            requires one. Never state or imply a hiring, firing,
            promotion, disciplinary or salary decision - only a human
            with the appropriate authority makes those.""";

    private static final String DOCUMENT_SUMMARY_SYSTEM_PROMPT = """
            Summarize the given HR document in a few clear, neutral
            sentences a busy HR professional could skim. Do not invent
            facts that are not in the document.""";

    private static final String JOB_DESCRIPTION_SYSTEM_PROMPT = """
            Draft a clear, inclusive job posting description from the
            given job requisition details. This is a DRAFT for a human
            recruiter to review and edit before it is ever published -
            say so is not necessary in the text itself, just write the
            draft.""";

    private static final String CANDIDATE_MATCH_SYSTEM_PROMPT = """
            You are assisting a recruiter by summarizing how well each
            candidate's application appears to fit a job posting, based
            only on the information given. Produce a short ranked list
            with a one-line rationale per candidate. This is an
            advisory recommendation only - it is not a hiring decision,
            and the recruiter makes the actual decision.""";

    private static final String SKILLS_RECOMMENDATION_SYSTEM_PROMPT = """
            Given an employee's job title and current recorded skills,
            suggest a short list of additional skills that would be
            valuable for their role and career growth, with a brief
            reason for each. This is a development suggestion for the
            employee and their manager to discuss, not an evaluation.""";

    private static final String LEARNING_RECOMMENDATION_SYSTEM_PROMPT = """
            Given an employee's current skills and the available course
            catalog, recommend a short list of courses from that catalog
            that would help their development, with a brief reason for
            each. Only recommend courses that are actually in the
            catalog provided.""";

    private static final String ANALYTICS_EXPLANATION_SYSTEM_PROMPT = """
            Explain the given HR analytics figures in plain, neutral
            language a manager without a data background could
            understand. Describe what the numbers show; do not
            speculate about causes you cannot see in the data, and do
            not recommend personnel actions about any specific
            individual.""";

    private final AiProvider aiProvider;
    private final IntegrationConfigRepository integrationConfigRepository;
    private final AiSuggestionRepository aiSuggestionRepository;
    private final AuditService auditService;
    private final EmployeeRepository employeeRepository;
    private final DocumentRepository documentRepository;
    private final DocumentStorageService documentStorageService;
    private final JobRequisitionRepository jobRequisitionRepository;
    private final JobPostingRepository jobPostingRepository;
    private final ApplicationRepository applicationRepository;
    private final CandidateRepository candidateRepository;
    private final EmployeeSkillRepository employeeSkillRepository;
    private final SkillRepository skillRepository;
    private final CourseRepository courseRepository;
    private final AnalyticsService analyticsService;

    public AiService(
            AiProvider aiProvider,
            IntegrationConfigRepository integrationConfigRepository,
            AiSuggestionRepository aiSuggestionRepository,
            AuditService auditService,
            EmployeeRepository employeeRepository,
            DocumentRepository documentRepository,
            DocumentStorageService documentStorageService,
            JobRequisitionRepository jobRequisitionRepository,
            JobPostingRepository jobPostingRepository,
            ApplicationRepository applicationRepository,
            CandidateRepository candidateRepository,
            EmployeeSkillRepository employeeSkillRepository,
            SkillRepository skillRepository,
            CourseRepository courseRepository,
            AnalyticsService analyticsService
    ) {
        this.aiProvider = aiProvider;
        this.integrationConfigRepository = integrationConfigRepository;
        this.aiSuggestionRepository = aiSuggestionRepository;
        this.auditService = auditService;
        this.employeeRepository = employeeRepository;
        this.documentRepository = documentRepository;
        this.documentStorageService = documentStorageService;
        this.jobRequisitionRepository = jobRequisitionRepository;
        this.jobPostingRepository = jobPostingRepository;
        this.applicationRepository = applicationRepository;
        this.candidateRepository = candidateRepository;
        this.employeeSkillRepository = employeeSkillRepository;
        this.skillRepository = skillRepository;
        this.courseRepository = courseRepository;
        this.analyticsService = analyticsService;
    }

    @Transactional
    public AiSuggestionResponse askHrAssistant(String question, UUID requesterUserId) {
        return complete(
                AiSuggestion.TYPE_HR_ASSISTANT, null, null, requesterUserId,
                HR_ASSISTANT_SYSTEM_PROMPT, question);
    }

    @Transactional
    public AiSuggestionResponse summarizeDocument(UUID documentId, UUID requesterUserId) {

        Document document = documentRepository.findById(documentId)
                .orElseThrow(() -> new DocumentNotFoundException("Document not found: " + documentId));

        if (document.getContentType() == null || !document.getContentType().startsWith("text/")) {
            throw new BusinessRuleException(
                    "Only plain-text documents can be summarized today; " + document.getContentType()
                            + " is not supported");
        }

        byte[] content = documentStorageService.retrieve(document.getStorageKey());
        String text = new String(content, java.nio.charset.StandardCharsets.UTF_8);

        return complete(
                AiSuggestion.TYPE_DOCUMENT_SUMMARY, "Document", documentId, requesterUserId,
                DOCUMENT_SUMMARY_SYSTEM_PROMPT, text);
    }

    @Transactional
    public AiSuggestionResponse generateJobDescription(UUID jobRequisitionId, UUID requesterUserId) {

        JobRequisition requisition = jobRequisitionRepository.findById(jobRequisitionId)
                .orElseThrow(() -> new RecruitmentNotFoundException(
                        "Job requisition not found: " + jobRequisitionId));

        String prompt = "Job title: %s\nNumber of openings: %d\nExisting notes: %s".formatted(
                requisition.getTitle(), requisition.getNumberOfOpenings(),
                requisition.getDescription() == null ? "(none)" : requisition.getDescription());

        return complete(
                AiSuggestion.TYPE_JOB_DESCRIPTION, "JobRequisition", jobRequisitionId, requesterUserId,
                JOB_DESCRIPTION_SYSTEM_PROMPT, prompt);
    }

    @Transactional
    public AiSuggestionResponse matchCandidates(UUID jobPostingId, UUID requesterUserId) {

        JobPosting jobPosting = jobPostingRepository.findById(jobPostingId)
                .orElseThrow(() -> new RecruitmentNotFoundException("Job posting not found: " + jobPostingId));

        List<Application> applications = applicationRepository.findByJobPostingIdOrderByAppliedAtAsc(jobPostingId);

        if (applications.isEmpty()) {
            throw new BusinessRuleException("Job posting has no applications to match: " + jobPostingId);
        }

        List<Candidate> candidates = candidateRepository.findAllById(
                applications.stream().map(Application::getCandidateId).toList());

        String candidateLines = applications.stream()
                .map(application -> {
                    Candidate candidate = candidates.stream()
                            .filter(c -> c.getId().equals(application.getCandidateId()))
                            .findFirst()
                            .orElse(null);
                    String name = candidate == null ? "Unknown candidate" : candidate.getFullName();
                    return "- %s (application status: %s)".formatted(name, application.getStatus());
                })
                .collect(Collectors.joining("\n"));

        String prompt = "Job posting: %s\n%s\n\nCandidates:\n%s".formatted(
                jobPosting.getTitle(),
                jobPosting.getDescription() == null ? "" : jobPosting.getDescription(),
                candidateLines);

        return complete(
                AiSuggestion.TYPE_CANDIDATE_MATCH, "JobPosting", jobPostingId, requesterUserId,
                CANDIDATE_MATCH_SYSTEM_PROMPT, prompt);
    }

    @Transactional
    public AiSuggestionResponse recommendSkills(UUID employeeId, UUID requesterUserId, boolean requesterCanManage) {

        Employee employee = findEmployeeOrThrow(employeeId);
        assertCanAccessEmployee(employee, requesterUserId, requesterCanManage);

        String skillNames = resolveSkillNames(employeeSkillRepository.findByEmployeeId(employeeId));

        String prompt = "Job title: %s\nCurrent skills: %s".formatted(
                employee.getJobTitle(), skillNames.isBlank() ? "(none recorded)" : skillNames);

        return complete(
                AiSuggestion.TYPE_SKILLS_RECOMMENDATION, "Employee", employeeId, requesterUserId,
                SKILLS_RECOMMENDATION_SYSTEM_PROMPT, prompt);
    }

    @Transactional
    public AiSuggestionResponse recommendLearning(UUID employeeId, UUID requesterUserId, boolean requesterCanManage) {

        Employee employee = findEmployeeOrThrow(employeeId);
        assertCanAccessEmployee(employee, requesterUserId, requesterCanManage);

        String skillNames = resolveSkillNames(employeeSkillRepository.findByEmployeeId(employeeId));

        List<Course> activeCourses = courseRepository.findByActiveTrue();
        String courseCatalog = activeCourses.stream()
                .map(course -> "- %s (%s)".formatted(course.getTitle(), course.getCategory()))
                .collect(Collectors.joining("\n"));

        String prompt = "Job title: %s\nCurrent skills: %s\n\nAvailable courses:\n%s".formatted(
                employee.getJobTitle(), skillNames.isBlank() ? "(none recorded)" : skillNames,
                courseCatalog.isBlank() ? "(no active courses)" : courseCatalog);

        return complete(
                AiSuggestion.TYPE_LEARNING_RECOMMENDATION, "Employee", employeeId, requesterUserId,
                LEARNING_RECOMMENDATION_SYSTEM_PROMPT, prompt);
    }

    @Transactional
    public AiSuggestionResponse explainHeadcountAnalytics(UUID requesterUserId) {
        String prompt = "Headcount analytics: " + analyticsService.getHeadcountAnalytics();
        return complete(
                AiSuggestion.TYPE_ANALYTICS_EXPLANATION, "Analytics", null, requesterUserId,
                ANALYTICS_EXPLANATION_SYSTEM_PROMPT, prompt);
    }

    public AiSuggestionResponse getSuggestion(UUID suggestionId, UUID requesterUserId, boolean requesterCanManage) {

        AiSuggestion suggestion = aiSuggestionRepository.findById(suggestionId)
                .orElseThrow(() -> new AiNotFoundException("AI suggestion not found: " + suggestionId));

        if (!requesterCanManage && !suggestion.getRequestedByUserId().equals(requesterUserId)) {
            throw new AccessDeniedException("You do not have permission to view this AI suggestion");
        }

        return AiSuggestionResponse.from(suggestion);
    }

    public Page<AiSuggestionResponse> getMySuggestions(UUID requesterUserId, Pageable pageable) {
        return aiSuggestionRepository.findByRequestedByUserIdOrderByCreatedAtDesc(requesterUserId, pageable)
                .map(AiSuggestionResponse::from);
    }

    private AiSuggestionResponse complete(
            String type, String subjectType, UUID subjectId, UUID requesterUserId,
            String systemPrompt, String userPrompt
    ) {
        if (!isAiEnabled()) {
            throw new BusinessRuleException(
                    "AI capabilities are not enabled. An administrator must enable the AI integration first.");
        }

        String responseText = aiProvider.complete(systemPrompt, userPrompt);

        AiSuggestion suggestion = new AiSuggestion(
                type, subjectType, subjectId, requesterUserId, aiProvider.providerName(), userPrompt, responseText);
        AiSuggestion saved = aiSuggestionRepository.save(suggestion);

        auditService.log(requesterUserId, "AI_SUGGESTION_GENERATED:" + type, "AiSuggestion", saved.getId(),
                AuditService.RESULT_SUCCESS);

        return AiSuggestionResponse.from(saved);
    }

    private boolean isAiEnabled() {
        return integrationConfigRepository.findByType(IntegrationConfig.TYPE_AI)
                .map(IntegrationConfig::isEnabled)
                .orElse(false);
    }

    private String resolveSkillNames(List<EmployeeSkill> employeeSkills) {

        if (employeeSkills.isEmpty()) {
            return "";
        }

        Map<UUID, String> skillNamesById = skillRepository
                .findAllById(employeeSkills.stream().map(EmployeeSkill::getSkillId).toList())
                .stream()
                .collect(Collectors.toMap(Skill::getId, Skill::getName));

        return employeeSkills.stream()
                .map(es -> skillNamesById.getOrDefault(es.getSkillId(), "Unknown skill"))
                .collect(Collectors.joining(", "));
    }

    private Employee findEmployeeOrThrow(UUID employeeId) {
        return employeeRepository.findById(employeeId)
                .orElseThrow(() -> new EmployeeNotFoundException("Employee not found: " + employeeId));
    }

    private void assertCanAccessEmployee(Employee employee, UUID requesterUserId, boolean requesterCanManage) {
        if (requesterCanManage) {
            return;
        }

        if (!employee.getUserId().equals(requesterUserId)) {
            throw new AccessDeniedException("You do not have permission to request AI recommendations for this employee");
        }
    }
}
