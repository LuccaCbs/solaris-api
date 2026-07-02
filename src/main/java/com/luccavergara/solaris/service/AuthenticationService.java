package com.luccavergara.solaris.service;

import com.luccavergara.solaris.dto.AuthenticationRequest;
import com.luccavergara.solaris.dto.AuthenticationResponse;
import com.luccavergara.solaris.dto.RegisterRequest;
import com.luccavergara.solaris.dto.SelectOrganizationRequest;
import com.luccavergara.solaris.entity.Role;
import com.luccavergara.solaris.entity.User;
import com.luccavergara.solaris.repository.UserRepository;
import com.luccavergara.solaris.security.JwtService;
import com.luccavergara.solaris.entity.Category;
import com.luccavergara.solaris.repository.CategoryRepository;
import com.luccavergara.solaris.dto.RegisterResponse;
import com.luccavergara.solaris.exception.DuplicateResourceException;
import java.time.LocalDateTime;
import com.luccavergara.solaris.entity.AuditAction;
import com.luccavergara.solaris.entity.AuditEntityType;

import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthenticationService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;
    private final CategoryRepository categoryRepository;
    private final EmailVerificationService emailVerificationService;
    private final AuditLogService auditLogService;
    private final OrganizationMembershipService organizationMembershipService;
    private final AuthenticatedUserService authenticatedUserService;

    public RegisterResponse register(RegisterRequest request) {

        if (userRepository.findByEmail(request.getEmail()).isPresent()) {
            throw new DuplicateResourceException("Email already registered");
        }

        var user = User.builder()
                .firstname(request.getFirstname())
                .lastname(request.getLastname())
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .role(Role.USER)
                .emailVerified(false)
                .build();

        User savedUser = userRepository.save(user);

        auditLogService.log(
                AuditAction.REGISTER_USER,
                AuditEntityType.USER,
                savedUser.getId(),
                savedUser.getFirstname() + " " + savedUser.getLastname(),
                "User registered"
        );

        emailVerificationService.createAndLogVerificationToken(savedUser);

        categoryRepository.save(
                Category.builder()
                        .name("General")
                        .description("Default category")
                        .createdAt(LocalDateTime.now())
                        .systemCategory(true)
                        .user(savedUser)
                        .build()
        );

        return RegisterResponse.builder()
                .message("Account created successfully. Please verify your email.")
                .build();
    }

    public AuthenticationResponse authenticate(AuthenticationRequest request) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.getEmail(),
                        request.getPassword()
                )
        );

        var user = userRepository.findByEmail(request.getEmail())
                .orElseThrow();

        auditLogService.log(
                AuditAction.LOGIN,
                AuditEntityType.USER,
                user.getId(),
                user.getFirstname() + " " + user.getLastname(),
                "User logged in"
        );

        var jwtToken = organizationMembershipService.findPrimaryMembership(user)
                .map(membership -> jwtService.generateToken(
                        organizationMembershipService.buildJwtClaims(membership),
                        user
                ))
                .orElseGet(() -> jwtService.generateToken(user));

        return AuthenticationResponse.builder()
                .token(jwtToken)
                .build();
    }

    public AuthenticationResponse selectOrganization(SelectOrganizationRequest request) {
        User user = authenticatedUserService.getCurrentUser();

        var membership = organizationMembershipService.resolveMembershipForOrganization(
                user,
                request.getOrganizationId()
        );

        if (request.getStoreId() != null) {
            organizationMembershipService.validateStoreInOrganization(
                    request.getOrganizationId(),
                    request.getStoreId()
            );
        }

        Map<String, Object> claims = organizationMembershipService.buildJwtClaims(
                membership,
                request.getStoreId()
        );
        String jwtToken = jwtService.generateToken(claims, user);

        return AuthenticationResponse.builder()
                .token(jwtToken)
                .build();
    }
}