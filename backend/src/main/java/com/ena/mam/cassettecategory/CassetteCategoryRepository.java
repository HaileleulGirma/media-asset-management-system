package com.ena.mam.cassettecategory;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface CassetteCategoryRepository extends
        JpaRepository<CassetteCategory, Long>,
        JpaSpecificationExecutor<CassetteCategory>
{
}
