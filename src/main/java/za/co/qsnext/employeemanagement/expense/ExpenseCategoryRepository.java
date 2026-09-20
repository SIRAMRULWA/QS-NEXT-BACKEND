package za.co.qsnext.employeemanagement.expense;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ExpenseCategoryRepository extends JpaRepository<ExpenseCategory, UUID> {

    boolean existsByName(String name);

    List<ExpenseCategory> findByActiveTrue();
}
