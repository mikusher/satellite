package io.github.mikusher.satellite.egress;

import java.util.Objects;

public final class ValidationError {
    private final String code;
    private final String keyName;
    private final String message;

    public ValidationError(String code, String keyName, String message) {
        this.code = Objects.requireNonNull(code, "code");
        this.keyName = keyName;
        this.message = Objects.requireNonNull(message, "message");
    }

    public String getCode() {
        return code;
    }

    public String getKeyName() {
        return keyName;
    }

    public String getMessage() {
        return message;
    }

    @Override
    public String toString() {
        return code + "(" + keyName + "): " + message;
    }
}
