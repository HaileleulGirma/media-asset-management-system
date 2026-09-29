package com.ena.mam.photo;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface PhotoRepository extends
        JpaRepository<Photo, Long>,
        JpaSpecificationExecutor<Photo>
{
}
