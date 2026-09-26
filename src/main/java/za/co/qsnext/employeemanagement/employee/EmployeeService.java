package za.co.qsnext.employeemanagement.employee;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import za.co.qsnext.employeemanagement.department.DepartmentService;
import za.co.qsnext.employeemanagement.exception.BusinessRuleException;
import za.co.qsnext.employeemanagement.exception.DuplicateResourceException;
import za.co.qsnext.employeemanagement.exception.EmployeeNotFoundException;
import za.co.qsnext.employeemanagement.auth.RefreshTokenService;
import za.co.qsnext.employeemanagement.user.User;
import za.co.qsnext.employeemanagement.user.UserService;

import java.time.LocalDate;
import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class EmployeeService {

    private final EmployeeRepository employeeRepository;
    private static final String MANAGER_ROLE = "MANAGER";
    private static final String EMPLOYEE_ROLE = "EMPLOYEE";
    private static final String APPLICANT_ROLE = "APPLICANT";
    private static final String ACTIVE = "ACTIVE";
    private static final String SUSPENDED = "SUSPENDED";
    private static final String TERMINATED = "TERMINATED";

    private final UserService userService;
    private final RefreshTokenService refreshTokenService;
    private final DepartmentService departmentService;

    public EmployeeService(
            EmployeeRepository employeeRepository,
            UserService userService,
            DepartmentService departmentService,
            RefreshTokenService refreshTokenService
    ) {
        this.employeeRepository = employeeRepository;
        this.userService = userService;
        this.departmentService = departmentService;
        this.refreshTokenService = refreshTokenService;
    }

    public Employee getById(UUID employeeId) {
        return employeeRepository.findById(employeeId)
                .orElseThrow(() ->
                        new EmployeeNotFoundException(
                                "Employee not found: " + employeeId
                        )
                );
    }

    public Page<Employee> getAll(Pageable pageable) {
        return employeeRepository.findAll(pageable);
    }

    public Page<Employee> getByDepartment(
            UUID departmentId,
            Pageable pageable
    ) {
        return employeeRepository.findByDepartmentId(
                departmentId,
                pageable
        );
    }

    public Page<Employee> getByStatus(
            String status,
            Pageable pageable
    ) {
        return employeeRepository.findByEmploymentStatus(
                status,
                pageable
        );
    }

    public Page<Employee> searchByLastName(
            String lastName,
            Pageable pageable
    ) {
        return employeeRepository
                .findByLastNameContainingIgnoreCase(
                        lastName,
                        pageable
                );
    }

    @Transactional
    public Employee create(
            UUID userId,
            UUID departmentId,
            String employeeNumber,
            String firstName,
            String lastName,
            String phoneNumber,
            String jobTitle,
            LocalDate hireDate
    ) {

        // Verify referenced user exists.
        User user = userService.getById(userId);

        // Verify referenced department exists.
        departmentService.getById(departmentId);

        // Prevent duplicate employee-user relationship.
        if (employeeRepository.existsByUserId(userId)) {
            throw new DuplicateResourceException(
                    "User is already linked to an employee: " + userId
            );
        }

        // Prevent duplicate employee number.
        if (employeeRepository.existsByEmployeeNumber(employeeNumber)) {
            throw new DuplicateResourceException(
                    "Employee number already exists: " + employeeNumber
            );
        }

        Employee employee = new Employee(
                userId,
                departmentId,
                employeeNumber,
                firstName,
                lastName,
                phoneNumber,
                jobTitle,
                hireDate
        );

        Employee saved = employeeRepository.save(employee);

        // Linking a login to an employee record is what makes it an
        // employee; an applicant stops being an applicant at that point.
        userService.assignRole(userId, EMPLOYEE_ROLE);

        if (user != null && user.hasRole(APPLICANT_ROLE)) {
            userService.removeRole(userId, APPLICANT_ROLE);
        }

        return saved;
    }

    @Transactional
    public Employee update(
            UUID employeeId,
            UUID departmentId,
            String firstName,
            String lastName,
            String phoneNumber,
            String jobTitle
    ) {

        Employee employee = getById(employeeId);

        // Verify department exists.
        departmentService.getById(departmentId);

        employee.update(
                departmentId,
                firstName,
                lastName,
                phoneNumber,
                jobTitle
        );

        return employee;
    }

    @Transactional
    public Employee changeStatus(
            UUID employeeId,
            String employmentStatus
    ) {

        Employee employee = getById(employeeId);

        employee.changeStatus(employmentStatus);

        // Offboarding: a terminated or suspended employee can no longer
        // sign in, and every session they have open is ended. Reactivating
        // them restores access.
        if (TERMINATED.equals(employmentStatus) || SUSPENDED.equals(employmentStatus)) {
            userService.disable(employee.getUserId());
            refreshTokenService.revokeAllActiveForUser(employee.getUserId());
        } else if (ACTIVE.equals(employmentStatus)) {
            userService.enable(employee.getUserId());
        }

        return employee;
    }

    @Transactional
    public Employee assignManager(
            UUID employeeId,
            UUID managerId
    ) {

        Employee employee = getById(employeeId);

        if (managerId != null) {

            if (managerId.equals(employeeId)) {
                throw new BusinessRuleException(
                        "An employee cannot be their own manager"
                );
            }

            // Verify the manager exists and reject a cycle
            // (managerId reporting, directly or indirectly, to employeeId).
            UUID currentId = managerId;
            int depth = 0;

            while (currentId != null) {

                if (currentId.equals(employeeId)) {
                    throw new BusinessRuleException(
                            "Assigning this manager would create a reporting cycle"
                    );
                }

                if (++depth > 50) {
                    throw new BusinessRuleException(
                            "Manager chain is too deep to validate"
                    );
                }

                currentId = employeeRepository.findById(currentId)
                        .orElseThrow(() ->
                                new EmployeeNotFoundException(
                                        "Employee not found: " + managerId
                                )
                        )
                        .getManagerId();
            }
        }

        employee.assignManager(managerId);

        // A manager needs the MANAGER role to see and approve their
        // reports' requests; grant it the moment they get a report.
        if (managerId != null) {
            employeeRepository.findById(managerId)
                    .ifPresent(manager -> userService.assignRole(manager.getUserId(), MANAGER_ROLE));
        }

        return employee;
    }
}