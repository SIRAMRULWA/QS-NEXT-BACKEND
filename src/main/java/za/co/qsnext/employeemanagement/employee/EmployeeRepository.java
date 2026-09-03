package za.co.qsnext.employeemanagement.employee;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface EmployeeRepository extends JpaRepository<Employee, UUID> {

    boolean existsByEmployeeNumber(String employeeNumber);

    boolean existsByUserId(UUID userId);

    Page<Employee> findByDepartmentId(UUID departmentId, Pageable pageable);

    Page<Employee> findByEmploymentStatus(
            String employmentStatus,
            Pageable pageable
    );

    Page<Employee> findByLastNameContainingIgnoreCase(
            String lastName,
            Pageable pageable
    );
}