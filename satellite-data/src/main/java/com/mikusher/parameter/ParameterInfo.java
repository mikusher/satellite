package com.mikusher.parameter;

public class ParameterInfo {

    public static final String STRING = ParameterTypes.String.toString();
    public static final String INTEGER = ParameterTypes.Integer.toString();
    public static final String LONG = ParameterTypes.Long.toString();
    public static final String BOOLEAN = ParameterTypes.Boolean.toString();
    public static final String FLOAT = ParameterTypes.Float.toString();
    public static final String DOUBLE = ParameterTypes.Double.toString();
    public static final String MAP = ParameterTypes.Map.toString();
    public static final String ARRAY = ParameterTypes.Array.toString();
    public static final String DATE = ParameterTypes.Date.toString();
    public static final String NULL = ParameterTypes.Null.toString();
    public static final String DECIMAL = ParameterTypes.Decimal.toString();

    private final String _name;
    private final ParameterTypes _type;
    private final String _description;
    private final boolean _mandatory;
    private final Object _defaultValue;


    public ParameterInfo(String name, String type, String description, boolean mandatory, Object defaultValue) {

        this(name, ParameterTypes.matchType(type), description, mandatory, defaultValue);
    }


    public ParameterInfo(String name, ParameterTypes type, String description, boolean mandatory, Object defaultValue) {

        _name = name;
        _type = type;
        _description = description;
        _mandatory = mandatory;
        _defaultValue = defaultValue;
    }


    public String getName() {

        return _name;
    }


    public String getType() {

        return _type.toString();
    }


    public ParameterTypes getParameterType() {

        return _type;
    }


    public String getDescription() {

        return _description;
    }


    public boolean isMandatory() {

        return _mandatory;
    }


    public Object getDefaultValue() {

        return _defaultValue;
    }
}
