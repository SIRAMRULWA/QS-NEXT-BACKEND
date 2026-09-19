package za.co.qsnext.employeemanagement.directory;

import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.Operation;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import za.co.qsnext.employeemanagement.directory.dto.DirectoryDepartmentResponse;
import za.co.qsnext.employeemanagement.directory.dto.DirectoryEmployeeResponse;

import java.util.List;
import java.util.UUID;

@Tag(name = "Directory", description = "Company employee directory search.")
@RestController
@RequestMapping("/api/v1/directory")
@PreAuthorize("hasAuthority('DIRECTORY_READ')")
public class DirectoryController {

    private final DirectoryService directoryService;

    public DirectoryController(DirectoryService directoryService) {
        this.directoryService = directoryService;
    }

    @Operation(summary = "Search employees")
    @GetMapping("/employees")
    public ResponseEntity<Page<DirectoryEmployeeResponse>> searchEmployees(
            @RequestParam(required = false) String query,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return ResponseEntity.ok(
                directoryService.searchEmployees(query, createPageable(page, size, "lastName"))
        );
    }

    @Operation(summary = "Get employee profile")
    @GetMapping("/employees/{employeeId}")
    public ResponseEntity<DirectoryEmployeeResponse> getEmployeeProfile(
            @PathVariable UUID employeeId
    ) {
        return ResponseEntity.ok(directoryService.getEmployeeProfile(employeeId));
    }

    @Operation(summary = "Get manager chain")
    @GetMapping("/employees/{employeeId}/manager-chain")
    public ResponseEntity<List<DirectoryEmployeeResponse>> getManagerChain(
            @PathVariable UUID employeeId
    ) {
        return ResponseEntity.ok(directoryService.getManagerChain(employeeId));
    }

    @Operation(summary = "Get direct reports")
    @GetMapping("/employees/{employeeId}/direct-reports")
    public ResponseEntity<List<DirectoryEmployeeResponse>> getDirectReports(
            @PathVariable UUID employeeId
    ) {
        return ResponseEntity.ok(directoryService.getDirectReports(employeeId));
    }

    @Operation(summary = "Search departments")
    @GetMapping("/departments")
    public ResponseEntity<Page<DirectoryDepartmentResponse>> searchDepartments(
            @RequestParam(required = false) String query,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return ResponseEntity.ok(
                directoryService.searchDepartments(query, createPageable(page, size, "name"))
        );
    }

    private Pageable createPageable(int page, int size, String sortProperty) {

        int safePage = Math.max(page, 0);
        int safeSize = Math.min(Math.max(size, 1), 100);

        return PageRequest.of(safePage, safeSize, Sort.by(Sort.Direction.ASC, sortProperty));
    }
}
