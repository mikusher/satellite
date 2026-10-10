package io.github.mikusher.satellite.egress.policy;

import io.github.mikusher.satellite.egress.SatelliteEntry;

/** Redaction must produce a safe, non-empty replacement string. */
public interface Redactor {
    String redact(SatelliteEntry<?> entry);
}
