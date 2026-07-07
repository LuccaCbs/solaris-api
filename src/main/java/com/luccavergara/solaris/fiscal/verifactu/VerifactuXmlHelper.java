package com.luccavergara.solaris.fiscal.verifactu;

import org.springframework.stereotype.Component;

@Component
public class VerifactuXmlHelper {

    public String textContent(String xml, String tagName) {
        if (xml == null || tagName == null) {
            return null;
        }

        String open = "<" + tagName + ">";
        String close = "</" + tagName + ">";
        int start = xml.indexOf(open);
        if (start < 0) {
            String namespacedOpen = ":" + tagName + ">";
            start = xml.indexOf(namespacedOpen);
            if (start < 0) {
                return null;
            }
            start += namespacedOpen.length();
            int end = xml.indexOf("</", start);
            return end < 0 ? null : xml.substring(start, end).trim();
        }

        start += open.length();
        int end = xml.indexOf(close, start);
        return end < 0 ? null : xml.substring(start, end).trim();
    }

    public String firstFault(String xml) {
        if (xml == null) {
            return null;
        }

        if (xml.contains("Fault")) {
            String faultString = textContent(xml, "faultstring");
            if (faultString != null) {
                return faultString;
            }
            return textContent(xml, "Reason");
        }

        return null;
    }

    public static String escapeXml(String value) {
        if (value == null) {
            return "";
        }

        return value
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&apos;");
    }
}
