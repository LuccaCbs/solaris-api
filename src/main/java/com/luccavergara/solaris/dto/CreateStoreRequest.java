package com.luccavergara.solaris.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateStoreRequest {

    @NotBlank
    @Size(max = 255)
    private String name;

    @Size(max = 500)
    private String address;

    @Min(1)
    private Integer afipPuntoVenta;
}
