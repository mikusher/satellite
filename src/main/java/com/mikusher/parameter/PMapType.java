package com.mikusher.parameter;

import java.math.BigDecimal;
import java.util.*;

public enum PMapType {
    STRING(CharSequence.class, ParameterInfo.STRING, "s", "Str", "string"),
    FLOAT(Float.class, ParameterInfo.FLOAT, "f", "float"),
    DOUBLE(Double.class, ParameterInfo.DOUBLE, "d", "double"),
    INT(Integer.class, ParameterInfo.INTEGER, "i", "Int", "int"),
    LONG(Long.class, ParameterInfo.LONG, "l", "long"),
    BOOLEAN(Boolean.class, ParameterInfo.BOOLEAN, "b", "boolean"),
    DECIMAL(BigDecimal.class, ParameterInfo.DECIMAL, "c", "Dec", "decimal"),
    DATE(Date.class, ParameterInfo.DATE, "t", "date"),
    MAP(Map.class, ParameterInfo.MAP, "m", "map"),
    ARRAY(List.class, ParameterInfo.ARRAY, "a", "array"),
    NULL(null, ParameterInfo.NULL, "n", "null");

    private static final Map<String, PMapType> _mapping;
    private static final Map<Class<?>, PMapType> _classMapping = new IdentityHashMap<>();

    static {
        Map<String, PMapType> mapping = new IdentityHashMap<>();
        for (PMapType type : values()) {
            for (String typeS : type.getTypeAlias()) {
                mapping.put(typeS, type);
            }
            mapping.put(type.getShortName(), type);
            mapping.put(type.getOldPMapName(), type);

            _classMapping.put(type.getClass(), type);
        }

        _mapping = Collections.unmodifiableMap(mapping);
    }

    private final String[] _typeNames;
    private final String _shortName;
    private final String _oldPMapName;
    private final Class<?> _javaClass;


    PMapType(Class<?> javaClass, String pmapType, String shortName, String... strValues) {

        final String[] values = new String[strValues.length];
        for (int i = 0; i < strValues.length; i++) {
            values[i] = strValues[i].intern();
        }

        _typeNames = values;
        _javaClass = javaClass;
        _oldPMapName = pmapType.intern();
        _shortName = shortName.intern();
    }

    public static PMapType parameterTypeToPMapType(ParameterTypes parameterType) {

        PMapType pMapType;

        switch (parameterType) {
            case String:
                pMapType = PMapType.STRING;
                break;
            case Integer:
                pMapType = PMapType.INT;
                break;
            case Long:
                pMapType = PMapType.LONG;
                break;
            case Boolean:
                pMapType = PMapType.BOOLEAN;
                break;
            case Float:
                pMapType = PMapType.FLOAT;
                break;
            case Double:
                pMapType = PMapType.DOUBLE;
                break;
            case Map:
                pMapType = PMapType.MAP;
                break;
            case Array:
                pMapType = PMapType.ARRAY;
                break;
            case Date:
                pMapType = PMapType.DATE;
                break;
            case Decimal:
                pMapType = PMapType.DECIMAL;
                break;
            default:
                pMapType = PMapType.NULL;
                break;
        }

        return pMapType;

    }

    public static PMapType lookup(String typeName) {

        return _mapping.get(typeName.intern());
    }

    public static PMapType lookup(Object value) {

        if (value == null) {
            return NULL;
        }

        return internalLookup(value.getClass());
    }

    public static PMapType lookup(Class<?> mapClass) {

        if (mapClass == null) {
            return NULL;
        }

        return internalLookup(mapClass);
    }

    private static PMapType internalLookup(Class<?> mapClass) {

        PMapType value = _classMapping.get(mapClass);
        if (value != null) {
            return value;
        }

        for (PMapType type : values()) {
            if (type != PMapType.NULL && type.getJavaClass().isAssignableFrom(mapClass)) {
                _classMapping.put(mapClass, type);
                return type;
            }
        }

        return null;
    }

    public Class<?> getJavaClass() {

        return _javaClass;
    }

    public String[] getTypeAlias() {

        return _typeNames;
    }

    public String getOldPMapName() {

        return _oldPMapName;
    }

    public String getShortName() {

        return _shortName;
    }
}
