package za.co.qsnext.employeemanagement.payroll;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface PayPeriodRepository extends JpaRepository<PayPeriod, UUID> {

    boolean existsByName(String name);
}
