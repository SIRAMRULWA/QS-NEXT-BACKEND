package za.co.qsnext.employeemanagement.payroll;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface TaxBracketRepository extends JpaRepository<TaxBracket, UUID> {

    List<TaxBracket> findByTaxConfigurationIdOrderByMinAmountAsc(UUID taxConfigurationId);
}
