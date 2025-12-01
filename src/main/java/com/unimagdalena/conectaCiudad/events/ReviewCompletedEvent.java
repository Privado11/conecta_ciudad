package com.unimagdalena.conectaCiudad.events;

import com.unimagdalena.conectaCiudad.entities.Project;
import com.unimagdalena.conectaCiudad.entities.Review;
import com.unimagdalena.conectaCiudad.entities.User;
import com.unimagdalena.conectaCiudad.enums.EntityType;
import com.unimagdalena.conectaCiudad.enums.ProjectStatus;
import lombok.Getter;

import java.util.HashMap;
import java.util.Map;

@Getter
public class ReviewCompletedEvent extends BaseAuditEvent {
    
    private final Review review;
    private final ProjectStatus decision;
    
    public ReviewCompletedEvent(Object source, Review review, ProjectStatus decision, User curator) {
        super(source, "REVIEW_COMPLETE",
              "Completed review for project: " + review.getProject().getName() + " - Decision: " + decision,
              EntityType.REVIEW,
              review.getId(),
              curator,
              buildMetadata(review, decision));
        this.review = review;
        this.decision = decision;
    }
    
    private static Map<String, Object> buildMetadata(Review review, ProjectStatus decision) {
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("projectId", review.getProject().getId());
        metadata.put("projectName", review.getProject().getName());
        metadata.put("decision", decision.name());
        return metadata;
    }
}
