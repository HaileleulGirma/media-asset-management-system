package com.ena.mam.appuser;
import com.ena.mam.approle.AppRole;
import com.ena.mam.approle.AppRoleRepository;
import com.ena.mam.dto.request.CreateAppUserRequest;
import com.ena.mam.dto.request.UpdateAppUserRequest;
import com.ena.mam.dto.response.CreateAppUserResponse;
import com.ena.mam.exception.ResourceNotFoundException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Set;

@Service
public class AppUserService {

    // The only roles this service will ever assign. The admin role is
    // deliberately left out -- there is exactly one system administrator
    // account, created outside this flow (see DataSeeder), and it can
    // never be created, promoted into, or deleted here. It CAN be edited
    // by itself (see update() below) for name/username/password, but its
    // role can never change and no one else can edit it.
    private static final Set<String> ASSIGNABLE_ROLE_NAMES = Set.of("VIEWER", "STAFF");

    private final AppUserMapper appUserMapper;
    private final AppUserRepository appUserRepository;
    private final PasswordEncoder passwordEncoder;
    private final AppRoleRepository appRoleRepository;

    public AppUserService(AppUserMapper appUserMapper,
                          AppUserRepository appUserRepository,
                          PasswordEncoder passwordEncoder,
                          AppRoleRepository appRoleRepository) {
        this.appUserMapper = appUserMapper;
        this.appUserRepository = appUserRepository;
        this.passwordEncoder = passwordEncoder;
        this.appRoleRepository = appRoleRepository;
    }

    private Long currentUserId() {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        return appUserRepository.findByUsernameWithRoles(username)
                .orElseThrow(() -> new ResourceNotFoundException("Authenticated user not found"))
                .getUserId();
    }

    private boolean isAssignable(AppRole role) {
        return role != null && ASSIGNABLE_ROLE_NAMES.contains(role.getRoleName());
    }

    public CreateAppUserResponse create(CreateAppUserRequest request) {
        AppRole role = appRoleRepository.findById(request.role())
                .orElseThrow(() -> new ResourceNotFoundException("Role not found: " + request.role()));

        if (!isAssignable(role)) {
            throw new IllegalArgumentException("That role cannot be assigned to a new user.");
        }

        AppUser appUser = appUserMapper.toAppUser(request);
        appUser.setPassword(passwordEncoder.encode(appUser.getPassword()));
        AppUser savedAppUser = appUserRepository.save(appUser);
        return appUserMapper.toResponse(savedAppUser);
    }

    public CreateAppUserResponse update(Long appUserId, UpdateAppUserRequest request) {
        AppUser appUser = appUserRepository.findById(appUserId)
                .orElseThrow(() -> new ResourceNotFoundException("AppUser with id %d not found.".formatted(appUserId)));

        boolean isSelf = appUserId.equals(currentUserId());
        boolean targetIsNonAssignable = !isAssignable(appUser.getRole());

        // A non-assignable account (i.e. the system admin) can only ever be
        // edited by itself. Anyone else touching it -- even another admin,
        // if one somehow existed -- is rejected outright, same as before.
        if (targetIsNonAssignable && !isSelf) {
            throw new IllegalArgumentException("This account cannot be edited here.");
        }

        if (targetIsNonAssignable) {
            // Admin editing its own profile: role is fixed and intentionally
            // never looked up or reassigned here, so there's no risk of the
            // admin role being changed or removed through this path.
            appUser.setFullName(request.fullName());
            appUser.setUsername(request.username());

            if (request.password() != null && !request.password().trim().isEmpty()) {
                appUser.setPassword(passwordEncoder.encode(request.password()));
            }

            AppUser savedAppUser = appUserRepository.save(appUser);
            return appUserMapper.toResponse(savedAppUser);
        }

        AppRole newRole = appRoleRepository.findById(request.role())
                .orElseThrow(() -> new ResourceNotFoundException("Role not found: " + request.role()));

        if (!isAssignable(newRole)) {
            throw new IllegalArgumentException("That role cannot be assigned.");
        }

        if (isSelf && !appUser.getRole().getRoleId().equals(request.role())) {
            throw new IllegalArgumentException("You cannot change your own role.");
        }

        appUser.setFullName(request.fullName());
        appUser.setUsername(request.username());
        appUser.setRole(newRole);

        if (request.password() != null && !request.password().trim().isEmpty()) {
            appUser.setPassword(passwordEncoder.encode(request.password()));
        }

        AppUser savedAppUser = appUserRepository.save(appUser);
        return appUserMapper.toResponse(savedAppUser);
    }

    public void delete(Long id) {
        AppUser target = appUserRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("AppUser with id %d not found.".formatted(id)));

        if (!isAssignable(target.getRole())) {
            throw new IllegalArgumentException("This account cannot be deleted.");
        }
        if (id.equals(currentUserId())) {
            throw new IllegalArgumentException("You cannot delete your own account.");
        }
        appUserRepository.deleteById(id);
    }

    public CreateAppUserResponse findById(Long id) {
        AppUser appUser = appUserRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("AppUser with id %d not found.".formatted(id)));
        return appUserMapper.toResponse(appUser);
    }

    public List<CreateAppUserResponse> findAll() {
        return appUserRepository.findAll()
                .stream()
                .map(appUserMapper::toResponse)
                .toList();
    }
}