package za.co.qsnext.employeemanagement.analytics;

import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.Operation;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import za.co.qsnext.employeemanagement.analytics.dto.AttendanceTrendResponse;
import za.co.qsnext.employeemanagement.analytics.dto.DepartmentDistributionResponse;
import za.co.qsnext.employeemanagement.analytics.dto.EngagementAnalyticsResponse;
import za.co.qsnext.employeemanagement.analytics.dto.ExpenseTrendAnalyticsResponse;
import za.co.qsnext.employeemanagement.analytics.dto.HeadcountAnalyticsResponse;
import za.co.qsnext.employeemanagement.analytics.dto.LeaveTrendResponse;
import za.co.qsnext.employeemanagement.analytics.dto.LearningActivityAnalyticsResponse;
import za.co.qsnext.employeemanagement.analytics.dto.OvertimeAnalyticsResponse;
import za.co.qsnext.employeemanagement.analytics.dto.PayrollSummaryAnalyticsResponse;
import za.co.qsnext.employeemanagement.analytics.dto.PerformanceSummaryAnalyticsResponse;
import za.co.qsnext.employeemanagement.analytics.dto.RecruitmentFunnelResponse;
import za.co.qsnext.employeemanagement.analytics.dto.TurnoverAnalyticsResponse;

import java.time.LocalDate;
import java.util.UUID;

@Tag(name = "Analytics", description = "Org-wide HR analytics dashboards backed by real SQL aggregation.")
@RestController
@RequestMapping("/api/v1/analytics")
public class AnalyticsController {

    private final AnalyticsService analyticsService;

    public AnalyticsController(AnalyticsService analyticsService) {
        this.analyticsService = analyticsService;
    }

    @PreAuthorize("hasAuthority('ANALYTICS_READ')")
    @Operation(summary = "Get headcount")
    @GetMapping("/headcount")
    public ResponseEntity<HeadcountAnalyticsResponse> getHeadcount() {
        return ResponseEntity.ok(analyticsService.getHeadcountAnalytics());
    }

    @PreAuthorize("hasAuthority('ANALYTICS_READ')")
    @Operation(summary = "Get department distribution")
    @GetMapping("/departments")
    public ResponseEntity<DepartmentDistributionResponse> getDepartmentDistribution() {
        return ResponseEntity.ok(analyticsService.getDepartmentDistribution());
    }

    @PreAuthorize("hasAuthority('ANALYTICS_READ')")
    @Operation(summary = "Get turnover")
    @GetMapping("/turnover")
    public ResponseEntity<TurnoverAnalyticsResponse> getTurnover(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to
    ) {
        return ResponseEntity.ok(analyticsService.getTurnoverAnalytics(from, to));
    }

    @PreAuthorize("hasAuthority('ANALYTICS_READ')")
    @Operation(summary = "Get attendance trend")
    @GetMapping("/attendance")
    public ResponseEntity<AttendanceTrendResponse> getAttendanceTrend(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to
    ) {
        return ResponseEntity.ok(analyticsService.getAttendanceTrend(from, to));
    }

    @PreAuthorize("hasAuthority('ANALYTICS_READ')")
    @Operation(summary = "Get leave trend")
    @GetMapping("/leave")
    public ResponseEntity<LeaveTrendResponse> getLeaveTrend(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to
    ) {
        return ResponseEntity.ok(analyticsService.getLeaveTrend(from, to));
    }

    @PreAuthorize("hasAuthority('ANALYTICS_READ')")
    @Operation(summary = "Get overtime")
    @GetMapping("/overtime")
    public ResponseEntity<OvertimeAnalyticsResponse> getOvertime(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to
    ) {
        return ResponseEntity.ok(analyticsService.getOvertimeAnalytics(from, to));
    }

    @PreAuthorize("hasAuthority('ANALYTICS_READ')")
    @Operation(summary = "Get recruitment funnel")
    @GetMapping("/recruitment-funnel")
    public ResponseEntity<RecruitmentFunnelResponse> getRecruitmentFunnel() {
        return ResponseEntity.ok(analyticsService.getRecruitmentFunnel());
    }

    @PreAuthorize("hasAuthority('ANALYTICS_READ')")
    @Operation(summary = "Get payroll summary")
    @GetMapping("/payroll/{payrollRunId}")
    public ResponseEntity<PayrollSummaryAnalyticsResponse> getPayrollSummary(@PathVariable UUID payrollRunId) {
        return ResponseEntity.ok(analyticsService.getPayrollSummary(payrollRunId));
    }

    @PreAuthorize("hasAuthority('ANALYTICS_READ')")
    @Operation(summary = "Get expense trend")
    @GetMapping("/expenses")
    public ResponseEntity<ExpenseTrendAnalyticsResponse> getExpenseTrend(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to
    ) {
        return ResponseEntity.ok(analyticsService.getExpenseTrend(from, to));
    }

    @PreAuthorize("hasAuthority('ANALYTICS_READ')")
    @Operation(summary = "Get performance summary")
    @GetMapping("/performance")
    public ResponseEntity<PerformanceSummaryAnalyticsResponse> getPerformanceSummary() {
        return ResponseEntity.ok(analyticsService.getPerformanceSummary());
    }

    @PreAuthorize("hasAuthority('ANALYTICS_READ')")
    @Operation(summary = "Get learning activity")
    @GetMapping("/learning")
    public ResponseEntity<LearningActivityAnalyticsResponse> getLearningActivity() {
        return ResponseEntity.ok(analyticsService.getLearningActivity());
    }

    @PreAuthorize("hasAuthority('ANALYTICS_READ')")
    @Operation(summary = "Get engagement")
    @GetMapping("/engagement")
    public ResponseEntity<EngagementAnalyticsResponse> getEngagement() {
        return ResponseEntity.ok(analyticsService.getEngagementAnalytics());
    }
}
