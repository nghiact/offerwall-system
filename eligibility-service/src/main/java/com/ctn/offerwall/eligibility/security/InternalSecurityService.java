package com.ctn.offerwall.eligibility.security;

import com.ctn.offerwall.eligibility.config.InternalSecurityProperties;
import com.ctn.offerwall.eligibility.exception.AuthenticationRequiredException;
import org.springframework.stereotype.Service;

@Service
public class InternalSecurityService {

    public static final String INTERNAL_SERVICE_KEY_HEADER = "X-Internal-Service-Key";

    private final InternalSecurityProperties properties;

    public InternalSecurityService(InternalSecurityProperties properties) {
        this.properties = properties;
    }

    public void requireInternalServiceKey(String internalServiceKey) {
        if (!hasText(properties.getApiKey()) || !hasText(internalServiceKey)
                || !properties.getApiKey().equals(internalServiceKey.trim())) {
            throw new AuthenticationRequiredException("Internal service key is required.");
        }
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
