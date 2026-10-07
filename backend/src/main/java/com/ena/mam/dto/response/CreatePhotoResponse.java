package com.ena.mam.dto.response;

import java.time.LocalDate;
import java.util.Set;

public record CreatePhotoResponse(
        Long photoId,
        Set<Long> cameramen,
        String title,
        String filePath,
        Integer numberOfFiles,
        Double fileSizeMb,
        LocalDate photoDate,
        Long importedBy,
        Long version
) {
}
