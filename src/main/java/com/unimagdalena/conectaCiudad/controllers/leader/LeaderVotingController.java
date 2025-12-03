package com.unimagdalena.conectaCiudad.controllers.leader;

import com.unimagdalena.conectaCiudad.Dto.leader.ProjectVotingResultDto;
import com.unimagdalena.conectaCiudad.services.leader.LeaderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/leader/voting")
@RequiredArgsConstructor
@Tag(name = "Leader Voting", description = "Voting results operations for community leaders")
public class LeaderVotingController {

    private final LeaderService leaderService;

    @PreAuthorize("hasAuthority('PROJECT_VIEW')")
    @GetMapping("/results")
    @Operation(summary = "Get voting results for my closed projects")
    public ResponseEntity<List<ProjectVotingResultDto>> getMyVotingResults(HttpServletRequest request) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        Long creatorId = (Long) auth.getDetails();
        String token = extractToken(request);
        return ResponseEntity.ok(leaderService.getMyClosedVotingResults(creatorId, token));
    }

    private String extractToken(HttpServletRequest request) {
        String authHeader = request.getHeader("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            return authHeader.substring(7);
        }
        return null;
    }
}
