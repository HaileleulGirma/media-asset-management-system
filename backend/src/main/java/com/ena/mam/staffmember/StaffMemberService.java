package com.ena.mam.staffmember;

import com.ena.mam.dto.request.CreateStaffMemberRequest;
import com.ena.mam.dto.response.CreateStaffMemberResponse;
import com.ena.mam.exception.ResourceNotFoundException;
import org.springframework.stereotype.Service;

import java.util.List;

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

    public CreateStaffMemberResponse update(Long id, CreateStaffMemberRequest request){
        StaffMember staffMember = staffMemberRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Staff Member with id %d not found.".formatted(id)));

        staffMember.setStaffMemberName(request.staffMemberName());
        staffMember.setActive(request.isActive());

        StaffMember savedStaffMember = staffMemberRepository.save(staffMember);
        return staffMemberMapper.toResponse(savedStaffMember);
    }

    public void delete(Long id){
        staffMemberRepository.deleteById(id);
    }

    public CreateStaffMemberResponse findById(Long id){
        StaffMember staffMember = staffMemberRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Staff Member with id %d not found.".formatted(id)));
        return staffMemberMapper.toResponse(staffMember);
    }

    public List<CreateStaffMemberResponse> findAll(Boolean activeOnly){
        List<StaffMember> staffMembers = (activeOnly != null && activeOnly)
                ? staffMemberRepository.findByIsActiveTrue()
                : staffMemberRepository.findAll();

        return staffMembers.stream()
                .map(staffMemberMapper::toResponse)
                .toList();
    }
}
