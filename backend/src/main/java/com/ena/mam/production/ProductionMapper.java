package com.ena.mam.production;

import com.ena.mam.dto.request.CreateProductionRequest;
import com.ena.mam.dto.response.CreateProductionResponse;
import org.springframework.stereotype.Component;

@Component
public class ProductionMapper {
    public Production toProduction(CreateProductionRequest request){
        Production production = new Production();
        production.setTitle(request.title());
        production.setFilePath(request.filePath());
        production.setFileSizeGb(request.fileSizeGb());

        return production;
    }

    public CreateProductionResponse toResponse(Production production){
        return new CreateProductionResponse(
                production.getProductionId(),
                production.getTitle(),
                production.getFilePath(),
                production.getFileSizeGb(),
                production.getVersion()
                );
    }
}
