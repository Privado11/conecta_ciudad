package com.unimagdalena.conectaCiudad.services.admin.statistics;

import com.unimagdalena.conectaCiudad.Dto.page.Statistics;
import com.unimagdalena.conectaCiudad.Dto.project.ProjectDto;

public interface StatisticsService {

    Statistics<ProjectDto> getGlobalStatistics();
}
