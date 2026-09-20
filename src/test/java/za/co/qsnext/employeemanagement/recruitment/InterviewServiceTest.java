package za.co.qsnext.employeemanagement.recruitment;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import org.springframework.security.access.AccessDeniedException;

import za.co.qsnext.employeemanagement.audit.AuditService;
import za.co.qsnext.employeemanagement.calendar.CalendarEvent;
import za.co.qsnext.employeemanagement.calendar.CalendarService;
import za.co.qsnext.employeemanagement.email.EmailService;
import za.co.qsnext.employeemanagement.email.EmailTemplate;
import za.co.qsnext.employeemanagement.exception.BusinessRuleException;
import za.co.qsnext.employeemanagement.notification.NotificationPublisher;
import za.co.qsnext.employeemanagement.notification.NotificationType;
import za.co.qsnext.employeemanagement.recruitment.dto.InterviewFeedbackResponse;
import za.co.qsnext.employeemanagement.recruitment.dto.InterviewResponse;

import java.lang.reflect.Field;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class InterviewServiceTest {

    @Mock
    private InterviewRepository interviewRepository;
    @Mock
    private InterviewFeedbackRepository feedbackRepository;
    @Mock
    private ApplicationRepository applicationRepository;
    @Mock
    private CandidateRepository candidateRepository;
    @Mock
    private JobPostingRepository postingRepository;
    @Mock
    private CalendarService calendarService;
    @Mock
    private EmailService emailService;
    @Mock
    private NotificationPublisher notificationPublisher;
    @Mock
    private AuditService auditService;

    private InterviewService interviewService;

    @BeforeEach
    void setUp() {
        interviewService = new InterviewService(
                interviewRepository, feedbackRepository, applicationRepository, candidateRepository,
                postingRepository, calendarService, emailService, notificationPublisher, auditService);
    }

    private Application applicationWithId(UUID id, UUID candidateId, UUID postingId) {
        Application application = new Application(candidateId, postingId);
        setId(application, id);
        return application;
    }

    private Candidate candidateWithId(UUID id) {
        Candidate candidate = new Candidate("Jane", "Doe", "jane@example.com", "0123456789", null, "referral");
        setId(candidate, id);
        return candidate;
    }

    private JobPosting postingWithId(UUID id) {
        JobPosting posting = new JobPosting(UUID.randomUUID(), "Backend Engineer", "Desc", "Remote", "FULL_TIME");
        setId(posting, id);
        return posting;
    }

    private Interview interviewWithId(UUID id, UUID applicationId, UUID interviewerUserId) {
        Interview interview = new Interview(
                applicationId, interviewerUserId, OffsetDateTime.now().plusDays(1), 60, "Zoom");
        setId(interview, id);
        return interview;
    }

    @Test
    void scheduleInterview_savesInterviewAndTriggersIntegrations() {
        UUID applicationId = UUID.randomUUID();
        UUID candidateId = UUID.randomUUID();
        UUID postingId = UUID.randomUUID();
        UUID interviewerUserId = UUID.randomUUID();

        Application application = applicationWithId(applicationId, candidateId, postingId);

        when(applicationRepository.findById(applicationId)).thenReturn(Optional.of(application));
        when(candidateRepository.findById(candidateId)).thenReturn(Optional.of(candidateWithId(candidateId)));
        when(postingRepository.findById(postingId)).thenReturn(Optional.of(postingWithId(postingId)));
        when(interviewRepository.save(any())).thenAnswer(invocation -> {
            Interview interview = invocation.getArgument(0);
            setId(interview, UUID.randomUUID());
            return interview;
        });

        OffsetDateTime scheduledAt = OffsetDateTime.now().plusDays(2);

        InterviewResponse response = interviewService.scheduleInterview(
                applicationId, interviewerUserId, scheduledAt, 45, "Room 1");

        assertThat(response.status()).isEqualTo(Interview.STATUS_SCHEDULED);
        assertThat(application.getStatus()).isEqualTo(Application.STATUS_INTERVIEWING);
        verify(calendarService).recordSystemEvent(
                any(), eq(CalendarEvent.TYPE_INTERVIEW), eq(scheduledAt), eq(scheduledAt.plusMinutes(45)),
                eq(interviewerUserId), isNull());
        verify(notificationPublisher).publish(
                eq(interviewerUserId), eq(NotificationType.INTERVIEW_ASSIGNED), any(), any());
        verify(emailService).queueEmail(eq(EmailTemplate.INTERVIEW_INVITATION), eq("jane@example.com"), any());
    }

    @Test
    void scheduleInterview_throws_whenApplicationIsInactive() {
        UUID applicationId = UUID.randomUUID();
        Application application = applicationWithId(applicationId, UUID.randomUUID(), UUID.randomUUID());
        application.withdraw();

        when(applicationRepository.findById(applicationId)).thenReturn(Optional.of(application));

        assertThatThrownBy(() -> interviewService.scheduleInterview(
                applicationId, UUID.randomUUID(), OffsetDateTime.now().plusDays(1), 30, null))
                .isInstanceOf(BusinessRuleException.class);
    }

    @Test
    void completeInterview_isDenied_forSomeoneOtherThanTheInterviewer() {
        UUID interviewId = UUID.randomUUID();
        Interview interview = interviewWithId(interviewId, UUID.randomUUID(), UUID.randomUUID());

        when(interviewRepository.findById(interviewId)).thenReturn(Optional.of(interview));

        assertThatThrownBy(() -> interviewService.completeInterview(interviewId, UUID.randomUUID(), false))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void completeInterview_isAllowed_fortheAssignedInterviewer() {
        UUID interviewId = UUID.randomUUID();
        UUID interviewerUserId = UUID.randomUUID();
        Interview interview = interviewWithId(interviewId, UUID.randomUUID(), interviewerUserId);

        when(interviewRepository.findById(interviewId)).thenReturn(Optional.of(interview));

        InterviewResponse response = interviewService.completeInterview(interviewId, interviewerUserId, false);

        assertThat(response.status()).isEqualTo(Interview.STATUS_COMPLETED);
    }

    @Test
    void submitFeedback_savesFeedback() {
        UUID interviewId = UUID.randomUUID();
        UUID interviewerUserId = UUID.randomUUID();
        Interview interview = interviewWithId(interviewId, UUID.randomUUID(), interviewerUserId);

        when(interviewRepository.findById(interviewId)).thenReturn(Optional.of(interview));
        when(feedbackRepository.existsByInterviewId(interviewId)).thenReturn(false);
        when(feedbackRepository.save(any())).thenAnswer(invocation -> {
            InterviewFeedback feedback = invocation.getArgument(0);
            setId(feedback, UUID.randomUUID());
            return feedback;
        });

        InterviewFeedbackResponse response = interviewService.submitFeedback(
                interviewId, interviewerUserId, false, 4, "Strong candidate", InterviewFeedback.RECOMMENDATION_HIRE);

        assertThat(response.recommendation()).isEqualTo(InterviewFeedback.RECOMMENDATION_HIRE);
    }

    @Test
    void submitFeedback_rejectsADuplicateSubmission() {
        UUID interviewId = UUID.randomUUID();
        UUID interviewerUserId = UUID.randomUUID();
        Interview interview = interviewWithId(interviewId, UUID.randomUUID(), interviewerUserId);

        when(interviewRepository.findById(interviewId)).thenReturn(Optional.of(interview));
        when(feedbackRepository.existsByInterviewId(interviewId)).thenReturn(true);

        assertThatThrownBy(() -> interviewService.submitFeedback(
                interviewId, interviewerUserId, false, 4, "Comments", InterviewFeedback.RECOMMENDATION_HIRE))
                .isInstanceOf(BusinessRuleException.class);
    }

    @Test
    void getFeedbackForApplication_aggregatesAcrossAllInterviews() {
        UUID applicationId = UUID.randomUUID();
        UUID interviewId1 = UUID.randomUUID();
        UUID interviewId2 = UUID.randomUUID();

        Interview interview1 = interviewWithId(interviewId1, applicationId, UUID.randomUUID());
        Interview interview2 = interviewWithId(interviewId2, applicationId, UUID.randomUUID());

        InterviewFeedback feedback1 = new InterviewFeedback(
                interviewId1, interview1.getInterviewerUserId(), 4, "Good", InterviewFeedback.RECOMMENDATION_HIRE);
        setId(feedback1, UUID.randomUUID());

        when(interviewRepository.findByApplicationId(applicationId)).thenReturn(List.of(interview1, interview2));
        when(feedbackRepository.findByInterviewId(interviewId1)).thenReturn(Optional.of(feedback1));
        when(feedbackRepository.findByInterviewId(interviewId2)).thenReturn(Optional.empty());

        List<InterviewFeedbackResponse> results = interviewService.getFeedbackForApplication(applicationId);

        assertThat(results).hasSize(1);
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
