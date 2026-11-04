package com.ena.mam.news;
import com.ena.mam.util.AmharicNormalizer;
import com.ena.mam.util.WebSearchParser;
import jakarta.persistence.criteria.Expression;
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
    // News.importer / News.ingestor are @ManyToOne StaffMember fields, not
    // flat importerId/ingestorId columns on News itself -- so the filter has
    // to traverse the relationship (root.get("importer")) and then reach
    // StaffMember's own id field, the same way hasReporter/hasCameraman/
    // hasLocation reach into their joined entity's id field above.
    public static Specification<News> hasImporterId(Long importerId) {
        return (root, query, cb) -> cb.equal(root.get("importer").get("staffMemberId"), importerId);
    }
    public static Specification<News> hasIngestorId(Long ingestorId) {
        return (root, query, cb) -> cb.equal(root.get("ingestor").get("staffMemberId"), ingestorId);
    }
    public static Specification<News> isPublic() {
        return (root, query, cb) ->
                cb.isFalse(root.get("isPrivate"));
    }
    public static Specification<News> hasPrivacyStatus(boolean isPrivate) {
        return (root, query, cb) ->
                cb.equal(root.get("isPrivate"), isPrivate);
    }

    public static Specification<News> hasSearchTerm(String searchTerm) {
        return (root, query, cb) -> {

            // 1. Normalize the Java string (e.g., "-ፀሎት" becomes "-ጸሎት")
            String normalizedTerm = AmharicNormalizer.normalize(searchTerm);

            // 2. Wrap the DB column so the query reads: amh_norm(title) ILIKE ...
            // This ensures it perfectly hits your idx_news_title_trgm index!
            Expression<String> indexedColumn = cb.function("amh_norm", String.class, root.get("title"));

            // 3. Pass both to your existing parser without changing a single line of the parser
            return WebSearchParser.buildWebSearchPredicate(
                    normalizedTerm,
                    indexedColumn,
                    cb
            );
        };
    }

}