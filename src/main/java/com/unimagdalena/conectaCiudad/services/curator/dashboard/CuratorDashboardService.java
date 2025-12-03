package com.unimagdalena.conectaCiudad.services.curator.dashboard;

import com.unimagdalena.conectaCiudad.Dto.curator.*;
import java.util.List;

public interface CuratorDashboardService {
    
    CuratorDashboardStatsDto getCuratorDashboardStats(Long curatorId);
    
    List<CuratorProjectStatusDataDto> getCuratorProjectStatusDistribution(Long curatorId);
    
    List<CuratorReviewTrendDataDto> getCuratorReviewTrend(Long curatorId);
    
    List<UrgentProjectDataDto> getUrgentProjects(Long curatorId, int limit);
    
    List<CuratorRecentActivityDto> getCuratorRecentActivities(Long curatorId, int limit);
}
