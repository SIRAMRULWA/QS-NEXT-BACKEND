package za.co.qsnext.employeemanagement.leave;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import za.co.qsnext.employeemanagement.employee.Employee;
import za.co.qsnext.employeemanagement.employee.EmployeeRepository;
import za.co.qsnext.employeemanagement.exception.BusinessRuleException;
import za.co.qsnext.employeemanagement.leave.dto.LeavePolicyRequest;
import za.co.qsnext.employeemanagement.leave.dto.LeavePolicyResponse;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.YearMonth;
import java.util.List;
import java.util.Set;

/**
 * Credits leave to every active employee according to each leave type's
 * {@link LeavePolicy}. Runs on the first of every month; HR can also run
 * the current month by hand. A {@link LeaveAccrualRun} per policy per
 * month makes both safe to repeat.
 *
 * The first credit of a new year opens that year's balance with the
 * previous year's unused days, capped at the policy's carry-over limit.
 */
@Service
public class LeaveAccrualService {

    private static final Logger log = LoggerFactory.getLogger(LeaveAccrualService.class);

    private static final String ACTIVE = "ACTIVE";
    private static final Set<String> LEAVE_TYPES = Set.of(
            "ANNUAL", "SICK", "FAMILY_RESPONSIBILITY", "MATERNITY",
            "PATERNITY", "UNPAID", "STUDY", "OTHER"
    );
    private static final Set<String> METHODS = Set.of(LeavePolicy.METHOD_MONTHLY, LeavePolicy.METHOD_ANNUAL);

    private final LeavePolicyRepository policyRepository;
    private final LeaveAccrualRunRepository runRepository;
    private final LeaveBalanceRepository balanceRepository;
    private final EmployeeRepository employeeRepository;

    public LeaveAccrualService(
            LeavePolicyRepository policyRepository,
            LeaveAccrualRunRepository runRepository,
            LeaveBalanceRepository balanceRepository,
            EmployeeRepository employeeRepository
    ) {
        this.policyRepository = policyRepository;
        this.runRepository = runRepository;
        this.balanceRepository = balanceRepository;
        this.employeeRepository = employeeRepository;
    }

    @Transactional(readOnly = true)
    public List<LeavePolicyResponse> getPolicies() {
        return policyRepository.findAllByOrderByLeaveTypeAsc().stream()
                .map(LeavePolicyResponse::from)
                .toList();
    }

    @Transactional
    public LeavePolicyResponse savePolicy(LeavePolicyRequest request) {

        if (!LEAVE_TYPES.contains(request.leaveType())) {
            throw new BusinessRuleException("Unknown leave type: " + request.leaveType());
        }

        if (!METHODS.contains(request.accrualMethod())) {
            throw new BusinessRuleException("Accrual method must be MONTHLY or ANNUAL");
        }

        LeavePolicy policy = policyRepository.findByLeaveType(request.leaveType())
                .orElseGet(() -> new LeavePolicy(request.leaveType()));

        policy.configure(
                request.annualDays(),
                request.accrualMethod(),
                request.carryOverMaxDays() == null ? BigDecimal.ZERO : request.carryOverMaxDays(),
                request.active() == null || request.active()
        );

        return LeavePolicyResponse.from(policyRepository.save(policy));
    }

    @Transactional
    @Scheduled(cron = "${leave.accrual.cron:0 0 1 1 * *}")
    public void accrueCurrentMonthOnSchedule() {
        int credited = accrue(YearMonth.now());
        log.info("Leave accrual for {} credited {} balances", YearMonth.now(), credited);
    }

    /**
     * Credits the given month for every active policy that hasn't been
     * credited for it yet. Returns how many employee balances were credited.
     */
    @Transactional
    public int accrue(YearMonth month) {

        String period = month.toString();
        List<Employee> activeEmployees = employeeRepository
                .findByEmploymentStatus(ACTIVE, Pageable.unpaged())
                .getContent();

        int credited = 0;

        for (LeavePolicy policy : policyRepository.findByActiveTrue()) {

            if (runRepository.existsByPolicyIdAndPeriod(policy.getId(), period)) {
                continue;
            }

            int creditedForPolicy = 0;

            for (Employee employee : activeEmployees) {
                if (credit(policy, employee, month)) {
                    creditedForPolicy++;
                }
            }

            runRepository.save(new LeaveAccrualRun(policy.getId(), period, creditedForPolicy));
            credited += creditedForPolicy;
        }

        return credited;
    }

    private boolean credit(LeavePolicy policy, Employee employee, YearMonth month) {

        int year = month.getYear();

        LeaveBalance existing = balanceRepository
                .findByEmployeeIdAndLeaveTypeAndLeaveYear(employee.getId(), policy.getLeaveType(), year)
                .orElse(null);

        boolean opening = existing == null;

        LeaveBalance balance = opening
                ? balanceRepository.save(new LeaveBalance(
                        employee.getId(), policy.getLeaveType(), year, carryOver(policy, employee, year)))
                : existing;

        BigDecimal days = LeavePolicy.METHOD_MONTHLY.equals(policy.getAccrualMethod())
                ? policy.getAnnualDays().divide(BigDecimal.valueOf(12), 2, RoundingMode.HALF_UP)
                : (opening || month.getMonthValue() == 1 ? policy.getAnnualDays() : BigDecimal.ZERO);

        if (days.signum() > 0) {
            balance.allocateAdditionalDays(days);
            return true;
        }

        return opening;
    }

    private BigDecimal carryOver(LeavePolicy policy, Employee employee, int year) {
        return balanceRepository
                .findByEmployeeIdAndLeaveTypeAndLeaveYear(employee.getId(), policy.getLeaveType(), year - 1)
                .map(previous -> previous.getRemainingDays().max(BigDecimal.ZERO).min(policy.getCarryOverMaxDays()))
                .orElse(BigDecimal.ZERO);
    }
}
