package com.ena.mam.cassettecut;

import com.ena.mam.cassettecategory.CassetteCategory;
import com.ena.mam.cassettecategory.CassetteCategoryRepository;
import com.ena.mam.digitizedmedia.DigitizedMedia;
import com.ena.mam.digitizedmedia.DigitizedMediaRepository;
import com.ena.mam.dto.request.CreateCassetteCutRequest;
import com.ena.mam.dto.request.filter.CassetteCutFilter;
import com.ena.mam.dto.response.CreateCassetteCutResponse;
import com.ena.mam.exception.ResourceNotFoundException;
import com.ena.mam.staffmember.StaffMemberRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CassetteCutService {
    private final CassetteCutRepository cassetteCutRepository;
    private final CassetteCutMapper cassetteCutMapper;
    private final CassetteCategoryRepository cassetteCategoryRepository;
    private final StaffMemberRepository staffMemberRepository;
    private final DigitizedMediaRepository digitizedMediaRepository;

    public CassetteCutService(CassetteCutRepository cassetteCutRepository, CassetteCutMapper cassetteCutMapper, CassetteCategoryRepository cassetteCategoryRepository, StaffMemberRepository staffMemberRepository, DigitizedMediaRepository digitizedMediaRepository) {
        this.cassetteCutRepository = cassetteCutRepository;
        this.cassetteCutMapper = cassetteCutMapper;
        this.cassetteCategoryRepository = cassetteCategoryRepository;
        this.staffMemberRepository = staffMemberRepository;
        this.digitizedMediaRepository = digitizedMediaRepository;
    }

    @Transactional
    public CreateCassetteCutResponse create(CreateCassetteCutRequest request) {

        DigitizedMedia digitizedMedia = digitizedMediaRepository
                .findById(request.digitizedMediaId())
                .orElseThrow(() ->new ResourceNotFoundException("Digitized Media not found: "+ request.digitizedMediaId()
                        )
                );

        CassetteCategory cassetteCategory = cassetteCategoryRepository
                .findById(request.cassetteCategoryId())
                .orElseThrow(() ->
                        new ResourceNotFoundException("Cassette category not found: "+ request.cassetteCategoryId()
                        )
                );

        CassetteCut cassetteCut = cassetteCutMapper.toCassetteCut(
                request,
                digitizedMedia,
                cassetteCategory
        );

        CassetteCut savedCassetteCut =
                cassetteCutRepository.save(cassetteCut);

        return cassetteCutMapper.toResponse(savedCassetteCut);
    }

    @Transactional
    public CreateCassetteCutResponse update(
            Long cutId,
            CreateCassetteCutRequest request) {

        if (request.version() == null) {
            throw new IllegalArgumentException(
                    "Version number is required when updating Cassette Cut."
            );
        }

        CassetteCut cassetteCut = cassetteCutRepository
                .findById(cutId)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Cassette Cut with id %d not found."
                                        .formatted(cutId)
                        )
                );

        if (!request.version().equals(cassetteCut.getVersion())) {
            throw new org.springframework.orm.ObjectOptimisticLockingFailureException(
                    CassetteCut.class,
                    cutId
            );
        }

        DigitizedMedia digitizedMedia = digitizedMediaRepository
                .findById(request.digitizedMediaId())
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Digitized Media not found: "
                                        + request.digitizedMediaId()
                        )
                );

        CassetteCategory cassetteCategory = cassetteCategoryRepository
                .findById(request.cassetteCategoryId())
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Cassette category not found: "
                                        + request.cassetteCategoryId()
                        )
                );

        cassetteCutMapper.updateCassetteCut(
                cassetteCut,
                request,
                digitizedMedia,
                cassetteCategory
        );

        CassetteCut savedCassetteCut =
                cassetteCutRepository.save(cassetteCut);

        return cassetteCutMapper.toResponse(savedCassetteCut);
    }

    @Transactional
    public void delete(Long cutId) {
        cassetteCutRepository.deleteById(cutId);
    }

    @Transactional(readOnly = true)
    public Page<CreateCassetteCutResponse> search(
            CassetteCutFilter filter,
            Pageable pageable) {

        Specification<CassetteCut> spec = Specification.allOf();

        if (filter.digitizedMediaId() != null) {
            spec = spec.and(
                    CassetteCutSpecification.hasDigitizedMediaId(
                            filter.digitizedMediaId()
                    )
            );
        }

        if (filter.categoryId() != null) {
            spec = spec.and(
                    CassetteCutSpecification.hasCategoryId(
                            filter.categoryId()
                    )
            );
        }

        if (filter.cutDate() != null) {
            spec = spec.and(
                    CassetteCutSpecification.hasCutDate(
                            filter.cutDate()
                    )
            );
        }

        if (filter.searchTerm() != null
                && !filter.searchTerm().isBlank()) {

            spec = spec.and(
                    CassetteCutSpecification.hasSearchTerm(
                            filter.searchTerm()
                    )
            );
        }

        Pageable sortedPageable = PageRequest.of(
                pageable.getPageNumber(),
                pageable.getPageSize(),
                Sort.by("cutId").descending()
        );

        return cassetteCutRepository
                .findAll(spec, sortedPageable)
                .map(cassetteCutMapper::toResponse);
    }

}
