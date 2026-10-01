package com.ena.mam.cassettecategory;

import com.ena.mam.dto.request.CreateCassetteCategoryRequest;
import com.ena.mam.dto.response.CreateCassetteCategoryResponse;
import org.springframework.stereotype.Component;

@Component
public class CassetteCategoryMapper {
    public CassetteCategory toCassette(CreateCassetteCategoryRequest request){
        CassetteCategory cassetteCategory = new CassetteCategory();
        cassetteCategory.setCategoryName(request.categoryName());

        return cassetteCategory;
    }

    public CreateCassetteCategoryResponse toResponse(CassetteCategory cassetteCategory){
        return new CreateCassetteCategoryResponse(cassetteCategory.getCategoryId(), cassetteCategory.getCategoryName(), cassetteCategory.getVersion());
    }
}
