package io.github.mikusher.satellite.egress.opentelemetry;

import io.github.mikusher.satellite.egress.EgressEnvelope;
import io.github.mikusher.satellite.egress.SatelliteMap;
import io.github.mikusher.satellite.egress.policy.EgressContext;
import io.github.mikusher.satellite.egress.policy.EgressProcessor;
import io.github.mikusher.satellite.egress.policy.EgressReport;
import io.github.mikusher.satellite.egress.policy.EgressSink;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.common.AttributesBuilder;
import io.opentelemetry.api.trace.Span;

import java.util.Map;
import java.util.Objects;

/**
 * Converts policy-approved Satellite data into OpenTelemetry span attributes.
 */
public final class OpenTelemetrySpanAdapter {
    private final EgressProcessor processor;

    public OpenTelemetrySpanAdapter(EgressProcessor processor) {
        this.processor = Objects.requireNonNull(processor, "processor");
    }

    public OpenTelemetryEgressResult prepareAttributes(EgressEnvelope envelope, String purpose) {
        EgressReport report = processor.process(
                Objects.requireNonNull(envelope, "envelope"),
                EgressContext.of(EgressSink.TRACE, purpose));

        AttributesBuilder builder = Attributes.builder();
        for (Map.Entry<String, Object> entry : report.getOutput().entrySet()) {
            put(builder, entry.getKey(), entry.getValue());
        }

        return new OpenTelemetryEgressResult(builder.build(), report);
    }

    public EgressReport applyToSpan(Span span, EgressEnvelope envelope, String purpose) {
        Objects.requireNonNull(span, "span");
        OpenTelemetryEgressResult result = prepareAttributes(envelope, purpose);
        span.setAllAttributes(result.getAttributes());
        return result.getReport();
    }

    @Deprecated
    public OpenTelemetryEgressResult prepareAttributes(SatelliteMap map, String purpose) {
        return prepareAttributes(map.asEgressEnvelope(), purpose);
    }

    @Deprecated
    public EgressReport applyToSpan(Span span, SatelliteMap map, String purpose) {
        return applyToSpan(span, map.asEgressEnvelope(), purpose);
    }

    private static void put(AttributesBuilder builder, String key, Object value) {
        if (value == null) {
            return;
        }
        if (value instanceof Boolean) {
            builder.put(key, (Boolean) value);
        } else if (value instanceof Byte || value instanceof Short
                || value instanceof Integer || value instanceof Long) {
            builder.put(key, ((Number) value).longValue());
        } else if (value instanceof Float || value instanceof Double) {
            builder.put(key, ((Number) value).doubleValue());
        } else {
            builder.put(key, String.valueOf(value));
        }
    }
}
