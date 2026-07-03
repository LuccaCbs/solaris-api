package com.luccavergara.solaris.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrganizationResponse {

    private Long id;
    private String displayName;
    private String razonSocial;
    private String timezone;
}
