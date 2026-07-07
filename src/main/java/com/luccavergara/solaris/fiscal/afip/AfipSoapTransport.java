package com.luccavergara.solaris.fiscal.afip;

@FunctionalInterface
public interface AfipSoapTransport {

    String post(String url, String soapAction, String soapEnvelope);
}
