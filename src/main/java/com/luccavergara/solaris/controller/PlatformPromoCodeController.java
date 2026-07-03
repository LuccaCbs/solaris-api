package com.luccavergara.solaris.controller;

import com.luccavergara.solaris.dto.*;
import com.luccavergara.solaris.service.PromoCodeService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/platform/promo-codes")
@RequiredArgsConstructor
@PreAuthorize("@platformSecurity.isPlatformOperator()")
public class PlatformPromoCodeController {

    private final PromoCodeService promoCodeService;

    @GetMapping
    public List<PromoCodeResponse> listPromoCodes() {
        return promoCodeService.listPromoCodes();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public PromoCodeResponse createPromoCode(@Valid @RequestBody CreatePromoCodeRequest request) {
        return promoCodeService.createPromoCode(request);
    }

    @GetMapping("/{promoCodeId}")
    public PromoCodeResponse getPromoCode(@PathVariable Long promoCodeId) {
        return promoCodeService.getPromoCode(promoCodeId);
    }

    @GetMapping("/{promoCodeId}/redemptions")
    public List<PromoCodeRedemptionResponse> listRedemptions(@PathVariable Long promoCodeId) {
        return promoCodeService.listPromoCodeRedemptions(promoCodeId);
    }

    @PostMapping("/{promoCodeId}/revoke")
    public PromoCodeResponse revokePromoCode(
            @PathVariable Long promoCodeId,
            @RequestBody(required = false) RevokePromoCodeRequest request
    ) {
        return promoCodeService.revokePromoCode(promoCodeId, request);
    }

    @PostMapping("/{promoCodeId}/disable")
    public PromoCodeResponse disablePromoCode(@PathVariable Long promoCodeId) {
        return promoCodeService.disablePromoCode(promoCodeId);
    }
}
