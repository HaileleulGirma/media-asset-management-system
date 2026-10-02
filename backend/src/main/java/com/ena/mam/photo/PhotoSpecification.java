package com.ena.mam.photo;

import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDate;

public class PhotoSpecification {

    public static Specification<Photo> hasImporterId(Long importerId) {
        return (root, query, cb) ->
                cb.equal(
                        root.get("importedBy").get("staffMemberId"),
                        importerId
                );
    }

    public static Specification<Photo> hasPhotoDateBetween(
            LocalDate startDate,
            LocalDate endDate
    ) {
        return (root, query, cb) ->
                cb.between(
                        root.get("photoDate"),
                        startDate,
                        endDate
                );
    }

    public static Specification<Photo> hasSearchTerm(String searchTerm) {
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