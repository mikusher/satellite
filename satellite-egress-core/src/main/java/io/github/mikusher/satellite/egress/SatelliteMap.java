package io.github.mikusher.satellite.egress;

import java.util.Collection;
import java.util.Optional;

/**
 * @deprecated Use {@link EgressEnvelope}. SatelliteMap is kept as a
 * compatibility facade for the pre-2.x foundation API.
 */
@Deprecated
public final class SatelliteMap {
    private final EgressEnvelope delegate;

    private SatelliteMap(EgressEnvelope delegate) {
        this.delegate = delegate;
    }

    public static Builder builder() {
        return new Builder();
    }

    public <T> T get(Key<T> key) {
        return delegate.get(key);
    }

    public <T> Optional<SatelliteEntry<T>> entry(Key<T> key) {
        return delegate.entry(key);
    }

    public boolean contains(Key<?> key) {
        return delegate.contains(key);
    }

    public int size() {
        return delegate.size();
    }

    public Collection<SatelliteEntry<?>> entries() {
        return delegate.entries();
    }

    /**
     * Returns the new canonical representation without copying values.
     */
    public EgressEnvelope asEgressEnvelope() {
        return delegate;
    }

    public static SatelliteMap fromEgressEnvelope(EgressEnvelope envelope) {
        if (envelope == null) {
            throw new NullPointerException("envelope");
        }
        return new SatelliteMap(envelope);
    }

    @Override
    public String toString() {
        return "SatelliteMap{delegate=" + delegate + '}';
    }

    public static final class Builder {
        private final EgressEnvelope.Builder delegate = EgressEnvelope.builder();

        public <T> Builder put(Key<T> key, T value) {
            delegate.put(key, value);
            return this;
        }

        public <T> Builder put(Key<T> key, T value, ValueMetadata metadata) {
            delegate.put(key, value, metadata);
            return this;
        }

        public SatelliteMap build() {
            return new SatelliteMap(delegate.build());
        }
    }
}
