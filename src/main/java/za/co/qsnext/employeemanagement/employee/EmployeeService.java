package za.co.qsnext.employeemanagement.employee;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import za.co.qsnext.employeemanagement.department.DepartmentService;
import za.co.qsnext.employeemanagement.exception.BusinessRuleException;
import za.co.qsnext.employeemanagement.exception.DuplicateResourceException;
import za.co.qsnext.employeemanagement.exception.EmployeeNotFoundException;
import za.co.qsnext.employeemanagement.user.UserService;

import java.time.LocalDate;
import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class EmployeeService {

    private final EmployeeRepository employeeRepository;
    private final UserService userService;
    private final DepartmentService departmentService;

    public EmployeeService(
            EmployeeRepository employeeRepository,
            UserService userService,
            DepartmentService departmentService
    ) {
        this.employeeRepository = employeeRepository;
        this.userService = userService;
        this.departmentService = departmentService;
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
        userService.getById(userId);

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

        return employeeRepository.save(employee);
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

        return employee;
    }
}