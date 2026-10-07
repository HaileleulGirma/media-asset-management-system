package com.ena.mam.digitizedmedia;

import com.ena.mam.cassettecategory.CassetteCategory;
import com.ena.mam.cassettecategory.CassetteCategoryRepository;
import com.ena.mam.dto.request.CreateDigitizedMediaRequest;
import com.ena.mam.dto.request.filter.DigitizedMediaFilter;
import com.ena.mam.dto.response.CreateDigitizedMediaResponse;
import com.ena.mam.exception.DuplicateResourceException;
import com.ena.mam.exception.ResourceNotFoundException;
import com.ena.mam.staffmember.StaffMember;
import com.ena.mam.staffmember.StaffMemberRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DigitizedMediaService {
    private final StaffMemberRepository staffMemberRepository;
    private final DigitizedMediaMapper digitizedMediaMapper;
    private final DigitizedMediaRepository digitizedMediaRepository;
    private final CassetteCategoryRepository cassetteCategoryRepository;


    public DigitizedMediaService(StaffMemberRepository staffMemberRepository, DigitizedMediaMapper digitizedMediaMapper, DigitizedMediaRepository digitizedMediaRepository, CassetteCategoryRepository cassetteCategoryRepository)  {
        this.staffMemberRepository = staffMemberRepository;
        this.digitizedMediaMapper = digitizedMediaMapper;
        this.digitizedMediaRepository = digitizedMediaRepository;
        this.cassetteCategoryRepository = cassetteCategoryRepository;
    }

    @Transactional
    public CreateDigitizedMediaResponse create(CreateDigitizedMediaRequest request){

        if (digitizedMediaRepository
                .existsByCassetteCategoryCategoryIdAndIdentifierNumber(
                        request.identifierCategoryId(),
                        request.identifierNumber()
                )) {

            throw new DuplicateResourceException(
                    "A Digitized Media with category ID %d and identifier number %d already exists."
                            .formatted(
                                    request.identifierCategoryId(),
                                    request.identifierNumber()
                            )
            );
        }

        StaffMember staffMember = staffMemberRepository.findById(request.importedBy())
                .orElseThrow(() -> new ResourceNotFoundException("Importer not found: " + request.importedBy()));

        CassetteCategory cassetteCategory = cassetteCategoryRepository.findById(request.identifierCategoryId())
                .orElseThrow(() -> new ResourceNotFoundException("Cassette category not found: " + request.identifierCategoryId()));


        DigitizedMedia digitizedMedia = digitizedMediaMapper.toDigitizedMedia(request, staffMember, cassetteCategory);
        DigitizedMedia savedDigitizedMedia = digitizedMediaRepository.save(digitizedMedia);
        return digitizedMediaMapper.toResponse(savedDigitizedMedia);

    }

    @Transactional
    public CreateDigitizedMediaResponse update(Long digitizedMediaId, CreateDigitizedMediaRequest request){

        if (request.version() == null){
            throw new IllegalArgumentException("Version number is required when updating Digitized Media.");
        }

        if (digitizedMediaRepository
                .existsByCassetteCategoryCategoryIdAndIdentifierNumberAndDigitizedMediaIdNot(
                        request.identifierCategoryId(),
                        request.identifierNumber(),
                        digitizedMediaId
                )) {

            throw new DuplicateResourceException(
                    "A Digitized Media with category ID %d and identifier number %d already exists."
                            .formatted(
                                    request.identifierCategoryId(),
                                    request.identifierNumber()
                            )
            );
        }

        DigitizedMedia digitizedMedia = digitizedMediaRepository
                .findById(digitizedMediaId).orElseThrow(() -> new ResourceNotFoundException("Digitized Media with id %d not found.".formatted(digitizedMediaId)));

        if (!request.version().equals(digitizedMedia.getVersion())){
            throw new org.springframework.orm.ObjectOptimisticLockingFailureException(DigitizedMedia.class, digitizedMediaId);
        }

        StaffMember staffMember = staffMemberRepository.findById(request.importedBy())
                .orElseThrow(() -> new ResourceNotFoundException("Importer not found: " + request.importedBy()));

        CassetteCategory cassetteCategory = cassetteCategoryRepository.findById(request.identifierCategoryId())
                .orElseThrow(() -> new ResourceNotFoundException("Cassette category not found: " + request.identifierCategoryId()));

        digitizedMediaMapper.updateDigitizedMedia(digitizedMedia, request, staffMember, cassetteCategory);

        DigitizedMedia savedDigitizedMedia = digitizedMediaRepository.save(digitizedMedia);

        return digitizedMediaMapper.toResponse(savedDigitizedMedia);
    }

    @Transactional
    public void delete(Long digitizedMediaId){digitizedMediaRepository.deleteById(digitizedMediaId);}

    @Transactional(readOnly = true)
    public Page<CreateDigitizedMediaResponse> search(
            DigitizedMediaFilter filter,
            Pageable pageable) {

        Specification<DigitizedMedia> spec = Specification.allOf();

        if (filter.importerId() != null) {
            spec = spec.and(
                    DigitizedMediaSpecification.hasImporterId(
                            filter.importerId()
                    )
            );
        }

        if (filter.categoryId() != null) {
            spec = spec.and(
                    DigitizedMediaSpecification.hasCategoryId(
                            filter.categoryId()
                    )
            );
        }

        if (filter.identifierNumber() != null) {
            spec = spec.and(
                    DigitizedMediaSpecification.hasIdentifierNumber(
                            filter.identifierNumber()
                    )
            );
        }

        Pageable sortedPageable = PageRequest.of(
                pageable.getPageNumber(),
                pageable.getPageSize(),
                Sort.by("digitizedMediaId").descending()
        );

        return digitizedMediaRepository
                .findAll(spec, sortedPageable)
                .map(digitizedMediaMapper::toResponse);
    }
}
