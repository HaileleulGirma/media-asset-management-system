package com.ena.mam.staffmember;

import com.ena.mam.dto.request.CreateStaffMemberRequest;
import com.ena.mam.dto.response.CreateStaffMemberResponse;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class StaffMemberController {

    private final StaffMemberService staffMemberService;

    public StaffMemberController(StaffMemberService staffMemberService) {
        this.staffMemberService = staffMemberService;
    }

    @PostMapping("/api/staffmember/create")
    public CreateStaffMemberResponse create(
            @Valid @RequestBody CreateStaffMemberRequest request
            ){
       return staffMemberService.create(request);

    }
}
