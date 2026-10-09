package io.github.mikusher.satellite.egress.jackson;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.github.mikusher.satellite.egress.DataCategory;
import io.github.mikusher.satellite.egress.DataClassification;
import io.github.mikusher.satellite.egress.Key;
import io.github.mikusher.satellite.egress.SatelliteSchema;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class JsonSchemaImporterTest {

    @Test
    public void roundTripsSatelliteMetadataAndJavaTypes() {
        ObjectMapper mapper = new ObjectMapper();
        Key<Integer> id = Key.integer("id")
                .classifiedAs(DataClassification.PUBLIC)
                .required();
        Key<String> email = Key.string("email")
                .classifiedAs(DataClassification.CONFIDENTIAL)
                .category(DataCategory.PERSONAL_DATA);

        SatelliteSchema source = SatelliteSchema.builder("User")
                .required(id)
                .optional(email)
                .build();

        JsonNode exported = new JsonSchemaExporter(mapper).export(source);
        SatelliteSchema imported = new JsonSchemaImporter().importSchema(exported);

        assertEquals("User", imported.getName());
        assertFalse(imported.isAllowUnknownKeys());
        assertEquals(2, imported.getKnownKeys().size());
        assertEquals(1, imported.getRequiredKeys().size());

        Key<?> importedId = imported.getKnownKeys().stream()
                .filter(key -> "id".equals(key.getName()))
                .findFirst()
                .orElseThrow(AssertionError::new);
        Key<?> importedEmail = imported.getKnownKeys().stream()
                .filter(key -> "email".equals(key.getName()))
                .findFirst()
                .orElseThrow(AssertionError::new);

        assertEquals(Integer.class, importedId.getType());
        assertEquals(DataClassification.CONFIDENTIAL, importedEmail.getClassification());
        assertTrue(importedEmail.getCategories().contains(DataCategory.PERSONAL_DATA));
    }

    @Test(expected = IllegalArgumentException.class)
    public void rejectsAmbiguousUnionTypes() {
        ObjectMapper mapper = new ObjectMapper();
        ObjectNode schema = mapper.createObjectNode();
        schema.put("type", "object");

        ObjectNode id = schema.putObject("properties").putObject("id");
        ArrayNode types = id.putArray("type");
        types.add("string");
        types.add("null");

        new JsonSchemaImporter().importSchema(schema);
    }

    @Test(expected = IllegalArgumentException.class)
    public void rejectsRequiredPropertyMissingFromProperties() {
        ObjectMapper mapper = new ObjectMapper();
        ObjectNode schema = mapper.createObjectNode();
        schema.put("type", "object");
        schema.putArray("required").add("id");
        schema.putObject("properties");

        new JsonSchemaImporter().importSchema(schema);
    }

    @Test(expected = IllegalArgumentException.class)
    public void rejectsUnapprovedJavaTypeExtension() {
        ObjectMapper mapper = new ObjectMapper();
        ObjectNode schema = mapper.createObjectNode();
        schema.put("type", "object");
        ObjectNode value = schema.putObject("properties").putObject("value");
        value.put("type", "string");
        value.put("x-satellite-java-type", "java.lang.Runtime");

        new JsonSchemaImporter().importSchema(schema);
    }

    @Test
    public void followsJsonSchemaDefaultForAdditionalProperties() {
        ObjectMapper mapper = new ObjectMapper();
        ObjectNode schema = mapper.createObjectNode();
        schema.put("title", "Open");
        schema.put("type", "object");
        schema.putObject("properties");

        assertTrue(new JsonSchemaImporter().importSchema(schema).isAllowUnknownKeys());
    }
}
