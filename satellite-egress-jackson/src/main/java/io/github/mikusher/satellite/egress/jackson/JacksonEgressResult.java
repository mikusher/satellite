package io.github.mikusher.satellite.egress.jackson;

import com.fasterxml.jackson.databind.JsonNode;
import io.github.mikusher.satellite.egress.policy.EgressReport;

import java.util.Objects;

public final class JacksonEgressResult {
    private final JsonNode json;
    private final EgressReport report;

    JacksonEgressResult(JsonNode json, EgressReport report) {
        this.json = Objects.requireNonNull(json, "json");
        this.report = Objects.requireNonNull(report, "report");
    }

    public JsonNode getJson() {
        return json;
    }

    public EgressReport getReport() {
        return report;
    }
}
