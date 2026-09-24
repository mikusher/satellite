package io.github.mikusher.satellite.egress.jackson;

import com.fasterxml.jackson.databind.JsonNode;
import io.github.mikusher.satellite.egress.DataCategory;
import io.github.mikusher.satellite.egress.DataClassification;
import io.github.mikusher.satellite.egress.Key;
import io.github.mikusher.satellite.egress.SatelliteSchema;

import java.math.BigDecimal;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * Imports the flat object subset of JSON Schema used by Satellite.
 *
 * <p>Unsupported or ambiguous schema constructs are rejected instead of being
 * guessed. Nested object/item schemas remain the responsibility of application code.</p>
 */
public final class JsonSchemaImporter {

    public SatelliteSchema importSchema(JsonNode root) {
        Objects.requireNonNull(root, "root");

        String type = text(root, "type", null);
        if (!"object".equals(type)) {
            throw new IllegalArgumentException("Satellite schema root must have type=object");
        }

        String title = text(root, "title", "ImportedSchema");
        SatelliteSchema.Builder builder = SatelliteSchema.builder(title);

        JsonNode additionalProperties = root.get("additionalProperties");
        builder.allowUnknownKeys(additionalProperties == null
                || !additionalProperties.isBoolean()
                || additionalProperties.asBoolean());

        Set<String> required = requiredNames(root.get("required"));

        JsonNode properties = root.get("properties");
        if (properties == null || properties.isNull()) {
            return builder.build();
        }
        if (!properties.isObject()) {
            throw new IllegalArgumentException("properties must be a JSON object");
        }

        properties.fields().forEachRemaining(entry -> {
            Key<?> key = importKey(entry.getKey(), entry.getValue(), required.contains(entry.getKey()));
            if (required.contains(entry.getKey())) {
                builder.required(key);
            } else {
                builder.optional(key);
            }
        });

        return builder.build();
    }

    private static Key<?> importKey(String name, JsonNode property, boolean required) {
        if (property == null || !property.isObject()) {
            throw new IllegalArgumentException("Property '" + name + "' must be an object schema");
        }

        JsonNode typeNode = property.get("type");
        if (typeNode == null || !typeNode.isTextual()) {
            throw new IllegalArgumentException(
                    "Property '" + name + "' must have one supported scalar type");
        }

        Key<?> key = keyForType(name, typeNode.asText());

        JsonNode classification = property.get("x-satellite-classification");
        if (classification != null) {
            if (!classification.isTextual()) {
                throw new IllegalArgumentException(
                        "x-satellite-classification must be a string for '" + name + "'");
            }
            try {
                key = key.classifiedAs(DataClassification.valueOf(classification.asText()));
            } catch (IllegalArgumentException e) {
                throw new IllegalArgumentException(
                        "Unknown Satellite classification for '" + name + "': "
                                + classification.asText(), e);
            }
        }

        JsonNode categories = property.get("x-satellite-categories");
        if (categories != null) {
            if (!categories.isArray()) {
                throw new IllegalArgumentException(
                        "x-satellite-categories must be an array for '" + name + "'");
            }
            for (JsonNode category : categories) {
                if (!category.isTextual()) {
                    throw new IllegalArgumentException(
                            "Satellite categories must be strings for '" + name + "'");
                }
                try {
                    key = key.category(DataCategory.valueOf(category.asText()));
                } catch (IllegalArgumentException e) {
                    throw new IllegalArgumentException(
                            "Unknown Satellite category for '" + name + "': "
                                    + category.asText(), e);
                }
            }
        }

        return required ? key.required() : key;
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private static Key<?> keyForType(String name, String type) {
        Class javaType;
        switch (type) {
            case "string":
                javaType = String.class;
                break;
            case "integer":
                javaType = Long.class;
                break;
            case "number":
                javaType = BigDecimal.class;
                break;
            case "boolean":
                javaType = Boolean.class;
                break;
            case "array":
                javaType = List.class;
                break;
            case "object":
                javaType = Map.class;
                break;
            default:
                throw new IllegalArgumentException(
                        "Unsupported JSON Schema type for '" + name + "': " + type);
        }
        return Key.of(name, javaType);
    }

    private static Set<String> requiredNames(JsonNode node) {
        Set<String> names = new HashSet<String>();
        if (node == null || node.isNull()) {
            return names;
        }
        if (!node.isArray()) {
            throw new IllegalArgumentException("required must be an array");
        }
        for (JsonNode value : node) {
            if (!value.isTextual()) {
                throw new IllegalArgumentException("required entries must be strings");
            }
            names.add(value.asText());
        }
        return names;
    }

    private static String text(JsonNode node, String field, String defaultValue) {
        JsonNode value = node.get(field);
        if (value == null || value.isNull()) {
            return defaultValue;
        }
        if (!value.isTextual()) {
            throw new IllegalArgumentException(field + " must be a string");
        }
        return value.asText();
    }
}
