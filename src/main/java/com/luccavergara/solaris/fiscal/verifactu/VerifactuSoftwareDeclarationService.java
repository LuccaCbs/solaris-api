package com.luccavergara.solaris.fiscal.verifactu;

import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class VerifactuSoftwareDeclarationService {

    public VerifactuSoftwareDeclaration build(VerifactuProperties properties) {
        VerifactuProperties.Software software = properties.getSoftware();
        String declarationText = resolveDeclarationText(properties);

        return new VerifactuSoftwareDeclaration(
                software.getName(),
                software.getId(),
                software.getVersion(),
                software.getInstallationNumber(),
                StringUtils.hasText(software.getVendorName()) ? software.getVendorName() : software.getName(),
                software.getVendorNif(),
                declarationText,
                properties.getResponsibleDeclaration().getUrl()
        );
    }

    private String resolveDeclarationText(VerifactuProperties properties) {
        String override = properties.getResponsibleDeclaration().getText();
        if (StringUtils.hasText(override)) {
            return override.trim();
        }

        VerifactuProperties.Software software = properties.getSoftware();
        String vendor = StringUtils.hasText(software.getVendorName()) ? software.getVendorName() : software.getName();
        return """
                Declaración responsable del sistema informático de facturación (SIF) conforme al Real Decreto \
                1007/2023 y la Orden HAC/1177/2024 (Verifactu).

                Productor: %s
                NIF productor: %s
                Nombre del SIF: %s
                Identificador del SIF: %s
                Versión: %s
                Número de instalación: %s
                """.formatted(
                vendor,
                StringUtils.hasText(software.getVendorNif()) ? software.getVendorNif() : "—",
                software.getName(),
                software.getId(),
                software.getVersion(),
                software.getInstallationNumber()
        ).trim();
    }

    public record VerifactuSoftwareDeclaration(
            String softwareName,
            String softwareId,
            String softwareVersion,
            String installationNumber,
            String vendorName,
            String vendorNif,
            String declarationText,
            String declarationUrl
    ) {
    }
}
