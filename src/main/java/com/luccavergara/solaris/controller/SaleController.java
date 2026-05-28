package com.luccavergara.solaris.controller;

import com.luccavergara.solaris.dto.SaleRequest;
import com.luccavergara.solaris.dto.SaleResponse;
import com.luccavergara.solaris.service.SaleService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import com.luccavergara.solaris.dto.DailySalesSummaryResponse;
import java.time.LocalDate;


import java.util.List;

@RestController
@RequestMapping("/api/v1/sales")
@RequiredArgsConstructor
public class SaleController {

    private final SaleService saleService;

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
}