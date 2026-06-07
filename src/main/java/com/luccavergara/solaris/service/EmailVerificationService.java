package com.luccavergara.solaris.service;

import com.luccavergara.solaris.entity.EmailVerificationToken;
import com.luccavergara.solaris.entity.User;
import com.luccavergara.solaris.exception.ResourceNotFoundException;
import com.luccavergara.solaris.repository.EmailVerificationTokenRepository;
import com.luccavergara.solaris.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import org.springframework.beans.factory.annotation.Value;
import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class EmailVerificationService {

    private final EmailVerificationTokenRepository emailVerificationTokenRepository;
    private final UserRepository userRepository;
    private final EmailService emailService;

    @Value("${application.frontend.url}")
    private String frontendUrl;

    public void createAndLogVerificationToken(User user) {
        String token = UUID.randomUUID().toString();

        EmailVerificationToken verificationToken = EmailVerificationToken.builder()
                .token(token)
                .user(user)
                .expiresAt(LocalDateTime.now().plusHours(24))
                .used(false)
                .createdAt(LocalDateTime.now())
                .build();

        emailVerificationTokenRepository.save(verificationToken);

        String verificationLink = frontendUrl + "/verify-email?token=" + token;

        emailService.sendEmailVerification(user.getEmail(), verificationLink);

        System.out.println("==================================================");
        System.out.println("SOLARIS EMAIL VERIFICATION LINK:");
        System.out.println(verificationLink);
        System.out.println("==================================================");
    }

    public void verifyEmail(String token) {
        EmailVerificationToken verificationToken = emailVerificationTokenRepository.findByToken(token)
                .orElseThrow(() -> new ResourceNotFoundException("Verification token not found"));

        if (Boolean.TRUE.equals(verificationToken.getUsed())) {
            throw new IllegalStateException("Verification token already used");
        }

        if (verificationToken.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new IllegalStateException("Verification token expired");
        }

        User user = verificationToken.getUser();
        user.setEmailVerified(true);

        verificationToken.setUsed(true);

        userRepository.save(user);
        emailVerificationTokenRepository.save(verificationToken);
    }
}