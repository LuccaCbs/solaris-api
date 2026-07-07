package com.luccavergara.solaris.fiscal.afip;

import org.w3c.dom.Document;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilderFactory;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

final class AfipXmlHelper {

    private AfipXmlHelper() {
    }

    static String textContent(String xml, String... localNames) {
        if (xml == null) {
            return null;
        }

        for (String localName : localNames) {
            Pattern pattern = Pattern.compile(
                    "<(?:[\\w:]+:)?" + Pattern.quote(localName) + "[^>]*>([^<]*)</(?:[\\w:]+:)?"
                            + Pattern.quote(localName) + ">",
                    Pattern.DOTALL
            );
            Matcher matcher = pattern.matcher(xml);
            if (matcher.find()) {
                String value = matcher.group(1).trim();
                if (!value.isEmpty()) {
                    return value;
                }
            }
        }

        return null;
    }

    static String firstFault(String xml) {
        String faultString = textContent(xml, "faultstring");
        if (faultString != null) {
            return faultString;
        }
        return textContent(xml, "Fault", "faultcode");
    }

    static LocalDate parseAfipDate(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }

        String trimmed = value.trim();
        if (trimmed.length() == 8 && trimmed.chars().allMatch(Character::isDigit)) {
            int year = Integer.parseInt(trimmed.substring(0, 4));
            int month = Integer.parseInt(trimmed.substring(4, 6));
            int day = Integer.parseInt(trimmed.substring(6, 8));
            return LocalDate.of(year, month, day);
        }

        try {
            return LocalDate.parse(trimmed.substring(0, 10));
        } catch (Exception ex) {
            return null;
        }
    }

    static Instant parseInstant(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }

        try {
            return Instant.parse(value.trim());
        } catch (Exception ex) {
            try {
                return LocalDate.parse(value.trim().substring(0, 10))
                        .atStartOfDay()
                        .toInstant(ZoneOffset.UTC);
            } catch (Exception ignored) {
                return null;
            }
        }
    }

    static String formatGenerationTime(Instant instant) {
        return DateTimeFormatter.ISO_INSTANT.format(instant).replace("Z", "");
    }

    static Document parseDocument(String xml) {
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setNamespaceAware(true);
            factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            return factory.newDocumentBuilder()
                    .parse(new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception ex) {
            throw new IllegalStateException("Failed to parse AFIP XML response", ex);
        }
    }

    static String nodeText(Document document, String localName) {
        NodeList nodes = document.getElementsByTagNameNS("*", localName);
        if (nodes.getLength() == 0) {
            return null;
        }
        String text = nodes.item(0).getTextContent();
        return text != null ? text.trim() : null;
    }
}
