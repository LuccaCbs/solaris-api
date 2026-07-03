package com.luccavergara.solaris.security;

import com.luccavergara.solaris.entity.User;
import com.luccavergara.solaris.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component("platformSecurity")
@RequiredArgsConstructor
public class PlatformSecurity {

    private final UserRepository userRepository;

    public boolean isPlatformOperator() {
        return resolveAuthenticatedUser()
                .map(user -> Boolean.TRUE.equals(user.getPlatformOperator()))
                .orElse(false);
    }

    private Optional<User> resolveAuthenticatedUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || !authentication.isAuthenticated()) {
            return Optional.empty();
        }

        Object principal = authentication.getPrincipal();

        if (principal instanceof User user) {
            return userRepository.findById(user.getId());
        }

        String email = authentication.getName();

        if (email == null || email.isBlank()) {
            return Optional.empty();
        }

        return userRepository.findByEmail(email);
    }
}
