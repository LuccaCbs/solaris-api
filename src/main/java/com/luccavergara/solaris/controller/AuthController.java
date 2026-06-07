package com.luccavergara.solaris.controller;

import com.luccavergara.solaris.dto.AuthenticationRequest;
import com.luccavergara.solaris.dto.AuthenticationResponse;
import com.luccavergara.solaris.dto.RegisterRequest;
import com.luccavergara.solaris.service.AuthenticationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.luccavergara.solaris.service.EmailVerificationService;
import com.luccavergara.solaris.dto.RegisterResponse;
import com.luccavergara.solaris.dto.ForgotPasswordRequest;
import com.luccavergara.solaris.dto.MessageResponse;
import com.luccavergara.solaris.dto.ResetPasswordRequest;
import com.luccavergara.solaris.service.PasswordResetService;


@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthenticationService authenticationService;
    private final EmailVerificationService emailVerificationService;
    private final PasswordResetService passwordResetService;

    @PostMapping("/register")
    public ResponseEntity<RegisterResponse> register(
            @Valid @RequestBody RegisterRequest request
    ) {
        return ResponseEntity.ok(authenticationService.register(request));
    }

    @PostMapping("/authenticate")
    public ResponseEntity<AuthenticationResponse> authenticate(
            @Valid @RequestBody AuthenticationRequest request
    ) {
        return ResponseEntity.ok(authenticationService.authenticate(request));
    }

    @GetMapping("/verify-email")
    public ResponseEntity<String> verifyEmail(
            @RequestParam String token
    ) {
        emailVerificationService.verifyEmail(token);

        return ResponseEntity.ok("Email verified successfully. You can now log in.");
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<MessageResponse> forgotPassword(
            @Valid @RequestBody ForgotPasswordRequest request
    ) {
        return ResponseEntity.ok(passwordResetService.requestPasswordReset(request));
    }

    @PostMapping("/reset-password")
    public ResponseEntity<MessageResponse> resetPassword(
            @Valid @RequestBody ResetPasswordRequest request
    ) {
        return ResponseEntity.ok(passwordResetService.resetPassword(request));
    }
}