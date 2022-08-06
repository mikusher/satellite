package com.mikusher.converters;


import com.mikusher.error.IncorrectTypeException;
import com.mikusher.error.UnsupportedValueForType;
import com.mikusher.parameter.ParameterTypes;
import org.apache.commons.lang3.math.NumberUtils;

import java.sql.ResultSet;
import java.sql.SQLException;


public class FloatConverter implements Converter<Float> {


    private static final float checkConsistency(float value) throws IncorrectTypeException {

        if (ConversionUtils.isFinite(value)) {
            return value;
        }
        throw new UnsupportedValueForType(ParameterTypes.Float, value);
    }


    @Override
    public final Float cast(Object source) throws IncorrectTypeException {

        if (source instanceof Float) {
            return (Float) source;
        }
        if (source instanceof Double) {
            double dbl = (Double) source;

            if (dbl > Float.MAX_VALUE || dbl < -Float.MAX_VALUE) {
                throw new UnsupportedValueForType(ParameterTypes.Float, dbl);
            }
            return (float) dbl;
        }

        if (source instanceof CharSequence) {
            return checkConsistency(Float.valueOf(source.toString()));
        }

        if (source instanceof Number) {
            return checkConsistency(((Number) source).floatValue());
        }

        if (source instanceof Boolean) {
            return ((Boolean) source) ? NumberUtils.FLOAT_ONE : NumberUtils.FLOAT_ZERO;
        }

        throw new IncorrectTypeException(Float.class, source.getClass());
    }


    @Override
    public Float fromResultSet(ResultSet rs, int columnIndex) throws SQLException {

        return rs.getFloat(columnIndex);
    }


    @Override
    public Float fromResultSet(ResultSet rs, String columnName) throws SQLException {

        return rs.getFloat(columnName);
    }

}
