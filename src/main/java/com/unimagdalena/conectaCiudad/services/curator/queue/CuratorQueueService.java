package com.unimagdalena.conectaCiudad.services.curator.queue;

import com.unimagdalena.conectaCiudad.Dto.page.PagedResponse;
import com.unimagdalena.conectaCiudad.Dto.review.PendingReviewDto;
import com.unimagdalena.conectaCiudad.Dto.review.PendingReviewQueueDto;
import com.unimagdalena.conectaCiudad.Dto.review.ReviewHistoryDto;
import com.unimagdalena.conectaCiudad.Dto.review.ReviewHistoryFilterDto;
import org.springframework.data.domain.Pageable;


public interface CuratorQueueService {

    PendingReviewQueueDto getPendingReviewQueue(Long curatorId);


    PendingReviewDto getPendingReviewDetails(Long projectId, Long curatorId);

    PagedResponse<ReviewHistoryDto> getReviewHistory(Long curatorId, ReviewHistoryFilterDto filters, 
                                                     Pageable pageable);
}
