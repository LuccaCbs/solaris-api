package com.luccavergara.solaris.service;

import com.luccavergara.solaris.dto.ForgotPasswordRequest;
import com.luccavergara.solaris.dto.MessageResponse;
import com.luccavergara.solaris.dto.ResetPasswordRequest;
import com.luccavergara.solaris.entity.PasswordResetToken;
import com.luccavergara.solaris.entity.User;
import com.luccavergara.solaris.exception.ResourceNotFoundException;
import com.luccavergara.solaris.repository.PasswordResetTokenRepository;
import com.luccavergara.solaris.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PasswordResetService {

    private final UserRepository userRepository;
    private final PasswordResetTokenRepository passwordResetTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;

    @Value("${application.frontend.url}")
    private String frontendUrl;

    public MessageResponse requestPasswordReset(ForgotPasswordRequest request) {
        userRepository.findByEmail(request.getEmail())
                .ifPresent(this::createAndSendPasswordResetToken);

        return MessageResponse.builder()
                .message("If an account exists for that email, a password reset link has been sent.")
                .build();
    }

    public MessageResponse resetPassword(ResetPasswordRequest request) {
        PasswordResetToken resetToken = passwordResetTokenRepository.findByToken(request.getToken())
                .orElseThrow(() -> new ResourceNotFoundException("Password reset token not found"));

        if (Boolean.TRUE.equals(resetToken.getUsed())) {
            throw new IllegalStateException("Password reset token already used");
        }

        if (resetToken.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new IllegalStateException("Password reset token expired");
        }

        User user = resetToken.getUser();

        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        resetToken.setUsed(true);

        userRepository.save(user);
        passwordResetTokenRepository.save(resetToken);

        return MessageResponse.builder()
                .message("Password updated successfully.")
                .build();
    }

    private void createAndSendPasswordResetToken(User user) {
        String token = UUID.randomUUID().toString();

        PasswordResetToken resetToken = PasswordResetToken.builder()
                .token(token)
                .user(user)
                .expiresAt(LocalDateTime.now().plusHours(1))
                .used(false)
                .createdAt(LocalDateTime.now())
                .build();

        passwordResetTokenRepository.save(resetToken);

        String resetLink = frontendUrl + "/reset-password?token=" + token;

        emailService.sendPasswordReset(user.getEmail(), resetLink);

        System.out.println("==================================================");
        System.out.println("SOLARIS PASSWORD RESET LINK:");
        System.out.println(resetLink);
        System.out.println("==================================================");
    }
}