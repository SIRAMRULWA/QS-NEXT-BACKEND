package za.co.qsnext.employeemanagement.careers;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import za.co.qsnext.employeemanagement.careers.dto.ApplyRequest;
import za.co.qsnext.employeemanagement.careers.dto.JobOpeningResponse;
import za.co.qsnext.employeemanagement.careers.dto.MyApplicationResponse;
import za.co.qsnext.employeemanagement.careers.dto.MyInterviewResponse;
import za.co.qsnext.employeemanagement.exception.BusinessRuleException;
import za.co.qsnext.employeemanagement.exception.RecruitmentNotFoundException;
import za.co.qsnext.employeemanagement.recruitment.Application;
import za.co.qsnext.employeemanagement.recruitment.ApplicationRepository;
import za.co.qsnext.employeemanagement.recruitment.Candidate;
import za.co.qsnext.employeemanagement.recruitment.CandidateRepository;
import za.co.qsnext.employeemanagement.recruitment.InterviewRepository;
import za.co.qsnext.employeemanagement.recruitment.JobPosting;
import za.co.qsnext.employeemanagement.recruitment.JobPostingRepository;
import za.co.qsnext.employeemanagement.recruitment.RecruitmentService;
import za.co.qsnext.employeemanagement.recruitment.dto.ApplicationResponse;
import za.co.qsnext.employeemanagement.user.User;
import za.co.qsnext.employeemanagement.user.UserService;

import java.util.Comparator;
import java.util.List;
import java.util.UUID;

/**
 * The applicant's side of recruitment: browse open jobs, apply, and follow
 * their own applications. Everything here is scoped to the caller's own
 * candidate record; HR's recruitment endpoints stay HR-only.
 */
@Service
public class CareersService {

    private static final String SOURCE_JOB_BOARD = "Job board";

    private final JobPostingRepository postingRepository;
    private final CandidateRepository candidateRepository;
    private final ApplicationRepository applicationRepository;
    private final InterviewRepository interviewRepository;
    private final RecruitmentService recruitmentService;
    private final UserService userService;

    public CareersService(
            JobPostingRepository postingRepository,
            CandidateRepository candidateRepository,
            ApplicationRepository applicationRepository,
            InterviewRepository interviewRepository,
            RecruitmentService recruitmentService,
            UserService userService
    ) {
        this.postingRepository = postingRepository;
        this.candidateRepository = candidateRepository;
        this.applicationRepository = applicationRepository;
        this.interviewRepository = interviewRepository;
        this.recruitmentService = recruitmentService;
        this.userService = userService;
    }

    @Transactional(readOnly = true)
    public List<JobOpeningResponse> getOpenings() {
        return postingRepository.findByStatus(JobPosting.STATUS_OPEN).stream()
                .sorted(Comparator.comparing(
                        JobPosting::getPublishedAt,
                        Comparator.nullsLast(Comparator.reverseOrder())
                ))
                .map(JobOpeningResponse::from)
                .toList();
    }

    @Transactional
    public MyApplicationResponse apply(UUID userId, UUID postingId, ApplyRequest request) {

        User user = userService.getById(userId);

        if (!user.isEmailVerified()) {
            throw new BusinessRuleException("Confirm your email address before applying");
        }

        Candidate candidate = candidateFor(user, request);

        ApplicationResponse created = recruitmentService.createApplication(candidate.getId(), postingId);

        return toMyApplication(applicationRepository.findById(created.id())
                .orElseThrow(() -> new RecruitmentNotFoundException("Application not found: " + created.id())));
    }

    @Transactional(readOnly = true)
    public List<MyApplicationResponse> getMyApplications(UUID userId) {
        return candidateRepository.findByUserId(userId)
                .map(candidate -> applicationRepository.findByCandidateId(candidate.getId()).stream()
                        .sorted(Comparator.comparing(Application::getAppliedAt).reversed())
                        .map(this::toMyApplication)
                        .toList())
                .orElse(List.of());
    }

    @Transactional
    public MyApplicationResponse withdraw(UUID userId, UUID applicationId) {

        Application application = ownApplicationOrThrow(userId, applicationId);

        recruitmentService.withdrawApplication(application.getId());

        return toMyApplication(application);
    }

    private Candidate candidateFor(User user, ApplyRequest request) {

        return candidateRepository.findByUserId(user.getId())
                .orElseGet(() -> candidateRepository.findByEmail(user.getEmail())
                        .map(existing -> {
                            // HR may already have entered this person by hand;
                            // the verified email proves the login owns the record.
                            if (existing.getUserId() != null && !existing.getUserId().equals(user.getId())) {
                                throw new BusinessRuleException(
                                        "This email is already linked to another applicant account"
                                );
                            }
                            existing.linkUser(user.getId());
                            return existing;
                        })
                        .orElseGet(() -> {
                            Candidate created = new Candidate(
                                    request.firstName().trim(),
                                    request.lastName().trim(),
                                    user.getEmail(),
                                    request.phone() == null || request.phone().isBlank() ? null : request.phone().trim(),
                                    null,
                                    SOURCE_JOB_BOARD
                            );
                            created.linkUser(user.getId());
                            return candidateRepository.save(created);
                        }));
    }

    private Application ownApplicationOrThrow(UUID userId, UUID applicationId) {

        UUID candidateId = candidateRepository.findByUserId(userId)
                .map(Candidate::getId)
                .orElse(null);

        return applicationRepository.findById(applicationId)
                .filter(application -> application.getCandidateId().equals(candidateId))
                .orElseThrow(() -> new RecruitmentNotFoundException("Application not found: " + applicationId));
    }

    private MyApplicationResponse toMyApplication(Application application) {

        JobPosting posting = postingRepository.findById(application.getJobPostingId()).orElse(null);

        List<MyInterviewResponse> interviews = interviewRepository.findByApplicationId(application.getId()).stream()
                .sorted(Comparator.comparing(interview -> interview.getScheduledAt()))
                .map(MyInterviewResponse::from)
                .toList();

        return new MyApplicationResponse(
                application.getId(),
                application.getJobPostingId(),
                posting == null ? null : posting.getTitle(),
                posting == null ? null : posting.getLocation(),
                application.getStatus(),
                application.getAppliedAt(),
                application.getUpdatedAt(),
                interviews
        );
    }
}
