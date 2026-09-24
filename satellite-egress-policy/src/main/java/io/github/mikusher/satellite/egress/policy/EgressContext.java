package io.github.mikusher.satellite.egress.policy;

import java.util.Objects;

public final class EgressContext {
    private final EgressSink sink;
    private final String purpose;

    private EgressContext(EgressSink sink, String purpose) {
        this.sink = Objects.requireNonNull(sink, "sink");
        this.purpose = validatePurpose(purpose);
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

    private static String validatePurpose(String value) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException("purpose must not be blank");
        }
        for (int i = 0; i < value.length(); i++) {
            if (Character.isISOControl(value.charAt(i))) {
                throw new IllegalArgumentException("purpose must not contain control characters");
            }
        }
        return value;
    }
}
