package za.co.qsnext.employeemanagement.ai;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import org.springframework.security.access.AccessDeniedException;

import za.co.qsnext.employeemanagement.ai.dto.AiSuggestionResponse;
import za.co.qsnext.employeemanagement.analytics.AnalyticsService;
import za.co.qsnext.employeemanagement.analytics.dto.HeadcountAnalyticsResponse;
import za.co.qsnext.employeemanagement.audit.AuditService;
import za.co.qsnext.employeemanagement.document.Document;
import za.co.qsnext.employeemanagement.document.DocumentRepository;
import za.co.qsnext.employeemanagement.document.DocumentStorageService;
import za.co.qsnext.employeemanagement.employee.Employee;
import za.co.qsnext.employeemanagement.employee.EmployeeRepository;
import za.co.qsnext.employeemanagement.exception.AiNotFoundException;
import za.co.qsnext.employeemanagement.exception.BusinessRuleException;
import za.co.qsnext.employeemanagement.exception.DocumentNotFoundException;
import za.co.qsnext.employeemanagement.integration.IntegrationConfig;
import za.co.qsnext.employeemanagement.integration.IntegrationConfigRepository;
import za.co.qsnext.employeemanagement.learning.CourseRepository;
import za.co.qsnext.employeemanagement.learning.EmployeeSkillRepository;
import za.co.qsnext.employeemanagement.learning.SkillRepository;
import za.co.qsnext.employeemanagement.recruitment.Application;
import za.co.qsnext.employeemanagement.recruitment.ApplicationRepository;
import za.co.qsnext.employeemanagement.recruitment.Candidate;
import za.co.qsnext.employeemanagement.recruitment.CandidateRepository;
import za.co.qsnext.employeemanagement.recruitment.JobPosting;
import za.co.qsnext.employeemanagement.recruitment.JobPostingRepository;
import za.co.qsnext.employeemanagement.recruitment.JobRequisition;
import za.co.qsnext.employeemanagement.recruitment.JobRequisitionRepository;

