package com.mikusher.converters;


import com.mikusher.error.IncorrectTypeException;
import com.mikusher.error.UnsupportedValueForType;
import com.mikusher.parameter.ParameterTypes;
import org.apache.commons.lang3.math.NumberUtils;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Date;

public class LongConverter implements Converter<Long> {

    @Override
    public final Long cast(Object source) throws IncorrectTypeException {

        if (source instanceof Integer) {
            return ((Integer) source).longValue();
        }

        if (source instanceof Number) {
            double dbl = ((Number) source).doubleValue();
            if ((dbl > Long.MAX_VALUE) || (dbl < Long.MIN_VALUE) || (ConversionUtils.hasDecimalPart(dbl))) {
                throw new UnsupportedValueForType(ParameterTypes.Long, dbl);
            }
            return ((Number) source).longValue();
        }

        if (source instanceof CharSequence) {
            return Long.valueOf(source.toString());
        }

        if (source instanceof Boolean) {
            return ((Boolean) source) ? NumberUtils.LONG_ONE : NumberUtils.LONG_ZERO;
        }

        if (source instanceof Date) {
            return ConversionUtils.FastDateFormat.SECOND.date2Long((Date) source);
        }

        throw new IncorrectTypeException(Long.class, source.getClass(), source);
    }


    @Override
    public Long fromResultSet(ResultSet rs, int columnIndex) throws SQLException {

        return rs.getLong(columnIndex);
    }


    @Override
    public Long fromResultSet(ResultSet rs, String columnName) throws SQLException {

        return rs.getLong(columnName);
    }

}
