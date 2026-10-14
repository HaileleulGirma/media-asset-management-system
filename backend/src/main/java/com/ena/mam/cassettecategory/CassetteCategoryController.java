package com.ena.mam.cassettecategory;

import com.ena.mam.dto.request.CreateCassetteCategoryRequest;
import com.ena.mam.dto.response.CreateCassetteCategoryResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
public class CassetteCategoryController {

    private final CassetteCategoryService cassetteCategoryService;

    public CassetteCategoryController(CassetteCategoryService cassetteCategoryService) {
        this.cassetteCategoryService = cassetteCategoryService;
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/api/cassette-category")
    public CreateCassetteCategoryResponse create(CreateCassetteCategoryRequest request){

        return cassetteCategoryService.create(request);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/api/cassette-category/{cassetteCategoryId}")
    public CreateCassetteCategoryResponse update(
            @RequestParam
            CreateCassetteCategoryRequest request,
            @PathVariable
            Long cassetteCategoryId){

        return cassetteCategoryService.update(cassetteCategoryId, request);

    }

    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/api/cassette-category/{cassetteCategoryId}")
    public ResponseEntity<Void> delete(
           @PathVariable Long cassetteCategoryId){
        cassetteCategoryService.delete(cassetteCategoryId);
        return ResponseEntity.noContent().build();

    }

    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
    @GetMapping("/api/cassette-category/{cassetteCategoryId}")
    public CreateCassetteCategoryResponse findById(
            @PathVariable
            Long cassetteCategoryId
    ){

        return cassetteCategoryService.findById(cassetteCategoryId);
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
    @GetMapping("/api/cassette-category")
    public List<CreateCassetteCategoryResponse> findAll(){

        return cassetteCategoryService.findAll();
    }
}
