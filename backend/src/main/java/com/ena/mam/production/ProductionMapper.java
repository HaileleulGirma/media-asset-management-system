package com.ena.mam.production;

import com.ena.mam.dto.request.CreateProductionRequest;
import com.ena.mam.dto.response.CreateProductionResponse;
import com.ena.mam.util.DurationUtils;
import org.springframework.stereotype.Component;

@Component
public class ProductionMapper {

    public Production toProduction(CreateProductionRequest request) {
        Production production = new Production();

        production.setProductionDate(request.productionDate());
        production.setTitle(request.title());
        production.setFilePath(request.filePath());
        production.setFileSizeGb(request.fileSizeGb());
        production.setProductionDurationSec(
                DurationUtils.toTotalSeconds(
                        request.durationHours(),
                        request.durationMinutes(),
                        request.durationSeconds()
                )
        );

        return production;
    }

    public CreateProductionResponse toResponse(Production production) {
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

    public void updateProduction(Production production, CreateProductionRequest request) {
        production.setProductionDate(request.productionDate());
        production.setTitle(request.title());
        production.setFilePath(request.filePath());
        production.setFileSizeGb(request.fileSizeGb());
        production.setProductionDurationSec(
                DurationUtils.toTotalSeconds(
                        request.durationHours(),
                        request.durationMinutes(),
                        request.durationSeconds()
                )
        );
    }
}