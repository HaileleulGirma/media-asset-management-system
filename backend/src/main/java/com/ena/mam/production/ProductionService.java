package com.ena.mam.production;

import com.ena.mam.dto.request.CreateProductionRequest;
import com.ena.mam.dto.request.filter.ProductionFilter;
import com.ena.mam.dto.response.CreateProductionResponse;
import com.ena.mam.exception.ResourceNotFoundException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ProductionService {
    private final ProductionRepository repository;
    private final ProductionMapper mapper;

    public ProductionService(ProductionRepository repository, ProductionMapper mapper) {
        this.repository = repository;
        this.mapper = mapper;
    }

    @Transactional
    public CreateProductionResponse create(CreateProductionRequest request){
        Production production = mapper.toProduction(request);
        Production savedProduction = repository.save(production);

        return mapper.toResponse(savedProduction);
    }

    @Transactional
    public CreateProductionResponse update(Long id, CreateProductionRequest request){
        if (request.version() == null) {
            throw new IllegalArgumentException(
                    "Version number is required when updating Production."
            );
        }

        Production production = repository
                .findById(id)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Production with id %d not found."
                                        .formatted(id)
                        )
                );

        if (!request.version().equals(production.getVersion())) {
            throw new ObjectOptimisticLockingFailureException(
                    Production.class,
                    id
            );
        }

        mapper.updateProduction(production, request);

        Production savedProduction = repository.save(production);
        return mapper.toResponse(savedProduction);
    }

    @Transactional
    public void delete(Long id){repository.deleteById(id);}

    @Transactional(readOnly = true)
    public Page<CreateProductionResponse> search(
            ProductionFilter filter,
            Pageable pageable
    ){

        Specification<Production> spec = Specification.allOf();

        if (filter.searchTerm() != null && !filter.searchTerm().isBlank()) {
            spec = spec.and(
                    ProductionSpecification.hasSearchTerm(
                            filter.searchTerm()
                    )
            );
        }

        if (filter.startDate() != null) {
            spec = spec.and(
                    ProductionSpecification.hasNewsDateBetween(
                            filter.startDate(),
                            filter.effectiveEndDate()
                    )
            );
        }

        Pageable sortedPageable = PageRequest.of(
                pageable.getPageNumber(),
                pageable.getPageSize(),
                Sort.by("productionId").descending()
        );

        return repository
                .findAll(spec, sortedPageable)
                .map(mapper::toResponse);
    }

}
