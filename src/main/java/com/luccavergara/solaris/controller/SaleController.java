package com.luccavergara.solaris.controller;



import com.luccavergara.solaris.dto.DailySalesSummaryResponse;

import com.luccavergara.solaris.dto.EmitInvoiceRequest;

import com.luccavergara.solaris.dto.FiscalDocumentResponse;

import com.luccavergara.solaris.dto.SaleRequest;

import com.luccavergara.solaris.dto.SaleResponse;

import com.luccavergara.solaris.service.FiscalDocumentService;

import com.luccavergara.solaris.service.SaleService;

import jakarta.validation.Valid;

import lombok.RequiredArgsConstructor;

import org.springframework.http.HttpStatus;

import org.springframework.security.access.prepost.PreAuthorize;

import org.springframework.web.bind.annotation.*;



import java.time.LocalDate;

import java.util.List;



@RestController

@RequestMapping("/api/v1/sales")

@RequiredArgsConstructor

@PreAuthorize("@organizationSecurity.hasMinimumRole(T(com.luccavergara.solaris.entity.OrganizationMemberRole).CASHIER)")

public class SaleController {



    private final SaleService saleService;

    private final FiscalDocumentService fiscalDocumentService;



    @PostMapping

    @ResponseStatus(HttpStatus.CREATED)

    public SaleResponse createSale(

            @Valid @RequestBody SaleRequest request

    ) {

        return saleService.createSale(request);

    }



    @GetMapping

    public List<SaleResponse> getAllSales() {

        return saleService.getAllSales();

    }



    @GetMapping("/daily-summary")

    public DailySalesSummaryResponse getDailySummary(

            @RequestParam(required = false) LocalDate date

    ) {

        return saleService.getDailySummary(date);

    }



    @GetMapping("/{id}")

    public SaleResponse getSaleById(@PathVariable Long id) {

        return saleService.getSaleById(id);

    }



    @PostMapping("/{id}/invoice")

    @ResponseStatus(HttpStatus.CREATED)

    public FiscalDocumentResponse emitInvoice(

            @PathVariable Long id,

            @RequestBody(required = false) EmitInvoiceRequest request

    ) {

        return fiscalDocumentService.emitInvoiceForSale(id, request);

    }

}

