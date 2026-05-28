package com.luccavergara.solaris.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SupplierRequest {

    @NotBlank
    private String name;

    private String contactName;

    @Email
    private String email;

    @Pattern(
            regexp = "^\\+?[1-9]\\d{7,14}$",
            message = "Phone number must be in international format"
    )
    private String phone;

    private String address;

    private String notes;

    private Boolean active;
}