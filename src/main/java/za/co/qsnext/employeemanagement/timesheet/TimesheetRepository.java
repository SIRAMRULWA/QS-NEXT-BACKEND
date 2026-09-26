package za.co.qsnext.employeemanagement.timesheet;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface TimesheetRepository
        extends JpaRepository<Timesheet, UUID> {

    Page<Timesheet> findByEmployeeId(
            UUID employeeId,
            Pageable pageable
    );

    /**
     * Used by Payroll to source approved hours for a pay period - see
     * {@code PayrollRunService}. Only an APPROVED timesheet counts
     * towards pay; a still-pending or rejected one does not.
     */
    Optional<Timesheet> findByEmployeeIdAndPeriodStartAndPeriodEndAndStatus(
            UUID employeeId,
            LocalDate periodStart,
            LocalDate periodEnd,
            String status
    );

    /**
     * Batch form of {@link #findByEmployeeIdAndPeriodStartAndPeriodEndAndStatus}
     * for Payroll's per-run overtime lookup, which otherwise issues one query
     * per active employee for the same pay period and status.
     */
    List<Timesheet> findByEmployeeIdInAndPeriodStartAndPeriodEndAndStatus(
            List<UUID> employeeIds,
            LocalDate periodStart,
            LocalDate periodEnd,
            String status
    );

    Page<Timesheet> findByStatus(
            String status,
            Pageable pageable
    );

    boolean existsByEmployeeIdAndPeriodStartAndPeriodEnd(
            UUID employeeId,
            LocalDate periodStart,
            LocalDate periodEnd
    );

    List<Timesheet> findByEmployeeIdAndPeriodStartBetweenOrderByPeriodStartAsc(
            UUID employeeId,
            LocalDate from,
            LocalDate to
    );

    List<Timesheet> findByEmployeeIdInAndStatusOrderByPeriodStartAsc(
            List<UUID> employeeIds,
            String status
    );
}
