package io.github.mikusher.satellite.egress.jackson;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.mikusher.satellite.egress.DataCategory;
import io.github.mikusher.satellite.egress.DataClassification;
import io.github.mikusher.satellite.egress.Key;
import io.github.mikusher.satellite.egress.SatelliteMap;
import io.github.mikusher.satellite.egress.policy.EgressPolicyEngine;
import io.github.mikusher.satellite.egress.policy.EgressProcessor;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class JacksonEgressSerializerTest {

    @Test
    public void serializesOnlyPolicyApprovedRepresentation() throws Exception {
        Key<String> publicValue = Key.string("status")
                .classifiedAs(DataClassification.PUBLIC);
        Key<String> internalValue = Key.string("internal.note");
        Key<String> secret = Key.string("auth.token")
                .classifiedAs(DataClassification.RESTRICTED)
                .category(DataCategory.CREDENTIAL);

        SatelliteMap map = SatelliteMap.builder()
                .put(publicValue, "ok")
                .put(internalValue, "internal-only")
                .put(secret, "never-serialize")
                .build();

        JacksonEgressSerializer serializer = new JacksonEgressSerializer(
                new ObjectMapper(),
                new EgressProcessor(EgressPolicyEngine.secureDefaults()));

        JacksonEgressResult result = serializer.toJsonNode(map, "api-response");
        String json = serializer.toJson(map, "api-response");

        assertEquals("ok", result.getJson().get("status").asText());
        assertEquals("[REDACTED]", result.getJson().get("internal.note").asText());
        assertFalse(result.getJson().has("auth.token"));
        assertFalse(json.contains("never-serialize"));
        assertTrue(result.getReport().hasViolations());
    }
}
