package com.unimagdalena.conectaCiudad.services.curator;

import com.unimagdalena.conectaCiudad.Dto.page.PagedResponse;
import com.unimagdalena.conectaCiudad.Dto.project.ProjectDto;
import com.unimagdalena.conectaCiudad.Dto.review.PendingReviewDto;
import com.unimagdalena.conectaCiudad.Dto.review.PendingReviewQueueDto;
import com.unimagdalena.conectaCiudad.Dto.review.ReviewHistoryDto;
import com.unimagdalena.conectaCiudad.Dto.review.ReviewHistoryFilterDto;
import com.unimagdalena.conectaCiudad.enums.ProjectStatus;
import com.unimagdalena.conectaCiudad.services.curator.queue.CuratorQueueService;
import com.unimagdalena.conectaCiudad.services.curator.review.CuratorReviewService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;


@Service
@RequiredArgsConstructor
public class CuratorServiceImpl implements CuratorService {

    private final CuratorReviewService curatorReviewService;
    private final CuratorQueueService curatorQueueService;


    @Override
    public ProjectDto addObservations(Long projectId, Long curatorId, String notes, Long accessId) {
        return curatorReviewService.addObservations(projectId, curatorId, notes, accessId);
    }

    @Override
    public ProjectDto approveProject(Long projectId, Long curatorId, LocalDate votingStartAt, 
                                    LocalDate votingEndAt, Long accessId) {
        return curatorReviewService.approveProject(projectId, curatorId, votingStartAt, votingEndAt, accessId);
    }

    @Override
    public List<ProjectDto> findByCurator(Long curatorId, ProjectStatus status) {
        return curatorReviewService.findByCurator(curatorId, status);
    }


    @Override
    public PendingReviewQueueDto getPendingReviewQueue(Long curatorId) {
        return curatorQueueService.getPendingReviewQueue(curatorId);
    }

    @Override
    public PendingReviewDto getPendingReviewDetails(Long projectId, Long curatorId) {
        return curatorQueueService.getPendingReviewDetails(projectId, curatorId);
    }

    @Override
    public PagedResponse<ReviewHistoryDto> getReviewHistory(Long curatorId, ReviewHistoryFilterDto filters, 
                                                            Pageable pageable) {
        return curatorQueueService.getReviewHistory(curatorId, filters, pageable);
    }
}
