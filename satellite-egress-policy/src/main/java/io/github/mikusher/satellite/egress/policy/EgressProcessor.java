package io.github.mikusher.satellite.egress.policy;

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

    public EgressReport process(SatelliteMap map, EgressContext context) {
        Objects.requireNonNull(map, "map");
        Objects.requireNonNull(context, "context");

        Map<String, Object> output = new LinkedHashMap<String, Object>();
        List<EgressDecisionRecord> decisions = new ArrayList<EgressDecisionRecord>();
        List<PrivacyViolation> violations = new ArrayList<PrivacyViolation>();

        for (SatelliteEntry<?> entry : map.entries()) {
            PolicyDecision decision = policyEngine.decide(context, entry);
            EgressAction action = decision.getAction();

            if (action == EgressAction.ALLOW) {
                output.put(entry.getKey().getName(), entry.getValue());
            } else if (action == EgressAction.REDACT) {
                output.put(entry.getKey().getName(), redactor.redact(entry));
            } else if (action == EgressAction.TOKENIZE) {
                if (tokenizer == null) {
                    action = EgressAction.DENY;
                    violations.add(violation(entry, context, "TOKENIZER_NOT_CONFIGURED"));
                } else {
                    output.put(entry.getKey().getName(), tokenizer.tokenize(entry));
                }
            } else {
                violations.add(violation(entry, context, decision.getCode()));
            }

            decisions.add(new EgressDecisionRecord(
                    entry.getKey().getName(),
                    action,
                    action == EgressAction.DENY && decision.getAction() == EgressAction.TOKENIZE
                            ? "TOKENIZER_NOT_CONFIGURED"
                            : decision.getCode()));
        }

        return new EgressReport(output, decisions, violations);
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
