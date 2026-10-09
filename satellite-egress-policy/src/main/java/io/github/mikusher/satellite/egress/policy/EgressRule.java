package io.github.mikusher.satellite.egress.policy;

import io.github.mikusher.satellite.egress.SatelliteEntry;

import java.util.Optional;

public interface EgressRule {
    Optional<PolicyDecision> evaluate(EgressContext context, SatelliteEntry<?> entry);
}
