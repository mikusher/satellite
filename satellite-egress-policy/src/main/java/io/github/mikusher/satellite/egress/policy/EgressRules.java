package io.github.mikusher.satellite.egress.policy;

import io.github.mikusher.satellite.egress.Key;

import java.util.Objects;
import java.util.Optional;

public final class EgressRules {
    private EgressRules() {
    }

    public static EgressRule forKey(final Key<?> key,
                                    final EgressSink sink,
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
