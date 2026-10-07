package com.ena.mam.photo;

import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDate;

public class PhotoSpecification {

    public static Specification<Photo> hasImporterId(Long importerId) {
        return (root, query, cb) ->
                cb.equal(
                        root.get("importer").get("staffMemberId"),
                        importerId
                );
    }

    public static Specification<Photo> hasCameramanId(Long cameramanId) {
        return (root, query, cb) -> {
            query.distinct(true);

            return cb.equal(
                    root.join("cameramen").get("cameramanId"),
                    cameramanId
            );
        };
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