package com.unimagdalena.conectaCiudad.enums;

import lombok.Getter;

@Getter
public enum ProjectStatus {

    DRAFT,
    PENDING_REVIEW,
    IN_REVIEW,
    RETURNED_WITH_OBSERVATIONS,
    READY_TO_PUBLISH,
    PUBLISHED,
    OPEN_FOR_VOTING,
    VOTING_CLOSED,
    REJECTED;

    public boolean isEditable() {
        return this == DRAFT || this == RETURNED_WITH_OBSERVATIONS;
    }

    public boolean canBeSubmitted() {
        return this == DRAFT || this == RETURNED_WITH_OBSERVATIONS;
    }

    public boolean canBeReviewed() {
        return this == PENDING_REVIEW || this == IN_REVIEW;
    }

    public boolean isPublished() {
        return this == PUBLISHED || this == OPEN_FOR_VOTING;
    }

    public boolean isVotingOpen() {
        return this == OPEN_FOR_VOTING;
    }
}
