package za.co.qsnext.employeemanagement.leave;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface LeavePolicyRepository extends JpaRepository<LeavePolicy, UUID> {

    Optional<LeavePolicy> findByLeaveType(String leaveType);

    List<LeavePolicy> findByActiveTrue();

    List<LeavePolicy> findAllByOrderByLeaveTypeAsc();
}
