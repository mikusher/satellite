package io.github.mikusher.satellite.egress.observability;

import io.github.mikusher.satellite.egress.DataCategory;
import io.github.mikusher.satellite.egress.DataClassification;
import io.github.mikusher.satellite.egress.Key;
import io.github.mikusher.satellite.egress.EgressEnvelope;
import io.github.mikusher.satellite.egress.policy.EgressPolicyEngine;
import io.github.mikusher.satellite.egress.policy.EgressProcessor;
import org.junit.Test;
import org.slf4j.LoggerFactory;

import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class Slf4jEgressLoggerTest {

    @Test
    public void preparedLineCannotLeakDeniedSecretAndEscapesNewlines() {
        Key<String> publicValue = Key.string("public")
                .classifiedAs(DataClassification.PUBLIC);
        Key<String> email = Key.string("email")
                .classifiedAs(DataClassification.CONFIDENTIAL)
                .category(DataCategory.PERSONAL_DATA);
        Key<String> token = Key.string("token")
                .classifiedAs(DataClassification.RESTRICTED)
                .category(DataCategory.CREDENTIAL);

        EgressEnvelope map = EgressEnvelope.builder()
                .put(publicValue, "line1\nline2")
                .put(email, "user@example.com")
                .put(token, "do-not-log")
                .build();

        AtomicInteger violations = new AtomicInteger();
        Slf4jEgressLogger logger = new Slf4jEgressLogger(
                LoggerFactory.getLogger("test"),
                new EgressProcessor(EgressPolicyEngine.secureDefaults()),
                violation -> violations.incrementAndGet());

        SafeLogEvent event = logger.prepare("request\ncomplete", map, "request-log");

        assertTrue(event.getLine().contains("request\\ncomplete"));
        assertTrue(event.getLine().contains("line1\\nline2"));
        assertTrue(event.getLine().contains("[REDACTED]"));
        assertFalse(event.getLine().contains("user@example.com"));
        assertFalse(event.getLine().contains("do-not-log"));
        assertTrue(violations.get() > 0);
    }
}
