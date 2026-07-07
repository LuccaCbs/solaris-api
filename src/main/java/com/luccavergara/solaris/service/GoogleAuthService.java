package com.luccavergara.solaris.service;

import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import com.luccavergara.solaris.entity.AuditAction;
import com.luccavergara.solaris.entity.AuditEntityType;
import com.luccavergara.solaris.entity.AuthProvider;
import com.luccavergara.solaris.entity.Category;
import com.luccavergara.solaris.entity.Role;
import com.luccavergara.solaris.entity.User;
import com.luccavergara.solaris.repository.CategoryRepository;
import com.luccavergara.solaris.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class GoogleAuthService {

    private final UserRepository userRepository;
    private final CategoryRepository categoryRepository;
    private final AuditLogService auditLogService;

    @Value("${google.client-id:}")
    private String googleClientId;

    public record GoogleTokenPayload(
            String sub,
            String email,
            String givenName,
            String familyName,
            boolean emailVerified
    ) {}

    public record GoogleAuthResult(User user, boolean newlyCreated) {}

    public GoogleTokenPayload verifyIdToken(String idToken) {
        if (googleClientId == null || googleClientId.isBlank()) {
            throw new IllegalStateException("Google sign-in is not configured");
        }

        try {
            GoogleIdTokenVerifier verifier = new GoogleIdTokenVerifier.Builder(
                    new NetHttpTransport(),
                    GsonFactory.getDefaultInstance()
            )
                    .setAudience(Collections.singletonList(googleClientId))
                    .build();

            GoogleIdToken googleIdToken = verifier.verify(idToken);
            if (googleIdToken == null) {
                throw new IllegalArgumentException("Invalid Google ID token");
            }

            GoogleIdToken.Payload payload = googleIdToken.getPayload();
            String email = payload.getEmail();
            if (email == null || email.isBlank()) {
                throw new IllegalArgumentException("Google account email is missing");
            }

            boolean emailVerified = Boolean.TRUE.equals(payload.getEmailVerified());
            String givenName = payload.get("given_name") instanceof String value ? value : null;
            String familyName = payload.get("family_name") instanceof String value ? value : null;

            return new GoogleTokenPayload(
                    payload.getSubject(),
                    email,
                    givenName,
                    familyName,
                    emailVerified
            );
        } catch (IllegalArgumentException | IllegalStateException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new IllegalArgumentException("Invalid Google ID token");
        }
    }

    public GoogleAuthResult findOrCreateUser(GoogleTokenPayload payload) {
        if (!payload.emailVerified()) {
            throw new IllegalArgumentException("Google email is not verified");
        }

        Optional<User> existingByEmail = userRepository.findByEmailIgnoreCase(payload.email());
        if (existingByEmail.isPresent()) {
            return new GoogleAuthResult(linkExistingUser(existingByEmail.get(), payload), false);
        }

        Optional<User> existingBySub = userRepository.findByGoogleSub(payload.sub());
        if (existingBySub.isPresent()) {
            User user = existingBySub.get();
            syncGoogleSub(user, payload.sub());
            return new GoogleAuthResult(user, false);
        }

        return new GoogleAuthResult(createGoogleUser(payload), true);
    }

    private User linkExistingUser(User user, GoogleTokenPayload payload) {
        if (user.getAuthProvider() == AuthProvider.GOOGLE || user.getGoogleSub() != null) {
            syncGoogleSub(user, payload.sub());
            return user;
        }

        user.setAuthProvider(AuthProvider.GOOGLE);
        user.setGoogleSub(payload.sub());
        user.setEmailVerified(true);
        return userRepository.save(user);
    }

    private void syncGoogleSub(User user, String googleSub) {
        if (googleSub.equals(user.getGoogleSub())) {
            return;
        }
        user.setGoogleSub(googleSub);
        userRepository.save(user);
    }

    private User createGoogleUser(GoogleTokenPayload payload) {
        String firstname = payload.givenName() != null && !payload.givenName().isBlank()
                ? payload.givenName()
                : "Google";
        String lastname = payload.familyName() != null && !payload.familyName().isBlank()
                ? payload.familyName()
                : "User";

        User savedUser = userRepository.save(
                User.builder()
                        .firstname(firstname)
                        .lastname(lastname)
                        .email(payload.email())
                        .password(null)
                        .role(Role.USER)
                        .emailVerified(true)
                        .authProvider(AuthProvider.GOOGLE)
                        .googleSub(payload.sub())
                        .build()
        );

        auditLogService.log(
                AuditAction.REGISTER_USER,
                AuditEntityType.USER,
                savedUser.getId(),
                savedUser.getFirstname() + " " + savedUser.getLastname(),
                "User registered via Google"
        );

        categoryRepository.save(
                Category.builder()
                        .name("General")
                        .description("Default category")
                        .createdAt(LocalDateTime.now())
                        .systemCategory(true)
                        .user(savedUser)
                        .build()
        );

        return savedUser;
    }
}
