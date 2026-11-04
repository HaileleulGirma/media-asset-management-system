package com.ena.mam.production;

import com.ena.mam.util.AmharicNormalizer;
import com.ena.mam.util.WebSearchParser;
import jakarta.persistence.criteria.Expression;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDate;

public class ProductionSpecification {
    public static Specification<Production> hasNewsDateBetween(LocalDate startDate, LocalDate endDate) {
        return (root, query, cb) -> cb.between(root.get("productionDate"), startDate, endDate);
    }

    public static Specification<Production> hasSearchTerm(String searchTerm) {
        return (root, query, cb) -> {
            String normalizedTerm = AmharicNormalizer.normalize(searchTerm);
            Expression<String> indexedColumn = cb.function("amh_norm", String.class, root.get("title"));

            return WebSearchParser.buildWebSearchPredicate(normalizedTerm, indexedColumn, cb);
        };
    }
}
