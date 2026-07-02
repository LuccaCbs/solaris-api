package com.luccavergara.solaris.controller;

import com.luccavergara.solaris.dto.SupplierOrderRequest;
import com.luccavergara.solaris.dto.SupplierOrderResponse;
import com.luccavergara.solaris.service.SupplierOrderService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/supplier-orders")
@RequiredArgsConstructor
public class SupplierOrderController {

    private final SupplierOrderService supplierOrderService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public SupplierOrderResponse createSupplierOrder(
            @Valid @RequestBody SupplierOrderRequest request
    ) {
        return supplierOrderService.createSupplierOrder(request);
    }

    @GetMapping
    public List<SupplierOrderResponse> getAllSupplierOrders() {
        return supplierOrderService.getAllSupplierOrders();
    }

    @GetMapping("/{id}")
    public SupplierOrderResponse getSupplierOrderById(@PathVariable Long id) {
        return supplierOrderService.getSupplierOrderById(id);
    }

    @PutMapping("/{id}")
    public SupplierOrderResponse updateSupplierOrder(
            @PathVariable Long id,
            @Valid @RequestBody SupplierOrderRequest request
    ) {
        return supplierOrderService.updateSupplierOrder(id, request);
    }

    @PatchMapping("/{id}/sent")
    public SupplierOrderResponse markAsSent(@PathVariable Long id) {
        return supplierOrderService.markAsSent(id);
    }

    @PatchMapping("/{id}/completed")
    public SupplierOrderResponse markAsCompleted(@PathVariable Long id) {
        return supplierOrderService.markAsCompleted(id);
    }

    @PatchMapping("/{id}/cancel")
    public SupplierOrderResponse cancelSupplierOrder(@PathVariable Long id) {
        return supplierOrderService.cancelSupplierOrder(id);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteSupplierOrder(@PathVariable Long id) {
        supplierOrderService.deleteSupplierOrder(id);
    }
}