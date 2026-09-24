package io.github.mikusher.satellite.egress.jackson;

import com.fasterxml.jackson.databind.JsonNode;
import io.github.mikusher.satellite.egress.DataCategory;
import io.github.mikusher.satellite.egress.DataClassification;
import io.github.mikusher.satellite.egress.Key;
import io.github.mikusher.satellite.egress.SatelliteSchema;

import java.math.BigDecimal;
import java.util.HashSet;
import java.util.Iterator;
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
        if (additionalProperties == null || additionalProperties.isNull()) {
            builder.allowUnknownKeys(true);
        } else if (additionalProperties.isBoolean()) {
            builder.allowUnknownKeys(additionalProperties.asBoolean());
        } else {
            throw new IllegalArgumentException(
                    "Schema-valued additionalProperties is not supported by Satellite import");
        }

        Set<String> required = requiredNames(root.get("required"));

        JsonNode properties = root.get("properties");
        if (properties == null || properties.isNull()) {
            if (!required.isEmpty()) {
                throw new IllegalArgumentException(
                        "required contains names but properties is missing");
            }
            return builder.build();
        }
        if (!properties.isObject()) {
            throw new IllegalArgumentException("properties must be a JSON object");
        }

        Set<String> propertyNames = new HashSet<String>();
        Iterator<Map.Entry<String, JsonNode>> fields = properties.fields();
        while (fields.hasNext()) {
            Map.Entry<String, JsonNode> entry = fields.next();
            propertyNames.add(entry.getKey());

            Key<?> key = importKey(
                    entry.getKey(),
                    entry.getValue(),
                    required.contains(entry.getKey()));

            if (required.contains(entry.getKey())) {
                builder.required(key);
            } else {
                builder.optional(key);
            }
        }

        for (String requiredName : required) {
            if (!propertyNames.contains(requiredName)) {
                throw new IllegalArgumentException(
                        "Required property has no definition: " + requiredName);
            }
        }

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

        String jsonType = typeNode.asText();
        Key<?> key = Key.of(name, javaClass(property, jsonType, name));

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

    private static Class<?> javaClass(JsonNode property, String jsonType, String name) {
        JsonNode javaTypeNode = property.get("x-satellite-java-type");
        if (javaTypeNode == null || javaTypeNode.isNull()) {
            return fallbackJavaClass(jsonType, name);
        }
        if (!javaTypeNode.isTextual()) {
            throw new IllegalArgumentException(
                    "x-satellite-java-type must be a string for '" + name + "'");
        }

        String javaType = javaTypeNode.asText();
        Class<?> resolved;
        switch (javaType) {
            case "java.lang.String":
                resolved = String.class;
                break;
            case "java.lang.Integer":
                resolved = Integer.class;
                break;
            case "java.lang.Long":
                resolved = Long.class;
                break;
            case "java.lang.Boolean":
                resolved = Boolean.class;
                break;
            case "java.lang.Float":
                resolved = Float.class;
                break;
            case "java.lang.Double":
                resolved = Double.class;
                break;
            case "java.math.BigDecimal":
                resolved = BigDecimal.class;
                break;
            case "java.util.List":
                resolved = List.class;
                break;
            case "java.util.Map":
                resolved = Map.class;
                break;
            default:
                throw new IllegalArgumentException(
                        "Unsupported Satellite Java type for '" + name + "': " + javaType);
        }

        String expectedJsonType = expectedJsonType(resolved);
        if (!jsonType.equals(expectedJsonType)) {
            throw new IllegalArgumentException(
                    "JSON type '" + jsonType + "' conflicts with Satellite Java type '"
                            + javaType + "' for '" + name + "'");
        }

        return resolved;
    }

    private static Class<?> fallbackJavaClass(String type, String name) {
        switch (type) {
            case "string":
                return String.class;
            case "integer":
                return Long.class;
            case "number":
                return BigDecimal.class;
            case "boolean":
                return Boolean.class;
            case "array":
                return List.class;
            case "object":
                return Map.class;
            default:
                throw new IllegalArgumentException(
                        "Unsupported JSON Schema type for '" + name + "': " + type);
        }
    }

    private static String expectedJsonType(Class<?> type) {
        if (String.class.equals(type)) {
            return "string";
        }
        if (Integer.class.equals(type) || Long.class.equals(type)) {
            return "integer";
        }
        if (Float.class.equals(type) || Double.class.equals(type) || BigDecimal.class.equals(type)) {
            return "number";
        }
        if (Boolean.class.equals(type)) {
            return "boolean";
        }
        if (List.class.equals(type)) {
            return "array";
        }
        if (Map.class.equals(type)) {
            return "object";
        }
        throw new IllegalArgumentException("Unsupported Satellite Java type: " + type.getName());
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
