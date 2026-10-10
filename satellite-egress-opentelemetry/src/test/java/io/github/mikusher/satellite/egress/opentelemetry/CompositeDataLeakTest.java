package io.github.mikusher.satellite.egress.opentelemetry;

import io.github.mikusher.satellite.egress.DataClassification;
import io.github.mikusher.satellite.egress.EgressEnvelope;
import io.github.mikusher.satellite.egress.Key;
import org.junit.Test;

import java.util.Collections;
import java.util.Map;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class CompositeDataLeakTest {

    @Test
    public void publicOuterMapCannotExportNestedSecretToTraces() {
        Key<Map> payload = Key.of("payload", Map.class)
                .classifiedAs(DataClassification.PUBLIC);

        EgressEnvelope envelope = EgressEnvelope.builder()
                .put(payload, Collections.singletonMap("apiKey", "unclassified-secret"))
                .build();

        OpenTelemetryEgressResult result = OpenTelemetrySpanAdapter.secure()
                .prepareAttributes(envelope, "request-trace");

        assertFalse(result.getAttributes().toString().contains("unclassified-secret"));
        assertFalse(result.getAttributes().toString().contains("apiKey"));
        assertTrue(result.getReport().hasViolations());
    }
}
