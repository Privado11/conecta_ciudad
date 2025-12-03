package com.unimagdalena.conectaCiudad.controllers.curator;


import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import com.unimagdalena.conectaCiudad.Dto.page.PagedResponse;
import com.unimagdalena.conectaCiudad.Dto.project.ProjectApprovalDto;
import com.unimagdalena.conectaCiudad.Dto.project.ProjectDto;
import com.unimagdalena.conectaCiudad.Dto.project.ReviewNotesDto;
import com.unimagdalena.conectaCiudad.Dto.review.PendingReviewDto;
import com.unimagdalena.conectaCiudad.Dto.review.PendingReviewQueueDto;
import com.unimagdalena.conectaCiudad.Dto.review.ReviewHistoryDto;
import com.unimagdalena.conectaCiudad.Dto.review.ReviewHistoryFilterDto;
import com.unimagdalena.conectaCiudad.enums.ProjectStatus;
import com.unimagdalena.conectaCiudad.services.curator.CuratorService;

import java.time.OffsetDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/v1/curator")
@RequiredArgsConstructor
@Tag(name = "Curator Reviews", description = "Project review operations for curators")
public class CuratorReviewController {

    private final CuratorService curatorService;

    @PreAuthorize("hasAuthority('PROJECT_ADD_OBSERVATIONS')")
    @PutMapping("/project/{id}/observations")
    @Operation(summary = "Add observations to project during review")
    public ResponseEntity<ProjectDto> addObservations(
            @PathVariable Long id,
            @Valid @RequestBody ReviewNotesDto body,
            HttpServletRequest request) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        Long curatorId = (Long) auth.getDetails();
        Long accessId = (Long) request.getAttribute("currentAccessId");

        return ResponseEntity.ok(curatorService.addObservations(id, curatorId, body.notes(), accessId));
    }

    @PreAuthorize("hasAuthority('PROJECT_APPROVE')")
    @PutMapping("/project/{id}/approve")
    @Operation(summary = "Approve project after review")
    public ResponseEntity<ProjectDto> approveProject(
            @PathVariable Long id,
            @Valid @RequestBody ProjectApprovalDto approvalDto,
            HttpServletRequest request) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        Long curatorId = (Long) auth.getDetails();
        Long accessId = (Long) request.getAttribute("currentAccessId");

        return ResponseEntity.ok(
                curatorService.approveProject(
                        id,
                        curatorId,
                        approvalDto.votingStartAt(),
                        approvalDto.votingEndAt(),
                        accessId
                )
        );
    }

    @PreAuthorize("hasAuthority('PROJECT_VIEW_ASSIGNED')")
    @GetMapping("/projects/my-curated")
    @Operation(summary = "List projects assigned to curator")
    public ResponseEntity<List<ProjectDto>> listMyCuratedProjects(
            @Parameter(description = "Filter by status (optional)")
            @RequestParam(required = false) ProjectStatus status) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        Long curatorId = (Long) auth.getDetails();

        return ResponseEntity.ok(curatorService.findByCurator(curatorId, status));
    }

    @PreAuthorize("hasAuthority('PROJECT_VIEW_ASSIGNED')")
    @GetMapping("/reviews/pending-queue")
    @Operation(summary = "Get pending review queue")
    public ResponseEntity<PendingReviewQueueDto> getPendingReviewQueue() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        Long curatorId = (Long) auth.getDetails();

        return ResponseEntity.ok(curatorService.getPendingReviewQueue(curatorId));
    }

    @PreAuthorize("hasAuthority('PROJECT_VIEW_ASSIGNED')")
    @GetMapping("/reviews/pending/{projectId}")
    @Operation(summary = "Get pending review details")
    public ResponseEntity<PendingReviewDto> getPendingReviewDetails(
            @Parameter(description = "Project ID to review")
            @PathVariable Long projectId) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        Long curatorId = (Long) auth.getDetails();

        return ResponseEntity.ok(curatorService.getPendingReviewDetails(projectId, curatorId));
    }

    @PreAuthorize("hasAuthority('PROJECT_VIEW_ASSIGNED')")
    @GetMapping("/reviews/history")
    @Operation(summary = "Get paginated review history")
    public ResponseEntity<PagedResponse<ReviewHistoryDto>> getReviewHistory(
            @Parameter(description = "Search by project or creator name")
            @RequestParam(required = false) String searchTerm,
            @Parameter(description = "Filter by project status")
            @RequestParam(required = false) ProjectStatus status,
            @Parameter(description = "Filter by outcome: APROBADO, DEVUELTO, RECHAZADO, all")
            @RequestParam(required = false, defaultValue = "all") String outcome,
            @Parameter(description = "Filter by overdue reviews")
            @RequestParam(required = false) Boolean wasOverdue,
            @Parameter(description = "Filter by resubmissions")
            @RequestParam(required = false) Boolean isResubmission,
            @Parameter(description = "Reviewed from date (ISO 8601)")
            @RequestParam(required = false) OffsetDateTime reviewedFrom,
            @Parameter(description = "Reviewed to date (ISO 8601)")
            @RequestParam(required = false) OffsetDateTime reviewedTo,
            @Parameter(description = "Page number (0-indexed)")
            @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size")
            @RequestParam(defaultValue = "10") int size,
            @Parameter(description = "Sort field")
            @RequestParam(defaultValue = "reviewedAt") String sortBy,
            @Parameter(description = "Sort direction (asc/desc)")
            @RequestParam(defaultValue = "desc") String sortDir) {

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        Long curatorId = (Long) auth.getDetails();

        ReviewHistoryFilterDto filters = new ReviewHistoryFilterDto(
                searchTerm,
                status,
                outcome,
                wasOverdue,
                isResubmission,
                reviewedFrom,
                reviewedTo
        );

        Sort sort = sortDir.equalsIgnoreCase("asc")
                ? Sort.by(sortBy).ascending()
                : Sort.by(sortBy).descending();

        Pageable pageable = PageRequest.of(page, size, sort);

        return ResponseEntity.ok(curatorService.getReviewHistory(curatorId, filters, pageable));
    }
}
