package com.ena.mam.photo;

import com.ena.mam.util.WebSearchParser;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Subquery;
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

    public static Specification<Photo> hasLocation(Long locationId) {
        return (root, query, cb) -> {
            Subquery<Long> subquery = query.subquery(Long.class);
            Root<Photo> subRoot = subquery.from(Photo.class);
            Join<Object, Object> locations = subRoot.join("locations");
            subquery.select(subRoot.get("photoId"))
                    .where(cb.equal(locations.get("locationId"), locationId));
            return root.get("photoId").in(subquery);
        };
    }

    public static Specification<Photo> hasPrivacyStatus(boolean isPrivate) {
        return (root, query, cb) ->
                cb.equal(root.get("isPrivate"), isPrivate);
    }

    public static Specification<Photo> isPublic() {
        return (root, query, cb) ->
                cb.isFalse(root.get("isPrivate"));
    }


    public static Specification<Photo> hasSearchTerm(String searchTerm) {
        return (root, query, cb) ->
                WebSearchParser.buildWebSearchPredicate(searchTerm, root.<String>get("title"), cb);
    }
}