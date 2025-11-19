package com.unimagdalena.conectaCiudad.specifications;

import com.unimagdalena.conectaCiudad.entities.Project;
import com.unimagdalena.conectaCiudad.entities.Review;
import com.unimagdalena.conectaCiudad.entities.User;
import com.unimagdalena.conectaCiudad.enums.ProjectStatus;
import jakarta.persistence.criteria.*;
import org.springframework.data.jpa.domain.Specification;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

public class ReviewSpecifications {

    public static Specification<Review> withHistoryFilters(
            Long curatorId,
            String searchTerm,
            ProjectStatus status,
            String outcome,
            Boolean wasOverdue,
            Boolean isResubmission,
            OffsetDateTime reviewedFrom,
            OffsetDateTime reviewedTo
    ) {
        return (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();

            predicates.add(criteriaBuilder.equal(root.get("curator").get("id"), curatorId));


            predicates.add(criteriaBuilder.isNotNull(root.get("reviewedAt")));

  
            if (status != null) {
                Join<Review, Project> projectJoin = root.join("project", JoinType.INNER);
                predicates.add(criteriaBuilder.equal(projectJoin.get("status"), status));
            }

            if (outcome != null && !outcome.equals("all")) {
                Join<Review, Project> projectJoin = root.join("project", JoinType.INNER);
                
                switch (outcome) {
                    case "APROBADO" -> predicates.add(
                        criteriaBuilder.equal(projectJoin.get("status"), ProjectStatus.READY_TO_PUBLISH)
                    );
                    case "DEVUELTO" -> predicates.add(
                        criteriaBuilder.equal(projectJoin.get("status"), ProjectStatus.RETURNED_WITH_OBSERVATIONS)
                    );
                    case "RECHAZADO" -> predicates.add(
                        criteriaBuilder.equal(projectJoin.get("status"), ProjectStatus.REJECTED)
                    );
                }
            }

            if (wasOverdue != null) {
                if (wasOverdue) {
                    predicates.add(
                        criteriaBuilder.greaterThan(root.get("reviewedAt"), root.get("dueAt"))
                    );
                } else {
                    predicates.add(
                        criteriaBuilder.lessThanOrEqualTo(root.get("reviewedAt"), root.get("dueAt"))
                    );
                }
            }

            if (isResubmission != null) {
                if (isResubmission) {
                    predicates.add(criteriaBuilder.isNotNull(root.get("notes")));
                    predicates.add(criteriaBuilder.notEqual(root.get("notes"), ""));
                } else {
                    Predicate notesNull = criteriaBuilder.isNull(root.get("notes"));
                    Predicate notesEmpty = criteriaBuilder.equal(root.get("notes"), "");
                    predicates.add(criteriaBuilder.or(notesNull, notesEmpty));
                }
            }

            if (reviewedFrom != null) {
                predicates.add(criteriaBuilder.greaterThanOrEqualTo(
                    root.get("reviewedAt"), 
                    reviewedFrom
                ));
            }
            if (reviewedTo != null) {
                predicates.add(criteriaBuilder.lessThanOrEqualTo(
                    root.get("reviewedAt"), 
                    reviewedTo
                ));
            }

            if (searchTerm != null && !searchTerm.trim().isEmpty()) {
                String pattern = "%" + searchTerm.toLowerCase().trim() + "%";
                
                Join<Review, Project> projectJoin = root.join("project", JoinType.INNER);
                Join<Project, User> creatorJoin = projectJoin.join("creator", JoinType.LEFT);
                
                Predicate namePredicate = criteriaBuilder.like(
                    criteriaBuilder.lower(projectJoin.get("name")), 
                    pattern
                );
                Predicate objectivesPredicate = criteriaBuilder.like(
                    criteriaBuilder.lower(projectJoin.get("objectives")), 
                    pattern
                );
                Predicate creatorNamePredicate = criteriaBuilder.like(
                    criteriaBuilder.lower(creatorJoin.get("name")), 
                    pattern
                );
                
                predicates.add(criteriaBuilder.or(
                    namePredicate,
                    objectivesPredicate,
                    creatorNamePredicate
                ));
            }

            query.orderBy(criteriaBuilder.desc(root.get("reviewedAt")));
            query.distinct(true);

            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        };
    }
}