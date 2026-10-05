package com.ena.mam.cassettecut;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface CassetteCutRepository extends
        JpaRepository<CasseteCut, Long>,
        JpaSpecificationExecutor<CasseteCut>

{
}
