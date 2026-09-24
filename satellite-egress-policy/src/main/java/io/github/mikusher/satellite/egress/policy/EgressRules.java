package io.github.mikusher.satellite.egress.policy;

import io.github.mikusher.satellite.egress.Key;

import java.util.Objects;
import java.util.Optional;

public final class EgressRules {
    private EgressRules() {
    }

    /**
     * Convenience rule for restrictive actions. Positive egress decisions must
     * use a purpose-scoped rule.
     */
    public static EgressRule forKey(final Key<?> key,
                                    final EgressSink sink,
                                    final EgressAction action,
                                    final String reasonCode) {
        Objects.requireNonNull(action, "action");
        if (action == EgressAction.ALLOW || action == EgressAction.TOKENIZE) {
            throw new IllegalArgumentException(
                    action + " requires a purpose-scoped rule");
        }
        return forKeyAndPurposeInternal(key, sink, null, action, reasonCode);
    }

    public static EgressRule forKeyAndPurpose(final Key<?> key,
                                              final EgressSink sink,
                                              final String purpose,
                                              final EgressAction action,
                                              final String reasonCode) {
        Objects.requireNonNull(purpose, "purpose");
        if (purpose.trim().isEmpty()) {
            throw new IllegalArgumentException("purpose must not be blank");
        }
        return forKeyAndPurposeInternal(key, sink, purpose, action, reasonCode);
    }

    private static EgressRule forKeyAndPurposeInternal(final Key<?> key,
                                                       final EgressSink sink,
                                                       final String purpose,
                                                       final EgressAction action,
                                                       final String reasonCode) {
        Objects.requireNonNull(key, "key");
        Objects.requireNonNull(sink, "sink");
        Objects.requireNonNull(action, "action");
        Objects.requireNonNull(reasonCode, "reasonCode");

        return (context, entry) -> {
            if (!sink.equals(context.getSink()) || !key.equals(entry.getKey())) {
                return Optional.empty();
            }
            if (purpose != null && !purpose.equals(context.getPurpose())) {
                return Optional.empty();
            }

            switch (action) {
                case ALLOW:
                    return Optional.of(PolicyDecision.allow(reasonCode, "Explicit key policy"));
                case REDACT:
                    return Optional.of(PolicyDecision.redact(reasonCode, "Explicit key policy"));
                case TOKENIZE:
                    return Optional.of(PolicyDecision.tokenize(reasonCode, "Explicit key policy"));
                case DENY:
                default:
                    return Optional.of(PolicyDecision.deny(reasonCode, "Explicit key policy"));
            }
        };
    }
}
