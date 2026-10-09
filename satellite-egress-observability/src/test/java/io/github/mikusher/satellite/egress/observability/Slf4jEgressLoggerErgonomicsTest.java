package io.github.mikusher.satellite.egress.observability;

import io.github.mikusher.satellite.egress.DataCategory;
import io.github.mikusher.satellite.egress.DataClassification;
import io.github.mikusher.satellite.egress.EgressEnvelope;
import io.github.mikusher.satellite.egress.Key;
import org.junit.Test;
import org.slf4j.LoggerFactory;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class Slf4jEgressLoggerErgonomicsTest {

    @Test
    public void secureFactoryUsesSecureDefaults() {
        Key<String> email = Key.string("email")
                .classifiedAs(DataClassification.CONFIDENTIAL)
                .category(DataCategory.PERSONAL_DATA);

        Key<String> token = Key.string("token")
                .classifiedAs(DataClassification.RESTRICTED)
                .category(DataCategory.CREDENTIAL);

        SafeLogEvent event = Slf4jEgressLogger
                .secure(LoggerFactory.getLogger("ergonomics-test"))
                .prepare(
                        "request",
                        EgressEnvelope.builder()
                                .put(email, "user@example.com")
                                .put(token, "do-not-log")
                                .build(),
                        "request-log");

        assertTrue(event.getLine().contains("[REDACTED]"));
        assertFalse(event.getLine().contains("user@example.com"));
        assertFalse(event.getLine().contains("do-not-log"));
    }
}
