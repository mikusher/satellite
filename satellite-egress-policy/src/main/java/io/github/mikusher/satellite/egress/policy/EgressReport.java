package io.github.mikusher.satellite.egress.policy;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class EgressReport {
    private final Map<String, Object> output;
    private final List<EgressDecisionRecord> decisions;
    private final List<PrivacyViolation> violations;

    EgressReport(Map<String, Object> output,
                 List<EgressDecisionRecord> decisions,
                 List<PrivacyViolation> violations) {
        this.output = Collections.unmodifiableMap(new LinkedHashMap<String, Object>(output));
        this.decisions = Collections.unmodifiableList(new ArrayList<EgressDecisionRecord>(decisions));
        this.violations = Collections.unmodifiableList(new ArrayList<PrivacyViolation>(violations));
    }

    public Map<String, Object> getOutput() {
        return output;
    }

    public List<EgressDecisionRecord> getDecisions() {
        return decisions;
    }

    public List<PrivacyViolation> getViolations() {
        return violations;
    }

    public boolean hasViolations() {
        return !violations.isEmpty();
    }
}
