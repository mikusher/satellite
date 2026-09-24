package io.github.mikusher.satellite.egress;

import java.util.Objects;

/**
 * A typed value plus its runtime metadata.
 */
public final class SatelliteEntry<T> {
    private final Key<T> key;
    private final T value;
    private final ValueMetadata metadata;

    SatelliteEntry(Key<T> key, T value, ValueMetadata metadata) {
        this.key = Objects.requireNonNull(key, "key");
        this.value = value;
        this.metadata = Objects.requireNonNull(metadata, "metadata");
    }

    public Key<T> getKey() {
        return key;
    }

    public T getValue() {
        return value;
    }

    public ValueMetadata getMetadata() {
        return metadata;
    }

    @Override
    public String toString() {
        return "SatelliteEntry{key=" + key.getName()
                + ", classification=" + key.getClassification()
                + ", categories=" + key.getCategories()
                + ", metadata=" + metadata
                + ", value=[PROTECTED]}";
    }
}
