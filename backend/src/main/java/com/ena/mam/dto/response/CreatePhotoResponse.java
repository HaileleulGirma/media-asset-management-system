package com.ena.mam.dto.response;

import java.time.LocalDate;

public record CreatePhotoResponse(
        String title,
        String filePath,
        Integer numberOfFiles,
        Double fileSizeMb,
        LocalDate photoDate,
        Long importedBy,
        Long version
) {
}
