package com.luccavergara.solaris.dto;

import lombok.*;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductImportResponse {

    private Integer createdCount;

    private Integer failedCount;

    private List<String> errors;

    private Integer updatedCount;
}