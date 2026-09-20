package za.co.qsnext.employeemanagement.employee;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
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

    Optional<Employee> findByUserId(UUID userId);

    List<Employee> findByManagerId(UUID managerId);

    /**
     * Directory search: matches against first name, last name, job title
     * or employee number. A blank/null query matches everything (browse
     * mode).
     */
    @Query("""
            select e from Employee e
            where :query is null
            or lower(e.firstName) like lower(concat('%', :query, '%'))
            or lower(e.lastName) like lower(concat('%', :query, '%'))
            or lower(e.jobTitle) like lower(concat('%', :query, '%'))
            or lower(e.employeeNumber) like lower(concat('%', :query, '%'))
            """)
    Page<Employee> search(@Param("query") String query, Pageable pageable);

    long countByEmploymentStatus(String employmentStatus);

    long countByHireDateBetween(LocalDate from, LocalDate to);

    /**
     * An imperfect proxy for "terminated in this window" - see
     * AnalyticsService's javadoc on employee turnover for why.
     */
    long countByEmploymentStatusAndUpdatedAtBetween(
            String employmentStatus, OffsetDateTime from, OffsetDateTime to);

    @Query("select e.employmentStatus as status, count(e) as employeeCount from Employee e group by e.employmentStatus")
    List<EmploymentStatusCount> countGroupedByEmploymentStatus();

    @Query("select e.departmentId as departmentId, count(e) as employeeCount from Employee e group by e.departmentId")
    List<DepartmentHeadcount> countGroupedByDepartment();
}