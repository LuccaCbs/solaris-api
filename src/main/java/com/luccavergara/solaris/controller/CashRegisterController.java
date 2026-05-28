package com.luccavergara.solaris.controller;

import com.luccavergara.solaris.dto.CashRegisterSessionResponse;
import com.luccavergara.solaris.service.CashRegisterService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import com.luccavergara.solaris.dto.CashRegisterAuthorizationRequest;
import jakarta.validation.Valid;


@RestController
@RequestMapping("/api/v1/cash-register")
@RequiredArgsConstructor
public class CashRegisterController {

    private final CashRegisterService cashRegisterService;

    @GetMapping("/current")
    public CashRegisterSessionResponse getCurrentSession() {
        return cashRegisterService.getCurrentSession();
    }

    @PostMapping("/open")
    @ResponseStatus(HttpStatus.CREATED)
    public CashRegisterSessionResponse openCashRegister(
            @Valid @RequestBody CashRegisterAuthorizationRequest request
    ) {
        return cashRegisterService.openCashRegister(request);
    }

    @PostMapping("/close")
    public CashRegisterSessionResponse closeCashRegister(
            @Valid @RequestBody CashRegisterAuthorizationRequest request
    ) {
        return cashRegisterService.closeCashRegister(request);
    }

    @PostMapping("/reopen/{id}")
    public CashRegisterSessionResponse reopenCashRegister(
            @PathVariable Long id,
            @Valid @RequestBody CashRegisterAuthorizationRequest request
    ) {
        return cashRegisterService.reopenCashRegister(id, request);
    }

    @GetMapping("/today")
    public CashRegisterSessionResponse getTodaySession() {
        return cashRegisterService.getTodaySession();
    }
}