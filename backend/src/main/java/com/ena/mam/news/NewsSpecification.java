package com.ena.mam.news;

import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Subquery;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDate;

public class NewsSpecification {

    // Changed from Set<Long> to a single Long id
    public static Specification<News> hasReporter(Long reporterId) {
        return (root, query, cb) -> {
            Subquery<Long> subquery = query.subquery(Long.class);
            Root<News> subRoot = subquery.from(News.class);
            Join<Object, Object> reporters = subRoot.join("reporters");

            subquery.select(subRoot.get("newsId"))
                    .where(cb.equal(reporters.get("reporterId"), reporterId));

            return root.get("newsId").in(subquery);
        };
    }

    // Changed from Set<Long> to a single Long id
    public static Specification<News> hasCameraman(Long cameramanId) {
        return (root, query, cb) -> {
            Subquery<Long> subquery = query.subquery(Long.class);
            Root<News> subRoot = subquery.from(News.class);
            Join<Object, Object> cameramen = subRoot.join("cameramen");

            subquery.select(subRoot.get("newsId"))
                    .where(cb.equal(cameramen.get("cameramanId"), cameramanId));

            return root.get("newsId").in(subquery);
        };
    }

    // Changed from Set<Long> to a single Long id
    public static Specification<News> hasLocation(Long locationId) {
        return (root, query, cb) -> {
            Subquery<Long> subquery = query.subquery(Long.class);
            Root<News> subRoot = subquery.from(News.class);
            Join<Object, Object> locations = subRoot.join("locations");

            subquery.select(subRoot.get("newsId"))
                    .where(cb.equal(locations.get("locationId"), locationId));

            return root.get("newsId").in(subquery);
        };
    }

    public static Specification<News> hasNewsDateBetween(LocalDate startDate, LocalDate endDate) {
        return (root, query, cb) -> cb.between(root.get("newsDate"), startDate, endDate);
    }

    public static Specification<News> hasImporterId(Long importerId) {
        return (root, query, cb) -> cb.equal(root.get("importerId").get("memberId"), importerId);
    }

    public static Specification<News> hasIngestorId(Long ingestorId) {
        return (root, query, cb) -> cb.equal(root.get("ingestorId").get("memberId"), ingestorId);
    }

    public static Specification<News> hasSearchTerm(String searchTerm) {
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