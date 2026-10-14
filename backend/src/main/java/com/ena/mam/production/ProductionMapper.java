package com.ena.mam.production;

import com.ena.mam.dto.request.CreateProductionRequest;
import com.ena.mam.dto.response.CreateProductionResponse;
import org.springframework.stereotype.Component;

@Component
public class ProductionMapper {
    public Production toProduction(CreateProductionRequest request){
        Production production = new Production();

        production.setProductionDate(request.productionDate());
        production.setTitle(request.title());
        production.setFilePath(request.filePath());
        production.setFileSizeGb(request.fileSizeGb());

        int hours = request.durationHours() != null ? request.durationHours() : 0;
        int minutes = request.durationMinutes() != null ? request.durationMinutes() : 0;
        int seconds = request.durationSeconds() != null ? request.durationSeconds() : 0;

        int totalSeconds = (hours * 3600) + (minutes * 60) + seconds;

        production.setProductionDurationSec(totalSeconds);

        return production;
    }

    public CreateProductionResponse toResponse(Production production){
        return new CreateProductionResponse(
                production.getProductionId(),
                production.getProductionDate(),
                production.getTitle(),
                production.getFilePath(),
                production.getFileSizeGb(),
                production.getProductionDurationSec(),
                production.getVersion()
                );
    }
}