import java.lang.reflect.Field;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AiServiceTest {

    @Mock
    private AiProvider aiProvider;
    @Mock
    private IntegrationConfigRepository integrationConfigRepository;
    @Mock
    private AiSuggestionRepository aiSuggestionRepository;
    @Mock
    private AuditService auditService;
    @Mock
    private EmployeeRepository employeeRepository;
    @Mock
    private DocumentRepository documentRepository;
    @Mock
    private DocumentStorageService documentStorageService;
    @Mock
    private JobRequisitionRepository jobRequisitionRepository;
    @Mock
    private JobPostingRepository jobPostingRepository;
    @Mock
    private ApplicationRepository applicationRepository;
    @Mock
    private CandidateRepository candidateRepository;
    @Mock
    private EmployeeSkillRepository employeeSkillRepository;
    @Mock
    private SkillRepository skillRepository;
    @Mock
    private CourseRepository courseRepository;
    @Mock
    private AnalyticsService analyticsService;

    private AiService aiService;

    @BeforeEach
    void setUp() {
        aiService = new AiService(
                aiProvider, integrationConfigRepository, aiSuggestionRepository, auditService,
                employeeRepository, documentRepository, documentStorageService,
                jobRequisitionRepository, jobPostingRepository, applicationRepository, candidateRepository,
                employeeSkillRepository, skillRepository, courseRepository, analyticsService);
    }

    private void enableAi() {
        IntegrationConfig config = new IntegrationConfig(IntegrationConfig.TYPE_AI, "anthropic", true);
        when(integrationConfigRepository.findByType(IntegrationConfig.TYPE_AI))
                .thenReturn(Optional.of(config));
    }

    private Employee employee(UUID id, UUID userId) {
        Employee employee = new Employee(
                userId, UUID.randomUUID(), "EMP-1", "Jane", "Doe",
                "0123456789", "Engineer", LocalDate.of(2020, 1, 1));
        setId(employee, id);
        return employee;
    }

    @Test
    void askHrAssistant_throws_whenAiIsNotEnabled() {
        when(integrationConfigRepository.findByType(IntegrationConfig.TYPE_AI)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> aiService.askHrAssistant("How many leave days do I get?", UUID.randomUUID()))
                .isInstanceOf(BusinessRuleException.class);
    }

    @Test
    void askHrAssistant_persistsAndReturnsASuggestion_whenAiIsEnabled() {
        enableAi();
        when(aiProvider.providerName()).thenReturn("anthropic");
        when(aiProvider.complete(anyString(), anyString())).thenReturn("You get 21 days of annual leave.");
        when(aiSuggestionRepository.save(any(AiSuggestion.class))).thenAnswer(invocation -> {
            AiSuggestion suggestion = invocation.getArgument(0);
            setId(suggestion, UUID.randomUUID());
            return suggestion;
        });

        UUID requesterId = UUID.randomUUID();
        AiSuggestionResponse response = aiService.askHrAssistant("How much leave do I get?", requesterId);

        assertThat(response.type()).isEqualTo(AiSuggestion.TYPE_HR_ASSISTANT);
        assertThat(response.responseText()).isEqualTo("You get 21 days of annual leave.");
        assertThat(response.providerName()).isEqualTo("anthropic");
    }

    @Test
    void summarizeDocument_throws_whenDocumentDoesNotExist() {
        UUID documentId = UUID.randomUUID();
        when(documentRepository.findById(documentId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> aiService.summarizeDocument(documentId, UUID.randomUUID()))
                .isInstanceOf(DocumentNotFoundException.class);
    }

    @Test
    void summarizeDocument_rejectsNonTextDocuments() {
        UUID documentId = UUID.randomUUID();
        Document document = new Document(
                UUID.randomUUID(), "POLICY", "Handbook", "desc", UUID.randomUUID(), 1,
                "key", "handbook.pdf", "application/pdf", 1024, null, UUID.randomUUID());
        setId(document, documentId);

        when(documentRepository.findById(documentId)).thenReturn(Optional.of(document));

        assertThatThrownBy(() -> aiService.summarizeDocument(documentId, UUID.randomUUID()))
                .isInstanceOf(BusinessRuleException.class);
    }

    @Test
    void summarizeDocument_summarizesTextDocuments() {
        enableAi();
        UUID documentId = UUID.randomUUID();
        Document document = new Document(
                UUID.randomUUID(), "POLICY", "Notice", "desc", UUID.randomUUID(), 1,
                "key", "notice.txt", "text/plain", 100, null, UUID.randomUUID());
        setId(document, documentId);

        when(documentRepository.findById(documentId)).thenReturn(Optional.of(document));
        when(documentStorageService.retrieve("key")).thenReturn("Office closes at 5pm.".getBytes());
        when(aiProvider.providerName()).thenReturn("anthropic");
        when(aiProvider.complete(anyString(), anyString())).thenReturn("The office closes at 5pm.");
        when(aiSuggestionRepository.save(any(AiSuggestion.class))).thenAnswer(invocation -> {
            AiSuggestion suggestion = invocation.getArgument(0);
            setId(suggestion, UUID.randomUUID());
            return suggestion;
        });

        AiSuggestionResponse response = aiService.summarizeDocument(documentId, UUID.randomUUID());

        assertThat(response.type()).isEqualTo(AiSuggestion.TYPE_DOCUMENT_SUMMARY);
        assertThat(response.subjectId()).isEqualTo(documentId);
    }

    @Test
    void generateJobDescription_throws_whenRequisitionDoesNotExist() {
        UUID requisitionId = UUID.randomUUID();
        when(jobRequisitionRepository.findById(requisitionId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> aiService.generateJobDescription(requisitionId, UUID.randomUUID()))
                .isInstanceOf(za.co.qsnext.employeemanagement.exception.RecruitmentNotFoundException.class);
    }

    @Test
    void matchCandidates_throws_whenThereAreNoApplications() {
        UUID jobPostingId = UUID.randomUUID();
        JobPosting jobPosting = new JobPosting(UUID.randomUUID(), "Engineer", "desc", "Remote", "FULL_TIME");
        setId(jobPosting, jobPostingId);

        when(jobPostingRepository.findById(jobPostingId)).thenReturn(Optional.of(jobPosting));
        when(applicationRepository.findByJobPostingIdOrderByAppliedAtAsc(jobPostingId)).thenReturn(List.of());

        assertThatThrownBy(() -> aiService.matchCandidates(jobPostingId, UUID.randomUUID()))
                .isInstanceOf(BusinessRuleException.class);
    }

    @Test
    void matchCandidates_summarizesApplicantsAgainstThePosting() {
        enableAi();
        UUID jobPostingId = UUID.randomUUID();
        JobPosting jobPosting = new JobPosting(UUID.randomUUID(), "Engineer", "desc", "Remote", "FULL_TIME");
        setId(jobPosting, jobPostingId);

        Candidate candidate = new Candidate("John", "Smith", "john@example.com", "0123456789", null, "referral");
        UUID candidateId = UUID.randomUUID();
        setId(candidate, candidateId);

        Application application = new Application(candidateId, jobPostingId);

        when(jobPostingRepository.findById(jobPostingId)).thenReturn(Optional.of(jobPosting));
        when(applicationRepository.findByJobPostingIdOrderByAppliedAtAsc(jobPostingId))
                .thenReturn(List.of(application));
        when(candidateRepository.findAllById(List.of(candidateId))).thenReturn(List.of(candidate));
        when(aiProvider.providerName()).thenReturn("anthropic");
        when(aiProvider.complete(anyString(), anyString())).thenReturn("John Smith looks like a strong fit.");
        when(aiSuggestionRepository.save(any(AiSuggestion.class))).thenAnswer(invocation -> {
            AiSuggestion suggestion = invocation.getArgument(0);
            setId(suggestion, UUID.randomUUID());
            return suggestion;
        });

        AiSuggestionResponse response = aiService.matchCandidates(jobPostingId, UUID.randomUUID());

        assertThat(response.type()).isEqualTo(AiSuggestion.TYPE_CANDIDATE_MATCH);
    }

    @Test
    void recommendSkills_isDenied_forANonOwnerWithoutManageAuthority() {
        UUID employeeId = UUID.randomUUID();
        when(employeeRepository.findById(employeeId))
                .thenReturn(Optional.of(employee(employeeId, UUID.randomUUID())));

        assertThatThrownBy(() -> aiService.recommendSkills(employeeId, UUID.randomUUID(), false))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void recommendSkills_isAllowedForTheEmployeesOwnRecord() {
        enableAi();
        UUID employeeId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        when(employeeRepository.findById(employeeId)).thenReturn(Optional.of(employee(employeeId, userId)));
        when(employeeSkillRepository.findByEmployeeId(employeeId)).thenReturn(List.of());
        when(aiProvider.providerName()).thenReturn("anthropic");
        when(aiProvider.complete(anyString(), anyString())).thenReturn("Consider learning Kubernetes.");
        when(aiSuggestionRepository.save(any(AiSuggestion.class))).thenAnswer(invocation -> {
            AiSuggestion suggestion = invocation.getArgument(0);
            setId(suggestion, UUID.randomUUID());
            return suggestion;
        });

        AiSuggestionResponse response = aiService.recommendSkills(employeeId, userId, false);

        assertThat(response.type()).isEqualTo(AiSuggestion.TYPE_SKILLS_RECOMMENDATION);
    }

    @Test
    void recommendLearning_usesTheActiveCourseCatalog() {
        enableAi();
        UUID employeeId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        when(employeeRepository.findById(employeeId)).thenReturn(Optional.of(employee(employeeId, userId)));
        when(employeeSkillRepository.findByEmployeeId(employeeId)).thenReturn(List.of());
        when(courseRepository.findByActiveTrue()).thenReturn(List.of());
        when(aiProvider.providerName()).thenReturn("anthropic");
        when(aiProvider.complete(anyString(), anyString())).thenReturn("No active courses to recommend yet.");
        when(aiSuggestionRepository.save(any(AiSuggestion.class))).thenAnswer(invocation -> {
            AiSuggestion suggestion = invocation.getArgument(0);
            setId(suggestion, UUID.randomUUID());
            return suggestion;
        });

        AiSuggestionResponse response = aiService.recommendLearning(employeeId, userId, true);

        assertThat(response.type()).isEqualTo(AiSuggestion.TYPE_LEARNING_RECOMMENDATION);
    }

    @Test
    void explainHeadcountAnalytics_wrapsTheAnalyticsServiceResult() {
        enableAi();
        when(analyticsService.getHeadcountAnalytics())
                .thenReturn(new HeadcountAnalyticsResponse(42, List.of()));
        when(aiProvider.providerName()).thenReturn("anthropic");
        when(aiProvider.complete(anyString(), anyString())).thenReturn("Headcount is 42 employees.");
        when(aiSuggestionRepository.save(any(AiSuggestion.class))).thenAnswer(invocation -> {
            AiSuggestion suggestion = invocation.getArgument(0);
            setId(suggestion, UUID.randomUUID());
            return suggestion;
        });

        AiSuggestionResponse response = aiService.explainHeadcountAnalytics(UUID.randomUUID());

        assertThat(response.type()).isEqualTo(AiSuggestion.TYPE_ANALYTICS_EXPLANATION);
        assertThat(response.prompt()).contains("42");
    }

    @Test
    void getSuggestion_throws_whenSuggestionDoesNotExist() {
        UUID suggestionId = UUID.randomUUID();
        when(aiSuggestionRepository.findById(suggestionId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> aiService.getSuggestion(suggestionId, UUID.randomUUID(), false))
                .isInstanceOf(AiNotFoundException.class);
    }

    @Test
    void getSuggestion_isDenied_forANonOwnerWithoutManageAuthority() {
        UUID suggestionId = UUID.randomUUID();
        UUID ownerId = UUID.randomUUID();
        AiSuggestion suggestion = new AiSuggestion(
                AiSuggestion.TYPE_HR_ASSISTANT, null, null, ownerId, "anthropic", "q", "a");
        setId(suggestion, suggestionId);

        when(aiSuggestionRepository.findById(suggestionId)).thenReturn(Optional.of(suggestion));

        assertThatThrownBy(() -> aiService.getSuggestion(suggestionId, UUID.randomUUID(), false))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void getSuggestion_isAllowed_fortheOwner() {
        UUID suggestionId = UUID.randomUUID();
        UUID ownerId = UUID.randomUUID();
        AiSuggestion suggestion = new AiSuggestion(
                AiSuggestion.TYPE_HR_ASSISTANT, null, null, ownerId, "anthropic", "q", "a");
        setId(suggestion, suggestionId);

        when(aiSuggestionRepository.findById(suggestionId)).thenReturn(Optional.of(suggestion));

        AiSuggestionResponse response = aiService.getSuggestion(suggestionId, ownerId, false);

        assertThat(response.id()).isEqualTo(suggestionId);
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
