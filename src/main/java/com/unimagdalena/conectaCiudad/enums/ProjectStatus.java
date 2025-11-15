package com.unimagdalena.conectaCiudad.enums;

import lombok.Getter;

@Getter
public enum ProjectStatus {

    DRAFT("Draft", "The project is being prepared by the community leader"),

    PENDING_REVIEW("Pending Review", "The project has been submitted and is waiting to be reviewed"),

    IN_REVIEW("In Review", "The curator is reviewing the project"),

    RETURNED_WITH_OBSERVATIONS("Returned with Observations", "The curator requests changes before approval"),

    READY_TO_PUBLISH("Ready to Publish", "The project has been approved and can be published"),

    PUBLISHED("Published", "The project is visible for citizen voting"),

    REJECTED("Rejected", "The project does not meet the required criteria"),

    VOTING_CLOSED("Voting Closed", "The voting period has ended");

    private final String displayName;
    private final String description;

    ProjectStatus(String displayName, String description) {
        this.displayName = displayName;
        this.description = description;
    }

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
        return this == PUBLISHED;
    }
}
