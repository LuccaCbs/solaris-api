package com.luccavergara.solaris.controller;

import com.luccavergara.solaris.dto.ChangePasswordRequest;
import com.luccavergara.solaris.dto.MessageResponse;
import com.luccavergara.solaris.dto.UpdateUserProfileRequest;
import com.luccavergara.solaris.dto.UserProfileResponse;
import com.luccavergara.solaris.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @GetMapping("/me")
    public ResponseEntity<UserProfileResponse> getCurrentUserProfile() {
        return ResponseEntity.ok(userService.getCurrentUserProfile());
    }

    @PutMapping("/me")
    public ResponseEntity<UserProfileResponse> updateCurrentUserProfile(
            @Valid @RequestBody UpdateUserProfileRequest request
    ) {
        return ResponseEntity.ok(userService.updateCurrentUserProfile(request));
    }

    @PutMapping("/me/password")
    public ResponseEntity<MessageResponse> changeCurrentUserPassword(
            @Valid @RequestBody ChangePasswordRequest request
    ) {
        return ResponseEntity.ok(userService.changeCurrentUserPassword(request));
    }
}