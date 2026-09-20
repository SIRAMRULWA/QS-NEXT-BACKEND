package za.co.qsnext.employeemanagement.learning;

import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.Operation;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import za.co.qsnext.employeemanagement.learning.dto.AssignSkillRequest;
import za.co.qsnext.employeemanagement.learning.dto.CreateSkillRequest;
import za.co.qsnext.employeemanagement.learning.dto.EmployeeSkillResponse;
import za.co.qsnext.employeemanagement.learning.dto.SkillResponse;
import za.co.qsnext.employeemanagement.security.CustomUserDetails;

import java.util.List;
import java.util.UUID;

@Tag(name = "Skills", description = "Skill catalog management.")
@RestController
@RequestMapping("/api/v1/skills")
public class SkillController {

    private static final String MANAGE_AUTHORITY = "LEARNING_MANAGE";

    private final SkillService skillService;

    public SkillController(SkillService skillService) {
        this.skillService = skillService;
    }

    @PreAuthorize("hasAuthority('LEARNING_MANAGE')")
    @Operation(summary = "Create skill")
    @PostMapping
    public ResponseEntity<SkillResponse> createSkill(@Valid @RequestBody CreateSkillRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(skillService.createSkill(request.name(), request.category()));
    }

    @PreAuthorize("hasAnyAuthority('LEARNING_MANAGE', 'LEARNING_READ')")
    @Operation(summary = "Get all skills")
    @GetMapping
    public ResponseEntity<List<SkillResponse>> getAllSkills() {
        return ResponseEntity.ok(skillService.getAllSkills());
    }

    @PreAuthorize("hasAnyAuthority('LEARNING_MANAGE', 'LEARNING_READ')")
    @Operation(summary = "Assign skill to employee")
    @PostMapping("/employees/{employeeId}")
    public ResponseEntity<EmployeeSkillResponse> assignSkillToEmployee(
            Authentication authentication,
            @PathVariable UUID employeeId,
            @Valid @RequestBody AssignSkillRequest request
    ) {
        CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();

        return ResponseEntity.status(HttpStatus.CREATED).body(skillService.assignSkillToEmployee(
                employeeId, request.skillId(), request.proficiencyLevel(),
                userDetails.getUserId(), canManage(authentication)
        ));
    }

    @PreAuthorize("hasAnyAuthority('LEARNING_MANAGE', 'LEARNING_READ')")
    @Operation(summary = "Get employee skills")
    @GetMapping("/employees/{employeeId}")
    public ResponseEntity<List<EmployeeSkillResponse>> getEmployeeSkills(
            Authentication authentication,
            @PathVariable UUID employeeId
    ) {
        CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();

        return ResponseEntity.ok(skillService.getEmployeeSkills(
                employeeId, userDetails.getUserId(), canManage(authentication)
        ));
    }

    private boolean canManage(Authentication authentication) {
        return authentication.getAuthorities().stream()
                .anyMatch(authority -> MANAGE_AUTHORITY.equals(authority.getAuthority()));
    }
}
