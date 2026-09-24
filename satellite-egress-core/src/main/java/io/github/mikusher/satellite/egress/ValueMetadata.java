package io.github.mikusher.satellite.egress;

import java.util.Objects;

/**
 * Runtime metadata that belongs to a value rather than to its key.
 */
public final class ValueMetadata {
    private static final ValueMetadata UNKNOWN = new ValueMetadata(DataOrigin.UNKNOWN, TrustLevel.UNKNOWN);

    private final DataOrigin origin;
    private final TrustLevel trustLevel;

    private ValueMetadata(DataOrigin origin, TrustLevel trustLevel) {
        this.origin = Objects.requireNonNull(origin, "origin");
        this.trustLevel = Objects.requireNonNull(trustLevel, "trustLevel");
    }

    public static ValueMetadata of(DataOrigin origin, TrustLevel trustLevel) {
        return new ValueMetadata(origin, trustLevel);
    }

    public static ValueMetadata unknown() {
        return UNKNOWN;
    }

    public DataOrigin getOrigin() {
        return origin;
    }

    public TrustLevel getTrustLevel() {
        return trustLevel;
    }

    @Override
    public String toString() {
        return "ValueMetadata{origin=" + origin + ", trustLevel=" + trustLevel + '}';
    }
}
