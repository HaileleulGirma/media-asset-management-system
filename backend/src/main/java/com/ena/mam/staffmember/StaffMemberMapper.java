package com.ena.mam.staffmember;

import com.ena.mam.dto.request.CreateStaffMemberRequest;
import com.ena.mam.dto.response.CreateStaffMemberResponse;
import org.springframework.stereotype.Component;

@Component
public class StaffMemberMapper {

    public StaffMember toStaffMember(CreateStaffMemberRequest request){
        StaffMember staffMember = new StaffMember();
        staffMember.setStaffMemberName(request.staffMemberName());
        staffMember.setActive(request.isActive());

        return staffMember;
    }

    public CreateStaffMemberResponse toResponse(StaffMember staffMember){
        return new CreateStaffMemberResponse(
                staffMember.getStaffMemberId(),
                staffMember.getStaffMemberName(),
                staffMember.getActive()
                );
    }
}
