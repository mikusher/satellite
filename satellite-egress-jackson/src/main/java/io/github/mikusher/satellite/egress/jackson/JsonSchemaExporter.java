package io.github.mikusher.satellite.egress.jackson;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.github.mikusher.satellite.egress.DataCategory;
import io.github.mikusher.satellite.egress.Key;
import io.github.mikusher.satellite.egress.SatelliteSchema;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Collection;
import java.util.Map;
import java.util.Objects;

/**
 * Exports SatelliteSchema to JSON Schema 2020-12 with Satellite metadata extensions.
 */
public final class JsonSchemaExporter {
    private static final String DRAFT_2020_12 = "https://json-schema.org/draft/2020-12/schema";

    private final ObjectMapper objectMapper;

    public JsonSchemaExporter(ObjectMapper objectMapper) {
        this.objectMapper = Objects.requireNonNull(objectMapper, "objectMapper");
    }

    public JsonNode export(SatelliteSchema schema) {
        Objects.requireNonNull(schema, "schema");

        ObjectNode root = objectMapper.createObjectNode();
        root.put("$schema", DRAFT_2020_12);
        root.put("title", schema.getName());
        root.put("type", "object");
        root.put("additionalProperties", schema.isAllowUnknownKeys());

        ObjectNode properties = root.putObject("properties");
        for (Key<?> key : schema.getKnownKeys()) {
            ObjectNode property = properties.putObject(key.getName());
            String jsonType = jsonType(key.getType());
            if (jsonType != null) {
                property.put("type", jsonType);
            }
            property.put("x-satellite-classification", key.getClassification().name());
            property.put("x-satellite-java-type", key.getType().getName());
            ArrayNode categories = property.putArray("x-satellite-categories");
            for (DataCategory category : key.getCategories()) {
                categories.add(category.name());
            }
        }

        if (!schema.getRequiredKeys().isEmpty()) {
            ArrayNode required = root.putArray("required");
            for (Key<?> key : schema.getRequiredKeys()) {
                required.add(key.getName());
            }
        }

        return root;
    }

    private static String jsonType(Class<?> type) {
        if (CharSequence.class.isAssignableFrom(type) || Character.class.equals(type)) {
            return "string";
        }
        if (Boolean.class.equals(type) || Boolean.TYPE.equals(type)) {
            return "boolean";
        }
        if (Byte.class.equals(type) || Short.class.equals(type) || Integer.class.equals(type)
                || Long.class.equals(type) || BigInteger.class.isAssignableFrom(type)
                || Byte.TYPE.equals(type) || Short.TYPE.equals(type)
                || Integer.TYPE.equals(type) || Long.TYPE.equals(type)) {
            return "integer";
        }
        if (Number.class.isAssignableFrom(type)
                || Float.TYPE.equals(type) || Double.TYPE.equals(type)
                || BigDecimal.class.isAssignableFrom(type)) {
            return "number";
        }
        if (type.isArray() || Collection.class.isAssignableFrom(type)) {
            return "array";
        }
        if (Map.class.isAssignableFrom(type)) {
            return "object";
        }
        return null;
    }
}
