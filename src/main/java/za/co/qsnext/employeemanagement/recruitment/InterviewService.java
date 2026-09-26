package za.co.qsnext.employeemanagement.recruitment;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import za.co.qsnext.employeemanagement.audit.AuditService;
import za.co.qsnext.employeemanagement.calendar.CalendarEvent;
import za.co.qsnext.employeemanagement.calendar.CalendarService;
import za.co.qsnext.employeemanagement.email.EmailService;
import za.co.qsnext.employeemanagement.email.EmailTemplate;
import za.co.qsnext.employeemanagement.exception.BusinessRuleException;
import za.co.qsnext.employeemanagement.exception.RecruitmentNotFoundException;
import za.co.qsnext.employeemanagement.user.UserService;
import za.co.qsnext.employeemanagement.notification.NotificationPublisher;
import za.co.qsnext.employeemanagement.notification.NotificationType;
import za.co.qsnext.employeemanagement.recruitment.dto.InterviewFeedbackResponse;
import za.co.qsnext.employeemanagement.recruitment.dto.InterviewResponse;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class InterviewService {

    private static final String INTERVIEWER_ROLE = "INTERVIEWER";

    private final InterviewRepository interviewRepository;
    private final InterviewFeedbackRepository feedbackRepository;
    private final ApplicationRepository applicationRepository;
    private final CandidateRepository candidateRepository;
    private final JobPostingRepository postingRepository;
    private final CalendarService calendarService;
    private final EmailService emailService;
    private final NotificationPublisher notificationPublisher;
    private final AuditService auditService;
    private final UserService userService;

    public InterviewService(
            InterviewRepository interviewRepository,
            InterviewFeedbackRepository feedbackRepository,
            ApplicationRepository applicationRepository,
            CandidateRepository candidateRepository,
            JobPostingRepository postingRepository,
            CalendarService calendarService,
            EmailService emailService,
            NotificationPublisher notificationPublisher,
            AuditService auditService,
            UserService userService
    ) {
        this.interviewRepository = interviewRepository;
        this.feedbackRepository = feedbackRepository;
        this.applicationRepository = applicationRepository;
        this.candidateRepository = candidateRepository;
        this.postingRepository = postingRepository;
        this.calendarService = calendarService;
        this.emailService = emailService;
        this.notificationPublisher = notificationPublisher;
        this.auditService = auditService;
        this.userService = userService;
    }

    @Transactional
    public InterviewResponse scheduleInterview(
            UUID applicationId,
            UUID interviewerUserId,
            OffsetDateTime scheduledAt,
            int durationMinutes,
            String location
    ) {
        Application application = applicationRepository.findById(applicationId)
                .orElseThrow(() -> new RecruitmentNotFoundException("Application not found: " + applicationId));

        if (!application.isActive()) {
            throw new BusinessRuleException("Cannot schedule an interview for an inactive application");
        }

        Candidate candidate = candidateRepository.findById(application.getCandidateId())
                .orElseThrow(() -> new RecruitmentNotFoundException(
                        "Candidate not found: " + application.getCandidateId()
                ));

        JobPosting posting = postingRepository.findById(application.getJobPostingId())
                .orElseThrow(() -> new RecruitmentNotFoundException(
                        "Job posting not found: " + application.getJobPostingId()
                ));

        // Being assigned an interview is what makes someone an interviewer;
        // "My Interviews" is no longer handed to every employee.
        userService.assignRole(interviewerUserId, INTERVIEWER_ROLE);

        Interview interview = interviewRepository.save(
                new Interview(applicationId, interviewerUserId, scheduledAt, durationMinutes, location)
        );

        calendarService.recordSystemEvent(
                "Interview: " + candidate.getFullName(),
                CalendarEvent.TYPE_INTERVIEW,
                scheduledAt,
                scheduledAt.plusMinutes(durationMinutes),
                interviewerUserId,
                null
        );

        notificationPublisher.publish(
                interviewerUserId,
                NotificationType.INTERVIEW_ASSIGNED,
                "Interview assigned",
                "You have been assigned to interview " + candidate.getFullName() + " for " + posting.getTitle() + "."
        );

        emailService.queueEmail(
                EmailTemplate.INTERVIEW_INVITATION,
                candidate.getEmail(),
                Map.of(
                        "candidateName", candidate.getFullName(),
                        "jobTitle", posting.getTitle(),
                        "scheduledAt", scheduledAt.toString(),
                        "locationSuffix", (location == null || location.isBlank()) ? "" : " at " + location
                )
        );

        if (Application.STATUS_APPLIED.equals(application.getStatus())
                || Application.STATUS_SCREENING.equals(application.getStatus())) {
            application.advanceToInterviewing();
        }

        auditService.log("INTERVIEW_SCHEDULED", "Interview", interview.getId(), AuditService.RESULT_SUCCESS);

        return InterviewResponse.from(interview);
    }

    public List<InterviewResponse> getInterviewsForApplication(UUID applicationId) {
        return interviewRepository.findByApplicationId(applicationId).stream()
                .map(InterviewResponse::from)
                .toList();
    }

    public List<InterviewResponse> getMyInterviews(UUID interviewerUserId) {
        return interviewRepository.findByInterviewerUserIdOrderByScheduledAtAsc(interviewerUserId).stream()
                .map(InterviewResponse::from)
                .toList();
    }

    @Transactional
    public InterviewResponse completeInterview(UUID interviewId, UUID requesterUserId, boolean requesterCanManage) {

        Interview interview = findInterviewOrThrow(interviewId);
        assertIsInterviewerOrManager(interview, requesterUserId, requesterCanManage);

        if (!interview.isScheduled()) {
            throw new BusinessRuleException("Only a scheduled interview can be completed");
        }

        interview.complete();

        return InterviewResponse.from(interview);
    }

    @Transactional
    public InterviewResponse cancelInterview(UUID interviewId, UUID requesterUserId, boolean requesterCanManage) {

        Interview interview = findInterviewOrThrow(interviewId);
        assertIsInterviewerOrManager(interview, requesterUserId, requesterCanManage);

        if (!interview.isScheduled()) {
            throw new BusinessRuleException("Only a scheduled interview can be cancelled");
        }

        interview.cancel();

        return InterviewResponse.from(interview);
    }

    @Transactional
    public InterviewFeedbackResponse submitFeedback(
            UUID interviewId,
            UUID requesterUserId,
            boolean requesterCanManage,
            Integer rating,
            String comments,
            String recommendation
    ) {
        Interview interview = findInterviewOrThrow(interviewId);
        assertIsInterviewerOrManager(interview, requesterUserId, requesterCanManage);

        if (feedbackRepository.existsByInterviewId(interviewId)) {
            throw new BusinessRuleException("Feedback has already been submitted for this interview");
        }

        InterviewFeedback feedback = feedbackRepository.save(new InterviewFeedback(
                interviewId, interview.getInterviewerUserId(), rating, comments, recommendation
        ));

        auditService.log(
                "INTERVIEW_FEEDBACK_SUBMITTED", "InterviewFeedback", feedback.getId(), AuditService.RESULT_SUCCESS
        );

        return InterviewFeedbackResponse.from(feedback);
    }

    public List<InterviewFeedbackResponse> getFeedbackForApplication(UUID applicationId) {

        List<UUID> interviewIds = interviewRepository.findByApplicationId(applicationId).stream()
                .map(Interview::getId)
                .toList();

        return feedbackRepository.findByInterviewIdIn(interviewIds).stream()
                .map(InterviewFeedbackResponse::from)
                .toList();
    }

    private Interview findInterviewOrThrow(UUID interviewId) {
        return interviewRepository.findById(interviewId)
                .orElseThrow(() -> new RecruitmentNotFoundException("Interview not found: " + interviewId));
    }

    private void assertIsInterviewerOrManager(
            Interview interview,
            UUID requesterUserId,
            boolean requesterCanManage
    ) {
        if (!requesterCanManage && !interview.getInterviewerUserId().equals(requesterUserId)) {
            throw new AccessDeniedException("You are not the interviewer assigned to this interview");
        }
    }
}
