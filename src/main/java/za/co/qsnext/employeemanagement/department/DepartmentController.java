package za.co.qsnext.employeemanagement.department;

import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.Operation;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import za.co.qsnext.employeemanagement.department.dto.CreateDepartmentRequest;
import za.co.qsnext.employeemanagement.department.dto.DepartmentResponse;
import za.co.qsnext.employeemanagement.department.dto.UpdateDepartmentRequest;

import java.util.UUID;

@Tag(name = "Departments", description = "Department CRUD and employee assignment.")
@RestController
@RequestMapping("/api/v1/departments")
public class DepartmentController {

    private final DepartmentService departmentService;

    public DepartmentController(
            DepartmentService departmentService
    ) {
        this.departmentService = departmentService;
    }

    @PreAuthorize("hasAuthority('DEPARTMENT_READ')")
    @Operation(summary = "Get by id")
    @GetMapping("/{departmentId}")
    public ResponseEntity<DepartmentResponse> getById(
            @PathVariable UUID departmentId
    ) {

        Department department =
                departmentService.getById(departmentId);

        return ResponseEntity.ok(
                DepartmentResponse.from(department)
        );
    }

    @PreAuthorize("hasAuthority('DEPARTMENT_CREATE')")
    @Operation(summary = "Create")
    @PostMapping
    public ResponseEntity<DepartmentResponse> create(
            @Valid @RequestBody CreateDepartmentRequest request
    ) {

        Department department =
                departmentService.create(
                        request.name(),
                        request.description()
                );

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(
                        DepartmentResponse.from(department)
                );
    }

    @PreAuthorize("hasAuthority('DEPARTMENT_UPDATE')")
    @Operation(summary = "Update")
    @PutMapping("/{departmentId}")
    public ResponseEntity<DepartmentResponse> update(
            @PathVariable UUID departmentId,
            @Valid @RequestBody UpdateDepartmentRequest request
    ) {

        Department department =
                departmentService.update(
                        departmentId,
                        request.name(),
                        request.description()
                );

        return ResponseEntity.ok(
                DepartmentResponse.from(department)
        );
    }

    @PreAuthorize("hasAuthority('DEPARTMENT_DELETE')")
    @Operation(summary = "Delete")
    @DeleteMapping("/{departmentId}")
    public ResponseEntity<Void> delete(
            @PathVariable UUID departmentId
    ) {

        departmentService.delete(departmentId);

        return ResponseEntity.noContent().build();
    }
}