package com.ena.mam.digitizedmedia;

import com.ena.mam.cassettecategory.CassetteCategory;
import com.ena.mam.dto.request.CreateDigitizedMediaRequest;
import com.ena.mam.dto.response.CreateDigitizedMediaResponse;
import com.ena.mam.staffmember.StaffMember;
import org.springframework.stereotype.Component;

@Component
public class DigitizedMediaMapper {
    public DigitizedMedia toDigitizedMedia(CreateDigitizedMediaRequest request, StaffMember staffMember, CassetteCategory cassetteCategory){
        DigitizedMedia digitizedMedia = new DigitizedMedia();

        digitizedMedia.setDigitizedDate(request.digitizedDate());
        digitizedMedia.setFilePath(request.filePath());
        digitizedMedia.setCassetteCategory(cassetteCategory);
        digitizedMedia.setIdentifierNumber(request.identifierNumber());
        digitizedMedia.setImporter(staffMember);
        digitizedMedia.setPrivate(request.isPrivate());

        return digitizedMedia;
    }

    public CreateDigitizedMediaResponse toResponse(DigitizedMedia digitizedMedia){
        return new CreateDigitizedMediaResponse(
                digitizedMedia.getDigitizedMediaId(),
                digitizedMedia.getDigitizedDate(),
                digitizedMedia.getFilePath(),
                digitizedMedia.getCassetteCategory().getCategoryId(),
                digitizedMedia.getIdentifierNumber(),
                digitizedMedia.getImporter().getStaffMemberId(),
                digitizedMedia.getPrivate(),
                digitizedMedia.getVersion());
    }

    public void updateDigitizedMedia(DigitizedMedia digitizedMedia, CreateDigitizedMediaRequest request, StaffMember staffMember, CassetteCategory cassetteCategory){
        digitizedMedia.setDigitizedDate(request.digitizedDate());
        digitizedMedia.setFilePath(request.filePath());
        digitizedMedia.setCassetteCategory(cassetteCategory);
        digitizedMedia.setIdentifierNumber(request.identifierNumber());
        digitizedMedia.setImporter(staffMember);
        digitizedMedia.setPrivate(request.isPrivate());

    }
}
