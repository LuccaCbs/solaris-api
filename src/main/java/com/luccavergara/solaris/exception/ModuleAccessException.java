package com.luccavergara.solaris.exception;

import com.luccavergara.solaris.entity.ModuleCode;
import lombok.Getter;

@Getter
public class ModuleAccessException extends RuntimeException {

    private final ModuleCode moduleCode;

    public ModuleAccessException(ModuleCode moduleCode, String message) {
        super(message);
        this.moduleCode = moduleCode;
    }
}
