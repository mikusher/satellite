package io.github.mikusher.satellite.egress.policy;

import java.util.Objects;

public final class PolicyDecision {
    private final EgressAction action;
    private final String code;
    private final String message;

    private PolicyDecision(EgressAction action, String code, String message) {
        this.action = Objects.requireNonNull(action, "action");
        this.code = Objects.requireNonNull(code, "code");
        this.message = Objects.requireNonNull(message, "message");
    }

    public static PolicyDecision allow(String code, String message) {
        return new PolicyDecision(EgressAction.ALLOW, code, message);
    }

    public static PolicyDecision redact(String code, String message) {
        return new PolicyDecision(EgressAction.REDACT, code, message);
    }

    public static PolicyDecision tokenize(String code, String message) {
        return new PolicyDecision(EgressAction.TOKENIZE, code, message);
    }

    public static PolicyDecision deny(String code, String message) {
        return new PolicyDecision(EgressAction.DENY, code, message);
    }

    public EgressAction getAction() {
        return action;
    }

    public String getCode() {
        return code;
    }

    public String getMessage() {
        return message;
    }
}
