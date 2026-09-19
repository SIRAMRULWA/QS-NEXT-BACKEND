package za.co.qsnext.employeemanagement.payroll;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface EmployeePayrollProfileRepository extends JpaRepository<EmployeePayrollProfile, UUID> {

    Optional<EmployeePayrollProfile> findByEmployeeId(UUID employeeId);

    List<EmployeePayrollProfile> findByActiveTrue();
}
