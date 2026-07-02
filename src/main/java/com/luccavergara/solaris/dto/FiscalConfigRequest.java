package com.luccavergara.solaris.dto;

import com.luccavergara.solaris.entity.CondicionIva;
import com.luccavergara.solaris.entity.FiscalProviderType;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class FiscalConfigRequest {

    @Size(max = 20)
    private String cuit;

    @Size(max = 255)
    private String razonSocial;

    private CondicionIva condicionIva;

    private Integer fiscalPuntoVenta;

    private FiscalProviderType fiscalProvider;

    private String fiscalApiKey;
}
