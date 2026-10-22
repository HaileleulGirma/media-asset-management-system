package com.ena.mam.cassettecut;

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

    public static Specification<CassetteCut> hasSearchTerm(String searchTerm) {
        return (root, query, cb) ->
                cb.isTrue(
                        cb.function(
                                "fts_match",
                                Boolean.class,
                                root.get("cutTitle"),
                                cb.literal(searchTerm)
                        )
                );
    }
}