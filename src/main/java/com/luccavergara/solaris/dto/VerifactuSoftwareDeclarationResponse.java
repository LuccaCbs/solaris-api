package com.luccavergara.solaris.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VerifactuSoftwareDeclarationResponse {

    private String softwareName;
    private String softwareId;
    private String softwareVersion;
    private String installationNumber;
    private String vendorName;
    private String vendorNif;
    private String declarationText;
    private String declarationUrl;
}
