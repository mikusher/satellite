package io.github.mikusher.satellite.egress.observability;

import io.github.mikusher.satellite.egress.policy.PrivacyViolation;

public interface PrivacyViolationListener {
    void onViolation(PrivacyViolation violation);

    static PrivacyViolationListener noop() {
        return violation -> {
            // Intentionally no-op.
        };
    }
}
