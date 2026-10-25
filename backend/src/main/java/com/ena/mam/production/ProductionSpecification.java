package com.ena.mam.production;

import com.ena.mam.util.WebSearchParser;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDate;

public class ProductionSpecification {
    public static Specification<Production> hasNewsDateBetween(LocalDate startDate, LocalDate endDate) {
        return (root, query, cb) -> cb.between(root.get("productionDate"), startDate, endDate);
    }

    public static Specification<Production> hasSearchTerm(String searchTerm) {
        return (root, query, cb) ->
                WebSearchParser.buildWebSearchPredicate(searchTerm, root.<String>get("title"), cb);
    }
}
