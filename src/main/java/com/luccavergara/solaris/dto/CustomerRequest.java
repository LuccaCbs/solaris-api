package com.luccavergara.solaris.dto;

import com.luccavergara.solaris.entity.CondicionIva;
import com.luccavergara.solaris.entity.DocumentType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
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

    private String email;

    private String phone;

    private String address;

    @NotNull
    private CondicionIva condicionIva;
}
