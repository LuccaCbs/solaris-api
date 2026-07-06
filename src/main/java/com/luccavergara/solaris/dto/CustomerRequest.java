package com.luccavergara.solaris.dto;

import com.luccavergara.solaris.entity.CondicionIva;
import com.luccavergara.solaris.entity.DocumentType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CustomerRequest {

    /** Legacy single-document fields; used when {@code documents} is empty. */
    private DocumentType documentType;

    private String documentNumber;

    @Valid
    private List<CustomerDocumentRequest> documents;

    @NotBlank
    private String razonSocial;

    private String email;

    private String phone;

    private String address;

    @NotNull
    private CondicionIva condicionIva;
}
