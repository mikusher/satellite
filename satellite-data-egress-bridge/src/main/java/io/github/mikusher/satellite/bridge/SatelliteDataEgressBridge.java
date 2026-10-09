package io.github.mikusher.satellite.bridge;

import com.mikusher.parameter.SatelliteData;
import io.github.mikusher.satellite.egress.DataOrigin;
import io.github.mikusher.satellite.egress.EgressEnvelope;
import io.github.mikusher.satellite.egress.Key;
import io.github.mikusher.satellite.egress.SatelliteSchema;
import io.github.mikusher.satellite.egress.TrustLevel;
import io.github.mikusher.satellite.egress.ValueMetadata;
import io.github.mikusher.satellite.egress.policy.EgressReport;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Optional bridge between SatelliteData and the security-aware Egress model.
 */
public final class SatelliteDataEgressBridge {
    private SatelliteDataEgressBridge() {
    }

    /**
     * Converts SatelliteData and rejects every source field without an explicit typed key.
     */
    public static DataBridgeResult toEnvelope(SatelliteData source,
                                              Collection<Key<?>> keys,
                                              ValueMetadata metadata) {
        return convert(source, keys, metadata, true);
    }

    public static DataBridgeResult toEnvelope(SatelliteData source,
                                              Collection<Key<?>> keys,
                                              DataOrigin origin,
                                              TrustLevel trustLevel) {
        return toEnvelope(
                source,
                keys,
                ValueMetadata.of(origin, trustLevel));
    }

    public static DataBridgeResult toEnvelope(SatelliteData source,
                                              SatelliteSchema schema,
                                              ValueMetadata metadata) {
        Objects.requireNonNull(schema, "schema");
        return toEnvelope(source, schema.getKnownKeys(), metadata);
    }

    public static DataBridgeResult toEnvelope(SatelliteData source,
                                              SatelliteSchema schema,
                                              DataOrigin origin,
                                              TrustLevel trustLevel) {
        return toEnvelope(
                source,
                schema,
                ValueMetadata.of(origin, trustLevel));
    }

    /**
     * Migration-only mode where unclassified fields are explicitly ignored and reported.
     */
    public static DataBridgeResult toEnvelopeLenient(SatelliteData source,
                                                     Collection<Key<?>> keys,
                                                     ValueMetadata metadata) {
        return convert(source, keys, metadata, false);
    }

    public static DataBridgeResult toEnvelopeLenient(SatelliteData source,
                                                     Collection<Key<?>> keys,
                                                     DataOrigin origin,
                                                     TrustLevel trustLevel) {
        return toEnvelopeLenient(
                source,
                keys,
                ValueMetadata.of(origin, trustLevel));
    }

    public static DataBridgeResult toEnvelopeLenient(SatelliteData source,
                                                     SatelliteSchema schema,
                                                     ValueMetadata metadata) {
        Objects.requireNonNull(schema, "schema");
        return toEnvelopeLenient(source, schema.getKnownKeys(), metadata);
    }

    public static DataBridgeResult toEnvelopeLenient(SatelliteData source,
                                                     SatelliteSchema schema,
                                                     DataOrigin origin,
                                                     TrustLevel trustLevel) {
        return toEnvelopeLenient(
                source,
                schema,
                ValueMetadata.of(origin, trustLevel));
    }

    /**
     * Converts only policy-approved output back into SatelliteData.
     */
    public static SatelliteData toSafeData(EgressReport report) {
        Objects.requireNonNull(report, "report");
        return new SatelliteData(
                new LinkedHashMap<String, Object>(report.getOutput()),
                true);
    }

    private static DataBridgeResult convert(SatelliteData source,
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
                throw new IllegalArgumentException(
                        "Duplicate key definition: " + key.getName());
            }
        }

        EgressEnvelope.Builder builder = EgressEnvelope.builder();
        List<String> ignored = new ArrayList<String>();

        for (String name : source.keySet()) {
            Key<?> key = registry.get(name);
            if (key == null) {
                if (rejectUnknown) {
                    throw new IllegalArgumentException(
                            "Unclassified SatelliteData key: " + name);
                }
                ignored.add(name);
                continue;
            }
            putCaptured(builder, key, source.get(name), metadata);
        }

        return new DataBridgeResult(builder.build(), ignored);
    }

    private static <T> void putCaptured(EgressEnvelope.Builder builder,
                                        Key<T> key,
                                        Object rawValue,
                                        ValueMetadata metadata) {
        builder.put(key, key.cast(rawValue), metadata);
    }
}
