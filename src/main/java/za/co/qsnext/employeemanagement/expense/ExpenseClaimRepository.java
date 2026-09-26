package za.co.qsnext.employeemanagement.expense;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public interface ExpenseClaimRepository extends JpaRepository<ExpenseClaim, UUID> {

    List<ExpenseClaim> findByEmployeeIdOrderByExpenseDateDesc(UUID employeeId);

    List<ExpenseClaim> findByEmployeeIdAndExpenseDateBetweenOrderByExpenseDateDesc(
            UUID employeeId, LocalDate from, LocalDate to);

    Page<ExpenseClaim> findByStatus(String status, Pageable pageable);

    @Query("""
            select c.categoryId as categoryId, coalesce(sum(c.amount), 0) as totalAmount, count(c) as claimCount
            from ExpenseClaim c
            where c.status in ('APPROVED', 'REIMBURSED')
            and c.expenseDate between :from and :to
            group by c.categoryId
            """)
    List<ExpenseCategorySummary> summarizeByCategory(@Param("from") LocalDate from, @Param("to") LocalDate to);

    @Query("""
            select coalesce(sum(c.amount), 0) as totalAmount, count(c) as claimCount
            from ExpenseClaim c
            where c.status in ('APPROVED', 'REIMBURSED')
            and c.expenseDate between :from and :to
            """)
    ExpenseTrendSummary summarizeForPeriod(@Param("from") LocalDate from, @Param("to") LocalDate to);

    List<ExpenseClaim> findByEmployeeIdInAndStatusOrderByExpenseDateAsc(List<UUID> employeeIds, String status);
}
