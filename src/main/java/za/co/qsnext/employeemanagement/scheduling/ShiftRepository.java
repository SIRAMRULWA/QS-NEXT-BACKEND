package za.co.qsnext.employeemanagement.scheduling;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface ShiftRepository extends JpaRepository<Shift, UUID> {
}
