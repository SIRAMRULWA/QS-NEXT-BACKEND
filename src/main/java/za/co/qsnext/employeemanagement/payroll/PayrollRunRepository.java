package za.co.qsnext.employeemanagement.payroll;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface PayrollRunRepository extends JpaRepository<PayrollRun, UUID> {

    Optional<PayrollRun> findByPayPeriodId(UUID payPeriodId);
}
