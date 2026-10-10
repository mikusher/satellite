package com.mikusher.parameter;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * UUID conversion for dynamic data; callers use the SatelliteDataUtils API.
 *
 * Invalid UUID values are ignored, not treated as an exception. In particular,
 * a heterogeneous collection must never be blindly cast to List<SatelliteData>.
 */
final class SatelliteUuidSupport {

    private static final String UUID_FIELD = "UUID";

    private SatelliteUuidSupport() {
    }

    static List<UUID> getUUIDList(String key, SatelliteData data) {
        Stream<?> values = listValues(key, data);
        return values == null ? null
                : values.map(SatelliteUuidSupport::toUUID)
                        .filter(Objects::nonNull)
                        .collect(Collectors.toList());
    }

    static List<UUID> getUUIDListFromObjectList(String key, SatelliteData data) {
        Stream<?> values = listValues(key, data);
        return values == null ? null
                : values.map(SatelliteUuidSupport::toUUIDFromData)
                        .filter(Objects::nonNull)
                        .collect(Collectors.toList());
    }

    static List<UUID> toUUIDList(Collection<?> values) {
        return values.stream()
                .map(SatelliteUuidSupport::toUUID)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    static Set<UUID> toUUIDSet(Collection<?> values) {
        return new HashSet<>(toUUIDList(values));
    }

    static Set<UUID> getUUIDSet(String key, SatelliteData data, Set<UUID> fallback) {
        List<UUID> values = getUUIDList(key, data);
        return values == null ? fallback : new HashSet<>(values);
    }

    static Set<UUID> getUUIDSetFromObjectList(String key, SatelliteData data) {
        List<UUID> values = getUUIDListFromObjectList(key, data);
        return values == null ? null : new HashSet<>(values);
    }

    static Set<UUID> getUUIDSetFromMixedList(String key, SatelliteData data) {
        Stream<?> values = listValues(key, data);
        return values == null ? null
                : values.map(SatelliteUuidSupport::toUUIDCandidate)
                        .filter(Objects::nonNull)
                        .collect(Collectors.toSet());
    }

    static void setUUIDList(String key, Collection<UUID> uuids, SatelliteData data) {
        if (key == null || data == null || uuids == null) {
            return;
        }
        data.put(key, uuids.stream()
                .filter(Objects::nonNull)
                .map(UUID::toString)
                .collect(Collectors.toList()));
    }

    static UUID getUUID(String key, SatelliteData data, UUID fallback) {
        if (key == null || data == null) {
            return fallback;
        }
        UUID result = toUUID(data.get(key));
        return result == null ? fallback : result;
    }

    static void setUUID(String key, UUID uuid, SatelliteData data) {
        if (key == null || data == null || uuid == null) {
            return;
        }
        data.put(key, uuid.toString());
    }

    /**
     * Preserves the original public contract: only strings represent UUID values.
     */
    static UUID toUUID(Object value) {
        if (!(value instanceof String)) {
            return null;
        }
        try {
            return UUID.fromString((String) value);
        } catch (IllegalArgumentException invalid) {
            return null;
        }
    }

    private static UUID toUUIDFromData(Object value) {
        if (!(value instanceof SatelliteData)) {
            return null;
        }
        return toUUID(((SatelliteData) value).get(UUID_FIELD));
    }

    private static UUID toUUIDCandidate(Object value) {
        if (value instanceof UUID) {
            return (UUID) value;
        }
        if (value instanceof SatelliteData) {
            return toUUIDFromData(value);
        }
        return toUUID(value);
    }

    private static Stream<?> listValues(String key, SatelliteData data) {
        if (key == null || data == null) {
            return null;
        }
        Object raw = data.get(key);
        return raw instanceof List ? ((List<?>) raw).stream() : null;
    }
}
