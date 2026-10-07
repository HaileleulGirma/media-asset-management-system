package com.ena.mam.photo;

import com.ena.mam.cameraman.Cameraman;
import com.ena.mam.cameraman.CameramanRepository;
import com.ena.mam.dto.request.CreatePhotoRequest;
import com.ena.mam.dto.request.filter.PhotoFilter;
import com.ena.mam.dto.response.CreatePhotoResponse;
import com.ena.mam.exception.ResourceNotFoundException;
import com.ena.mam.staffmember.StaffMember;
import com.ena.mam.staffmember.StaffMemberRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.Set;

@Service
public class PhotoService {

    private final StaffMemberRepository staffMemberRepository;
    private final PhotoMapper photoMapper;
    private final PhotoRepository photoRepository;
    private final CameramanRepository cameramanRepository;

    public PhotoService(
            StaffMemberRepository staffMemberRepository,
            PhotoMapper photoMapper,
            PhotoRepository photoRepository,
            CameramanRepository cameramanRepository
    ) {
        this.staffMemberRepository = staffMemberRepository;
        this.photoMapper = photoMapper;
        this.photoRepository = photoRepository;
        this.cameramanRepository = cameramanRepository;
    }

    @Transactional
    public CreatePhotoResponse create(CreatePhotoRequest request) {

        Set<Cameraman> cameramen =
                new HashSet<>(cameramanRepository.findAllById(request.cameramanIds()));

        StaffMember staffMember = staffMemberRepository
                .findById(request.importedBy())
                .orElseThrow(() ->
                        new ResourceNotFoundException("Importer not found: " + request.importedBy()
                        )
                );

        Photo photo = photoMapper.toPhoto(request, staffMember, cameramen);

        Photo savedPhoto = photoRepository.save(photo);

        return photoMapper.toResponse(savedPhoto);
    }

    @Transactional
    public CreatePhotoResponse update(
            Long photoId,
            CreatePhotoRequest request
    ) {

        if (request.version() == null) {
            throw new IllegalArgumentException(
                    "Version number is required when updating Photo."
            );
        }

        Photo photo = photoRepository
                .findById(photoId)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Photo with id %d not found."
                                        .formatted(photoId)
                        )
                );

        if (!request.version().equals(photo.getVersion())) {
            throw new ObjectOptimisticLockingFailureException(
                    Photo.class,
                    photoId
            );
        }

        StaffMember staffMember = staffMemberRepository
                .findById(request.importedBy())
                .orElseThrow(() ->
                        new ResourceNotFoundException("Importer not found: " + request.importedBy()
                        )
                );

        Set<Cameraman> cameramen =
                new HashSet<>(cameramanRepository.findAllById(request.cameramanIds()));

        photoMapper.updatePhoto(
                photo,
                request,
                staffMember,
                cameramen
        );

        Photo savedPhoto = photoRepository.save(photo);

        return photoMapper.toResponse(savedPhoto);
    }

    @Transactional
    public void delete(Long photoId) {
        photoRepository.deleteById(photoId);
    }

    @Transactional(readOnly = true)
    public Page<CreatePhotoResponse> search(
            PhotoFilter filter,
            Pageable pageable) {

        Specification<Photo> spec = Specification.allOf();

        if (filter.importerId() != null) {
            spec = spec.and(
                    PhotoSpecification.hasImporterId(
                            filter.importerId()
                    )
            );
        }

        if (filter.cameramanId() != null) {
            spec = spec.and(
                    PhotoSpecification.hasCameramanId(
                            filter.cameramanId()
                    )
            );
        }

        if (filter.startDate() != null && filter.endDate() != null) {
            spec = spec.and(
                    PhotoSpecification.hasPhotoDateBetween(
                            filter.startDate(),
                            filter.effectiveEndDate()
                    )
            );
        }

        if (filter.searchTerm() != null && !filter.searchTerm().isBlank()) {
            spec = spec.and(
                    PhotoSpecification.hasSearchTerm(
                            filter.searchTerm()
                    )
            );
        }

        Pageable sortedPageable = PageRequest.of(
                pageable.getPageNumber(),
                pageable.getPageSize(),
                Sort.by("photoId").descending()
        );

        return photoRepository
                .findAll(spec, sortedPageable)
                .map(photoMapper::toResponse);
    }
}