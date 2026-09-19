package za.co.qsnext.employeemanagement.payroll;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PayrollRunEntryRepository extends JpaRepository<PayrollRunEntry, UUID> {

    List<PayrollRunEntry> findByPayrollRunId(UUID payrollRunId);

    List<PayrollRunEntry> findByEmployeeIdOrderByCreatedAtDesc(UUID employeeId);

    Optional<PayrollRunEntry> findByPayrollRunIdAndEmployeeId(UUID payrollRunId, UUID employeeId);
}
