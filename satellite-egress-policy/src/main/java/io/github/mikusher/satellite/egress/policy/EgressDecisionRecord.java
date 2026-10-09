package io.github.mikusher.satellite.egress.policy;

public final class EgressDecisionRecord {
    private final String keyName;
    private final EgressAction action;
    private final String reasonCode;

    EgressDecisionRecord(String keyName, EgressAction action, String reasonCode) {
        this.keyName = keyName;
        this.action = action;
        this.reasonCode = reasonCode;
    }

    public String getKeyName() {
        return keyName;
    }

    public EgressAction getAction() {
        return action;
    }

    public String getReasonCode() {
        return reasonCode;
    }
}
