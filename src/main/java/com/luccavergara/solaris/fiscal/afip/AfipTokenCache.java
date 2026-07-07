package com.luccavergara.solaris.fiscal.afip;

import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class AfipTokenCache {

    private final Map<String, AfipAuthToken> tokens = new ConcurrentHashMap<>();

    public AfipAuthToken get(String cacheKey) {
        AfipAuthToken cached = tokens.get(cacheKey);
        if (cached != null && cached.isValid()) {
            return cached;
        }
        return null;
    }

    public void put(String cacheKey, AfipAuthToken token) {
        tokens.put(cacheKey, token);
    }
}
