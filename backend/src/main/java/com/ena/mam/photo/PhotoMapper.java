package com.ena.mam.photo;

import com.ena.mam.cameraman.Cameraman;
import com.ena.mam.dto.request.CreatePhotoRequest;
import com.ena.mam.dto.response.CreatePhotoResponse;
import com.ena.mam.location.Location;
import com.ena.mam.staffmember.StaffMember;
import org.springframework.stereotype.Component;

import java.util.Set;
import java.util.stream.Collectors;

@Component
public class PhotoMapper {

    public Photo toPhoto(
            CreatePhotoRequest request,
            StaffMember staffMember,
            Set<Cameraman> cameramen,
            Set<Location> locations
    ) {
        Photo photo = new Photo();

        photo.setCameramen(cameramen);
        photo.setLocations(locations);
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
                photo.getCameramen().stream().map(Cameraman::getCameramanId).collect(Collectors.toSet()),
                photo.getLocations().stream().map(Location::getLocationId).collect(Collectors.toSet()),
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
            StaffMember staffMember,
            Set<Cameraman> cameramen,
            Set<Location> locations
    ) {
        photo.setCameramen(cameramen);
        photo.setLocations(locations);
        photo.setTitle(request.title());
        photo.setFilePath(request.filePath());
        photo.setNumberOfFiles(request.numberOfFiles());
        photo.setFileSizeMb(request.fileSizeMb());
        photo.setPhotoDate(request.photoDate());
        photo.setImporter(staffMember);
    }
}