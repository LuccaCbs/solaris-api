package com.luccavergara.solaris.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StoreResponse {

    private Long id;
    private String name;
    private String address;
    private Integer afipPuntoVenta;
    private Boolean active;
}
