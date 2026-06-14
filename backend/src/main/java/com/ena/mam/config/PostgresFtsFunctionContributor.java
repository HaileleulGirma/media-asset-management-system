package com.ena.mam.config;

import org.hibernate.boot.model.FunctionContributions;
import org.hibernate.boot.model.FunctionContributor;
import org.hibernate.type.StandardBasicTypes;

public class PostgresFtsFunctionContributor implements FunctionContributor {

    @Override
    public void contributeFunctions(FunctionContributions functionContributions) {
        functionContributions.getFunctionRegistry()
                .registerPattern(
                        "fts_match",
                        // Changed to websearch_to_tsquery to support advanced operators
                        "(to_tsvector('simple', ?1) @@ websearch_to_tsquery('simple', ?2))",
                        functionContributions.getTypeConfiguration().getBasicTypeRegistry().resolve(StandardBasicTypes.BOOLEAN)
                );
    }
}
