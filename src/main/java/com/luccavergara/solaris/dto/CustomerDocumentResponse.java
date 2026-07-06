package com.luccavergara.solaris.dto;

import com.luccavergara.solaris.entity.DocumentType;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CustomerDocumentResponse {

    private Long id;
    private DocumentType documentType;
    private String documentNumber;
    private Boolean primary;
}
