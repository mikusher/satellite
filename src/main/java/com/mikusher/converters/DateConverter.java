package com.mikusher.converters;


import com.mikusher.error.IncorrectTypeException;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Date;


public class DateConverter implements Converter<Date> {

    /**
     * *
     */
    @Override
    public final Date cast(Object source) throws IncorrectTypeException {

        // Needed for descendants of class Date like Timestamp
        if (source instanceof Date) {
            return (Date) source;
        }

        if (source instanceof CharSequence) {
            return ConversionUtils.FastDateFormat.SECOND.string2Date((CharSequence) source);
        }

        if (source instanceof Long || (source instanceof Double && !ConversionUtils.hasDecimalPart((Double) source))) {
            return ConversionUtils.FastDateFormat.SECOND.long2Date(((Number) source).longValue());
        }

        throw new IncorrectTypeException(Date.class, source.getClass(), source);
    }


    @Override
    public Date fromResultSet(ResultSet rs, int columnIndex) throws SQLException {

        return rs.getTimestamp(columnIndex);
    }


    @Override
    public Date fromResultSet(ResultSet rs, String columnName) throws SQLException {

        return rs.getTimestamp(columnName);
    }

}
