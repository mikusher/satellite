package io.github.mikusher.satellite.egress.opentelemetry;

import io.github.mikusher.satellite.egress.policy.EgressReport;
import io.opentelemetry.api.common.Attributes;

import java.util.Objects;

public final class OpenTelemetryEgressResult {
    private final Attributes attributes;
    private final EgressReport report;

    OpenTelemetryEgressResult(Attributes attributes, EgressReport report) {
        this.attributes = Objects.requireNonNull(attributes, "attributes");
        this.report = Objects.requireNonNull(report, "report");
    }

    public Attributes getAttributes() {
        return attributes;
    }

    public EgressReport getReport() {
        return report;
    }
}
