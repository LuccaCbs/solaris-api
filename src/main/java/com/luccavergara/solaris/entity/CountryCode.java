package com.luccavergara.solaris.entity;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum CountryCode {

    AR("Argentina"),
    ES("Spain");

    private final String displayName;
}
