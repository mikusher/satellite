package com.mikusher.converters;


import com.mikusher.error.IncorrectTypeException;

import java.sql.ResultSet;
import java.sql.SQLException;


public class NumberConverter implements Converter<Number> {

    @Override
    public Number cast(Object source) throws IncorrectTypeException {

        if (source instanceof Number) {
            return (Number) source;
        }
        if (source instanceof String) {
            return Double.valueOf(source.toString());
        }
        if (source instanceof Boolean) {
            if (((Boolean) source).booleanValue()) {
                return 1d;
            } else {
                return 0d;
            }
        }
        throw new IncorrectTypeException(Double.class, source.getClass());
    }


    @Override
    public Number fromResultSet(ResultSet rs, int columnIndex) throws SQLException {

        return rs.getLong(columnIndex);
    }


    @Override
    public Number fromResultSet(ResultSet rs, String columnName) throws SQLException {

        return rs.getLong(columnName);
    }

}
