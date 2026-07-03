package com.luccavergara.solaris.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "plan_module_grants")
@IdClass(PlanModuleGrantId.class)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PlanModuleGrant {

    @Id
    @Column(name = "plan_code", nullable = false)
    private String planCode;

    @Id
    @Enumerated(EnumType.STRING)
    @Column(name = "module_code", nullable = false)
    private ModuleCode moduleCode;
}
