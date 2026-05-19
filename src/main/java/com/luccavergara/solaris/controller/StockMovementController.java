package com.luccavergara.solaris.controller;

import com.luccavergara.solaris.dto.StockMovementRequest;
import com.luccavergara.solaris.dto.StockMovementResponse;
import com.luccavergara.solaris.service.StockMovementService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/stock-movements")
@RequiredArgsConstructor
public class StockMovementController {

    private final StockMovementService stockMovementService;

    @PostMapping
    public ResponseEntity<StockMovementResponse> createMovement(
            @Valid @RequestBody StockMovementRequest request
    ) {
        return ResponseEntity.ok(stockMovementService.createMovement(request));
    }

    @GetMapping
    public ResponseEntity<List<StockMovementResponse>> getAllMovements() {
        return ResponseEntity.ok(stockMovementService.getAllMovements());
    }

    @GetMapping("/product/{productId}")
    public ResponseEntity<List<StockMovementResponse>> getMovementsByProduct(
            @PathVariable Long productId
    ) {
        return ResponseEntity.ok(stockMovementService.getMovementsByProduct(productId));
    }
}