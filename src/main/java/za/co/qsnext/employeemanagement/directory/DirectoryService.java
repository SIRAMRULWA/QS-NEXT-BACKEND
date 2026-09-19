package za.co.qsnext.employeemanagement.directory;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import za.co.qsnext.employeemanagement.department.Department;
import za.co.qsnext.employeemanagement.department.DepartmentRepository;
import za.co.qsnext.employeemanagement.directory.dto.DirectoryDepartmentResponse;
import za.co.qsnext.employeemanagement.directory.dto.DirectoryEmployeeResponse;
import za.co.qsnext.employeemanagement.employee.Employee;
import za.co.qsnext.employeemanagement.employee.EmployeeRepository;
import za.co.qsnext.employeemanagement.exception.EmployeeNotFoundException;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Read-only, company-wide view over employees/departments. Deliberately
 * has no entities of its own - it is a search/lookup layer, not a source
 * of truth.
 */
@Service
@Transactional(readOnly = true)
public class DirectoryService {

    private static final int MAX_MANAGER_CHAIN_DEPTH = 50;

    private final EmployeeRepository employeeRepository;
    private final DepartmentRepository departmentRepository;

    public DirectoryService(
            EmployeeRepository employeeRepository,
            DepartmentRepository departmentRepository
    ) {
        this.employeeRepository = employeeRepository;
        this.departmentRepository = departmentRepository;
    }

    public Page<DirectoryEmployeeResponse> searchEmployees(
            String query,
            Pageable pageable
    ) {
        Page<Employee> employees = employeeRepository.search(
                (query == null || query.isBlank()) ? null : query.trim(),
                pageable
        );

        return employees.map(employee -> toResponse(employee, employees.getContent()));
    }

    public DirectoryEmployeeResponse getEmployeeProfile(UUID employeeId) {

        Employee employee = employeeRepository.findById(employeeId)
                .orElseThrow(() ->
                        new EmployeeNotFoundException("Employee not found: " + employeeId)
                );

        return toResponse(employee, List.of(employee));
    }

    /**
     * The employee's chain of managers, closest first. Capped so a data
     * error (an accidental cycle slipping past assignManager's own check,
     * e.g. via a direct DB edit) can't loop forever.
     */
    public List<DirectoryEmployeeResponse> getManagerChain(UUID employeeId) {

        Employee employee = employeeRepository.findById(employeeId)
                .orElseThrow(() ->
                        new EmployeeNotFoundException("Employee not found: " + employeeId)
                );

        List<DirectoryEmployeeResponse> chain = new ArrayList<>();
        UUID currentManagerId = employee.getManagerId();
        int depth = 0;

        while (currentManagerId != null && depth < MAX_MANAGER_CHAIN_DEPTH) {

            Employee manager = employeeRepository.findById(currentManagerId).orElse(null);

            if (manager == null) {
                break;
            }

            chain.add(toResponse(manager, List.of(manager)));
            currentManagerId = manager.getManagerId();
            depth++;
        }

        return chain;
    }

    public List<DirectoryEmployeeResponse> getDirectReports(UUID employeeId) {

        return employeeRepository.findByManagerId(employeeId).stream()
                .map(employee -> toResponse(employee, List.of(employee)))
                .toList();
    }

    public Page<DirectoryDepartmentResponse> searchDepartments(
            String query,
            Pageable pageable
    ) {
        String safeQuery = (query == null) ? "" : query.trim();

        return departmentRepository
                .findByNameContainingIgnoreCase(safeQuery, pageable)
                .map(DirectoryDepartmentResponse::from);
    }

    /**
     * Batches department/manager lookups for a page of employees rather
     * than fetching them one at a time per row.
     */
    private DirectoryEmployeeResponse toResponse(Employee employee, List<Employee> batch) {

        Map<UUID, String> departmentNames = departmentNamesFor(batch);
        Map<UUID, String> managerNames = managerNamesFor(batch);

        return DirectoryEmployeeResponse.from(
                employee,
                departmentNames.get(employee.getDepartmentId()),
                employee.getManagerId() == null ? null : managerNames.get(employee.getManagerId())
        );
    }

    private Map<UUID, String> departmentNamesFor(List<Employee> employees) {

        List<UUID> departmentIds = employees.stream()
                .map(Employee::getDepartmentId)
                .distinct()
                .toList();

        return departmentRepository.findAllById(departmentIds).stream()
                .collect(Collectors.toMap(Department::getId, Department::getName));
    }

    private Map<UUID, String> managerNamesFor(List<Employee> employees) {

        List<UUID> managerIds = employees.stream()
                .map(Employee::getManagerId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();

        if (managerIds.isEmpty()) {
            return Map.of();
        }

        return employeeRepository.findAllById(managerIds).stream()
                .collect(Collectors.toMap(
                        Employee::getId,
                        manager -> manager.getFirstName() + " " + manager.getLastName()
                ));
    }
}
