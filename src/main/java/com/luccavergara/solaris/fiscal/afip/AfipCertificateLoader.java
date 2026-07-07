package com.luccavergara.solaris.fiscal.afip;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.cert.X509Certificate;
import java.util.Base64;

@Component
@RequiredArgsConstructor
public class AfipCertificateLoader {

    private final AfipProperties afipProperties;

    public AfipKeyMaterial load(AfipCredentials credentials) {
        String password = resolvePassword(credentials);
        if (!StringUtils.hasText(password)) {
            throw new IllegalStateException("AFIP certificate password is required");
        }

        try (InputStream inputStream = openCertificateStream(credentials)) {
            KeyStore keyStore = KeyStore.getInstance("PKCS12");
            char[] passwordChars = password.toCharArray();
            keyStore.load(inputStream, passwordChars);

            String alias = keyStore.aliases().nextElement();
            PrivateKey privateKey = (PrivateKey) keyStore.getKey(alias, passwordChars);
            X509Certificate certificate = (X509Certificate) keyStore.getCertificate(alias);

            if (privateKey == null || certificate == null) {
                throw new IllegalStateException("AFIP PKCS12 keystore is missing key or certificate");
            }

            return new AfipKeyMaterial(privateKey, certificate);
        } catch (Exception ex) {
            throw new IllegalStateException("Failed to load AFIP certificate: " + ex.getMessage(), ex);
        }
    }

    private String resolvePassword(AfipCredentials credentials) {
        if (credentials != null && StringUtils.hasText(credentials.certPassword())) {
            return credentials.certPassword();
        }
        return afipProperties.getCert().getPassword();
    }

    private InputStream openCertificateStream(AfipCredentials credentials) throws Exception {
        String base64 = credentials != null && StringUtils.hasText(credentials.certBase64())
                ? credentials.certBase64()
                : afipProperties.getCert().getBase64();

        if (StringUtils.hasText(base64)) {
            byte[] decoded = Base64.getDecoder().decode(base64.replaceAll("\\s", ""));
            return new ByteArrayInputStream(decoded);
        }

        String path = credentials != null && StringUtils.hasText(credentials.certPath())
                ? credentials.certPath()
                : afipProperties.getCert().getPath();

        if (!StringUtils.hasText(path)) {
            throw new IllegalStateException(
                    "AFIP certificate path or base64 is required (org fiscal_api_key or afip.cert.*)"
            );
        }

        return Files.newInputStream(Path.of(path));
    }

    public record AfipKeyMaterial(PrivateKey privateKey, X509Certificate certificate) {
    }
}
