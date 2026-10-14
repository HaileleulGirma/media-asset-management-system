package com.ena.mam.staffmember;

import com.ena.mam.dto.request.CreateStaffMemberRequest;
import com.ena.mam.dto.response.CreateStaffMemberResponse;
import com.ena.mam.util.ClientIpUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
public class StaffMemberController {

    private final StaffMemberService staffMemberService;

    public StaffMemberController(StaffMemberService staffMemberService) {
        this.staffMemberService = staffMemberService;
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/api/staffmember/")
    public CreateStaffMemberResponse create(
            @Valid @RequestBody CreateStaffMemberRequest request,
            Authentication authentication,
            HttpServletRequest httpRequest
    ) {
        String currentUsername = (authentication != null) ? authentication.getName() : "SYSTEM";
        CreateStaffMemberResponse response = staffMemberService.create(request);

        log.info("AUDIT_CREATE | Module: STAFF_MEMBER | Status: 201 | User: '{}' | IP: {} | Method: {} | CreatedStaffMemberId: {} | Payload: {}",
                currentUsername, ClientIpUtil.getClientIp(httpRequest), httpRequest.getMethod(), response.staffMemberId(), request);

        return response;
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/api/staffmember/{staffMemberId}")
    public CreateStaffMemberResponse update(
            @PathVariable Long staffMemberId,
            @Valid @RequestBody CreateStaffMemberRequest request,
            Authentication authentication,
            HttpServletRequest httpRequest
    ) {
        String currentUsername = (authentication != null) ? authentication.getName() : "SYSTEM";

        log.info("AUDIT_UPDATE | Module: STAFF_MEMBER | Status: 200 | User: '{}' | IP: {} | Method: {} | StaffMemberId: {} | Payload: {}",
                currentUsername, ClientIpUtil.getClientIp(httpRequest), httpRequest.getMethod(), staffMemberId, request);

        return staffMemberService.update(staffMemberId, request);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/api/staffmember/{staffMemberId}")
    public ResponseEntity<Void> delete(
            @PathVariable Long staffMemberId,
            Authentication authentication,
            HttpServletRequest httpRequest
    ) {
        String currentUsername = (authentication != null) ? authentication.getName() : "SYSTEM";

        log.info("AUDIT_DELETE | Module: STAFF_MEMBER | Status: 204 | User: '{}' | IP: {} | Method: {} | StaffMemberId: {}",
                currentUsername, ClientIpUtil.getClientIp(httpRequest), httpRequest.getMethod(), staffMemberId);

        staffMemberService.delete(staffMemberId);

        return ResponseEntity.noContent().build();
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
    @GetMapping("/api/staffmember/{staffMemberId}")
    public CreateStaffMemberResponse findById(
            @PathVariable Long staffMemberId,
            Authentication authentication,
            HttpServletRequest httpRequest
    ) {
        String currentUsername = (authentication != null) ? authentication.getName() : "SYSTEM";

        log.info("AUDIT_READ | Module: STAFF_MEMBER | Status: 200 | User: '{}' | IP: {} | Method: {} | StaffMemberId: {}",
                currentUsername, ClientIpUtil.getClientIp(httpRequest), httpRequest.getMethod(), staffMemberId);

        return staffMemberService.findById(staffMemberId);
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
    @GetMapping("/api/staffmember")
    public List<CreateStaffMemberResponse> findAll(
            @RequestParam(required = false, defaultValue = "true") Boolean activeOnly,
            Authentication authentication,
            HttpServletRequest httpRequest
    ) {
        String currentUsername = (authentication != null) ? authentication.getName() : "SYSTEM";

        log.info("AUDIT_READ | Module: STAFF_MEMBER | Status: 200 | User: '{}' | IP: {} | Method: {} | Action: FIND_ALL | ActiveOnly: {}",
                currentUsername, ClientIpUtil.getClientIp(httpRequest), httpRequest.getMethod(), activeOnly);

        return staffMemberService.findAll(activeOnly);
    }
}