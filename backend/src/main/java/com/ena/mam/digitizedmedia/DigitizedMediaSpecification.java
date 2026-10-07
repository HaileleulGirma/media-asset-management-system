package com.ena.mam.digitizedmedia;

import org.springframework.data.jpa.domain.Specification;

public class DigitizedMediaSpecification {

    public static Specification<DigitizedMedia> hasImporterId(Long importerId) {
        return (root, query, cb) ->
                cb.equal(
                        root.get("importedBy").get("staffMemberId"),
                        importerId
                );
    }

    public static Specification<DigitizedMedia> hasCategoryId(Long categoryId) {
        return (root, query, cb) ->
                cb.equal(
                        root.get("category").get("categoryId"),
                        categoryId
                );
    }

    public static Specification<DigitizedMedia> hasIdentifierNumber(Long identifierNumber) {
        return (root, query, cb) ->
                cb.equal(
                        root.get("identifierNumber"),
                        identifierNumber
                );
    }

    public static Specification<DigitizedMedia> hasSearchTerm(String searchTerm) {
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