package com.luccavergara.solaris.dto;

import com.luccavergara.solaris.entity.CondicionIva;
import com.luccavergara.solaris.entity.DocumentType;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CustomerRequest {

    @NotNull
    private DocumentType documentType;

    @NotBlank
    private String documentNumber;

    @NotBlank
    private String razonSocial;

    @Email
    private String email;

    @Pattern(
            regexp = "^\\+?[1-9]\\d{7,14}$",
            message = "Phone number must be in international format"
    )
    private String phone;

    private String address;

    @NotNull
    private CondicionIva condicionIva;
}
