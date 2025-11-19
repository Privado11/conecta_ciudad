package com.unimagdalena.conectaCiudad.services.dashboard;

import com.unimagdalena.conectaCiudad.Dto.dashboard.*;
import com.unimagdalena.conectaCiudad.entities.Action;
import com.unimagdalena.conectaCiudad.enums.ActionResult;
import com.unimagdalena.conectaCiudad.enums.ProjectStatus;
import com.unimagdalena.conectaCiudad.repositories.ActionRepository;
import com.unimagdalena.conectaCiudad.repositories.ProjectRepository;
import com.unimagdalena.conectaCiudad.repositories.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DashboardServiceImpl implements DashboardService {

    private final UserRepository userRepository;
    private final ProjectRepository projectRepository;
    private final ActionRepository actionRepository;

    private static final Map<String, String> STATUS_COLORS = Map.of(
        "DRAFT", "#94a3b8",
        "PENDING_REVIEW", "#fbbf24",
        "IN_REVIEW", "#60a5fa",
        "RETURNED_WITH_OBSERVATIONS", "#f97316",
        "READY_TO_PUBLISH", "#a78bfa",
        "PUBLISHED", "#34d399",
        "REJECTED", "#f87171",
        "VOTING_CLOSED", "#a78bfa"
    );

    private static final Map<String, String> ROLE_COLORS = Map.of(
        "CIUDADANO", "#60a5fa",
        "LIDER_COMUNITARIO", "#34d399",
        "CURATOR", "#a78bfa",
        "ADMIN", "#f87171"
    );

    @Override
    public DashboardStatsDto getDashboardStats() {
        long totalUsers = userRepository.count();
        long activeUsers = userRepository.countByActive(true);
        long totalProjects = projectRepository.count();
        long activeVotaciones = projectRepository.countActiveVotations();
        long newUsersThisMonth = userRepository.countUsersCreatedThisMonth();
        
        double participationRate = totalUsers > 0 
            ? Math.round((activeUsers * 100.0 / totalUsers) * 10.0) / 10.0 
            : 0.0;

        return new DashboardStatsDto(
            totalUsers,
            activeUsers,
            totalProjects,
            activeVotaciones,
            participationRate,
            newUsersThisMonth
        );
    }

    @Override
    public List<ProjectStatusDataDto> getProjectStatusDistribution() {
        List<ProjectStatusDataDto> distribution = new ArrayList<>();
        
        for (ProjectStatus status : ProjectStatus.values()) {
            long count = projectRepository.countByStatus(status);
            if (count > 0) {
                String color = STATUS_COLORS.getOrDefault(status.name(), "#6b7280");
                distribution.add(new ProjectStatusDataDto(
                    status.name(),
                    count,
                    color
                ));
            }
        }
        
        return distribution;
    }

    @Override
    public List<ProjectTrendDataDto> getProjectTrend() {
        // Get data for the last 6 months
        OffsetDateTime sixMonthsAgo = OffsetDateTime.now().minusMonths(6);
        List<Object[]> monthlyData = projectRepository.countProjectsByMonth(sixMonthsAgo);
        
        // Convert to DTOs and format month names
        Map<String, Long> dataMap = new HashMap<>();
        for (Object[] row : monthlyData) {
            String yearMonth = (String) row[0]; // Format: YYYY-MM
            Long count = ((Number) row[1]).longValue();
            
            // Parse and format to short month name (e.g., "Nov")
            String[] parts = yearMonth.split("-");
            int month = Integer.parseInt(parts[1]);
            String monthName = java.time.Month.of(month)
                .getDisplayName(TextStyle.SHORT, new Locale("es", "ES"));
            
            dataMap.put(monthName, count);
        }
        
        // Create list for last 6 months in order
        List<ProjectTrendDataDto> trend = new ArrayList<>();
        OffsetDateTime current = OffsetDateTime.now();
        
        for (int i = 5; i >= 0; i--) {
            OffsetDateTime monthDate = current.minusMonths(i);
            String monthName = monthDate.getMonth()
                .getDisplayName(TextStyle.SHORT, new Locale("es", "ES"));
            
            long count = dataMap.getOrDefault(monthName, 0L);
            trend.add(new ProjectTrendDataDto(monthName, count));
        }
        
        return trend;
    }

    @Override
    public List<RecentActivityDto> getRecentActivities(int limit) {
        Page<Action> recentActions = actionRepository.findRecentActions(
            PageRequest.of(0, limit)
        );
        
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
        
        return recentActions.getContent().stream()
            .map(action -> {
                String userName = action.getUser() != null 
                    ? action.getUser().getName() 
                    : "Sistema";
                
                String status = mapActionResultToStatus(action.getResult());
                
                String timestamp = action.getActionAt() != null 
                    ? action.getActionAt().format(formatter)
                    : "";
                
                return new RecentActivityDto(
                    action.getId(),
                    userName,
                    action.getDescription() != null ? action.getDescription() : action.getActionType(),
                    timestamp,
                    status
                );
            })
            .collect(Collectors.toList());
    }

    @Override
    public List<VotingActivityDataDto> getVotingActivity() {
        List<com.unimagdalena.conectaCiudad.entities.Project> activeVotations = 
            projectRepository.findActiveVotations();
        
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        
        return activeVotations.stream()
            .map(project -> new VotingActivityDataDto(
                project.getName(),
                0L, // Placeholder for votes count - will be implemented when voting system is ready
                project.getVotingEndAt() != null 
                    ? project.getVotingEndAt().format(formatter)
                    : ""
            ))
            .collect(Collectors.toList());
    }

    @Override
    public List<UserRoleDistributionDto> getUserRoleDistribution() {
        List<Object[]> roleData = userRepository.countUsersByRole();
        
        return roleData.stream()
            .map(row -> {
                String roleName = (String) row[0];
                Long count = ((Number) row[1]).longValue();
                String color = ROLE_COLORS.getOrDefault(roleName, "#6b7280");
                
                return new UserRoleDistributionDto(roleName, count, color);
            })
            .collect(Collectors.toList());
    }

    private String mapActionResultToStatus(ActionResult result) {
        if (result == null) {
            return "success";
        }
        
        return switch (result) {
            case SUCCESS -> "success";
            case FAILED -> "error";
            case PARTIAL -> "warning";
        };
    }
}
