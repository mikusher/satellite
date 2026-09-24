package io.github.mikusher.satellite.egress.policy;

import io.github.mikusher.satellite.egress.DataCategory;
import io.github.mikusher.satellite.egress.DataClassification;
import io.github.mikusher.satellite.egress.DataOrigin;
import io.github.mikusher.satellite.egress.Key;
import io.github.mikusher.satellite.egress.SatelliteEntry;
import io.github.mikusher.satellite.egress.TrustLevel;

import java.util.Objects;
import java.util.Optional;

/**
 * Declarative policy rule. All configured matchers must match before the action is returned.
 */
public final class PolicyRule implements EgressRule {
    private final String keyName;
    private final DataClassification classification;
    private final DataCategory category;
    private final DataOrigin origin;
    private final TrustLevel trustLevel;
    private final EgressSink sink;
    private final String purpose;
    private final EgressAction action;
    private final String reasonCode;
    private final String message;

    private PolicyRule(Builder builder) {
        this.keyName = builder.keyName;
        this.classification = builder.classification;
        this.category = builder.category;
        this.origin = builder.origin;
        this.trustLevel = builder.trustLevel;
        this.sink = builder.sink;
        this.purpose = builder.purpose;
        this.action = Objects.requireNonNull(builder.action, "action");
        this.reasonCode = requireText(builder.reasonCode, "reasonCode");
        this.message = builder.message == null ? "Explicit policy rule" : builder.message;
    }

    public static Builder builder() {
        return new Builder();
    }

    @Override
    public Optional<PolicyDecision> evaluate(EgressContext context, SatelliteEntry<?> entry) {
        if (keyName != null && !keyName.equals(entry.getKey().getName())) {
            return Optional.empty();
        }
        if (classification != null && classification != entry.getKey().getClassification()) {
            return Optional.empty();
        }
        if (category != null && !entry.getKey().getCategories().contains(category)) {
            return Optional.empty();
        }
        if (origin != null && origin != entry.getMetadata().getOrigin()) {
            return Optional.empty();
        }
        if (trustLevel != null && trustLevel != entry.getMetadata().getTrustLevel()) {
            return Optional.empty();
        }
        if (sink != null && sink != context.getSink()) {
            return Optional.empty();
        }
        if (purpose != null && !purpose.equals(context.getPurpose())) {
            return Optional.empty();
        }

        return Optional.of(decision(action, reasonCode, message));
    }

    private static PolicyDecision decision(EgressAction action, String code, String message) {
        switch (action) {
            case ALLOW:
                return PolicyDecision.allow(code, message);
            case REDACT:
                return PolicyDecision.redact(code, message);
            case TOKENIZE:
                return PolicyDecision.tokenize(code, message);
            case DENY:
            default:
                return PolicyDecision.deny(code, message);
        }
    }

    private static String requireText(String value, String name) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException(name + " must not be blank");
        }
        return value;
    }

    public static final class Builder {
        private String keyName;
        private DataClassification classification;
        private DataCategory category;
        private DataOrigin origin;
        private TrustLevel trustLevel;
        private EgressSink sink;
        private String purpose;
        private EgressAction action;
        private String reasonCode;
        private String message;

        public Builder key(Key<?> key) {
            this.keyName = Objects.requireNonNull(key, "key").getName();
            return this;
        }

        public Builder keyName(String value) {
            this.keyName = requireText(value, "keyName");
            return this;
        }

        public Builder classification(DataClassification value) {
            this.classification = Objects.requireNonNull(value, "classification");
            return this;
        }

        public Builder category(DataCategory value) {
            this.category = Objects.requireNonNull(value, "category");
            return this;
        }

        public Builder origin(DataOrigin value) {
            this.origin = Objects.requireNonNull(value, "origin");
            return this;
        }

        public Builder trustLevel(TrustLevel value) {
            this.trustLevel = Objects.requireNonNull(value, "trustLevel");
            return this;
        }

        public Builder sink(EgressSink value) {
            this.sink = Objects.requireNonNull(value, "sink");
            return this;
        }

        public Builder purpose(String value) {
            this.purpose = requireText(value, "purpose");
            return this;
        }

        public Builder action(EgressAction value) {
            this.action = Objects.requireNonNull(value, "action");
            return this;
        }

        public Builder reasonCode(String value) {
            this.reasonCode = requireText(value, "reasonCode");
            return this;
        }

        public Builder message(String value) {
            this.message = requireText(value, "message");
            return this;
        }

        public PolicyRule build() {
            return new PolicyRule(this);
        }
    }
}
