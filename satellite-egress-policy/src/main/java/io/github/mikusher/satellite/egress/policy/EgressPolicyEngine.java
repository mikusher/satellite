package io.github.mikusher.satellite.egress.policy;

import io.github.mikusher.satellite.egress.DataCategory;
import io.github.mikusher.satellite.egress.DataClassification;
import io.github.mikusher.satellite.egress.SatelliteEntry;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

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
                return applyNonBypassableGuardrails(context, entry, decision.get());
            }
        }

        return PolicyDecision.deny(
                "NO_MATCHING_POLICY",
                "No policy explicitly allowed this egress");
    }

    private static PolicyDecision applyNonBypassableGuardrails(EgressContext context,
                                                                SatelliteEntry<?> entry,
                                                                PolicyDecision decision) {
        if (decision.getAction() != EgressAction.ALLOW || !isObservabilitySink(context.getSink())) {
            return decision;
        }

        DataClassification classification = entry.getKey().getClassification();
        Set<DataCategory> categories = entry.getKey().getCategories();

        if (classification == DataClassification.CONFIDENTIAL
                || classification == DataClassification.RESTRICTED
                || isPrivacyOrSecretCategory(categories)) {
            return PolicyDecision.deny(
                    "OBSERVABILITY_RAW_SENSITIVE_DENIED",
                    "Sensitive values cannot be emitted raw to observability sinks");
        }

        return decision;
    }

    private static boolean isObservabilitySink(EgressSink sink) {
        return sink == EgressSink.LOG
                || sink == EgressSink.TRACE
                || sink == EgressSink.METRIC
                || sink == EgressSink.AUDIT;
    }

    private static boolean isPrivacyOrSecretCategory(Set<DataCategory> categories) {
        return categories.contains(DataCategory.CREDENTIAL)
                || categories.contains(DataCategory.SECRET)
                || categories.contains(DataCategory.PERSONAL_DATA)
                || categories.contains(DataCategory.FINANCIAL)
                || categories.contains(DataCategory.HEALTH)
                || categories.contains(DataCategory.LOCATION)
                || categories.contains(DataCategory.DEVICE_IDENTIFIER)
                || categories.contains(DataCategory.NETWORK_IDENTIFIER);
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
