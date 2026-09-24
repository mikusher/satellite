package io.github.mikusher.satellite.egress.jackson;

import com.fasterxml.jackson.databind.JsonNode;
import io.github.mikusher.satellite.egress.DataCategory;
import io.github.mikusher.satellite.egress.DataClassification;
import io.github.mikusher.satellite.egress.Key;
import io.github.mikusher.satellite.egress.SatelliteSchema;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.HashMap;
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
    private static final Map<String, Class<?>> SUPPORTED_JAVA_TYPES = supportedJavaTypes();

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
            if (!required.isEmpty()) {
                throw new IllegalArgumentException(
                        "required contains names but properties is missing");
            }
            return builder.build();
        }
        if (!properties.isObject()) {
            throw new IllegalArgumentException("properties must be a JSON object");
        }

        for (String requiredName : required) {
            if (!properties.has(requiredName)) {
                throw new IllegalArgumentException(
                        "Required property is not defined: " + requiredName);
            }
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

        String jsonType = typeNode.asText();
        Key<?> key = keyForType(name, jsonType, property.get("x-satellite-java-type"));

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
    private static Key<?> keyForType(String name, String jsonType, JsonNode javaTypeNode) {
        Class<?> javaType;

        if (javaTypeNode != null && !javaTypeNode.isNull()) {
            if (!javaTypeNode.isTextual()) {
                throw new IllegalArgumentException(
                        "x-satellite-java-type must be a string for '" + name + "'");
            }
            javaType = SUPPORTED_JAVA_TYPES.get(javaTypeNode.asText());
            if (javaType == null) {
                throw new IllegalArgumentException(
                        "Unsupported Satellite Java type for '" + name + "': "
                                + javaTypeNode.asText());
            }
            if (!compatible(jsonType, javaType)) {
                throw new IllegalArgumentException(
                        "JSON Schema type '" + jsonType + "' is incompatible with "
                                + javaType.getName() + " for '" + name + "'");
            }
        } else {
            javaType = defaultJavaType(jsonType, name);
        }

        return Key.of(name, (Class) javaType);
    }

    private static Class<?> defaultJavaType(String type, String name) {
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

    private static boolean compatible(String jsonType, Class<?> javaType) {
        switch (jsonType) {
            case "string":
                return javaType == String.class || javaType == Character.class;
            case "integer":
                return javaType == Byte.class
                        || javaType == Short.class
                        || javaType == Integer.class
                        || javaType == Long.class
                        || javaType == BigInteger.class;
            case "number":
                return Number.class.isAssignableFrom(javaType);
            case "boolean":
                return javaType == Boolean.class;
            case "array":
                return javaType == List.class;
            case "object":
                return javaType == Map.class;
            default:
                return false;
        }
    }

    private static Map<String, Class<?>> supportedJavaTypes() {
        Map<String, Class<?>> types = new HashMap<String, Class<?>>();
        types.put(String.class.getName(), String.class);
        types.put(Character.class.getName(), Character.class);
        types.put(Byte.class.getName(), Byte.class);
        types.put(Short.class.getName(), Short.class);
        types.put(Integer.class.getName(), Integer.class);
        types.put(Long.class.getName(), Long.class);
        types.put(Float.class.getName(), Float.class);
        types.put(Double.class.getName(), Double.class);
        types.put(BigInteger.class.getName(), BigInteger.class);
        types.put(BigDecimal.class.getName(), BigDecimal.class);
        types.put(Boolean.class.getName(), Boolean.class);
        types.put(List.class.getName(), List.class);
        types.put(Map.class.getName(), Map.class);
        return types;
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
