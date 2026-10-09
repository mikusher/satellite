package com.mikusher.converters;


import com.mikusher.error.IncorrectTypeException;
import com.mikusher.error.UnsupportedValueForType;
import com.mikusher.parameter.ParameterTypes;
import org.apache.commons.lang3.math.NumberUtils;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Date;


public class DoubleConverter implements Converter<Double> {


    private static final Double checkConsistency(Double value) throws IncorrectTypeException {

        if (ConversionUtils.isFinite(value)) {
            return value;
        }
        throw new UnsupportedValueForType(ParameterTypes.Double, value);
    }


    /**
     *
     */
    @Override
    public final Double cast(Object source) throws IncorrectTypeException {

        if (source instanceof Long || source instanceof Integer) {
            return ((Number) source).doubleValue();
        }

        if (source instanceof Number) {
            return checkConsistency(((Number) source).doubleValue());
        }

        if (source instanceof CharSequence) {
            return checkConsistency(Double.parseDouble(source.toString()));
        }

        if (source instanceof Boolean) {
            return ((Boolean) source) ? NumberUtils.DOUBLE_ONE : NumberUtils.DOUBLE_ZERO;
        }

        if (source instanceof Date) {
            return (double) ConversionUtils.FastDateFormat.SECOND.date2Long((Date) source);
        }

        throw new IncorrectTypeException(Double.class, source.getClass());
    }


    @Override
    public Double fromResultSet(ResultSet rs, int columnIndex) throws SQLException {

        return rs.getDouble(columnIndex);
    }


    @Override
    public Double fromResultSet(ResultSet rs, String columnName) throws SQLException {

        return rs.getDouble(columnName);
    }

}
