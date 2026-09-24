package com.mikusher.converters;


import com.mikusher.error.IncorrectTypeException;

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;


public class DecimalConverter implements Converter<BigDecimal> {

    @Override
    public BigDecimal cast(Object source) throws IncorrectTypeException {

        if (source instanceof Integer) {
            return BigDecimal.valueOf((Integer) source);
        } else if (source instanceof Long) {
            return BigDecimal.valueOf((Long) source);
        } else if (source instanceof Double) {
            return BigDecimal.valueOf((Double) source);
        } else if (source instanceof Float) {
            return BigDecimal.valueOf((Float) source);
        } else if (source instanceof String) {
            return new BigDecimal((String) source);
        } else if (source instanceof Boolean) {
            return ((Boolean) source) ? BigDecimal.ONE : BigDecimal.ZERO;
        }

        throw new IncorrectTypeException(BigDecimal.class, source.getClass());
    }


    @Override
    public BigDecimal fromResultSet(ResultSet rs, int columnIndex) throws SQLException {

        return rs.getBigDecimal(columnIndex);
    }


    @Override
    public BigDecimal fromResultSet(ResultSet rs, String columnName) throws SQLException {

        return rs.getBigDecimal(columnName);
    }
}
