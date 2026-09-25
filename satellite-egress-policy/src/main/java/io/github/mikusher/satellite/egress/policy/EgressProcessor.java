package io.github.mikusher.satellite.egress.policy;

import io.github.mikusher.satellite.egress.EgressEnvelope;
import io.github.mikusher.satellite.egress.SatelliteEntry;
import io.github.mikusher.satellite.egress.SatelliteMap;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public final class EgressProcessor {
    private final EgressPolicyEngine policyEngine;
    private final Redactor redactor;
    private final Tokenizer tokenizer;

    public EgressProcessor(EgressPolicyEngine policyEngine) {
        this(policyEngine, new ConstantRedactor(), null);
    }

    public EgressProcessor(EgressPolicyEngine policyEngine, Redactor redactor, Tokenizer tokenizer) {
        this.policyEngine = Objects.requireNonNull(policyEngine, "policyEngine");
        this.redactor = Objects.requireNonNull(redactor, "redactor");
        this.tokenizer = tokenizer;
    }

    public EgressReport process(EgressEnvelope envelope, EgressContext context) {
        Objects.requireNonNull(envelope, "envelope");
        Objects.requireNonNull(context, "context");

        Map<String, Object> output = new LinkedHashMap<String, Object>();
        List<EgressDecisionRecord> decisions = new ArrayList<EgressDecisionRecord>();
        List<PrivacyViolation> violations = new ArrayList<PrivacyViolation>();

        for (SatelliteEntry<?> entry : envelope.entries()) {
            PolicyDecision decision = policyEngine.decide(context, entry);
            EgressAction action = decision.getAction();
            String reasonCode = decision.getCode();

            if (action == EgressAction.ALLOW) {
                output.put(entry.getKey().getName(), entry.getValue());
            } else if (action == EgressAction.REDACT) {
                try {
                    output.put(entry.getKey().getName(), redactor.redact(entry));
                } catch (RuntimeException failure) {
                    action = EgressAction.DENY;
                    reasonCode = "REDACTION_FAILED";
                    violations.add(violation(entry, context, reasonCode));
                }
            } else if (action == EgressAction.TOKENIZE) {
                if (tokenizer == null) {
                    action = EgressAction.DENY;
                    reasonCode = "TOKENIZER_NOT_CONFIGURED";
                    violations.add(violation(entry, context, reasonCode));
                } else {
                    try {
                        output.put(entry.getKey().getName(), tokenizer.tokenize(entry));
                    } catch (RuntimeException failure) {
                        action = EgressAction.DENY;
                        reasonCode = "TOKENIZATION_FAILED";
                        violations.add(violation(entry, context, reasonCode));
                    }
                }
            } else {
                violations.add(violation(entry, context, reasonCode));
            }

            decisions.add(new EgressDecisionRecord(
                    entry.getKey().getName(),
                    action,
                    reasonCode));
        }

        return new EgressReport(output, decisions, violations);
    }

    /**
     * @deprecated Use {@link #process(EgressEnvelope, EgressContext)}.
     */
    @Deprecated
    public EgressReport process(SatelliteMap map, EgressContext context) {
        return process(
                Objects.requireNonNull(map, "map").asEgressEnvelope(),
                context);
    }

    private static PrivacyViolation violation(SatelliteEntry<?> entry,
                                              EgressContext context,
                                              String reasonCode) {
        return new PrivacyViolation(
                entry.getKey().getName(),
                context.getSink(),
                context.getPurpose(),
                reasonCode,
                entry.getKey().getClassification(),
                entry.getKey().getCategories(),
                entry.getMetadata().getOrigin(),
                entry.getMetadata().getTrustLevel());
    }
}
