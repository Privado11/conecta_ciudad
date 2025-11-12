package com.unimagdalena.conectaCiudad.specifications;

import com.unimagdalena.conectaCiudad.entities.Action;
import com.unimagdalena.conectaCiudad.entities.User;
import com.unimagdalena.conectaCiudad.enums.ActionResult;
import com.unimagdalena.conectaCiudad.enums.EntityType;
import jakarta.persistence.criteria.*;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;


public class ActionSpecifications {


    public static Specification<Action> withFilters(
            String actionType,
            ActionResult result,
            EntityType entityType,
            String searchTerm,
            LocalDateTime startDate,
            LocalDateTime endDate
    ) {
        return (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (actionType != null && !actionType.trim().isEmpty()) {
                predicates.add(criteriaBuilder.equal(root.get("actionType"), actionType));
            }

            if (result != null) {
                predicates.add(criteriaBuilder.equal(root.get("result"), result));
            }

            if (entityType != null) {
                predicates.add(criteriaBuilder.equal(root.get("entityType"), entityType));
            }
            if (startDate != null) {
                predicates.add(criteriaBuilder.greaterThanOrEqualTo(
                    root.get("actionAt"), 
                    startDate
                ));
            }
            if (endDate != null) {
                predicates.add(criteriaBuilder.lessThanOrEqualTo(
                    root.get("actionAt"), 
                    endDate
                ));
            }

            if (searchTerm != null && !searchTerm.trim().isEmpty()) {
                String pattern = "%" + searchTerm.toLowerCase().trim() + "%";
                
                Join<Action, User> userJoin = root.join("user", JoinType.LEFT);
                
                Predicate namePredicate = criteriaBuilder.like(
                    criteriaBuilder.lower(userJoin.get("name")), 
                    pattern
                );
                Predicate emailPredicate = criteriaBuilder.like(
                    criteriaBuilder.lower(userJoin.get("email")), 
                    pattern
                );
                Predicate descriptionPredicate = criteriaBuilder.like(
                    criteriaBuilder.lower(root.get("description")), 
                    pattern
                );
                
                predicates.add(criteriaBuilder.or(
                    namePredicate, 
                    emailPredicate, 
                    descriptionPredicate
                ));
            }

            query.orderBy(criteriaBuilder.desc(root.get("actionAt")));
            query.distinct(true);

            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        };
    }


    public static Specification<Action> byUserId(Long userId) {
        return (root, query, criteriaBuilder) -> {
            if (userId == null) {
                return criteriaBuilder.conjunction();
            }
            return criteriaBuilder.equal(root.get("user").get("id"), userId);
        };
    }

    public static Specification<Action> recentFailures(LocalDateTime since) {
        return (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();
            
            predicates.add(criteriaBuilder.equal(root.get("result"), ActionResult.FAILED));
            
            if (since != null) {
                predicates.add(criteriaBuilder.greaterThanOrEqualTo(
                    root.get("actionAt"), 
                    since
                ));
            }
            
            query.orderBy(criteriaBuilder.desc(root.get("actionAt")));
            
            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        };
    }


    public static Specification<Action> today() {
        return (root, query, criteriaBuilder) -> {
            LocalDateTime startOfDay = LocalDateTime.now()
                .withHour(0)
                .withMinute(0)
                .withSecond(0)
                .withNano(0);
            
            LocalDateTime endOfDay = LocalDateTime.now()
                .withHour(23)
                .withMinute(59)
                .withSecond(59)
                .withNano(999999999);
            
            return criteriaBuilder.between(
                root.get("actionAt"), 
                startOfDay, 
                endOfDay
            );
        };
    }
}