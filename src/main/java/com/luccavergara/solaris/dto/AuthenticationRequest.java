package com.luccavergara.solaris.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuthenticationRequest {

    @Email
    @NotBlank
    private String email;

    @NotBlank
    private String password;
}