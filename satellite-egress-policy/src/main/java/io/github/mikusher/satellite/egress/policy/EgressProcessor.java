package io.github.mikusher.satellite.egress.policy;

import io.github.mikusher.satellite.egress.EgressEnvelope;
import io.github.mikusher.satellite.egress.SatelliteEntry;

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

    public static EgressProcessor secureDefaults() {
        return new EgressProcessor(EgressPolicyEngine.secureDefaults());
    }

    public EgressReport process(EgressEnvelope envelope,
                                EgressSink sink,
                                String purpose) {
        return process(envelope, EgressContext.of(sink, purpose));
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
                    String replacement = redactor.redact(entry);
                    if (isInvalidReplacement(replacement, entry.getValue())) {
                        action = EgressAction.DENY;
                        reasonCode = "INVALID_REDACTION_OUTPUT";
                        violations.add(violation(entry, context, reasonCode));
                    } else {
                        output.put(entry.getKey().getName(), replacement);
                    }
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
                        String replacement = tokenizer.tokenize(entry);
                        if (isInvalidReplacement(replacement, entry.getValue())) {
                            action = EgressAction.DENY;
                            reasonCode = "INVALID_TOKEN_OUTPUT";
                            violations.add(violation(entry, context, reasonCode));
                        } else {
                            output.put(entry.getKey().getName(), replacement);
                        }
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

    private static boolean isInvalidReplacement(String replacement, Object original) {
        if (replacement == null || replacement.trim().isEmpty()) {
            return true;
        }
        for (int i = 0; i < replacement.length(); i++) {
            if (Character.isISOControl(replacement.charAt(i))) {
                return true;
            }
        }
        if (original instanceof CharSequence
                || original instanceof Number
                || original instanceof Boolean
                || original instanceof Character
                || original instanceof Enum) {
            return replacement.equals(String.valueOf(original));
        }
        return false;
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
