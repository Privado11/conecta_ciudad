package com.unimagdalena.conectaCiudad.controllers.curator;

import com.unimagdalena.conectaCiudad.Dto.curator.*;
import com.unimagdalena.conectaCiudad.services.curator.dashboard.CuratorDashboardService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/curator/dashboard")
@RequiredArgsConstructor
@Tag(name = "Curator Dashboard", description = "Dashboard statistics and metrics for curators")
public class CuratorDashboardController {

    private final CuratorDashboardService curatorDashboardService;

    @GetMapping("/stats")
    @Operation(summary = "Get curator dashboard statistics")
    public ResponseEntity<CuratorDashboardStatsDto> getCuratorDashboardStats() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        Long curatorId = (Long) auth.getDetails();
        return ResponseEntity.ok(curatorDashboardService.getCuratorDashboardStats(curatorId));
    }

    @GetMapping("/project-status")
    @Operation(summary = "Get project status distribution")
    public ResponseEntity<List<CuratorProjectStatusDataDto>> getCuratorProjectStatusDistribution() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        Long curatorId = (Long) auth.getDetails();
        return ResponseEntity.ok(curatorDashboardService.getCuratorProjectStatusDistribution(curatorId));
    }

    @GetMapping("/review-trend")
    @Operation(summary = "Get review trend (last 6 months)")
    public ResponseEntity<List<CuratorReviewTrendDataDto>> getCuratorReviewTrend() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        Long curatorId = (Long) auth.getDetails();
        return ResponseEntity.ok(curatorDashboardService.getCuratorReviewTrend(curatorId));
    }

    @GetMapping("/urgent-projects")
    @Operation(summary = "Get urgent projects requiring review")
    public ResponseEntity<List<UrgentProjectDataDto>> getUrgentProjects(
            @Parameter(description = "Maximum number of urgent projects to return")
            @RequestParam(defaultValue = "5") int limit) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        Long curatorId = (Long) auth.getDetails();
        return ResponseEntity.ok(curatorDashboardService.getUrgentProjects(curatorId, limit));
    }

    @GetMapping("/recent-activities")
    @Operation(summary = "Get recent curator activities")
    public ResponseEntity<List<CuratorRecentActivityDto>> getCuratorRecentActivities(
            @Parameter(description = "Maximum number of activities to return")
            @RequestParam(defaultValue = "8") int limit) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        Long curatorId = (Long) auth.getDetails();
        return ResponseEntity.ok(curatorDashboardService.getCuratorRecentActivities(curatorId, limit));
    }
}
