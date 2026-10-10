package io.github.mikusher.satellite.egress.jackson;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.mikusher.satellite.egress.DataClassification;
import io.github.mikusher.satellite.egress.EgressEnvelope;
import io.github.mikusher.satellite.egress.Key;
import org.junit.Test;

import java.util.Collections;
import java.util.Map;

import static org.junit.Assert.assertEquals;

public class CompositeDataLeakTest {

    @Test
    public void publicOuterMapCannotExportNestedSecretToJson() throws Exception {
        Key<Map> payload = Key.of("payload", Map.class)
                .classifiedAs(DataClassification.PUBLIC);

        EgressEnvelope envelope = EgressEnvelope.builder()
                .put(payload, Collections.singletonMap("token", "unclassified-secret"))
                .build();

        String json = JacksonEgressSerializer.secure(new ObjectMapper())
                .toJson(envelope, "api-response");

        assertEquals("{}", json);
    }
}
