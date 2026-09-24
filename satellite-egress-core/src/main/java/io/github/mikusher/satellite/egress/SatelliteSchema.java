package io.github.mikusher.satellite.egress;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;

public final class SatelliteSchema {
    private final String name;
    private final Set<Key<?>> knownKeys;
    private final Set<Key<?>> requiredKeys;
    private final boolean allowUnknownKeys;

    private SatelliteSchema(String name,
                            Set<Key<?>> knownKeys,
                            Set<Key<?>> requiredKeys,
                            boolean allowUnknownKeys) {
        this.name = name;
        this.knownKeys = new LinkedHashSet<Key<?>>(knownKeys);
        this.requiredKeys = new LinkedHashSet<Key<?>>(requiredKeys);
        this.allowUnknownKeys = allowUnknownKeys;
    }

    public static Builder builder(String name) {
        return new Builder(name);
    }

    public ValidationResult validate(SatelliteMap map) {
        Objects.requireNonNull(map, "map");
        ArrayList<ValidationError> errors = new ArrayList<ValidationError>();

        for (Key<?> key : requiredKeys) {
            if (!map.contains(key) || !hasNonNullValue(map, key)) {
                errors.add(new ValidationError(
                        "REQUIRED_VALUE_MISSING",
                        key.getName(),
                        "Required value is missing"));
            }
        }

        if (!allowUnknownKeys) {
            Collection<SatelliteEntry<?>> entries = map.entries();
            for (SatelliteEntry<?> entry : entries) {
                if (!knownKeys.contains(entry.getKey())) {
                    errors.add(new ValidationError(
                            "UNKNOWN_KEY",
                            entry.getKey().getName(),
                            "Key is not part of schema '" + name + "'"));
                }
            }
        }

        return new ValidationResult(errors);
    }

    private static <T> boolean hasNonNullValue(SatelliteMap map, Key<T> key) {
        return map.entry(key).map(entry -> entry.getValue() != null).orElse(false);
    }

    public static final class Builder {
        private final String name;
        private final Set<Key<?>> knownKeys = new LinkedHashSet<Key<?>>();
        private final Set<Key<?>> requiredKeys = new LinkedHashSet<Key<?>>();
        private boolean allowUnknownKeys;

        private Builder(String name) {
            if (name == null || name.trim().isEmpty()) {
                throw new IllegalArgumentException("name must not be blank");
            }
            this.name = name;
        }

        public Builder required(Key<?> key) {
            Objects.requireNonNull(key, "key");
            knownKeys.add(key);
            requiredKeys.add(key);
            return this;
        }

        public Builder optional(Key<?> key) {
            knownKeys.add(Objects.requireNonNull(key, "key"));
            return this;
        }

        public Builder allowUnknownKeys(boolean value) {
            this.allowUnknownKeys = value;
            return this;
        }

        public SatelliteSchema build() {
            return new SatelliteSchema(name, knownKeys, requiredKeys, allowUnknownKeys);
        }
    }
}
