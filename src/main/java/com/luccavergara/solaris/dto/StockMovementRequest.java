package com.luccavergara.solaris.dto;

import com.luccavergara.solaris.entity.StockMovementType;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StockMovementRequest {

    @NotNull
    private Long productId;

    @NotNull
    private StockMovementType type;

    @NotNull
    @Min(1)
    private Integer quantity;

    private String reason;
}