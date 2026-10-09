package io.github.mikusher.satellite.egress.jackson;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.mikusher.satellite.egress.DataCategory;
import io.github.mikusher.satellite.egress.DataClassification;
import io.github.mikusher.satellite.egress.EgressEnvelope;
import io.github.mikusher.satellite.egress.Key;
import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class JacksonEgressSerializerErgonomicsTest {

    @Test
    public void secureFactoryUsesSecureDefaults() throws Exception {
        Key<String> status = Key.string("status")
                .classifiedAs(DataClassification.PUBLIC);

        Key<String> token = Key.string("token")
                .classifiedAs(DataClassification.RESTRICTED)
                .category(DataCategory.CREDENTIAL);

        String json = JacksonEgressSerializer
                .secure(new ObjectMapper())
                .toJson(
                        EgressEnvelope.builder()
                                .put(status, "ok")
                                .put(token, "never-serialize")
                                .build(),
                        "api-response");

        assertTrue(json.contains("\"status\":\"ok\""));
        assertFalse(json.contains("never-serialize"));
    }
}
