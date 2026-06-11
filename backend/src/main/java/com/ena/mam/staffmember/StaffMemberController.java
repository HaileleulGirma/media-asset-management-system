package com.ena.mam.staffmember;

import com.ena.mam.dto.request.CreateStaffMemberRequest;
import com.ena.mam.dto.response.CreateStaffMemberResponse;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
public class StaffMemberController {

    private final StaffMemberService staffMemberService;

    public StaffMemberController(StaffMemberService staffMemberService) {
        this.staffMemberService = staffMemberService;
    }

    @PostMapping("/api/staffmember/")
    public CreateStaffMemberResponse create(
            @Valid @RequestBody CreateStaffMemberRequest request
            ){
       return staffMemberService.create(request);

    }

    @PutMapping("/api/staffmember/{staffMemberId}")
    public CreateStaffMemberResponse update(
           @PathVariable Long staffMemberId, @Valid @RequestBody CreateStaffMemberRequest request
    ){
        return staffMemberService.update(staffMemberId, request);
    }

    @DeleteMapping("/api/staffmember/{staffMemberId}")
    public ResponseEntity<Void> delete(
            @PathVariable Long staffMemberId
    ){
        staffMemberService.delete(staffMemberId);

        return ResponseEntity.noContent().build();
    }

    @GetMapping("/api/staffmember/{staffMemberId}")
    public CreateStaffMemberResponse findById(
            @PathVariable Long staffMemberId){
        return staffMemberService.findById(staffMemberId);
    }

    @GetMapping("/api/staffmember")
    public List<CreateStaffMemberResponse> findAll(){
        return staffMemberService.findAll();
    }

}
