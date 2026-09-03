package za.co.qsnext.employeemanagement.employee;

import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import za.co.qsnext.employeemanagement.employee.dto.CreateEmployeeRequest;
import za.co.qsnext.employeemanagement.employee.dto.EmployeeResponse;
import za.co.qsnext.employeemanagement.employee.dto.UpdateEmployeeRequest;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/employees")
public class EmployeeController {

    private final EmployeeService employeeService;

    public EmployeeController(EmployeeService employeeService) {
        this.employeeService = employeeService;
    }

    @GetMapping
    public ResponseEntity<Page<EmployeeResponse>> getAll(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {

        Pageable pageable = createPageable(page, size);

        Page<EmployeeResponse> response =
                employeeService.getAll(pageable)
                        .map(EmployeeResponse::from);

        return ResponseEntity.ok(response);
    }

    @GetMapping("/{employeeId}")
    public ResponseEntity<EmployeeResponse> getById(
            @PathVariable UUID employeeId
    ) {

        Employee employee = employeeService.getById(employeeId);

        return ResponseEntity.ok(
                EmployeeResponse.from(employee)
        );
    }

    @GetMapping("/department/{departmentId}")
    public ResponseEntity<Page<EmployeeResponse>> getByDepartment(
            @PathVariable UUID departmentId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {

        Pageable pageable = createPageable(page, size);

        Page<EmployeeResponse> response =
                employeeService
                        .getByDepartment(departmentId, pageable)
                        .map(EmployeeResponse::from);

        return ResponseEntity.ok(response);
    }

    @GetMapping("/status/{status}")
    public ResponseEntity<Page<EmployeeResponse>> getByStatus(
            @PathVariable String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {

        Pageable pageable = createPageable(page, size);

        Page<EmployeeResponse> response =
                employeeService
                        .getByStatus(status, pageable)
                        .map(EmployeeResponse::from);

        return ResponseEntity.ok(response);
    }

    @GetMapping("/search")
    public ResponseEntity<Page<EmployeeResponse>> search(
            @RequestParam String lastName,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {

        Pageable pageable = createPageable(page, size);

        Page<EmployeeResponse> response =
                employeeService
                        .searchByLastName(lastName, pageable)
                        .map(EmployeeResponse::from);

        return ResponseEntity.ok(response);
    }

    @PostMapping
    public ResponseEntity<EmployeeResponse> create(
            @Valid @RequestBody CreateEmployeeRequest request
    ) {

        Employee employee = employeeService.create(
                request.userId(),
                request.departmentId(),
                request.employeeNumber(),
                request.firstName(),
                request.lastName(),
                request.phoneNumber(),
                request.jobTitle(),
                request.hireDate()
        );

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(EmployeeResponse.from(employee));
    }

    @PutMapping("/{employeeId}")
    public ResponseEntity<EmployeeResponse> update(
            @PathVariable UUID employeeId,
            @Valid @RequestBody UpdateEmployeeRequest request
    ) {

        Employee employee = employeeService.update(
                employeeId,
                request.departmentId(),
                request.firstName(),
                request.lastName(),
                request.phoneNumber(),
                request.jobTitle()
        );

        return ResponseEntity.ok(
                EmployeeResponse.from(employee)
        );
    }

    @PatchMapping("/{employeeId}/status")
    public ResponseEntity<EmployeeResponse> changeStatus(
            @PathVariable UUID employeeId,
            @RequestParam String status
    ) {

        Employee employee =
                employeeService.changeStatus(
                        employeeId,
                        status
                );

        return ResponseEntity.ok(
                EmployeeResponse.from(employee)
        );
    }

    private Pageable createPageable(int page, int size) {

        if (page < 0) {
            page = 0;
        }

        if (size < 1 || size > 100) {
            size = 20;
        }

        return PageRequest.of(
                page,
                size,
                Sort.by(
                        Sort.Direction.ASC,
                        "lastName"
                )
        );
    }
}