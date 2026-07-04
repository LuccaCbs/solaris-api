package com.luccavergara.solaris.service;

import com.luccavergara.solaris.entity.User;
import com.luccavergara.solaris.repository.UserRepository;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

@Service
public class AuthenticatedUserService {

    private final UserRepository userRepository;

    public AuthenticatedUserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public User getCurrentUser() {
        Authentication authentication = SecurityContextHolder
                .getContext()
                .getAuthentication();

        if (authentication == null || !authentication.isAuthenticated()) {
            throw new AccessDeniedException("Not authenticated");
        }

        Object principal = authentication.getPrincipal();

        if (principal instanceof User user && user.getId() != null) {
            return user;
        }

        final String email;

        if (principal instanceof User userPrincipal) {
            email = userPrincipal.getEmail();
        } else if (principal instanceof UserDetails userDetails) {
            email = userDetails.getUsername();
        } else {
            email = authentication.getName();
        }

        if (email == null || email.isBlank() || "anonymousUser".equals(email)) {
            throw new AccessDeniedException("Authenticated principal does not contain a user email");
        }

        return userRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new AccessDeniedException("Authenticated user not found for email " + email));
    }
}