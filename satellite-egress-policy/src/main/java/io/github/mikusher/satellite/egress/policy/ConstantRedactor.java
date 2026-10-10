package io.github.mikusher.satellite.egress.policy;

import io.github.mikusher.satellite.egress.SatelliteEntry;

import java.util.Objects;

public final class ConstantRedactor implements Redactor {
    private final String marker;

    public ConstantRedactor() {
        this("[REDACTED]");
    }

    public ConstantRedactor(String marker) {
        this.marker = Objects.requireNonNull(marker, "marker");
    }

    @Override
    public String redact(SatelliteEntry<?> entry) {
        return marker;
    }
}
