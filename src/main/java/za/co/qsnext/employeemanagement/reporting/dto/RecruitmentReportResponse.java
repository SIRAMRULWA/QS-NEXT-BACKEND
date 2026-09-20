package za.co.qsnext.employeemanagement.reporting.dto;

import za.co.qsnext.employeemanagement.recruitment.Application;
import za.co.qsnext.employeemanagement.recruitment.JobPosting;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public record RecruitmentReportResponse(
        UUID jobPostingId,
        String title,
        String status,
        int totalApplications,
        int screeningApplications,
        int interviewingApplications,
        int offerApplications,
        int hiredApplications,
        int rejectedApplications,
        int withdrawnApplications,
        List<ApplicationSummary> applications
) {

    public static RecruitmentReportResponse from(JobPosting jobPosting, List<Application> applications) {

        int screening = 0;
        int interviewing = 0;
        int offer = 0;
        int hired = 0;
        int rejected = 0;
        int withdrawn = 0;

        for (Application application : applications) {
            switch (application.getStatus()) {
                case Application.STATUS_SCREENING -> screening++;
                case Application.STATUS_INTERVIEWING -> interviewing++;
                case Application.STATUS_OFFER -> offer++;
                case Application.STATUS_HIRED -> hired++;
                case Application.STATUS_REJECTED -> rejected++;
                case Application.STATUS_WITHDRAWN -> withdrawn++;
                default -> {
                    // APPLIED - the initial stage, not separately counted here.
                }
            }
        }

        return new RecruitmentReportResponse(
                jobPosting.getId(), jobPosting.getTitle(), jobPosting.getStatus(), applications.size(),
                screening, interviewing, offer, hired, rejected, withdrawn,
                applications.stream().map(ApplicationSummary::from).toList());
    }

    public record ApplicationSummary(
            UUID id,
            UUID candidateId,
            String status,
            OffsetDateTime appliedAt
    ) {

        public static ApplicationSummary from(Application application) {
            return new ApplicationSummary(
                    application.getId(), application.getCandidateId(),
                    application.getStatus(), application.getAppliedAt());
        }
    }
}
