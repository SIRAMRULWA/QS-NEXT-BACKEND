package za.co.qsnext.employeemanagement.leave;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface LeaveBalanceRepository
        extends JpaRepository<LeaveBalance, UUID> {

    Optional<LeaveBalance>
    findByEmployeeIdAndLeaveTypeAndLeaveYear(
            UUID employeeId,
            String leaveType,
            Integer leaveYear
    );

    List<LeaveBalance> findByEmployeeIdAndLeaveYear(
            UUID employeeId,
            Integer leaveYear
    );
}