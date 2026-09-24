package io.github.mikusher.satellite.bridge;

import com.mikusher.parameter.ParameterMap;
import io.github.mikusher.satellite.egress.Key;
import io.github.mikusher.satellite.egress.SatelliteMap;
import io.github.mikusher.satellite.egress.ValueMetadata;
import io.github.mikusher.satellite.egress.policy.EgressReport;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Optional bridge between the legacy ParameterMap API and the egress model.
 */
public final class ParameterMapEgressBridge {
    private ParameterMapEgressBridge() {
    }

    /**
     * Converts a ParameterMap and rejects every source field without an explicit typed key.
     */
    public static BridgeResult toSatelliteMap(ParameterMap source,
                                              Collection<Key<?>> keys,
                                              ValueMetadata metadata) {
        return convert(source, keys, metadata, true);
    }

    /**
     * Explicit opt-in for migration scenarios where unclassified fields should be ignored.
     */
    public static BridgeResult toSatelliteMapLenient(ParameterMap source,
                                                     Collection<Key<?>> keys,
                                                     ValueMetadata metadata) {
        return convert(source, keys, metadata, false);
    }

    /**
     * Converts policy-approved output back to ParameterMap. Denied values are absent.
     */
    public static ParameterMap toSafeParameterMap(EgressReport report) {
        Objects.requireNonNull(report, "report");
        return new ParameterMap(new LinkedHashMap<String, Object>(report.getOutput()), true);
    }

    private static BridgeResult convert(ParameterMap source,
                                        Collection<Key<?>> keys,
                                        ValueMetadata metadata,
                                        boolean rejectUnknown) {
        Objects.requireNonNull(source, "source");
        Objects.requireNonNull(keys, "keys");
        Objects.requireNonNull(metadata, "metadata");

        Map<String, Key<?>> registry = new LinkedHashMap<String, Key<?>>();
        for (Key<?> key : keys) {
            Key<?> previous = registry.put(key.getName(), key);
            if (previous != null) {
                throw new IllegalArgumentException("Duplicate key definition: " + key.getName());
            }
        }

        SatelliteMap.Builder builder = SatelliteMap.builder();
        List<String> ignored = new ArrayList<String>();

        for (String name : source.keySet()) {
            Key<?> key = registry.get(name);
            if (key == null) {
                if (rejectUnknown) {
                    throw new IllegalArgumentException("Unclassified ParameterMap key: " + name);
                }
                ignored.add(name);
                continue;
            }
            putCaptured(builder, key, source.get(name), metadata);
        }

        return new BridgeResult(builder.build(), ignored);
    }

    private static <T> void putCaptured(SatelliteMap.Builder builder,
                                        Key<T> key,
                                        Object rawValue,
                                        ValueMetadata metadata) {
        builder.put(key, key.cast(rawValue), metadata);
    }
}
