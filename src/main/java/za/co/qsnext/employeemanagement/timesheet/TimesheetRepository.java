package za.co.qsnext.employeemanagement.timesheet;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.UUID;

public interface TimesheetRepository
        extends JpaRepository<Timesheet, UUID> {

    Page<Timesheet> findByEmployeeId(
            UUID employeeId,
            Pageable pageable
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
}