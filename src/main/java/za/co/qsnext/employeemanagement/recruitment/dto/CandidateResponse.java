package za.co.qsnext.employeemanagement.recruitment.dto;

import za.co.qsnext.employeemanagement.recruitment.Candidate;

import java.util.UUID;

public record CandidateResponse(
        UUID id,
        String firstName,
        String lastName,
        String email,
        String phone,
        UUID resumeDocumentId,
        String source
) {

    public static CandidateResponse from(Candidate candidate) {
        return new CandidateResponse(
                candidate.getId(), candidate.getFirstName(), candidate.getLastName(), candidate.getEmail(),
                candidate.getPhone(), candidate.getResumeDocumentId(), candidate.getSource()
        );
    }
}
