package com.luccavergara.solaris.dto;

import com.luccavergara.solaris.entity.OrganizationMemberRole;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UpdateStoreRequest {

    @NotNull
    @Size(max = 255)
    private String name;

    @Size(max = 500)
    private String address;

    private Integer afipPuntoVenta;
}
