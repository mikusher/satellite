package io.github.mikusher.satellite.bridge;

import io.github.mikusher.satellite.egress.SatelliteMap;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

public final class BridgeResult {
    private final SatelliteMap satelliteMap;
    private final List<String> ignoredKeys;

    BridgeResult(SatelliteMap satelliteMap, List<String> ignoredKeys) {
        this.satelliteMap = Objects.requireNonNull(satelliteMap, "satelliteMap");
        this.ignoredKeys = Collections.unmodifiableList(new ArrayList<String>(ignoredKeys));
    }

    public SatelliteMap getSatelliteMap() {
        return satelliteMap;
    }

    public List<String> getIgnoredKeys() {
        return ignoredKeys;
    }

    public boolean hasIgnoredKeys() {
        return !ignoredKeys.isEmpty();
    }
}
