package com.luccavergara.solaris.dto;

import com.luccavergara.solaris.entity.CondicionIva;
import com.luccavergara.solaris.entity.DocumentType;
import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CustomerResponse {

    private Long id;
    private DocumentType documentType;
    private String documentNumber;
    private String razonSocial;
    private String email;
    private String phone;
    private String address;
    private CondicionIva condicionIva;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private Boolean active;
}
