package com.luccavergara.solaris.dto;

import com.luccavergara.solaris.entity.CondicionIva;
import com.luccavergara.solaris.entity.CountryCode;
import com.luccavergara.solaris.entity.FiscalJurisdiction;
import com.luccavergara.solaris.entity.FiscalProviderType;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FiscalConfigResponse {

    private String cuit;
    private String razonSocial;
    private CondicionIva condicionIva;
    private Integer fiscalPuntoVenta;
    private FiscalProviderType fiscalProvider;
    private boolean hasFiscalApiKey;
    private CountryCode countryCode;
    private FiscalJurisdiction fiscalJurisdiction;
    private VerifactuSoftwareDeclarationResponse verifactuSoftwareDeclaration;
}
