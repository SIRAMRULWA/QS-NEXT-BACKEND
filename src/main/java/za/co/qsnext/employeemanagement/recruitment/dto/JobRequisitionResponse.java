package za.co.qsnext.employeemanagement.recruitment.dto;

import za.co.qsnext.employeemanagement.recruitment.JobRequisition;

import java.util.UUID;

public record JobRequisitionResponse(
        UUID id,
        String title,
        UUID departmentId,
        String description,
        int numberOfOpenings,
        String status,
        UUID requestedBy
) {

    public static JobRequisitionResponse from(JobRequisition requisition) {
        return new JobRequisitionResponse(
                requisition.getId(), requisition.getTitle(), requisition.getDepartmentId(),
                requisition.getDescription(), requisition.getNumberOfOpenings(),
                requisition.getStatus(), requisition.getRequestedBy()
        );
    }
}
