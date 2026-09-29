package com.ena.mam.digitizedmedia;

import com.ena.mam.dto.request.CreateDigitizedMediaRequest;
import com.ena.mam.dto.request.DigitizedMediaFilter;
import com.ena.mam.dto.response.CreateDigitizedMediaResponse;
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


    public DigitizedMediaService(StaffMemberRepository staffMemberRepository, DigitizedMediaMapper digitizedMediaMapper, DigitizedMediaRepository digitizedMediaRepository)  {
        this.staffMemberRepository = staffMemberRepository;
        this.digitizedMediaMapper = digitizedMediaMapper;
        this.digitizedMediaRepository = digitizedMediaRepository;
    }

    @Transactional
    public CreateDigitizedMediaResponse create(CreateDigitizedMediaRequest request){
        StaffMember staffMember = staffMemberRepository.findById(request.importedBy())
                .orElseThrow(() -> new ResourceNotFoundException("Importer not found: " + request.importedBy()));



        DigitizedMedia digitizedMedia = digitizedMediaMapper.toDigitizedMedia(request, staffMember);
        DigitizedMedia savedDigitizedMedia = digitizedMediaRepository.save(digitizedMedia);
        return digitizedMediaMapper.toResponse(savedDigitizedMedia);

    }

    @Transactional
    public CreateDigitizedMediaResponse update(Long digitizedMediaId, CreateDigitizedMediaRequest request){
        if (request.version() == null){
            throw new IllegalArgumentException("Version number is required when updating Digitized Media.");
        }

        DigitizedMedia digitizedMedia = digitizedMediaRepository
                .findById(digitizedMediaId).orElseThrow(() -> new ResourceNotFoundException("Digitized Media with id %d not found.".formatted(digitizedMediaId)));

        if (request.version() != digitizedMedia.getVersion()){
            throw new org.springframework.orm.ObjectOptimisticLockingFailureException(DigitizedMedia.class, digitizedMediaId);
        }

        StaffMember staffMember = staffMemberRepository.findById(request.importedBy())
                .orElseThrow(() -> new ResourceNotFoundException("Importer not found: " + request.importedBy()));

        digitizedMediaMapper.updateDigitizedMedia(digitizedMedia, request, staffMember);

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

        if (filter.searchTerm() != null && !filter.searchTerm().isBlank()) {
            spec = spec.and(
                    DigitizedMediaSpecification.hasSearchTerm(
                            filter.searchTerm()
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
