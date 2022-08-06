package com.mikusher.parameter;

import com.mikusher.converters.*;
import com.mikusher.error.CoreError;
import com.mikusher.error.CoreException;
import com.mikusher.error.IncorrectTypeException;
import com.mikusher.utils.DataMap;
import org.apache.commons.lang3.ArrayUtils;

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Types;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Objects;


public enum ParameterTypes {

    String(String.class, true, new StringConverter()),
    Integer(Integer.class, true, new IntegerConverter()),
    Long(Long.class, true, new LongConverter()),
    Boolean(Boolean.class, true, new BooleanConverter()),
    Float(Float.class, true, new FloatConverter()),
    Double(Double.class, true, new DoubleConverter()),
    Map(ParameterMap.class, false, new ParameterMapConverter(), new Class<?>[]{java.util.Map.class, DataMap.class}),
    Array(List.class, false, new ListConverter(), new Class<?>[]{Object[].class}),
    Date(java.util.Date.class, true, new DateConverter()),
    Decimal(BigDecimal.class, true, new DecimalConverter()),
    Null(null, true, new IdentityConverter()),
    Choice(Object.class, false, new IdentityConverter()),
    Unknown(Object.class, true, new IdentityConverter());

    // Caching data for performance
    private static final ParameterTypes[] MATCHING_VALUES = {String, Integer, Long, Boolean,
            Float, Double, Map, Array,
            Date, Decimal, Null};
    private static final ParameterTypes[] ALL_VALUES = ParameterTypes.values();
    private static final IdentityHashMap<Class<?>, ParameterTypes> _classMapping = new IdentityHashMap<>();

    static {
        // Cache existing mappings for improved performance
        for (ParameterTypes pt : MATCHING_VALUES) {
            _classMapping.put(pt.getExpectedClass(), pt);

            for (Class<?> clazz : pt.getOtherClasses()) {
                _classMapping.put(clazz, pt);
            }
        }
    }

    //
    private final Class<?> _expectedClass;
    private final boolean _simpleType;
    private final Converter<?> _converter;
    private final Class<?>[] _otherClasses;


    <T> ParameterTypes(Class<? extends T> expectedClass, boolean simpleType, Converter<? extends T> converter) {

        this(expectedClass, simpleType, converter, ArrayUtils.EMPTY_CLASS_ARRAY);
    }


    <T> ParameterTypes(Class<? extends T> expectedClass,
                       boolean simpleType,
                       Converter<? extends T> converter,
                       Class<?>[] otherExpectedClasses) {

        _expectedClass = expectedClass;
        _simpleType = simpleType;
        _converter = Objects.requireNonNull(converter);
        _otherClasses = Objects.requireNonNull(otherExpectedClasses);
    }

    public static ParameterTypes matchType(Class<?> type) {

        if (type == null) {
            return ParameterTypes.Null;
        }

        // Check for type in cache
        ParameterTypes rtype = _classMapping.get(type);
        if (rtype != null) {
            return rtype;
        }

        // Iterate through all types
        for (ParameterTypes pt : MATCHING_VALUES) {
            if (pt.getExpectedClass() != null && pt.getExpectedClass().isAssignableFrom(type)) {
                // Add to cache
                putClassMapping(type, pt);
                return pt;
            }

            for (Class<?> clazz : pt.getOtherClasses()) {
                if (clazz.isAssignableFrom(type)) {
                    // Add to cache
                    putClassMapping(clazz, pt);
                    return pt;
                }
            }
        }

        return ParameterTypes.Unknown;
    }

    private static void putClassMapping(Class<?> clazz, ParameterTypes type) {

        // Use the class object to synchronize mapping validation and verification
        ParameterTypes oldType;
        synchronized (_classMapping) {
            oldType = _classMapping.putIfAbsent(clazz, type);
        }

        if (oldType != null && oldType != type) {
            throw new CoreError("Class ''{0}'' as more than one matching type: {1} and {2}",
                    new Object[]{clazz.getName(), oldType, type});
        }
    }

    /**
     * Matches a given MapType by its case insensitive name
     *
     * @param type The type name to check (E.g. "String)
     * @return The respective ParameterTypes value
     */
    public static ParameterTypes matchType(String type) {

        for (ParameterTypes pt : ALL_VALUES) {
            if (type.equalsIgnoreCase(pt.toString())) {
                return pt;
            }
        }
        throw new IllegalArgumentException("Invalid Type - " + type);
    }

    /**
     * Discovers the MapType associated to a given JDBC column
     *
     * @param rsmd        The open {@link ResultSetMetaData} with an open cursor to the database
     * @param columnIndex The column index being discovered
     * @return The discovered MapType for this column
     * @throws SQLException When there are errors connecting database or there are unsupported JDBC methods due to
     *                      incomplete implementation
     */
    public static ParameterTypes matchFromJDBCType(ResultSetMetaData rsmd, int columnIndex) throws SQLException {

        switch (rsmd.getColumnType(columnIndex)) {

            case Types.TINYINT:
            case Types.SMALLINT:
            case Types.INTEGER:
                return ParameterTypes.Integer;

            case Types.BIGINT:
                return ParameterTypes.Long;

            case Types.DECIMAL:
                return ParameterTypes.Decimal;

            case Types.NUMERIC:
                return rsmd.getScale(columnIndex) > 0 ? ParameterTypes.Double : ParameterTypes.Long;

            case Types.FLOAT:
            case Types.REAL:
            case Types.DOUBLE:
                return ParameterTypes.Double;

            case Types.CHAR:
            case Types.NVARCHAR:
            case Types.VARCHAR:
            case Types.NCHAR:
            case Types.NCLOB:
            case Types.LONGNVARCHAR:
            case Types.LONGVARCHAR:
            case Types.CLOB:
                return ParameterTypes.String;

            case Types.BOOLEAN:
            case Types.BIT:
                return ParameterTypes.Boolean;

            case Types.TIMESTAMP:
            case Types.DATE:
                return ParameterTypes.Date;

            case Types.ARRAY:
                return ParameterTypes.Array;

            default:
                return ParameterTypes.Unknown;
        }
    }

    public static int getMaxTypeOrdinal() {

        return MATCHING_VALUES.length;
    }

    public Object cast(Object source) throws IncorrectTypeException {

        if (source == null) {
            return source;
        }

        // If expected class have the same class reference as current class then,
        // there's no need to do anything extra and just return current value
        if (source.getClass() == _expectedClass) {
            return source;
        }

        try {
            return _converter.cast(source);
        } catch (RuntimeException re) {
            throw new IncorrectTypeException(_expectedClass, source.getClass(), re);
        }
    }

    public Object fromResultSet(ResultSet rs, int position) throws SQLException {

        return _converter.fromResultSet(rs, position);
    }

    public Object fromResultSet(ResultSet rs, String columnName) throws SQLException {

        return _converter.fromResultSet(rs, columnName);
    }

    public String toString(Object source) throws IncorrectTypeException {

        if (source == null) {
            return null;
        }

        return ParameterTypes.String.cast(source).toString();
    }

    public boolean isValid(Object source) throws CoreException {

        return _expectedClass.isInstance(source);
    }

    public Class<?> getExpectedClass() {

        return _expectedClass;
    }

    public boolean isSimpleType() {

        return _simpleType;
    }

    public Class<?>[] getOtherClasses() {

        return _otherClasses;
    }

    /**
     * Matches at least one of the types provided as argument
     *
     * @param types
     * @return
     */
    public boolean matchesAny(ParameterTypes... types) {

        for (ParameterTypes type : types) {
            if (this.equals(type)) {
                return true;
            }
        }
        return false;
    }
}
