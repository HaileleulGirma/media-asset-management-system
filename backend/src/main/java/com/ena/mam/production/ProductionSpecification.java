package com.ena.mam.production;

import org.springframework.data.jpa.domain.Specification;

public class ProductionSpecification {
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
