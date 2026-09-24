package com.mikusher.utils;

import com.mikusher.formats.StreamedPMapParser;
import com.mikusher.parameter.ParameterMap;

import javax.xml.stream.XMLStreamException;
import java.io.Reader;
import java.sql.ResultSet;
import java.sql.SQLException;

public class SqlUtils {

    private static final String NULL = "NULL";

    private SqlUtils() {

    }

    public static ParameterMap getParameterMap(ResultSet rs, String name) throws SQLException {

        return getParameterMap(rs, rs.findColumn(name));
    }

    public static ParameterMap getParameterMap(ResultSet rs, int fieldIndex) throws SQLException {

        Reader reader = rs.getCharacterStream(fieldIndex);
        if (reader == null) {
            return null;
        }

        try {
            return StreamedPMapParser.getInstance().getMap(reader);
        } catch (XMLStreamException e) {
            throw new SQLException(e);
        }
    }

}
