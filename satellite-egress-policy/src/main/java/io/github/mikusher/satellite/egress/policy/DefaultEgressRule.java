package io.github.mikusher.satellite.egress.policy;

import io.github.mikusher.satellite.egress.DataCategory;
import io.github.mikusher.satellite.egress.DataClassification;
import io.github.mikusher.satellite.egress.Key;
import io.github.mikusher.satellite.egress.SatelliteEntry;

import java.util.Optional;
import java.util.Set;

/**
 * Conservative baseline policy. Explicit application rules may be placed before this rule.
 */
public final class DefaultEgressRule implements EgressRule {

    @Override
    public Optional<PolicyDecision> evaluate(EgressContext context, SatelliteEntry<?> entry) {
        Key<?> key = entry.getKey();
        Set<DataCategory> categories = key.getCategories();

        if (categories.contains(DataCategory.CREDENTIAL) || categories.contains(DataCategory.SECRET)) {
            return Optional.of(PolicyDecision.deny(
                    "SECRET_CATEGORY_DENIED",
                    "Credentials and secrets are denied by the default policy"));
        }

        if (key.getClassification() == DataClassification.RESTRICTED) {
            return Optional.of(PolicyDecision.deny(
                    "RESTRICTED_DATA_DENIED",
                    "Restricted data requires an explicit application policy"));
        }

        if (key.getClassification() == DataClassification.CONFIDENTIAL || isPrivacySensitive(categories)) {
            if (isObservabilitySink(context.getSink())) {
                return Optional.of(PolicyDecision.redact(
                        "SENSITIVE_DATA_REDACTED",
                        "Sensitive data is redacted in observability sinks"));
            }
            return Optional.of(PolicyDecision.deny(
                    "SENSITIVE_DATA_DENIED",
                    "Sensitive data requires an explicit policy for this sink"));
        }

        if (key.getClassification() == DataClassification.INTERNAL) {
            if (context.getSink() == EgressSink.NETWORK) {
                return Optional.of(PolicyDecision.deny(
                        "INTERNAL_NETWORK_EGRESS_DENIED",
                        "Internal data requires an explicit network egress policy"));
            }
            if (context.getSink() == EgressSink.SERIALIZATION) {
                return Optional.of(PolicyDecision.redact(
                        "INTERNAL_SERIALIZATION_REDACTED",
                        "Internal data is redacted from generic serialization by default"));
            }
            return Optional.of(PolicyDecision.allow(
                    "INTERNAL_SINK_ALLOWED",
                    "Internal data is allowed for this local sink"));
        }

        return Optional.of(PolicyDecision.allow(
                "PUBLIC_DATA_ALLOWED",
                "Public data is allowed"));
    }

    private static boolean isPrivacySensitive(Set<DataCategory> categories) {
        return categories.contains(DataCategory.PERSONAL_DATA)
                || categories.contains(DataCategory.FINANCIAL)
                || categories.contains(DataCategory.HEALTH)
                || categories.contains(DataCategory.LOCATION)
                || categories.contains(DataCategory.DEVICE_IDENTIFIER)
                || categories.contains(DataCategory.NETWORK_IDENTIFIER);
    }

    private static boolean isObservabilitySink(EgressSink sink) {
        return sink == EgressSink.LOG
                || sink == EgressSink.TRACE
                || sink == EgressSink.METRIC
                || sink == EgressSink.AUDIT;
    }
}
