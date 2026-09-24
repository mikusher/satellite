package io.github.mikusher.satellite.egress;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * Immutable typed data envelope. It deliberately has no raw-map export API.
 */
public final class SatelliteMap {
    private final Map<Key<?>, SatelliteEntry<?>> entries;

    private SatelliteMap(Map<Key<?>, SatelliteEntry<?>> entries) {
        this.entries = Collections.unmodifiableMap(new LinkedHashMap<Key<?>, SatelliteEntry<?>>(entries));
    }

    public static Builder builder() {
        return new Builder();
    }

    public <T> T get(Key<T> key) {
        SatelliteEntry<?> entry = entries.get(Objects.requireNonNull(key, "key"));
        if (entry == null) {
            return null;
        }
        return key.cast(entry.getValue());
    }

    @SuppressWarnings("unchecked")
    public <T> Optional<SatelliteEntry<T>> entry(Key<T> key) {
        Objects.requireNonNull(key, "key");
        SatelliteEntry<?> value = entries.get(key);
        if (value == null) {
            return Optional.empty();
        }
        return Optional.of((SatelliteEntry<T>) value);
    }

    public boolean contains(Key<?> key) {
        return entries.containsKey(Objects.requireNonNull(key, "key"));
    }

    public int size() {
        return entries.size();
    }

    public Collection<SatelliteEntry<?>> entries() {
        return Collections.unmodifiableList(new ArrayList<SatelliteEntry<?>>(entries.values()));
    }

    @Override
    public String toString() {
        return "SatelliteMap{size=" + entries.size() + ", keys=" + entries.keySet() + '}';
    }

    public static final class Builder {
        private final Map<Key<?>, SatelliteEntry<?>> entries = new LinkedHashMap<Key<?>, SatelliteEntry<?>>();
        private final Map<String, Key<?>> keysByName = new LinkedHashMap<String, Key<?>>();

        public <T> Builder put(Key<T> key, T value) {
            return put(key, value, ValueMetadata.unknown());
        }

        public <T> Builder put(Key<T> key, T value, ValueMetadata metadata) {
            Objects.requireNonNull(key, "key");
            Objects.requireNonNull(metadata, "metadata");

            Key<?> existing = keysByName.get(key.getName());
            if (existing != null && !existing.equals(key)) {
                throw new IllegalArgumentException(
                        "Conflicting key definition for external name '" + key.getName() + "'");
            }

            T safeValue = key.cast(value);
            keysByName.put(key.getName(), key);
            entries.put(key, new SatelliteEntry<T>(key, safeValue, metadata));
            return this;
        }

        public SatelliteMap build() {
            return new SatelliteMap(entries);
        }
    }
}
