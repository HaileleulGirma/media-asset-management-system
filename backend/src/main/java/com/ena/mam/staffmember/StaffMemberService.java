package com.ena.mam.staffmember;

import com.ena.mam.dto.request.CreateStaffMemberRequest;
import com.ena.mam.dto.response.CreateStaffMemberResponse;
import org.springframework.stereotype.Service;

@Service
public class StaffMemberService {
    private final StaffMemberRepository staffMemberRepository;
    private final StaffMemberMapper staffMemberMapper;

    public StaffMemberService(StaffMemberRepository staffMemberRepository, StaffMemberMapper staffMemberMapper) {
        this.staffMemberRepository = staffMemberRepository;
        this.staffMemberMapper = staffMemberMapper;
    }

    public CreateStaffMemberResponse create(CreateStaffMemberRequest request){
        StaffMember staffMember = staffMemberMapper.toStaffMember(request);
        StaffMember savedStaffMember = staffMemberRepository.save(staffMember);
        return staffMemberMapper.toResponse(savedStaffMember);
    }
}
