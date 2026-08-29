package com.ena.mam.approle;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/approles")
public class AppRoleController {

    private final AppRoleService appRoleService;

    public AppRoleController(AppRoleService appRoleService) {
        this.appRoleService = appRoleService;
    }

    // Only Admin needs to load the user creation form
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping
    public List<AppRole> findAll() {
        return appRoleService.findAll();
    }
}