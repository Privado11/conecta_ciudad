package com.unimagdalena.conectaCiudad.controllers.admin;

import com.unimagdalena.conectaCiudad.Dto.dashboard.*;
import com.unimagdalena.conectaCiudad.services.admin.dashboard.DashboardService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/admin/dashboard")
@RequiredArgsConstructor
@Tag(name = "Admin Dashboard", description = "Dashboard statistics and metrics for administrators")
public class DashboardAdminController {

    private final DashboardService dashboardService;

    @GetMapping("/stats")
    @Operation(summary = "Get dashboard statistics")
    public ResponseEntity<DashboardStatsDto> getDashboardStats() {
        return ResponseEntity.ok(dashboardService.getDashboardStats());
    }

    @GetMapping("/project-status")
    @Operation(summary = "Get project status distribution")
    public ResponseEntity<List<ProjectStatusDataDto>> getProjectStatusDistribution() {
        return ResponseEntity.ok(dashboardService.getProjectStatusDistribution());
    }

    @GetMapping("/project-trend")
    @Operation(summary = "Get project creation trend (last 6 months)")
    public ResponseEntity<List<ProjectTrendDataDto>> getProjectTrend() {
        return ResponseEntity.ok(dashboardService.getProjectTrend());
    }


    @GetMapping("/recent-activities")
    @Operation(summary = "Get recent system activities")
    public ResponseEntity<List<RecentActivityDto>> getRecentActivities(
            @Parameter(description = "Number of activities to retrieve")
            @RequestParam(defaultValue = "10") int limit) {
        return ResponseEntity.ok(dashboardService.getRecentActivities(limit));
    }

    @GetMapping("/user-role-distribution")
    @Operation(summary = "Get user distribution by role")
    public ResponseEntity<List<UserRoleDistributionDto>> getUserRoleDistribution() {
        return ResponseEntity.ok(dashboardService.getUserRoleDistribution());
    }

    @GetMapping("/voting-activity")
    @Operation(summary = "Get active voting activity")
    public ResponseEntity<List<VotingActivityDataDto>> getVotingActivity(HttpServletRequest request) {
        String token = extractToken(request);
        return ResponseEntity.ok(dashboardService.getVotingActivity(token));
    }

    private String extractToken(HttpServletRequest request) {
        String authHeader = request.getHeader("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            return authHeader.substring(7);
        }
        return null;
    }
}
