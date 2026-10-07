package com.ena.mam.cassettecut;

import com.ena.mam.cassettecategory.CassetteCategory;
import com.ena.mam.digitizedmedia.DigitizedMedia;
import com.ena.mam.dto.request.CreateCassetteCutRequest;
import com.ena.mam.dto.response.CreateCassetteCutResponse;
import org.springframework.stereotype.Component;

@Component
public class CassetteCutMapper {
    public CassetteCut toCassetteCut(CreateCassetteCutRequest request, DigitizedMedia digitizedMedia, CassetteCategory cassetteCategory){

        CassetteCut cassetteCut = new CassetteCut();

        cassetteCut.setCassetteCategory(cassetteCategory);
        cassetteCut.setCutDate(request.cutDate());
        cassetteCut.setCutTitle(request.CutTitle());
        cassetteCut.setVersion(request.version());
        cassetteCut.setDigitizedMedia(digitizedMedia);

        return cassetteCut;
    }

    public CreateCassetteCutResponse toResponse(CassetteCut cassetteCut){
        return new CreateCassetteCutResponse(
                cassetteCut.getCutId(),
                cassetteCut.getCutTitle(),
                cassetteCut.getCutDate(),
                cassetteCut.getDigitizedMedia().getDigitizedMediaId(),
                cassetteCut.getCassetteCategory().getCategoryId(),
                cassetteCut.getVersion());
    }
}
