package za.co.qsnext.employeemanagement.leave;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface LeaveRequestRepository
        extends JpaRepository<LeaveRequest, UUID> {

    Page<LeaveRequest> findByEmployeeId(
            UUID employeeId,
            Pageable pageable
    );

    Page<LeaveRequest> findByStatus(
            String status,
            Pageable pageable
    );

    Page<LeaveRequest> findByEmployeeIdAndStatus(
            UUID employeeId,
            String status,
            Pageable pageable
    );
}