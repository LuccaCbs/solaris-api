package com.luccavergara.solaris.dto;

import com.luccavergara.solaris.entity.CondicionIva;
import com.luccavergara.solaris.entity.DocumentType;
import lombok.*;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CustomerResponse {

    private Long id;
    /** Primary document type (backward compatible). */
    private DocumentType documentType;
    /** Primary document number (backward compatible). */
    private String documentNumber;
    private List<CustomerDocumentResponse> documents;
    private String razonSocial;
    private String email;
    private String phone;
    private String address;
    private CondicionIva condicionIva;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private Boolean active;
}
