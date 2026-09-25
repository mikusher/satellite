package com.mikusher.converters;


import com.mikusher.error.IncorrectTypeException;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;


public class ListConverter implements Converter<List<?>> {

    @Override
    public final List<?> cast(Object source) throws IncorrectTypeException {

        if (source instanceof List<?>) {
            return (List<?>) source;
        }

        if (source instanceof Object[]) {
            return Arrays.asList((Object[]) source);
        }

        throw new IncorrectTypeException(List.class, source.getClass(), source);
    }


    @Override
    public List<?> fromResultSet(ResultSet rs, int columnIndex) throws SQLException {

        return Collections.singletonList(rs.getArray(columnIndex).getArray());
    }


    @Override
    public List<?> fromResultSet(ResultSet rs, String columnName) throws SQLException {

        return Collections.singletonList(rs.getArray(columnName).getArray());
    }

}
