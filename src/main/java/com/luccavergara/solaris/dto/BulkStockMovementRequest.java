package com.luccavergara.solaris.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import lombok.*;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BulkStockMovementRequest {

    private String reason;

    @NotEmpty
    @Valid
    private List<BulkStockMovementItemRequest> items;
}
