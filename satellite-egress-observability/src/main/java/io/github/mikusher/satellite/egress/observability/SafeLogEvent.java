package io.github.mikusher.satellite.egress.observability;

import io.github.mikusher.satellite.egress.policy.EgressReport;

import java.util.Objects;

public final class SafeLogEvent {
    private final String line;
    private final EgressReport report;

    SafeLogEvent(String line, EgressReport report) {
        this.line = Objects.requireNonNull(line, "line");
        this.report = Objects.requireNonNull(report, "report");
    }

    public String getLine() {
        return line;
    }

    public EgressReport getReport() {
        return report;
    }
}
