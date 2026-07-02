package com.ena.mam.security.config;

import com.ena.mam.appuser.AppUserRepository;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

@Configuration
public class UserDetailsConfig {

    private final AppUserRepository userRepository;

    public UserDetailsConfig(AppUserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Bean
    public UserDetailsService userDetailsService() {
        return username -> userRepository.findByUsernameWithRoles(username)
                .map(user -> new org.springframework.security.core.userdetails.User(
                        user.getUsername(),
                        user.getPassword(),
                        java.util.List.of(new SimpleGrantedAuthority("ROLE_" + user.getRole().getRoleName()))
                ))
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + username));
    }
}