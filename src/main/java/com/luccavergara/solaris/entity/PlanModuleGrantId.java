package com.luccavergara.solaris.entity;

import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
public class PlanModuleGrantId implements Serializable {

    private String planCode;
    private ModuleCode moduleCode;
}
