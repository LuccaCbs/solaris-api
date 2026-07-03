package com.luccavergara.solaris.dto;

import com.luccavergara.solaris.entity.CashRegisterStatus;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CashRegisterSessionResponse {

    private Long id;
    private LocalDateTime openedAt;
    private LocalDateTime closedAt;

    private String openedBy;
    private String closedBy;

    private CashRegisterStatus status;
    private Integer reopenCount;

    private BigDecimal closingAmount;

    private Long storeId;

    private Integer cashCount;
    private BigDecimal cashAmount;

    private Integer creditCardCount;
    private BigDecimal creditCardAmount;

    private Integer debitCardCount;
    private BigDecimal debitCardAmount;

    private Integer transferCount;
    private BigDecimal transferAmount;

    private Integer otherCount;
    private BigDecimal otherAmount;

}