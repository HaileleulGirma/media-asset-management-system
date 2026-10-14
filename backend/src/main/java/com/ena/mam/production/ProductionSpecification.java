package com.ena.mam.production;

import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDate;

public class ProductionSpecification {
    public static Specification<Production> hasNewsDateBetween(LocalDate startDate, LocalDate endDate) {
        return (root, query, cb) -> cb.between(root.get("productionDate"), startDate, endDate);
    }

    public static Specification<Production> hasSearchTerm(String searchTerm) {
        return (root, query, cb) ->
                cb.isTrue(
                        cb.function(
                                "fts_match",
                                Boolean.class,
                                root.get("title"),
                                cb.literal(searchTerm)
                        )
                );
    }
}
