package com.luccavergara.solaris.fiscal.verifactu;

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
public class VerifactuCertificateLoader {

    private final VerifactuProperties verifactuProperties;

    public VerifactuKeyMaterial load(VerifactuCredentials credentials) {
        String password = resolvePassword(credentials);
        if (!StringUtils.hasText(password)) {
            throw new IllegalStateException("Verifactu certificate password is required");
        }

        try (InputStream inputStream = openCertificateStream(credentials)) {
            KeyStore keyStore = KeyStore.getInstance("PKCS12");
            keyStore.load(inputStream, password.toCharArray());

            String alias = keyStore.aliases().nextElement();
            PrivateKey privateKey = (PrivateKey) keyStore.getKey(alias, password.toCharArray());
            X509Certificate certificate = (X509Certificate) keyStore.getCertificate(alias);

            if (privateKey == null || certificate == null) {
                throw new IllegalStateException("Verifactu PKCS12 keystore is missing key or certificate");
            }

            return new VerifactuKeyMaterial(privateKey, certificate, password.toCharArray());
        } catch (Exception ex) {
            throw new IllegalStateException("Failed to load Verifactu certificate: " + ex.getMessage(), ex);
        }
    }

    private String resolvePassword(VerifactuCredentials credentials) {
        if (credentials != null && StringUtils.hasText(credentials.certPassword())) {
            return credentials.certPassword();
        }
        return verifactuProperties.getCert().getPassword();
    }

    private InputStream openCertificateStream(VerifactuCredentials credentials) throws Exception {
        String base64 = credentials != null && StringUtils.hasText(credentials.certBase64())
                ? credentials.certBase64()
                : verifactuProperties.getCert().getBase64();

        if (StringUtils.hasText(base64)) {
            return new ByteArrayInputStream(Base64.getDecoder().decode(base64.trim()));
        }

        String path = credentials != null && StringUtils.hasText(credentials.certPath())
                ? credentials.certPath()
                : verifactuProperties.getCert().getPath();

        if (!StringUtils.hasText(path)) {
            throw new IllegalStateException(
                    "Verifactu certificate path or base64 is required (org fiscal_api_key or verifactu.cert.*)"
            );
        }

        return Files.newInputStream(Path.of(path.trim()));
    }

    public record VerifactuKeyMaterial(
            PrivateKey privateKey,
            X509Certificate certificate,
            char[] password
    ) {
    }
}
