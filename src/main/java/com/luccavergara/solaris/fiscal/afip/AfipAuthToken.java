package com.luccavergara.solaris.fiscal.afip;

import java.time.Instant;

public record AfipAuthToken(String token, String sign, Instant expirationTime) {

    public boolean isValid() {
        return token != null
                && sign != null
                && expirationTime != null
                && Instant.now().isBefore(expirationTime.minusSeconds(300));
    }
}
