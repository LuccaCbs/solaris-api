package com.luccavergara.solaris.service;

import com.luccavergara.solaris.dto.AuthenticationRequest;
import com.luccavergara.solaris.dto.AuthenticationResponse;
import com.luccavergara.solaris.dto.RegisterRequest;
import com.luccavergara.solaris.entity.Role;
import com.luccavergara.solaris.entity.User;
import com.luccavergara.solaris.repository.UserRepository;
import com.luccavergara.solaris.security.JwtService;
import com.luccavergara.solaris.entity.Category;
import com.luccavergara.solaris.repository.CategoryRepository;
import java.time.LocalDateTime;

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
    public AuthenticationResponse register(RegisterRequest request) {
        var user = User.builder()
                .firstname(request.getFirstname())
                .lastname(request.getLastname())
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .role(Role.USER)
                .build();

        User savedUser = userRepository.save(user);

        categoryRepository.save(
                Category.builder()
                        .name("General")
                        .description("Default category")
                        .createdAt(LocalDateTime.now())
                        .systemCategory(true)
                        .user(savedUser)
                        .build()
        );

        var jwtToken = jwtService.generateToken(savedUser);

        return AuthenticationResponse.builder()
                .token(jwtToken)
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

        var jwtToken = jwtService.generateToken(user);

        return AuthenticationResponse.builder()
                .token(jwtToken)
                .build();
    }
}