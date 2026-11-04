package com.ena.mam.cassettecut;

import com.ena.mam.cassettecategory.CassetteCategory;
import com.ena.mam.digitizedmedia.DigitizedMedia;
import com.ena.mam.dto.request.CreateCassetteCutRequest;
import com.ena.mam.dto.response.CreateCassetteCutResponse;
import org.springframework.stereotype.Component;

@Component
public class CassetteCutMapper {
    public CassetteCut toCassetteCut(
            CreateCassetteCutRequest request,
            DigitizedMedia digitizedMedia,
            CassetteCategory cassetteCategory){

        CassetteCut cassetteCut = new CassetteCut();

        cassetteCut.setCassetteCategory(cassetteCategory);
        cassetteCut.setCutDate(request.cutDate());
        cassetteCut.setCutTitle(request.cutTitle());
        cassetteCut.setVersion(request.version());
        cassetteCut.setDigitizedMedia(digitizedMedia);

        return cassetteCut;
    }

    public CreateCassetteCutResponse toResponse(CassetteCut cassetteCut){
        DigitizedMedia media = cassetteCut.getDigitizedMedia();
        return new CreateCassetteCutResponse(
                cassetteCut.getCutId(),
                cassetteCut.getCutTitle(),
                cassetteCut.getCutDate(),
                media.getDigitizedMediaId(),
                cassetteCut.getCassetteCategory().getCategoryId(),
                cassetteCut.getVersion(),
                media.getCassetteCategory() != null ? media.getCassetteCategory().getCategoryId() : null,
                media.getIdentifierNumber(),
                media.getFilePath(),
                Boolean.TRUE.equals(media.getPrivate()));
    }

    public void updateCassetteCut(CassetteCut cassetteCut,
                          CreateCassetteCutRequest request,
                          DigitizedMedia digitizedMedia,
                          CassetteCategory cassetteCategory){

        cassetteCut.setCutTitle(request.cutTitle());
        cassetteCut.setCutDate(request.cutDate());
        cassetteCut.setDigitizedMedia(digitizedMedia);
        cassetteCut.setCassetteCategory(cassetteCategory);
    }
}
