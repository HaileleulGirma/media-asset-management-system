package com.ena.mam.cassettecut;

import com.ena.mam.util.AmharicNormalizer;
import com.ena.mam.util.WebSearchParser;
import jakarta.persistence.criteria.Expression;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDate;

public class CassetteCutSpecification {

    public static Specification<CassetteCut> hasDigitizedMediaId(Long digitizedMediaId) {
        return (root, query, cb) ->
                cb.equal(
                        root.get("digitizedMedia").get("digitizedMediaId"),
                        digitizedMediaId
                );
    }

    public static Specification<CassetteCut> hasCategoryId(Long categoryId) {
        return (root, query, cb) ->
                cb.equal(
                        root.get("cassetteCategory").get("categoryId"),
                        categoryId
                );
    }

    public static Specification<CassetteCut> hasCassetteCutBetween(LocalDate startDate, LocalDate endDate){
        return (root, query, cb) -> cb.between(root.get("cutDate"), startDate, endDate);
    }

    public static Specification<CassetteCut> hasPrivacyStatus(boolean isPrivate) {
        return (root, query, cb) ->
                cb.equal(root.get("digitizedMedia").get("isPrivate"), isPrivate);
    }

    public static Specification<CassetteCut> isPublic() {
        return (root, query, cb) ->
                cb.isFalse(root.get("digitizedMedia").get("isPrivate"));
    }

    public static Specification<CassetteCut> hasSearchTerm(String searchTerm) {
        return (root, query, cb) -> {
            String normalizedTerm = AmharicNormalizer.normalize(searchTerm);
            Expression<String> indexedColumn = cb.function("amh_norm", String.class, root.get("cutTitle"));

            return WebSearchParser.buildWebSearchPredicate(normalizedTerm, indexedColumn, cb);
        };
    }
}