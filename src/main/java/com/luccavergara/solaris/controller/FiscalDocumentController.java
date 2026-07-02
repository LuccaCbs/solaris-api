package com.luccavergara.solaris.controller;

import com.luccavergara.solaris.dto.FiscalDocumentResponse;
import com.luccavergara.solaris.service.FiscalDocumentService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/fiscal-documents")
@RequiredArgsConstructor
@PreAuthorize("@organizationSecurity.hasMinimumRole(T(com.luccavergara.solaris.entity.OrganizationMemberRole).CASHIER)")
public class FiscalDocumentController {

    private final FiscalDocumentService fiscalDocumentService;

    @GetMapping
    public List<FiscalDocumentResponse> getAllFiscalDocuments() {
        return fiscalDocumentService.getAllFiscalDocuments();
    }

    @GetMapping("/{id}")
    public FiscalDocumentResponse getFiscalDocumentById(@PathVariable Long id) {
        return fiscalDocumentService.getFiscalDocumentById(id);
    }

    @GetMapping("/by-sale/{saleId}")
    public FiscalDocumentResponse getFiscalDocumentBySaleId(@PathVariable Long saleId) {
        return fiscalDocumentService.getFiscalDocumentBySaleId(saleId);
    }
}
