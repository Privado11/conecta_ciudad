package com.unimagdalena.conectaCiudad.events;

import com.unimagdalena.conectaCiudad.entities.Review;
import com.unimagdalena.conectaCiudad.entities.User;
import com.unimagdalena.conectaCiudad.enums.EntityType;
import lombok.Getter;

import java.util.HashMap;
import java.util.Map;

@Getter
public class ReviewAssignedEvent extends BaseAuditEvent {
    
    private final Review review;
    
    public ReviewAssignedEvent(Object source, Review review, User assigner) {
        super(source, "REVIEW_ASSIGN",
              "Assigned review for project: " + review.getProject().getName(),
              EntityType.REVIEW,
              review.getId(),
              assigner,
              buildMetadata(review));
        this.review = review;
    }
    
    private static Map<String, Object> buildMetadata(Review review) {
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("projectId", review.getProject().getId());
        metadata.put("projectName", review.getProject().getName());
        metadata.put("curatorId", review.getCurator().getId());
        metadata.put("curatorName", review.getCurator().getName());
        return metadata;
    }
}
