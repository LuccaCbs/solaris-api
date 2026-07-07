package com.luccavergara.solaris.fiscal.afip;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.bouncycastle.cert.jcajce.JcaCertStore;
import org.bouncycastle.cms.CMSProcessableByteArray;
import org.bouncycastle.cms.CMSSignedData;
import org.bouncycastle.cms.CMSSignedDataGenerator;
import org.bouncycastle.cms.jcajce.JcaSignerInfoGeneratorBuilder;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;
import org.bouncycastle.operator.jcajce.JcaDigestCalculatorProviderBuilder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.security.cert.X509Certificate;
import java.time.Instant;
import java.util.Base64;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class AfipWsaaClient {

    private static final String WSFE_SERVICE = "wsfe";
    private static final long TRA_VALIDITY_SECONDS = 300;

    private final AfipProperties afipProperties;
    private final AfipSoapTransport soapTransport;
    private final AfipCertificateLoader certificateLoader;
    private final AfipTokenCache tokenCache;

    public AfipAuthToken authenticate(AfipCredentials credentials, String cuit) {
        String cacheKey = cuit + ":" + WSFE_SERVICE;
        AfipAuthToken cached = tokenCache.get(cacheKey);
        if (cached != null) {
            return cached;
        }

        AfipCertificateLoader.AfipKeyMaterial keyMaterial = certificateLoader.load(credentials);
        String tra = buildLoginTicketRequest();
        String cmsBase64 = signTra(tra, keyMaterial);

        String envelope = """
                <?xml version="1.0" encoding="UTF-8"?>
                <soapenv:Envelope xmlns:soapenv="http://schemas.xmlsoap.org/soap/envelope/" xmlns:wsaa="http://wsaa.view.saf.gva.ar/">
                  <soapenv:Header/>
                  <soapenv:Body>
                    <wsaa:loginCms>
                      <wsaa:in0>%s</wsaa:in0>
                    </wsaa:loginCms>
                  </soapenv:Body>
                </soapenv:Envelope>
                """.formatted(cmsBase64);

        String response = soapTransport.post(
                afipProperties.getHomologation().getWsaaUrl(),
                "",
                envelope
        );

        AfipAuthToken authToken = parseLoginResponse(response);
        tokenCache.put(cacheKey, authToken);
        return authToken;
    }

    AfipAuthToken parseLoginResponse(String response) {
        String fault = AfipXmlHelper.firstFault(response);
        if (StringUtils.hasText(fault)) {
            throw new IllegalStateException("WSAA authentication failed: " + fault);
        }

        String loginResponse = AfipXmlHelper.textContent(response, "loginCmsReturn");
        String token = AfipXmlHelper.textContent(
                StringUtils.hasText(loginResponse) ? loginResponse : response,
                "token"
        );
        String sign = AfipXmlHelper.textContent(
                StringUtils.hasText(loginResponse) ? loginResponse : response,
                "sign"
        );
        String expiration = AfipXmlHelper.textContent(
                StringUtils.hasText(loginResponse) ? loginResponse : response,
                "expirationTime"
        );

        if (!StringUtils.hasText(token) || !StringUtils.hasText(sign)) {
            throw new IllegalStateException("WSAA response missing token or sign");
        }

        Instant expirationTime = AfipXmlHelper.parseInstant(expiration);
        if (expirationTime == null) {
            expirationTime = Instant.now().plusSeconds(43_200);
        }

        return new AfipAuthToken(token, sign, expirationTime);
    }

    String buildLoginTicketRequest() {
        Instant now = Instant.now();
        Instant expiration = now.plusSeconds(TRA_VALIDITY_SECONDS);
        long uniqueId = now.getEpochSecond();

        return """
                <?xml version="1.0" encoding="UTF-8"?>
                <loginTicketRequest version="1.0">
                  <header>
                    <uniqueId>%d</uniqueId>
                    <generationTime>%s</generationTime>
                    <expirationTime>%s</expirationTime>
                  </header>
                  <service>%s</service>
                </loginTicketRequest>
                """.formatted(
                uniqueId,
                AfipXmlHelper.formatGenerationTime(now),
                AfipXmlHelper.formatGenerationTime(expiration),
                WSFE_SERVICE
        );
    }

    private String signTra(String tra, AfipCertificateLoader.AfipKeyMaterial keyMaterial) {
        try {
            byte[] traBytes = tra.getBytes(java.nio.charset.StandardCharsets.UTF_8);
            CMSProcessableByteArray processable = new CMSProcessableByteArray(traBytes);
            X509Certificate certificate = keyMaterial.certificate();

            CMSSignedDataGenerator generator = new CMSSignedDataGenerator();
            generator.addSignerInfoGenerator(
                    new JcaSignerInfoGeneratorBuilder(
                            new JcaDigestCalculatorProviderBuilder().build()
                    ).build(
                            new JcaContentSignerBuilder("SHA256withRSA").build(keyMaterial.privateKey()),
                            certificate
                    )
            );
            generator.addCertificates(new JcaCertStore(List.of(certificate)));

            CMSSignedData signedData = generator.generate(processable, false);
            return Base64.getEncoder().encodeToString(signedData.getEncoded());
        } catch (Exception ex) {
            throw new IllegalStateException("Failed to sign AFIP TRA: " + ex.getMessage(), ex);
        }
    }
}
