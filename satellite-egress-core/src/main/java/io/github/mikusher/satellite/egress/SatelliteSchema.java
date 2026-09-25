package io.github.mikusher.satellite.egress;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
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
        this.knownKeys = Collections.unmodifiableSet(new LinkedHashSet<Key<?>>(knownKeys));
        this.requiredKeys = Collections.unmodifiableSet(new LinkedHashSet<Key<?>>(requiredKeys));
        this.allowUnknownKeys = allowUnknownKeys;
    }

    public static Builder builder(String name) {
        return new Builder(name);
    }

    public String getName() {
        return name;
    }

    public Set<Key<?>> getKnownKeys() {
        return knownKeys;
    }

    public Set<Key<?>> getRequiredKeys() {
        return requiredKeys;
    }

    public boolean isAllowUnknownKeys() {
        return allowUnknownKeys;
    }

    public ValidationResult validate(EgressEnvelope envelope) {
        Objects.requireNonNull(envelope, "envelope");
        ArrayList<ValidationError> errors = new ArrayList<ValidationError>();

        for (Key<?> key : requiredKeys) {
            if (!envelope.contains(key) || !hasNonNullValue(envelope, key)) {
                errors.add(new ValidationError(
                        "REQUIRED_VALUE_MISSING",
                        key.getName(),
                        "Required value is missing"));
            }
        }

        if (!allowUnknownKeys) {
            Collection<SatelliteEntry<?>> entries = envelope.entries();
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

    private static <T> boolean hasNonNullValue(EgressEnvelope envelope, Key<T> key) {
        return envelope.entry(key).map(entry -> entry.getValue() != null).orElse(false);
    }

    public static final class Builder {
        private final String name;
        private final Set<Key<?>> knownKeys = new LinkedHashSet<Key<?>>();
        private final Set<Key<?>> requiredKeys = new LinkedHashSet<Key<?>>();
        private final Map<String, Key<?>> keysByName = new LinkedHashMap<String, Key<?>>();
        private boolean allowUnknownKeys;

        private Builder(String name) {
            if (name == null || name.trim().isEmpty()) {
                throw new IllegalArgumentException("name must not be blank");
            }
            this.name = name;
        }

        public Builder required(Key<?> key) {
            register(key);
            requiredKeys.add(key);
            return this;
        }

        public Builder optional(Key<?> key) {
            register(key);
            return this;
        }

        public Builder allowUnknownKeys(boolean value) {
            this.allowUnknownKeys = value;
            return this;
        }

        public SatelliteSchema build() {
            return new SatelliteSchema(name, knownKeys, requiredKeys, allowUnknownKeys);
        }

        private void register(Key<?> key) {
            Objects.requireNonNull(key, "key");
            Key<?> existing = keysByName.get(key.getName());
            if (existing != null && !existing.equals(key)) {
                throw new IllegalArgumentException(
                        "Conflicting key definition for schema name '" + key.getName() + "'");
            }
            keysByName.put(key.getName(), key);
            knownKeys.add(key);
        }
    }
}
