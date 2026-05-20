package com.luccavergara.solaris.dto;

import com.luccavergara.solaris.entity.StockMovementType;
import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StockMovementResponse {

    private Long id;
    private Long productId;
    private String productName;
    private StockMovementType type;
    private Integer quantity;
    private Integer previousStock;
    private Integer currentStock;
    private String reason;
    private LocalDateTime createdAt;
}