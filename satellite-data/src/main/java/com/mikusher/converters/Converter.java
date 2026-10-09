package com.mikusher.converters;


import com.mikusher.error.IncorrectTypeException;

import java.sql.ResultSet;
import java.sql.SQLException;


public interface Converter<T> {

    T fromResultSet(ResultSet rs, int columnIndex) throws SQLException;

    T fromResultSet(ResultSet rs, String columnName) throws SQLException;

    T cast(Object source) throws IncorrectTypeException;

}