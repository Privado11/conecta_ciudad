package com.unimagdalena.conectaCiudad.Dto.project;

import java.time.LocalDate;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Future;

/**
 * DTO for approving a project and setting voting dates.
 * Used by curators when approving projects for voting.
 */
public record ProjectApprovalDto(
    @NotNull
    @Future
    LocalDate votingStartAt,
    
    @NotNull
    @Future
    LocalDate votingEndAt
) {
    /**
     * Custom validation to ensure end date is after start date.
     */
    public ProjectApprovalDto {
        if (votingStartAt != null && votingEndAt != null && votingEndAt.isBefore(votingStartAt)) {
            throw new IllegalArgumentException("Voting end date must be after voting start date");
        }
    }
}