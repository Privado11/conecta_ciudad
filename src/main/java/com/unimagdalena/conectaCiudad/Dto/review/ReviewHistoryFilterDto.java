package com.unimagdalena.conectaCiudad.Dto.review;

import com.unimagdalena.conectaCiudad.enums.ProjectStatus;
import java.time.OffsetDateTime;

public record ReviewHistoryFilterDto(
    String searchTerm,
    ProjectStatus status,
    String outcome, 
    Boolean wasOverdue,
    Boolean isResubmission,
    OffsetDateTime reviewedFrom,
    OffsetDateTime reviewedTo
) {
    public ReviewHistoryFilterDto() {
        this(null, null, "all", null, null, null, null);
    }
}