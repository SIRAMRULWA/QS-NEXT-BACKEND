package za.co.qsnext.employeemanagement.scheduling;

import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ShiftAssignmentRepository extends JpaRepository<ShiftAssignment, UUID> {

    boolean existsByEmployeeIdAndWorkDateAndStatus(
            UUID employeeId,
            LocalDate workDate,
            String status
    );

    Optional<ShiftAssignment> findByEmployeeIdAndWorkDate(UUID employeeId, LocalDate workDate);

    List<ShiftAssignment> findByEmployeeIdAndWorkDateBetweenOrderByWorkDateAsc(
            UUID employeeId,
            LocalDate rangeStart,
            LocalDate rangeEnd
    );
}
