package io.github.mikusher.satellite.egress.policy;

import io.github.mikusher.satellite.egress.SatelliteEntry;

public interface Tokenizer {
    String tokenize(SatelliteEntry<?> entry);
}
