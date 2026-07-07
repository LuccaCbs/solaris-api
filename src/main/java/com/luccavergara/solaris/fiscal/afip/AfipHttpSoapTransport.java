package com.luccavergara.solaris.fiscal.afip;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

@Slf4j
@Component
public class AfipHttpSoapTransport implements AfipSoapTransport {

    private RestClient restClient;

    @Override
    public String post(String url, String soapAction, String soapEnvelope) {
        try {
            return restClient().post()
                    .uri(url)
                    .contentType(MediaType.parseMediaType("text/xml; charset=utf-8"))
                    .header("SOAPAction", soapAction)
                    .body(soapEnvelope)
                    .retrieve()
                    .body(String.class);
        } catch (RestClientException ex) {
            log.error("AFIP SOAP call failed for {}: {}", url, ex.getMessage());
            throw ex;
        }
    }

    private RestClient restClient() {
        if (restClient == null) {
            SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
            requestFactory.setConnectTimeout(30_000);
            requestFactory.setReadTimeout(120_000);
            restClient = RestClient.builder().requestFactory(requestFactory).build();
        }
        return restClient;
    }
}
