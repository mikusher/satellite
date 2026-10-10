package io.github.mikusher.satellite.egress.policy;

import io.github.mikusher.satellite.egress.DataCategory;
import io.github.mikusher.satellite.egress.DataClassification;
import io.github.mikusher.satellite.egress.SatelliteEntry;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.temporal.TemporalAccessor;
import java.util.UUID;
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
        return builder().withSecureDefaults().build();
    }

    public PolicyDecision decide(EgressContext context, SatelliteEntry<?> entry) {
        Objects.requireNonNull(context, "context");
        Objects.requireNonNull(entry, "entry");

        for (EgressRule rule : rules) {
            final Optional<PolicyDecision> decision;
            try {
                decision = rule.evaluate(context, entry);
            } catch (RuntimeException failure) {
                return PolicyDecision.deny(
                        "POLICY_EVALUATION_FAILED",
                        "Egress denied because policy evaluation failed");
            }
            if (decision == null) {
                return PolicyDecision.deny(
                        "INVALID_POLICY_RESULT",
                        "Egress denied because a policy returned no result");
            }
            if (decision.isPresent()) {
                PolicyDecision resolved = decision.get();
                if (resolved == null) {
                    return PolicyDecision.deny(
                            "INVALID_POLICY_RESULT",
                            "Egress denied because a policy returned no decision");
                }
                return applyNonBypassableGuardrails(context, entry, resolved);
            }
        }

        return PolicyDecision.deny(
                "NO_MATCHING_POLICY",
                "No policy explicitly allowed this egress");
    }

    private static PolicyDecision applyNonBypassableGuardrails(EgressContext context,
                                                                SatelliteEntry<?> entry,
                                                                PolicyDecision decision) {
        if (decision.getAction() != EgressAction.ALLOW) {
            return decision;
        }

        // An outer PUBLIC label cannot authorize unclassified nested fields.
        // Only a known scalar can leave an egress boundary raw.
        if (!isSafeScalar(entry.getValue())) {
            return PolicyDecision.deny(
                    "UNCLASSIFIED_COMPLEX_VALUE_DENIED",
                    "Composite values require individual classification before raw egress");
        }

        if (!isObservabilitySink(context.getSink())) {
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

    private static boolean isSafeScalar(Object value) {
        if (value == null || value instanceof String
                || value instanceof Boolean || value instanceof Character
                || value instanceof Byte || value instanceof Short
                || value instanceof Integer || value instanceof Long
                || value instanceof Float || value instanceof Double
                || value instanceof BigInteger || value instanceof BigDecimal
                || value instanceof UUID || value instanceof Enum) {
            return true;
        }

        // java.time value types are immutable. Do not whitelist arbitrary
        // Object.toString(), mutable Maps, Collections, arrays or POJOs.
        return value instanceof TemporalAccessor
                && value.getClass().getName().startsWith("java.time.");
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
        private boolean secureDefaults;

        public Builder add(EgressRule rule) {
            rules.add(Objects.requireNonNull(rule, "rule"));
            return this;
        }

        /**
         * Appends Satellite's conservative default rule after all explicit rules.
         */
        public Builder withSecureDefaults() {
            secureDefaults = true;
            return this;
        }

        public EgressPolicyEngine build() {
            if (!secureDefaults) {
                return new EgressPolicyEngine(rules);
            }

            List<EgressRule> configured = new ArrayList<EgressRule>(rules);
            configured.add(new DefaultEgressRule());
            return new EgressPolicyEngine(configured);
        }
    }
}
