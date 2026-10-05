package com.ena.mam.cassettecut;

import com.ena.mam.dto.request.CreateCassetteCutRequest;
import com.ena.mam.dto.response.CreateCassetteCutResponse;
import org.springframework.stereotype.Component;

@Component
public class CassetteCutMapper {
    public CasseteCut toCassetteCut(CreateCassetteCutRequest request){

        CasseteCut casseteCut = new CasseteCut();

        casseteCut.setCassetteCategoryId(request.CassetteCategoryId());
        casseteCut.setCutDate(request.cutDate());
        casseteCut.setCutTitle(request.CutTitle());
        casseteCut.setVersion(request.version());
        casseteCut.setDigitizedMediaId(request.digitizedMediaId());

        return casseteCut;
    }

    public CreateCassetteCutResponse toResponse(CasseteCut casseteCut){
        return new CreateCassetteCutResponse(
                casseteCut.getCutId(),
                casseteCut.getCutTitle(),
                casseteCut.getCutDate(),
                casseteCut.getDigitizedMediaId(),
                casseteCut.getCassetteCategoryId(),
                casseteCut.getVersion());
    }
}
