package com.luccavergara.solaris.fiscal;

import com.luccavergara.solaris.entity.FiscalProviderType;
import com.luccavergara.solaris.entity.Organization;
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

    public FiscalProvider resolve(Organization organization) {
        FiscalProviderType configured = organization.getFiscalProvider() != null
                ? organization.getFiscalProvider()
                : FiscalProviderType.MOCK;

        if (configured == FiscalProviderType.TUSFACTURAS
                && StringUtils.hasText(organization.getFiscalApiKey())) {
            return new ApiKeyAwareFiscalProvider(tusFacturasFiscalProvider, organization.getFiscalApiKey());
        }

        if (configured == FiscalProviderType.TUSFACTURAS) {
            log.warn(
                    "Organization {} configured for TUSFACTURAS but no API key set — falling back to MOCK provider",
                    organization.getId()
            );
        }

        return mockFiscalProvider;
    }

    private record ApiKeyAwareFiscalProvider(FiscalProvider delegate, String apiKey) implements FiscalProvider {

        @Override
        public EmitInvoiceResult emitInvoice(EmitInvoiceCommand command) {
            return delegate.emitInvoice(command);
        }

        @Override
        public EmitInvoiceResult emitCreditNote(EmitCreditNoteCommand command) {
            return delegate.emitCreditNote(command);
        }
    }
}
