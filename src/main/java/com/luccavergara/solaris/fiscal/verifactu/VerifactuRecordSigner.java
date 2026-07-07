package com.luccavergara.solaris.fiscal.verifactu;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.InputSource;

import javax.xml.crypto.dsig.*;
import javax.xml.crypto.dsig.dom.DOMSignContext;
import javax.xml.crypto.dom.DOMStructure;
import javax.xml.crypto.dsig.keyinfo.KeyInfo;
import javax.xml.crypto.dsig.keyinfo.KeyInfoFactory;
import javax.xml.crypto.dsig.keyinfo.X509Data;
import javax.xml.crypto.dsig.spec.C14NMethodParameterSpec;
import javax.xml.crypto.dsig.spec.TransformParameterSpec;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.StringReader;
import java.io.StringWriter;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

/**
 * Signs Verifactu billing records with enveloped XML-DSig (RSA-SHA256) and minimal XAdES-BES metadata.
 */
@Slf4j
@Component
public class VerifactuRecordSigner {

    private static final String XADES_NS = "http://uri.etsi.org/01903/v1.3.2#";

    public String sign(String registroXml, VerifactuCertificateLoader.VerifactuKeyMaterial keyMaterial) {
        try {
            Document document = parseDocument(registroXml);
            Element root = document.getDocumentElement();

            DOMSignContext signContext = new DOMSignContext(keyMaterial.privateKey(), root);

            XMLSignatureFactory signatureFactory = XMLSignatureFactory.getInstance("DOM");
            Reference reference = signatureFactory.newReference(
                    "",
                    signatureFactory.newDigestMethod(DigestMethod.SHA256, null),
                    List.of(signatureFactory.newTransform(Transform.ENVELOPED, (TransformParameterSpec) null)),
                    null,
                    null
            );

            SignedInfo signedInfo = signatureFactory.newSignedInfo(
                    signatureFactory.newCanonicalizationMethod(
                            CanonicalizationMethod.INCLUSIVE,
                            (C14NMethodParameterSpec) null
                    ),
                    signatureFactory.newSignatureMethod(SignatureMethod.RSA_SHA256, null),
                    Collections.singletonList(reference)
            );

            KeyInfoFactory keyInfoFactory = signatureFactory.getKeyInfoFactory();
            X509Data x509Data = keyInfoFactory.newX509Data(Collections.singletonList(keyMaterial.certificate()));
            KeyInfo keyInfo = keyInfoFactory.newKeyInfo(Collections.singletonList(x509Data));

            String signedPropertiesId = "xades-" + UUID.randomUUID();
            XMLObject xadesObject = buildXadesObject(document, signatureFactory, signedPropertiesId);

            XMLSignature signature = signatureFactory.newXMLSignature(
                    signedInfo,
                    keyInfo,
                    List.of(xadesObject),
                    "Signature",
                    null
            );

            signature.sign(signContext);
            return documentToString(document);
        } catch (Exception ex) {
            throw new IllegalStateException("Failed to sign Verifactu record: " + ex.getMessage(), ex);
        }
    }

    private XMLObject buildXadesObject(
            Document document,
            XMLSignatureFactory signatureFactory,
            String signedPropertiesId
    ) {
        Element qualifyingProperties = document.createElementNS(XADES_NS, "xades:QualifyingProperties");
        qualifyingProperties.setAttributeNS("http://www.w3.org/2000/xmlns/", "xmlns:xades", XADES_NS);
        qualifyingProperties.setAttribute("Target", "#Signature");

        Element signedProperties = document.createElementNS(XADES_NS, "xades:SignedProperties");
        signedProperties.setAttribute("Id", signedPropertiesId);

        Element signedSignatureProperties = document.createElementNS(XADES_NS, "xades:SignedSignatureProperties");
        Element signingTime = document.createElementNS(XADES_NS, "xades:SigningTime");
        signingTime.setTextContent(OffsetDateTime.now().format(DateTimeFormatter.ISO_OFFSET_DATE_TIME));
        signedSignatureProperties.appendChild(signingTime);
        signedProperties.appendChild(signedSignatureProperties);
        qualifyingProperties.appendChild(signedProperties);

        return signatureFactory.newXMLObject(
                Collections.singletonList(new DOMStructure(qualifyingProperties)),
                null,
                null,
                null
        );
    }

    private Document parseDocument(String xml) throws Exception {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newDefaultInstance();
        factory.setNamespaceAware(true);
        return factory.newDocumentBuilder().parse(new InputSource(new StringReader(xml)));
    }

    private String documentToString(Document document) throws Exception {
        StringWriter writer = new StringWriter();
        TransformerFactory.newInstance()
                .newTransformer()
                .transform(new DOMSource(document), new StreamResult(writer));
        return writer.toString();
    }
}
