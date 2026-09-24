package io.github.mikusher.satellite.egress.policy;

import io.github.mikusher.satellite.egress.SatelliteEntry;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

public final class EgressPolicyEngine {
    private final List<EgressRule> rules;

    private EgressPolicyEngine(List<EgressRule> rules) {
        this.rules = Collections.unmodifiableList(new ArrayList<EgressRule>(rules));
    }

    public static Builder builder() {
        return new Builder();
    }

    public static EgressPolicyEngine secureDefaults() {
        return builder().add(new DefaultEgressRule()).build();
    }

    public PolicyDecision decide(EgressContext context, SatelliteEntry<?> entry) {
        Objects.requireNonNull(context, "context");
        Objects.requireNonNull(entry, "entry");

        for (EgressRule rule : rules) {
            Optional<PolicyDecision> decision = rule.evaluate(context, entry);
            if (decision.isPresent()) {
                return decision.get();
            }
        }

        return PolicyDecision.deny(
                "NO_MATCHING_POLICY",
                "No policy explicitly allowed this egress");
    }

    public static final class Builder {
        private final List<EgressRule> rules = new ArrayList<EgressRule>();

        public Builder add(EgressRule rule) {
            rules.add(Objects.requireNonNull(rule, "rule"));
            return this;
        }

        public EgressPolicyEngine build() {
            return new EgressPolicyEngine(rules);
        }
    }
}
