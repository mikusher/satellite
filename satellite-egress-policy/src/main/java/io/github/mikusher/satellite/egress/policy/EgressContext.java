package io.github.mikusher.satellite.egress.policy;

import java.util.Objects;

public final class EgressContext {
    private final EgressSink sink;
    private final String purpose;

    private EgressContext(EgressSink sink, String purpose) {
        this.sink = Objects.requireNonNull(sink, "sink");
        if (purpose == null || purpose.trim().isEmpty()) {
            throw new IllegalArgumentException("purpose must not be blank");
        }
        this.purpose = purpose;
    }

    public static EgressContext of(EgressSink sink, String purpose) {
        return new EgressContext(sink, purpose);
    }

    public EgressSink getSink() {
        return sink;
    }

    public String getPurpose() {
        return purpose;
    }
}
