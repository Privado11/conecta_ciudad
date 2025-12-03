package com.unimagdalena.conectaCiudad.services.admin.dashboard;

import com.unimagdalena.conectaCiudad.Dto.dashboard.*;

import java.util.List;

public interface DashboardService {
    
    DashboardStatsDto getDashboardStats();
    
    List<ProjectStatusDataDto> getProjectStatusDistribution();
    

    List<ProjectTrendDataDto> getProjectTrend();
    

    List<RecentActivityDto> getRecentActivities(int limit);
    
 
    List<VotingActivityDataDto> getVotingActivity(String token);
    
    List<UserRoleDistributionDto> getUserRoleDistribution();
}
