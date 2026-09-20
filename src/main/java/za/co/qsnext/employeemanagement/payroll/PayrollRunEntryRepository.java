package za.co.qsnext.employeemanagement.payroll;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PayrollRunEntryRepository extends JpaRepository<PayrollRunEntry, UUID> {

    List<PayrollRunEntry> findByPayrollRunId(UUID payrollRunId);

    List<PayrollRunEntry> findByEmployeeIdOrderByCreatedAtDesc(UUID employeeId);

    Optional<PayrollRunEntry> findByPayrollRunIdAndEmployeeId(UUID payrollRunId, UUID employeeId);

    @Query("""
            select
                coalesce(sum(e.totalEarnings), 0) as totalEarnings,
                coalesce(sum(e.totalDeductions), 0) as totalDeductions,
                coalesce(sum(e.totalEmployerContributions), 0) as totalEmployerContributions,
                coalesce(sum(e.netPay), 0) as totalNetPay,
                count(e) as employeeCount
            from PayrollRunEntry e
            where e.payrollRunId = :payrollRunId
            """)
    PayrollRunSummary summarizeByPayrollRunId(@Param("payrollRunId") UUID payrollRunId);

    @Query("""
            select e from PayrollRunEntry e
            where e.employeeId = :employeeId
            and e.payrollRunId in (
                select r.id from PayrollRun r
                where r.payPeriodId in (
                    select p.id from PayPeriod p
                    where p.startDate >= :from and p.endDate <= :to
                )
            )
            order by e.createdAt desc
            """)
    List<PayrollRunEntry> findByEmployeeIdAndPayPeriodBetween(
            @Param("employeeId") UUID employeeId,
            @Param("from") LocalDate from,
            @Param("to") LocalDate to
    );
}
