package io.github.mikusher.satellite.egress;

import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class SatelliteSchemaTest {

    @Test
    public void validatesRequiredAndUnknownKeys() {
        Key<String> id = Key.string("id").required();
        Key<String> extra = Key.string("extra");

        SatelliteSchema schema = SatelliteSchema.builder("User")
                .required(id)
                .build();

        EgressEnvelope invalid = EgressEnvelope.builder().put(extra, "x").build();
        ValidationResult result = schema.validate(invalid);

        assertFalse(result.isValid());
        assertTrue(result.getErrors().stream()
                .anyMatch(error -> "REQUIRED_VALUE_MISSING".equals(error.getCode())));
        assertTrue(result.getErrors().stream()
                .anyMatch(error -> "UNKNOWN_KEY".equals(error.getCode())));
    }

    @Test
    public void acceptsValidMap() {
        Key<String> id = Key.string("id").required();
        SatelliteSchema schema = SatelliteSchema.builder("User").required(id).build();

        assertTrue(schema.validate(EgressEnvelope.builder().put(id, "42").build()).isValid());
    }
}
