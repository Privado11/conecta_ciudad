package com.unimagdalena.conectaCiudad.services.dashboard;

import com.unimagdalena.conectaCiudad.Dto.dashboard.*;

import java.util.List;

public interface DashboardService {
    
    DashboardStatsDto getDashboardStats();
    
    List<ProjectStatusDataDto> getProjectStatusDistribution();
    

    List<ProjectTrendDataDto> getProjectTrend();
    

    List<RecentActivityDto> getRecentActivities(int limit);
    
 
    List<VotingActivityDataDto> getVotingActivity();
    
    List<UserRoleDistributionDto> getUserRoleDistribution();
}
