package io.github.mikusher.satellite.egress;

import java.util.Collections;
import java.util.EnumSet;
import java.util.Objects;
import java.util.Set;

/**
 * A typed key with static security and privacy metadata.
 */
public final class Key<T> {
    private final String name;
    private final Class<T> type;
    private final DataClassification classification;
    private final Set<DataCategory> categories;
    private final boolean required;

    private Key(String name,
                Class<T> type,
                DataClassification classification,
                Set<DataCategory> categories,
                boolean required) {
        validateName(name);
        this.name = name;
        this.type = Objects.requireNonNull(type, "type");
        this.classification = Objects.requireNonNull(classification, "classification");
        this.categories = immutableCategories(categories);
        this.required = required;
    }

    public static <T> Key<T> of(String name, Class<T> type) {
        return new Key<T>(name, type, DataClassification.INTERNAL,
                Collections.singleton(DataCategory.GENERAL), false);
    }

    public static Key<String> string(String name) {
        return of(name, String.class);
    }

    public static Key<Integer> integer(String name) {
        return of(name, Integer.class);
    }

    public static Key<Long> longKey(String name) {
        return of(name, Long.class);
    }

    public static Key<Boolean> bool(String name) {
        return of(name, Boolean.class);
    }

    public Key<T> classifiedAs(DataClassification value) {
        return new Key<T>(name, type, value, categories, required);
    }

    public Key<T> category(DataCategory value) {
        EnumSet<DataCategory> copy = enumSet(categories);
        copy.add(Objects.requireNonNull(value, "value"));
        if (value != DataCategory.GENERAL) {
            copy.remove(DataCategory.GENERAL);
        }
        return new Key<T>(name, type, classification, copy, required);
    }

    public Key<T> required() {
        return new Key<T>(name, type, classification, categories, true);
    }

    public T cast(Object value) {
        if (value == null) {
            return null;
        }
        if (!type.isInstance(value)) {
            throw new IllegalArgumentException(
                    "Value for key '" + name + "' must be " + type.getName()
                            + " but was " + value.getClass().getName());
        }
        return type.cast(value);
    }

    public String getName() {
        return name;
    }

    public Class<T> getType() {
        return type;
    }

    public DataClassification getClassification() {
        return classification;
    }

    public Set<DataCategory> getCategories() {
        return categories;
    }

    public boolean isRequired() {
        return required;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof Key)) {
            return false;
        }
        Key<?> that = (Key<?>) other;
        return required == that.required
                && name.equals(that.name)
                && type.equals(that.type)
                && classification == that.classification
                && categories.equals(that.categories);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, type, classification, categories, required);
    }

    @Override
    public String toString() {
        return "Key{name='" + name + "', type=" + type.getSimpleName()
                + ", classification=" + classification
                + ", categories=" + categories
                + ", required=" + required + '}';
    }

    private static void validateName(String value) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException("name must not be blank");
        }
        for (int i = 0; i < value.length(); i++) {
            if (Character.isISOControl(value.charAt(i))) {
                throw new IllegalArgumentException("name must not contain control characters");
            }
        }
    }

    private static Set<DataCategory> immutableCategories(Set<DataCategory> input) {
        EnumSet<DataCategory> copy = enumSet(input);
        if (copy.isEmpty()) {
            copy.add(DataCategory.GENERAL);
        }
        return Collections.unmodifiableSet(copy);
    }

    private static EnumSet<DataCategory> enumSet(Set<DataCategory> input) {
        Objects.requireNonNull(input, "categories");
        if (input.isEmpty()) {
            return EnumSet.noneOf(DataCategory.class);
        }
        return EnumSet.copyOf(input);
    }
}
