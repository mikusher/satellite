package io.github.mikusher.satellite.egress.observability;

import io.github.mikusher.satellite.egress.DataClassification;
import io.github.mikusher.satellite.egress.EgressEnvelope;
import io.github.mikusher.satellite.egress.Key;
import org.junit.Test;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.Map;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class CompositeDataLeakTest {

    @Test
    public void publicOuterMapCannotExportNestedSecretToLogs() {
        Key<Map> payload = Key.of("payload", Map.class)
                .classifiedAs(DataClassification.PUBLIC);

        EgressEnvelope envelope = EgressEnvelope.builder()
                .put(payload, Collections.singletonMap("password", "unclassified-secret"))
                .build();

        SafeLogEvent event = Slf4jEgressLogger
                .secure(LoggerFactory.getLogger("egress-leak-regression"))
                .prepare("request completed", envelope, "diagnostics");

        assertFalse(event.getLine().contains("unclassified-secret"));
        assertFalse(event.getLine().contains("password"));
        assertTrue(event.getReport().hasViolations());
    }
}
