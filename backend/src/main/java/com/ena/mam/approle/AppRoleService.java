package com.ena.mam.approle;

import org.springframework.stereotype.Service;
import java.util.List;

@Service
public class AppRoleService {

    private final AppRoleRepository appRoleRepository;

    public AppRoleService(AppRoleRepository appRoleRepository) {
        this.appRoleRepository = appRoleRepository;
    }

    public List<AppRole> findAll() {
        return appRoleRepository.findAll();
    }
}