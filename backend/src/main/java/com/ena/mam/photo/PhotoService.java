package com.ena.mam.photo;

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

@Service
public class PhotoService {

    private final StaffMemberRepository staffMemberRepository;
    private final PhotoMapper photoMapper;
    private final PhotoRepository photoRepository;

    public PhotoService(
            StaffMemberRepository staffMemberRepository,
            PhotoMapper photoMapper,
            PhotoRepository photoRepository
    ) {
        this.staffMemberRepository = staffMemberRepository;
        this.photoMapper = photoMapper;
        this.photoRepository = photoRepository;
    }

    @Transactional
    public CreatePhotoResponse create(CreatePhotoRequest request) {

        StaffMember staffMember = staffMemberRepository
                .findById(request.importedBy())
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Importer not found: " + request.importedBy()
                        )
                );

        Photo photo = photoMapper.toPhoto(request, staffMember);

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

        if (request.version() != photo.getVersion()) {
            throw new ObjectOptimisticLockingFailureException(
                    Photo.class,
                    photoId
            );
        }

        StaffMember staffMember = staffMemberRepository
                .findById(request.importedBy())
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Importer not found: " + request.importedBy()
                        )
                );

        photoMapper.updatePhoto(
                photo,
                request,
                staffMember
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
            Pageable pageable
    ) {

        Specification<Photo> spec = Specification.allOf();

        if (filter.startDate() != null) {
            spec = spec.and(
                    PhotoSpecification.hasPhotoDateBetween(
                            filter.startDate(),
                            filter.effectiveEndDate()
                    )
            );
        }

        if (filter.importerId() != null) {
            spec = spec.and(
                    PhotoSpecification.hasImporterId(
                            filter.importerId()
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
                Sort.by("photoDate").descending()
        );

        return photoRepository
                .findAll(spec, sortedPageable)
                .map(photoMapper::toResponse);
    }
}