package com.luccavergara.solaris.fiscal.verifactu;

import org.junit.jupiter.api.Test;

import java.math.BigInteger;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.cert.X509Certificate;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Date;

import static org.assertj.core.api.Assertions.assertThat;

class VerifactuRecordSignerTest {

    private final VerifactuRecordSigner signer = new VerifactuRecordSigner();

    @Test
    void sign_addsXmlSignatureToRegistroAlta() throws Exception {
        KeyPair keyPair = KeyPairGenerator.getInstance("RSA").generateKeyPair();
        X509Certificate certificate = selfSignedCertificate(keyPair);

        VerifactuCertificateLoader.VerifactuKeyMaterial keyMaterial =
                new VerifactuCertificateLoader.VerifactuKeyMaterial(
                        keyPair.getPrivate(),
                        certificate,
                        "test".toCharArray()
                );

        String registro = """
                <sfLR:RegistroAlta xmlns:sfLR="https://www2.agenciatributaria.gob.es/static_files/common/internet/dep/aplicaciones/es/aeat/tike/cont/ws/SuministroLR.xsd">
                  <sfLR:IDVersion>1.0</sfLR:IDVersion>
                  <sfLR:Huella>ABC</sfLR:Huella>
                </sfLR:RegistroAlta>
                """;

        String signed = signer.sign(registro, keyMaterial);

        assertThat(signed).contains("Signature");
        assertThat(signed).contains("SignedInfo");
        assertThat(signed).contains("xades:SigningTime");
    }

    private X509Certificate selfSignedCertificate(KeyPair keyPair) throws Exception {
        var builder = new org.bouncycastle.cert.jcajce.JcaX509v3CertificateBuilder(
                new org.bouncycastle.asn1.x500.X500Name("CN=Verifactu Test"),
                BigInteger.valueOf(System.currentTimeMillis()),
                Date.from(LocalDate.now().atStartOfDay(ZoneOffset.UTC).toInstant()),
                Date.from(LocalDate.now().plusYears(1).atStartOfDay(ZoneOffset.UTC).toInstant()),
                new org.bouncycastle.asn1.x500.X500Name("CN=Verifactu Test"),
                keyPair.getPublic()
        );

        var signer = new org.bouncycastle.operator.jcajce.JcaContentSignerBuilder("SHA256withRSA")
                .build(keyPair.getPrivate());
        return new org.bouncycastle.cert.jcajce.JcaX509CertificateConverter()
                .getCertificate(builder.build(signer));
    }
}
