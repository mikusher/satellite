package com.mikusher.converters;


import com.mikusher.error.IncorrectTypeException;
import com.mikusher.error.UnsupportedValueForType;
import com.mikusher.parameter.ParameterTypes;
import org.apache.commons.lang3.math.NumberUtils;

import java.sql.ResultSet;
import java.sql.SQLException;


public class IntegerConverter implements Converter<Integer> {


    @Override
    public final Integer cast(Object source) throws IncorrectTypeException {


        if (source instanceof Long) {
            long value = ((Long) source).longValue();
            if (value > Integer.MAX_VALUE || value < Integer.MIN_VALUE) {
                throw new UnsupportedValueForType(ParameterTypes.Integer, value);
            }

            return (int) value;
        }

        if (source instanceof Number) {
            double dbl = ((Number) source).doubleValue();
            if (dbl > Integer.MAX_VALUE || dbl < Integer.MIN_VALUE || ConversionUtils.hasDecimalPart(dbl)) {
                throw new UnsupportedValueForType(ParameterTypes.Integer, dbl);
            }
            return (int) dbl;
        }

        if (source instanceof CharSequence) {
            return Integer.valueOf(source.toString());
        }

        if (source instanceof Boolean) {
            return ((Boolean) source) ? NumberUtils.INTEGER_ONE : NumberUtils.INTEGER_ZERO;
        }

        throw new IncorrectTypeException(Long.class, source.getClass(), source);
    }


    @Override
    public Integer fromResultSet(ResultSet rs, int columnIndex) throws SQLException {

        return rs.getInt(columnIndex);
    }


    @Override
    public Integer fromResultSet(ResultSet rs, String columnName) throws SQLException {

        return rs.getInt(columnName);
    }
}
