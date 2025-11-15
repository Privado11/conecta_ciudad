package com.unimagdalena.conectaCiudad.specifications;

import com.unimagdalena.conectaCiudad.entities.Project;
import com.unimagdalena.conectaCiudad.entities.User;
import com.unimagdalena.conectaCiudad.enums.ProjectStatus;
import jakarta.persistence.criteria.*;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

public class ProjectSpecifications {

    public static Specification<Project> withFilters(
            String searchTerm,
            ProjectStatus status,
            Long creatorId,
            Long curatorId,
            LocalDate projectStartFrom,
            LocalDate projectStartTo,
            LocalDate projectEndFrom,
            LocalDate projectEndTo,
            LocalDate votingStartFrom,
            LocalDate votingStartTo,
            LocalDate votingEndFrom,
            LocalDate votingEndTo,
            OffsetDateTime createdFrom,
            OffsetDateTime createdTo
    ) {
        return (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();

            // Filtro por estado
            if (status != null) {
                predicates.add(criteriaBuilder.equal(root.get("status"), status));
            }

            // Filtro por creador
            if (creatorId != null) {
                predicates.add(criteriaBuilder.equal(root.get("creator").get("id"), creatorId));
            }

            // Filtro por curador
            if (curatorId != null) {
                predicates.add(criteriaBuilder.equal(root.get("curator").get("id"), curatorId));
            }

            // Filtros para fecha de inicio del proyecto
            if (projectStartFrom != null) {
                predicates.add(criteriaBuilder.greaterThanOrEqualTo(
                    root.get("startAt"), 
                    projectStartFrom
                ));
            }
            if (projectStartTo != null) {
                predicates.add(criteriaBuilder.lessThanOrEqualTo(
                    root.get("startAt"), 
                    projectStartTo
                ));
            }

            // Filtros para fecha de finalización del proyecto
            if (projectEndFrom != null) {
                predicates.add(criteriaBuilder.greaterThanOrEqualTo(
                    root.get("endAt"), 
                    projectEndFrom
                ));
            }
            if (projectEndTo != null) {
                predicates.add(criteriaBuilder.lessThanOrEqualTo(
                    root.get("endAt"), 
                    projectEndTo
                ));
            }

            // Filtros para fecha de inicio de votación
            if (votingStartFrom != null) {
                predicates.add(criteriaBuilder.greaterThanOrEqualTo(
                    root.get("votingStartAt"), 
                    votingStartFrom
                ));
            }
            if (votingStartTo != null) {
                predicates.add(criteriaBuilder.lessThanOrEqualTo(
                    root.get("votingStartAt"), 
                    votingStartTo
                ));
            }

            // Filtros para fecha de finalización de votación
            if (votingEndFrom != null) {
                predicates.add(criteriaBuilder.greaterThanOrEqualTo(
                    root.get("votingEndAt"), 
                    votingEndFrom
                ));
            }
            if (votingEndTo != null) {
                predicates.add(criteriaBuilder.lessThanOrEqualTo(
                    root.get("votingEndAt"), 
                    votingEndTo
                ));
            }

            // Filtros para fecha de creación del registro
            if (createdFrom != null) {
                predicates.add(criteriaBuilder.greaterThanOrEqualTo(
                    root.get("createdAt"), 
                    createdFrom
                ));
            }
            if (createdTo != null) {
                predicates.add(criteriaBuilder.lessThanOrEqualTo(
                    root.get("createdAt"), 
                    createdTo
                ));
            }

            // Filtro de búsqueda por texto
            if (searchTerm != null && !searchTerm.trim().isEmpty()) {
                String pattern = "%" + searchTerm.toLowerCase().trim() + "%";
                
                Join<Project, User> creatorJoin = root.join("creator", JoinType.LEFT);
                
                Predicate namePredicate = criteriaBuilder.like(
                    criteriaBuilder.lower(root.get("name")), 
                    pattern
                );
                Predicate objectivesPredicate = criteriaBuilder.like(
                    criteriaBuilder.lower(root.get("objectives")), 
                    pattern
                );
                Predicate beneficiaryPredicate = criteriaBuilder.like(
                    criteriaBuilder.lower(root.get("beneficiaryPopulations")), 
                    pattern
                );
                Predicate creatorNamePredicate = criteriaBuilder.like(
                    criteriaBuilder.lower(creatorJoin.get("name")), 
                    pattern
                );
                
                predicates.add(criteriaBuilder.or(
                    namePredicate,
                    objectivesPredicate,
                    beneficiaryPredicate,
                    creatorNamePredicate
                ));
            }

            query.orderBy(criteriaBuilder.desc(root.get("createdAt")));
            query.distinct(true);

            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        };
    }
}