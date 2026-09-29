package com.ena.mam.digitizedmedia;

import com.ena.mam.dto.request.CreateDigitizedMediaRequest;
import com.ena.mam.dto.response.CreateDigitizedMediaResponse;
import com.ena.mam.staffmember.StaffMember;
import org.springframework.stereotype.Component;

@Component
public class DigitizedMediaMapper {
    public DigitizedMedia toDigitizedMedia(CreateDigitizedMediaRequest request, StaffMember staffMember){
        DigitizedMedia digitizedMedia = new DigitizedMedia();

        digitizedMedia.setTitle(request.title());
        digitizedMedia.setFilePath(request.filePath());
        digitizedMedia.setImporter(staffMember);
        return digitizedMedia;
    }

    public CreateDigitizedMediaResponse toResponse(DigitizedMedia digitizedMedia){
        return new CreateDigitizedMediaResponse(
                digitizedMedia.getDigitizedMediaId(),
                digitizedMedia.getTitle(),
                digitizedMedia.getFilePath(),
                digitizedMedia.getImporter().getStaffMemberId(),
                digitizedMedia.getVersion());
    }

    public void updateDigitizedMedia(DigitizedMedia digitizedMedia, CreateDigitizedMediaRequest request, StaffMember staffMember){
        digitizedMedia.setTitle(request.title());
        digitizedMedia.setFilePath(request.filePath());
        digitizedMedia.setImporter(staffMember);

    }
}
