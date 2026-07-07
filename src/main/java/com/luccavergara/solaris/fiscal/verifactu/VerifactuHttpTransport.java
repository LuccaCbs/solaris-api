package com.luccavergara.solaris.fiscal.verifactu;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import java.net.HttpURLConnection;
import java.security.KeyStore;

@Slf4j
@Component
public class VerifactuHttpTransport {

    public String post(String url, String soapAction, String soapEnvelope, VerifactuCertificateLoader.VerifactuKeyMaterial keyMaterial) {
        try {
            return restClient(keyMaterial).post()
                    .uri(url)
                    .contentType(MediaType.parseMediaType("text/xml; charset=utf-8"))
                    .header("SOAPAction", soapAction)
                    .body(soapEnvelope)
                    .retrieve()
                    .body(String.class);
        } catch (RestClientException ex) {
            log.error("Verifactu SOAP call failed for {}: {}", url, ex.getMessage());
            throw ex;
        } catch (Exception ex) {
            log.error("Verifactu transport setup failed: {}", ex.getMessage());
            throw new IllegalStateException("Verifactu transport setup failed: " + ex.getMessage(), ex);
        }
    }

    private RestClient restClient(VerifactuCertificateLoader.VerifactuKeyMaterial keyMaterial) throws Exception {
        SSLContext sslContext = SSLContext.getInstance("TLS");
        KeyStore keyStore = KeyStore.getInstance("PKCS12");
        keyStore.load(null, null);
        keyStore.setKeyEntry(
                "verifactu",
                keyMaterial.privateKey(),
                keyMaterial.password(),
                new java.security.cert.Certificate[]{keyMaterial.certificate()}
        );

        KeyManagerFactory keyManagerFactory = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
        keyManagerFactory.init(keyStore, keyMaterial.password());
        sslContext.init(keyManagerFactory.getKeyManagers(), null, null);

        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory() {
            @Override
            protected void prepareConnection(HttpURLConnection connection, String httpMethod) throws java.io.IOException {
                super.prepareConnection(connection, httpMethod);
                if (connection instanceof HttpsURLConnection httpsConnection) {
                    httpsConnection.setSSLSocketFactory(sslContext.getSocketFactory());
                }
            }
        };
        requestFactory.setConnectTimeout(30_000);
        requestFactory.setReadTimeout(120_000);

        return RestClient.builder().requestFactory(requestFactory).build();
    }
}
