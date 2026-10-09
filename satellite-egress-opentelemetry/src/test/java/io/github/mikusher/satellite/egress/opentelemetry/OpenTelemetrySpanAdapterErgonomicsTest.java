package io.github.mikusher.satellite.egress.opentelemetry;

import io.github.mikusher.satellite.egress.DataCategory;
import io.github.mikusher.satellite.egress.DataClassification;
import io.github.mikusher.satellite.egress.EgressEnvelope;
import io.github.mikusher.satellite.egress.Key;
import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class OpenTelemetrySpanAdapterErgonomicsTest {

    @Test
    public void secureFactoryUsesSecureDefaults() {
        Key<String> userId = Key.string("user.id")
                .classifiedAs(DataClassification.PUBLIC);

        Key<String> token = Key.string("token")
                .classifiedAs(DataClassification.RESTRICTED)
                .category(DataCategory.CREDENTIAL);

        OpenTelemetryEgressResult result = OpenTelemetrySpanAdapter
                .secure()
                .prepareAttributes(
                        EgressEnvelope.builder()
                                .put(userId, "user-123")
                                .put(token, "do-not-trace")
                                .build(),
                        "request-trace");

        String rendered = result.getAttributes().toString();

        assertTrue(rendered.contains("user.id"));
        assertTrue(rendered.contains("user-123"));
        assertFalse(rendered.contains("do-not-trace"));
    }
}
