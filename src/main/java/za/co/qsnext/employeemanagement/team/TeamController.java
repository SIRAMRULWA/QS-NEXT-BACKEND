package za.co.qsnext.employeemanagement.team;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import za.co.qsnext.employeemanagement.team.dto.TeamOverviewResponse;

/**
 * A line manager's view of their direct reports: who they are and what is
 * waiting for the manager's approval. Every list is scoped to the caller's
 * own direct reports, never the wider organization.
 */
@RestController
@RequestMapping("/api/v1/team")
@Tag(name = "Team", description = "A manager's direct reports and their pending requests.")
@PreAuthorize("hasAuthority('TEAM_READ')")
public class TeamController {

    private final TeamService teamService;

    public TeamController(TeamService teamService) {
        this.teamService = teamService;
    }

    @Operation(summary = "My team and pending approvals")
    @GetMapping
    public ResponseEntity<TeamOverviewResponse> getOverview(Authentication authentication) {
        return ResponseEntity.ok(teamService.getOverview(authentication));
    }
}
