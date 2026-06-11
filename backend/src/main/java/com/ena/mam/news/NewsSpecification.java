package com.ena.mam.news;

import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDate;
import java.util.Set;

public class NewsSpecification {

    public static Specification<News> hasReporters(
            Set<Long> reporterIds) {

        return (root, query, cb) ->
                root.join("reporters")
                        .get("reporterId")
                        .in(reporterIds);
    }

    public static Specification<News> hasCameramen(
            Set<Long> cameramanIds) {

        return (root, query, cb) ->
                root.join("cameramen")
                        .get("cameramanId")
                        .in(cameramanIds);
    }

    public static Specification<News> hasLocations(
            Set<Long> locationIds) {

        return (root, query, cb) ->
                root.join("locations")
                        .get("locationId")
                        .in(locationIds);
    }

    public static Specification<News> hasNewsDateBetween(
            LocalDate startDate,
            LocalDate endDate) {

        return (root, query, cb) ->
                cb.between(
                        root.get("newsDate"),
                        startDate,
                        endDate
                );
    }

    public static Specification<News> hasImporterId(Long importerId) {
        return (root, query, cb) ->
                cb.equal(root.get("importerId"), importerId);
    }

    public static Specification<News> hasIngestorId(Long ingestorId) {
        return (root, query, cb) ->
                cb.equal(root.get("importerId"), ingestorId);
    }
}