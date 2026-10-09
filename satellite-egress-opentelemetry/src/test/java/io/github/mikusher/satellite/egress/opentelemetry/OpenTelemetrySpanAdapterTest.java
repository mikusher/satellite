package io.github.mikusher.satellite.egress.opentelemetry;

import io.github.mikusher.satellite.egress.DataCategory;
import io.github.mikusher.satellite.egress.DataClassification;
import io.github.mikusher.satellite.egress.Key;
import io.github.mikusher.satellite.egress.EgressEnvelope;
import io.github.mikusher.satellite.egress.policy.EgressPolicyEngine;
import io.github.mikusher.satellite.egress.policy.EgressProcessor;
import io.opentelemetry.api.common.AttributeKey;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class OpenTelemetrySpanAdapterTest {

    @Test
    public void traceAttributesArePolicyProcessedBeforeEmission() {
        Key<String> route = Key.string("http.route")
                .classifiedAs(DataClassification.PUBLIC);
        Key<String> email = Key.string("user.email")
                .classifiedAs(DataClassification.CONFIDENTIAL)
                .category(DataCategory.PERSONAL_DATA);
        Key<String> token = Key.string("auth.token")
                .classifiedAs(DataClassification.RESTRICTED)
                .category(DataCategory.CREDENTIAL);

        EgressEnvelope map = EgressEnvelope.builder()
                .put(route, "/accounts/{id}")
                .put(email, "user@example.com")
                .put(token, "never-export")
                .build();

        OpenTelemetryEgressResult result = new OpenTelemetrySpanAdapter(
                new EgressProcessor(EgressPolicyEngine.secureDefaults()))
                .prepareAttributes(map, "request-trace");

        assertEquals("/accounts/{id}",
                result.getAttributes().get(AttributeKey.stringKey("http.route")));
        assertEquals("[REDACTED]",
                result.getAttributes().get(AttributeKey.stringKey("user.email")));
        assertNull(result.getAttributes().get(AttributeKey.stringKey("auth.token")));
        assertTrue(result.getReport().hasViolations());
    }
}
