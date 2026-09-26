package za.co.qsnext.employeemanagement.leave;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import za.co.qsnext.employeemanagement.employee.Employee;
import za.co.qsnext.employeemanagement.employee.EmployeeRepository;

import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LeaveAccrualServiceTest {

    @Mock
    private LeavePolicyRepository policyRepository;

    @Mock
    private LeaveAccrualRunRepository runRepository;

    @Mock
    private LeaveBalanceRepository balanceRepository;

    @Mock
    private EmployeeRepository employeeRepository;

    private LeaveAccrualService service;

    private Employee employee;
    private LeavePolicy policy;

    @BeforeEach
    void setUp() {
        service = new LeaveAccrualService(policyRepository, runRepository, balanceRepository, employeeRepository);

        employee = new Employee(UUID.randomUUID(), UUID.randomUUID(), "E1", "Jane", "Doe", null, "Engineer",
                LocalDate.of(2020, 1, 1));
        setId(employee, UUID.randomUUID());

        policy = new LeavePolicy("ANNUAL");
        setId(policy, UUID.randomUUID());

        when(employeeRepository.findByEmploymentStatus(eq("ACTIVE"), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(employee)));
        when(policyRepository.findByActiveTrue()).thenReturn(List.of(policy));
    }

    @Test
    void monthlyPolicyCreditsOneTwelfthToAnExistingBalance() {
        policy.configure(new BigDecimal("15"), LeavePolicy.METHOD_MONTHLY, BigDecimal.ZERO, true);
        LeaveBalance balance = new LeaveBalance(employee.getId(), "ANNUAL", 2026, new BigDecimal("5.00"));

        when(runRepository.existsByPolicyIdAndPeriod(policy.getId(), "2026-03")).thenReturn(false);
        when(balanceRepository.findByEmployeeIdAndLeaveTypeAndLeaveYear(employee.getId(), "ANNUAL", 2026))
                .thenReturn(Optional.of(balance));

        int credited = service.accrue(YearMonth.of(2026, 3));

        assertThat(credited).isEqualTo(1);
        assertThat(balance.getAllocatedDays()).isEqualByComparingTo("6.25");
        verify(runRepository).save(any(LeaveAccrualRun.class));
    }

    @Test
    void firstCreditOfTheYearCarriesOverUnusedDaysUpToTheCap() {
        policy.configure(new BigDecimal("12"), LeavePolicy.METHOD_MONTHLY, new BigDecimal("5"), true);
        LeaveBalance lastYear = new LeaveBalance(employee.getId(), "ANNUAL", 2025, new BigDecimal("12"));
        lastYear.addUsedDays(new BigDecimal("4"));

        when(runRepository.existsByPolicyIdAndPeriod(policy.getId(), "2026-01")).thenReturn(false);
        when(balanceRepository.findByEmployeeIdAndLeaveTypeAndLeaveYear(employee.getId(), "ANNUAL", 2026))
                .thenReturn(Optional.empty());
        when(balanceRepository.findByEmployeeIdAndLeaveTypeAndLeaveYear(employee.getId(), "ANNUAL", 2025))
                .thenReturn(Optional.of(lastYear));
        when(balanceRepository.save(any(LeaveBalance.class))).thenAnswer(invocation -> invocation.getArgument(0));

        service.accrue(YearMonth.of(2026, 1));

        verify(balanceRepository).save(org.mockito.ArgumentMatchers.argThat(balance ->
                balance.getLeaveYear() == 2026
                        // 8 unused, capped at 5, plus one month of 12/12
                        && balance.getAllocatedDays().compareTo(new BigDecimal("6.00")) == 0));
    }

    @Test
    void aMonthAlreadyCreditedIsNotCreditedAgain() {
        policy.configure(new BigDecimal("15"), LeavePolicy.METHOD_MONTHLY, BigDecimal.ZERO, true);

        when(runRepository.existsByPolicyIdAndPeriod(policy.getId(), "2026-03")).thenReturn(true);

        int credited = service.accrue(YearMonth.of(2026, 3));

        assertThat(credited).isZero();
        verify(runRepository, never()).save(any(LeaveAccrualRun.class));
    }

    private static void setId(Object entity, UUID id) {
        try {
            Field field = entity.getClass().getDeclaredField("id");
            field.setAccessible(true);
            field.set(entity, id);
        } catch (ReflectiveOperationException ex) {
            throw new IllegalStateException(ex);
        }
    }
}
