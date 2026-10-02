package com.ena.mam.photo;

import com.ena.mam.dto.request.CreatePhotoRequest;
import com.ena.mam.dto.response.CreatePhotoResponse;
import com.ena.mam.staffmember.StaffMember;
import org.springframework.stereotype.Component;

@Component
public class PhotoMapper {

    public Photo toPhoto(
            CreatePhotoRequest request,
            StaffMember staffMember
    ) {
        Photo photo = new Photo();

        photo.setTitle(request.title());
        photo.setFilePath(request.filePath());
        photo.setNumberOfFiles(request.numberOfFiles());
        photo.setFileSizeMb(request.fileSizeMb());
        photo.setPhotoDate(request.photoDate());
        photo.setImporter(staffMember);

        return photo;
    }

    public CreatePhotoResponse toResponse(Photo photo) {
        return new CreatePhotoResponse(
                photo.getPhotoId(),
                photo.getTitle(),
                photo.getFilePath(),
                photo.getNumberOfFiles(),
                photo.getFileSizeMb(),
                photo.getPhotoDate(),
                photo.getImporter().getStaffMemberId(),
                photo.getVersion()
        );
    }

    public void updatePhoto(
            Photo photo,
            CreatePhotoRequest request,
            StaffMember staffMember
    ) {
        photo.setTitle(request.title());
        photo.setFilePath(request.filePath());
        photo.setNumberOfFiles(request.numberOfFiles());
        photo.setFileSizeMb(request.fileSizeMb());
        photo.setPhotoDate(request.photoDate());
        photo.setImporter(staffMember);
    }
}