package za.co.qsnext.employeemanagement.leave;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import za.co.qsnext.employeemanagement.employee.EmployeeService;
import za.co.qsnext.employeemanagement.exception.BusinessRuleException;
import za.co.qsnext.employeemanagement.exception.LeaveRequestNotFoundException;
import za.co.qsnext.employeemanagement.user.UserService;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class LeaveService {

    private final LeaveRequestRepository leaveRequestRepository;
    private final LeaveBalanceRepository leaveBalanceRepository;
    private final EmployeeService employeeService;
    private final UserService userService;

    public LeaveService(
            LeaveRequestRepository leaveRequestRepository,
            LeaveBalanceRepository leaveBalanceRepository,
            EmployeeService employeeService,
            UserService userService
    ) {
        this.leaveRequestRepository = leaveRequestRepository;
        this.leaveBalanceRepository = leaveBalanceRepository;
        this.employeeService = employeeService;
        this.userService = userService;
    }

    public LeaveRequest getById(UUID leaveRequestId) {
        return leaveRequestRepository.findById(leaveRequestId)
                .orElseThrow(() ->
                        new LeaveRequestNotFoundException(
                                "Leave request not found: " + leaveRequestId
                        )
                );
    }

    public Page<LeaveRequest> getByEmployee(
            UUID employeeId,
            Pageable pageable
    ) {
        employeeService.getById(employeeId);

        return leaveRequestRepository.findByEmployeeId(
                employeeId,
                pageable
        );
    }

    public Page<LeaveRequest> getByStatus(
            String status,
            Pageable pageable
    ) {
        return leaveRequestRepository.findByStatus(
                status,
                pageable
        );
    }

    @Transactional
    public LeaveRequest create(
            UUID employeeId,
            String leaveType,
            LocalDate startDate,
            LocalDate endDate,
            String reason
    ) {

        employeeService.getById(employeeId);

        validateDates(startDate, endDate);

        int leaveYear = startDate.getYear();

        LeaveBalance balance =
                leaveBalanceRepository
                        .findByEmployeeIdAndLeaveTypeAndLeaveYear(
                                employeeId,
                                leaveType,
                                leaveYear
                        )
                        .orElseThrow(() ->
                                new BusinessRuleException(
                                        "No leave balance exists for employee, leave type and year"
                                )
                        );

        BigDecimal requestedDays =
                BigDecimal.valueOf(
                        calculateLeaveDays(startDate, endDate)
                );

        if (balance.getRemainingDays().compareTo(requestedDays) < 0) {
            throw new BusinessRuleException(
                    "Insufficient leave balance"
            );
        }

        return leaveRequestRepository.save(
                new LeaveRequest(
                        employeeId,
                        leaveType,
                        startDate,
                        endDate,
                        reason
                )
        );
    }

    @Transactional
    public LeaveRequest approve(
            UUID leaveRequestId,
            UUID approverId
    ) {

        LeaveRequest leaveRequest = getById(leaveRequestId);

        userService.getById(approverId);

        if (!"PENDING".equals(leaveRequest.getStatus())) {
            throw new BusinessRuleException(
                    "Only pending leave requests can be approved"
            );
        }

        int leaveYear = leaveRequest.getStartDate().getYear();

        LeaveBalance balance =
                leaveBalanceRepository
                        .findByEmployeeIdAndLeaveTypeAndLeaveYear(
                                leaveRequest.getEmployeeId(),
                                leaveRequest.getLeaveType(),
                                leaveYear
                        )
                        .orElseThrow(() ->
                                new BusinessRuleException(
                                        "Leave balance not found"
                                )
                        );

        BigDecimal requestedDays =
                BigDecimal.valueOf(
                        calculateLeaveDays(
                                leaveRequest.getStartDate(),
                                leaveRequest.getEndDate()
                        )
                );

        if (balance.getRemainingDays().compareTo(requestedDays) < 0) {
            throw new BusinessRuleException(
                    "Insufficient leave balance for approval"
            );
        }

        balance.addUsedDays(requestedDays);

        leaveRequest.approve(approverId);

        return leaveRequest;
    }

    @Transactional
    public LeaveRequest reject(UUID leaveRequestId) {

        LeaveRequest leaveRequest = getById(leaveRequestId);

        if (!"PENDING".equals(leaveRequest.getStatus())) {
            throw new BusinessRuleException(
                    "Only pending leave requests can be rejected"
            );
        }

        leaveRequest.reject();

        return leaveRequest;
    }

    @Transactional
    public LeaveRequest cancel(UUID leaveRequestId) {

        LeaveRequest leaveRequest = getById(leaveRequestId);

        if ("APPROVED".equals(leaveRequest.getStatus())) {
            throw new BusinessRuleException(
                    "Approved leave cannot be cancelled directly"
            );
        }

        if ("CANCELLED".equals(leaveRequest.getStatus())) {
            throw new BusinessRuleException(
                    "Leave request is already cancelled"
            );
        }

        leaveRequest.cancel();

        return leaveRequest;
    }

    private void validateDates(
            LocalDate startDate,
            LocalDate endDate
    ) {

        if (startDate == null || endDate == null) {
            throw new BusinessRuleException(
                    "Leave start date and end date are required"
            );
        }

        if (endDate.isBefore(startDate)) {
            throw new BusinessRuleException(
                    "Leave end date cannot be before start date"
            );
        }
    }

    private long calculateLeaveDays(
            LocalDate startDate,
            LocalDate endDate
    ) {

        return endDate.toEpochDay()
                - startDate.toEpochDay()
                + 1;
    }
}