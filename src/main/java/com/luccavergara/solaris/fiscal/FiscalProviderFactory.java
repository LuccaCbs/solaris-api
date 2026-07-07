package com.luccavergara.solaris.fiscal;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.luccavergara.solaris.entity.FiscalJurisdiction;
import com.luccavergara.solaris.entity.FiscalProviderType;
import com.luccavergara.solaris.entity.Organization;
import com.luccavergara.solaris.fiscal.afip.AfipCredentials;
import com.luccavergara.solaris.fiscal.afip.AfipNativeFiscalProvider;
import com.luccavergara.solaris.fiscal.afip.AfipProperties;
import com.luccavergara.solaris.fiscal.verifactu.VerifactuCredentials;
import com.luccavergara.solaris.fiscal.verifactu.VerifactuNativeFiscalProvider;
import com.luccavergara.solaris.fiscal.verifactu.VerifactuProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Slf4j
@Component
@RequiredArgsConstructor
public class FiscalProviderFactory {

    private final MockFiscalProvider mockFiscalProvider;
    private final TusFacturasFiscalProvider tusFacturasFiscalProvider;
    private final AfipNativeFiscalProvider afipNativeFiscalProvider;
    private final VerifactuNativeFiscalProvider verifactuNativeFiscalProvider;
    private final AfipProperties afipProperties;
    private final VerifactuProperties verifactuProperties;
    private final ObjectMapper objectMapper;

    public FiscalProvider resolve(Organization organization) {
        FiscalProviderType configured = organization.getFiscalProvider() != null
                ? organization.getFiscalProvider()
                : FiscalProviderType.MOCK;

        if (configured == FiscalProviderType.TUSFACTURAS) {
            if (organization.getFiscalJurisdiction() == FiscalJurisdiction.ES_VERIFACTU) {
                log.warn(
                        "Organization {} is ES_VERIFACTU but TUSFACTURAS was configured — using MOCK provider",
                        organization.getId()
                );
                return mockFiscalProvider;
            }

            return TusFacturasCredentials.parse(organization.getFiscalApiKey(), objectMapper)
                    .<FiscalProvider>map(credentials -> new TusFacturasAwareProvider(tusFacturasFiscalProvider, credentials))
                    .orElseGet(() -> {
                        log.warn(
                                "Organization {} configured for TUSFACTURAS but credentials are missing or incomplete "
                                        + "(expected JSON with apikey, apitoken, usertoken) — falling back to MOCK provider",
                                organization.getId()
                        );
                        return mockFiscalProvider;
                    });
        }

        if (configured == FiscalProviderType.AFIP_NATIVE) {
            if (organization.getFiscalJurisdiction() == FiscalJurisdiction.ES_VERIFACTU) {
                log.warn(
                        "Organization {} is ES_VERIFACTU but AFIP_NATIVE was configured — using MOCK provider",
                        organization.getId()
                );
                return mockFiscalProvider;
            }

            if (!hasAfipCertificateConfig(organization)) {
                log.warn(
                        "Organization {} configured for AFIP_NATIVE but certificate is missing "
                                + "(expected org fiscal_api_key certPath/certBase64 or afip.cert.* env) "
                                + "— falling back to MOCK provider",
                        organization.getId()
                );
                return mockFiscalProvider;
            }

            AfipCredentials credentials = AfipCredentials.parse(organization.getFiscalApiKey(), objectMapper)
                    .orElse(AfipCredentials.empty());

            return new AfipNativeAwareProvider(afipNativeFiscalProvider, credentials);
        }

        if (configured == FiscalProviderType.VERIFACTU_NATIVE) {
            if (organization.getFiscalJurisdiction() != FiscalJurisdiction.ES_VERIFACTU) {
                log.warn(
                        "Organization {} is not ES_VERIFACTU but VERIFACTU_NATIVE was configured — using MOCK provider",
                        organization.getId()
                );
                return mockFiscalProvider;
            }

            if (!hasVerifactuCertificateConfig(organization)) {
                log.warn(
                        "Organization {} configured for VERIFACTU_NATIVE but certificate is missing "
                                + "(expected org fiscal_api_key certPath/certBase64 or verifactu.cert.* env) "
                                + "— falling back to MOCK provider",
                        organization.getId()
                );
                return mockFiscalProvider;
            }

            VerifactuCredentials credentials = VerifactuCredentials.parse(organization.getFiscalApiKey(), objectMapper)
                    .orElse(VerifactuCredentials.empty());

            return new VerifactuNativeAwareProvider(
                    verifactuNativeFiscalProvider,
                    credentials,
                    organization.getId()
            );
        }

        return mockFiscalProvider;
    }

    private boolean hasAfipCertificateConfig(Organization organization) {
        return AfipCredentials.parse(organization.getFiscalApiKey(), objectMapper)
                .map(AfipCredentials::hasCertificateReference)
                .orElse(false)
                || StringUtils.hasText(afipProperties.getCert().getPath())
                || StringUtils.hasText(afipProperties.getCert().getBase64());
    }

    private boolean hasVerifactuCertificateConfig(Organization organization) {
        return VerifactuCredentials.parse(organization.getFiscalApiKey(), objectMapper)
                .map(VerifactuCredentials::hasCertificateReference)
                .orElse(false)
                || StringUtils.hasText(verifactuProperties.getCert().getPath())
                || StringUtils.hasText(verifactuProperties.getCert().getBase64());
    }

    private record TusFacturasAwareProvider(
            TusFacturasFiscalProvider delegate,
            TusFacturasCredentials credentials
    ) implements FiscalProvider {

        @Override
        public EmitInvoiceResult emitInvoice(EmitInvoiceCommand command) {
            return delegate.emitInvoice(command, credentials);
        }

        @Override
        public EmitInvoiceResult emitCreditNote(EmitCreditNoteCommand command) {
            return delegate.emitCreditNote(command);
        }
    }

    private record AfipNativeAwareProvider(
            AfipNativeFiscalProvider delegate,
            AfipCredentials credentials
    ) implements FiscalProvider {

        @Override
        public EmitInvoiceResult emitInvoice(EmitInvoiceCommand command) {
            return delegate.emitInvoice(command, credentials);
        }

        @Override
        public EmitInvoiceResult emitCreditNote(EmitCreditNoteCommand command) {
            return delegate.emitCreditNote(command);
        }
    }

    private record VerifactuNativeAwareProvider(
            VerifactuNativeFiscalProvider delegate,
            VerifactuCredentials credentials,
            Long organizationId
    ) implements FiscalProvider {

        @Override
        public EmitInvoiceResult emitInvoice(EmitInvoiceCommand command) {
            return delegate.emitInvoice(command, credentials, organizationId);
        }

        @Override
        public EmitInvoiceResult emitCreditNote(EmitCreditNoteCommand command) {
            return delegate.emitCreditNote(command);
        }
    }
}
