package com.ena.mam.approle;

import com.ena.mam.util.ClientIpUtil;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/approles")
public class AppRoleController {

    private final AppRoleService appRoleService;

    public AppRoleController(AppRoleService appRoleService) {
        this.appRoleService = appRoleService;
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping
    public List<AppRole> findAll(Authentication authentication, HttpServletRequest httpRequest) {
        String currentUsername = (authentication != null) ? authentication.getName() : "SYSTEM";

        log.info("AUDIT_READ | Module: APP_ROLE | Status: 200 | User: '{}' | IP: {} | Method: {} | Action: FIND_ALL",
                currentUsername, ClientIpUtil.getClientIp(httpRequest), httpRequest.getMethod());

        return appRoleService.findAll();
    }
}