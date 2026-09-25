package io.github.mikusher.satellite.bridge;

import io.github.mikusher.satellite.egress.EgressEnvelope;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Result of converting SatelliteData into an EgressEnvelope.
 */
public final class DataBridgeResult {
    private final EgressEnvelope envelope;
    private final List<String> ignoredKeys;

    DataBridgeResult(EgressEnvelope envelope, List<String> ignoredKeys) {
        this.envelope = Objects.requireNonNull(envelope, "envelope");
        this.ignoredKeys = Collections.unmodifiableList(
                new ArrayList<String>(ignoredKeys));
    }

    public EgressEnvelope getEnvelope() {
        return envelope;
    }

    public List<String> getIgnoredKeys() {
        return ignoredKeys;
    }

    public boolean hasIgnoredKeys() {
        return !ignoredKeys.isEmpty();
    }
}
