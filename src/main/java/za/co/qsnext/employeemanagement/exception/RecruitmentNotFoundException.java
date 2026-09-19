package za.co.qsnext.employeemanagement.exception;

/**
 * Covers a missing JobRequisition, JobPosting, Candidate, Application,
 * Interview, InterviewFeedback or Offer.
 */
public class RecruitmentNotFoundException extends RuntimeException {

    public RecruitmentNotFoundException(String message) {
        super(message);
    }
}
