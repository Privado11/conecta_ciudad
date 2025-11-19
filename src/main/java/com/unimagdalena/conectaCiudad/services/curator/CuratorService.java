package com.unimagdalena.conectaCiudad.services.curator;

import com.unimagdalena.conectaCiudad.Dto.page.PagedResponse;
import com.unimagdalena.conectaCiudad.Dto.project.ProjectDto;
import com.unimagdalena.conectaCiudad.Dto.review.PendingReviewDto;
import com.unimagdalena.conectaCiudad.Dto.review.PendingReviewQueueDto;
import com.unimagdalena.conectaCiudad.Dto.review.ReviewHistoryDto;
import com.unimagdalena.conectaCiudad.Dto.review.ReviewHistoryFilterDto;
import com.unimagdalena.conectaCiudad.enums.ProjectStatus;

import java.time.LocalDate;
import java.util.List;

import org.springframework.data.domain.Pageable;

public interface CuratorService {
    
    ProjectDto addObservations(Long projectId, Long curatorId, String notes, Long accessId);
    
    ProjectDto approveProject(Long projectId, Long curatorId, LocalDate votingStartAt,
                              LocalDate votingEndAt, Long accessId);
    
    List<ProjectDto> findByCurator(Long curatorId, ProjectStatus status);
    
    PendingReviewQueueDto getPendingReviewQueue(Long curatorId);
    
    PendingReviewDto getPendingReviewDetails(Long projectId, Long curatorId);
    
   PagedResponse<ReviewHistoryDto> getReviewHistory(
        Long curatorId, 
        ReviewHistoryFilterDto filters, 
        Pageable pageable
    );
}