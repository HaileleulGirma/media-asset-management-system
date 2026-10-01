package com.ena.mam.cassettecategory;

import com.ena.mam.dto.request.CreateCassetteCategoryRequest;
import com.ena.mam.dto.response.CreateCassetteCategoryResponse;
import com.ena.mam.exception.ResourceNotFoundException;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class CassetteCategoryService {
    private final CassetteCategoryRepository categoryRepository;
    public final CassetteCategoryMapper cassetteCategoryMapper;

    public CassetteCategoryService(CassetteCategoryRepository categoryRepository, CassetteCategoryMapper cassetteCategoryMapper) {
        this.categoryRepository = categoryRepository;
        this.cassetteCategoryMapper = cassetteCategoryMapper;
    }

    public CreateCassetteCategoryResponse create(CreateCassetteCategoryRequest request){

        CassetteCategory cassetteCategory = cassetteCategoryMapper.toCassette(request);
        CassetteCategory savedCassetteCategory = categoryRepository.save(cassetteCategory);

        return cassetteCategoryMapper.toResponse(savedCassetteCategory);
    }

    public CreateCassetteCategoryResponse update(Long categoryId, CreateCassetteCategoryRequest request){
        if (request.version() == null){
            throw new IllegalArgumentException("Version number is required when updating cassette category.");
        }



        CassetteCategory cassetteCategory = categoryRepository.findById(categoryId)
                .orElseThrow(() -> new ResourceNotFoundException("Cassette category with id %d not found.".formatted(categoryId)));

        if (!request.version().equals(cassetteCategory.getVersion())){
            throw new org.springframework.orm.ObjectOptimisticLockingFailureException(CassetteCategory.class, categoryId);
        }

        cassetteCategory.setCategoryName(request.categoryName());

        CassetteCategory savedCassetteCategory = categoryRepository.save(cassetteCategory);

        return cassetteCategoryMapper.toResponse(savedCassetteCategory);
    }

    public void delete(Long categoryId){categoryRepository.deleteById(categoryId);}

    public List<CreateCassetteCategoryResponse> findAll(){
        List<CassetteCategory> cassetteCategories = categoryRepository.findAll();

        return cassetteCategories.stream()
                .map(cassetteCategoryMapper::toResponse)
                .toList();
    }




}
