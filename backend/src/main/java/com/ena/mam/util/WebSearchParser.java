package com.ena.mam.util;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Predicate;
import org.hibernate.query.criteria.HibernateCriteriaBuilder;

import java.util.ArrayList;
import java.util.List;

public class WebSearchParser {

    private static final int MIN_LEN = 2;

    public static Predicate buildWebSearchPredicate(
            String searchInput,
            Expression<String> column,
            CriteriaBuilder cb) {

        if (searchInput == null || searchInput.isBlank()) {
            return cb.conjunction();
        }

        HibernateCriteriaBuilder hcb = (HibernateCriteriaBuilder) cb;

        List<List<String>> orGroups = new ArrayList<>(); // AND of (OR of terms)
        List<String> excluded = new ArrayList<>();
        List<String> currentGroup = new ArrayList<>();
        boolean pendingOr = false;

        for (String token : searchInput.trim().split("\\s+")) {

            if (token.equalsIgnoreCase("OR")) {
                pendingOr = !currentGroup.isEmpty();
                continue;
            }

            boolean negated = token.startsWith("-");
            String word = negated ? token.substring(1) : token;

            if (word.length() < MIN_LEN) {
                continue; // pendingOr is left untouched on purpose
            }

            if (negated) {
                excluded.add(word);
                continue;
            }

            if (pendingOr) {
                currentGroup.add(word);
                pendingOr = false;
            } else {
                if (!currentGroup.isEmpty()) {
                    orGroups.add(currentGroup);
                }
                currentGroup = new ArrayList<>();
                currentGroup.add(word);
            }
        }
        if (!currentGroup.isEmpty()) {
            orGroups.add(currentGroup);
        }

        List<Predicate> result = new ArrayList<>();

        for (List<String> group : orGroups) {
            Predicate[] likes = group.stream()
                    .map(w -> hcb.ilike(column, "%" + escape(w) + "%", '\\'))
                    .toArray(Predicate[]::new);
            result.add(likes.length == 1 ? likes[0] : cb.or(likes));
        }

        for (String w : excluded) {
            // NULL-safe: rows with a NULL title are not silently dropped
            result.add(cb.or(
                    cb.isNull(column),
                    hcb.notIlike(column, "%" + escape(w) + "%", '\\')));
        }

        return result.isEmpty() ? cb.conjunction() : cb.and(result.toArray(Predicate[]::new));
    }

    /** Makes %, _ and \ in user input literal instead of wildcards. */
    private static String escape(String s) {
        return s.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
    }
}