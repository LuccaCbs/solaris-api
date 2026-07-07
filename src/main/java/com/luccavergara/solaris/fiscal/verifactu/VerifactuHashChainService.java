package com.luccavergara.solaris.fiscal.verifactu;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.luccavergara.solaris.entity.FiscalDocument;
import com.luccavergara.solaris.entity.FiscalDocumentStatus;
import com.luccavergara.solaris.repository.FiscalDocumentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.List;

@Component
@RequiredArgsConstructor
public class VerifactuHashChainService {

    private final FiscalDocumentRepository fiscalDocumentRepository;
    private final ObjectMapper objectMapper;

    public String resolvePreviousHash(Long organizationId) {
        List<FiscalDocument> documents = fiscalDocumentRepository.findAllByOrganizationIdOrderByCreatedAtDesc(organizationId);

        for (FiscalDocument document : documents) {
            if (document.getStatus() != FiscalDocumentStatus.AUTHORIZED) {
                continue;
            }

            String huella = extractHuella(document.getAfipRawJson());
            if (StringUtils.hasText(huella)) {
                return huella;
            }
        }

        return "";
    }

    private String extractHuella(String rawJson) {
        if (!StringUtils.hasText(rawJson)) {
            return null;
        }

        try {
            JsonNode root = objectMapper.readTree(rawJson);
            JsonNode huella = root.get("huella");
            if (huella != null && huella.isTextual()) {
                return huella.asText();
            }
        } catch (Exception ignored) {
            return null;
        }

        return null;
    }
}
