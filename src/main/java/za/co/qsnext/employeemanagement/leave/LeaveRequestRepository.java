package za.co.qsnext.employeemanagement.leave;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
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

    Page<LeaveRequest> findByEmployeeIdAndStartDateLessThanEqualAndEndDateGreaterThanEqual(
            UUID employeeId,
            LocalDate endDate,
            LocalDate startDate,
            Pageable pageable
    );

    @Query("""
            select l.status as status, count(l) as requestCount
            from LeaveRequest l
            where l.startDate <= :to and l.endDate >= :from
            group by l.status
            """)
    List<LeaveStatusCount> countGroupedByStatus(
            @Param("from") LocalDate from,
            @Param("to") LocalDate to
    );

    List<LeaveRequest> findByEmployeeIdInAndStatusOrderByStartDateAsc(
            List<UUID> employeeIds,
            String status
    );
}
