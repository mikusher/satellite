package io.github.mikusher.satellite.egress.jackson;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.mikusher.satellite.egress.DataCategory;
import io.github.mikusher.satellite.egress.DataClassification;
import io.github.mikusher.satellite.egress.Key;
import io.github.mikusher.satellite.egress.SatelliteSchema;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

public class JsonSchemaExporterTest {

    @Test
    public void exportsDraft202012AndSecurityMetadata() {
        Key<String> id = Key.string("id")
                .classifiedAs(DataClassification.PUBLIC)
                .required();
        Key<String> email = Key.string("email")
                .classifiedAs(DataClassification.CONFIDENTIAL)
                .category(DataCategory.PERSONAL_DATA);

        SatelliteSchema schema = SatelliteSchema.builder("User")
                .required(id)
                .optional(email)
                .build();

        JsonNode json = new JsonSchemaExporter(new ObjectMapper()).export(schema);

        assertEquals("https://json-schema.org/draft/2020-12/schema", json.get("$schema").asText());
        assertEquals("string", json.at("/properties/id/type").asText());
        assertEquals("CONFIDENTIAL",
                json.at("/properties/email/x-satellite-classification").asText());
        assertEquals("id", json.get("required").get(0).asText());
        assertFalse(json.get("additionalProperties").asBoolean());
    }
}
