package com.luccavergara.solaris.dto;

import com.luccavergara.solaris.entity.DocumentType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CustomerDocumentRequest {

    @NotNull
    private DocumentType documentType;

    @NotBlank
    private String documentNumber;

    private Boolean primary;
}
