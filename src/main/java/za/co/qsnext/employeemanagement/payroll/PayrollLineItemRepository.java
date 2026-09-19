package za.co.qsnext.employeemanagement.payroll;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface PayrollLineItemRepository extends JpaRepository<PayrollLineItem, UUID> {

    List<PayrollLineItem> findByPayrollRunEntryId(UUID payrollRunEntryId);
}
