package com.luccavergara.solaris.service;

import com.luccavergara.solaris.dto.ChangePasswordRequest;
import com.luccavergara.solaris.dto.MessageResponse;
import com.luccavergara.solaris.dto.UpdateUserProfileRequest;
import com.luccavergara.solaris.dto.UserProfileResponse;
import com.luccavergara.solaris.entity.User;
import com.luccavergara.solaris.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserService {

    private final AuthenticatedUserService authenticatedUserService;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public UserProfileResponse getCurrentUserProfile() {
        User currentUser = authenticatedUserService.getCurrentUser();

        return mapToResponse(currentUser);
    }

    public UserProfileResponse updateCurrentUserProfile(UpdateUserProfileRequest request) {
        User currentUser = authenticatedUserService.getCurrentUser();

        currentUser.setFirstname(request.getFirstname());
        currentUser.setLastname(request.getLastname());

        User savedUser = userRepository.save(currentUser);

        return mapToResponse(savedUser);
    }

    public MessageResponse changeCurrentUserPassword(ChangePasswordRequest request) {
        User currentUser = authenticatedUserService.getCurrentUser();

        if (!passwordEncoder.matches(request.getCurrentPassword(), currentUser.getPassword())) {
            throw new IllegalStateException("Current password is incorrect");
        }

        currentUser.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(currentUser);

        return MessageResponse.builder()
                .message("Password updated successfully.")
                .build();
    }

    private UserProfileResponse mapToResponse(User user) {
        return UserProfileResponse.builder()
                .id(user.getId())
                .firstname(user.getFirstname())
                .lastname(user.getLastname())
                .email(user.getEmail())
                .build();
    }
}