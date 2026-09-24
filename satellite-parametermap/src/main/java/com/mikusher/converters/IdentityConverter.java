package com.mikusher.converters;


import com.mikusher.error.IncorrectTypeException;

import java.sql.ResultSet;
import java.sql.SQLException;


public class IdentityConverter implements Converter<Object> {

    public IdentityConverter() {
        // Ensure default constructor is available
    }


    /**
     *
     */
    @Override
    public final Object cast(Object source) throws IncorrectTypeException {

        return source;
    }


    @Override
    public Object fromResultSet(ResultSet rs, int columnIndex) throws SQLException {

        throw new UnsupportedOperationException();
    }


    @Override
    public Object fromResultSet(ResultSet rs, String columnName) throws SQLException {

        throw new UnsupportedOperationException();
    }

}
