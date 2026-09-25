package com.mikusher.utils;

import com.mikusher.formats.StreamedPMapParser;
import com.mikusher.parameter.SatelliteData;

import javax.xml.stream.XMLStreamException;
import java.io.Reader;
import java.sql.ResultSet;
import java.sql.SQLException;

public class SqlUtils {

    private static final String NULL = "NULL";

    private SqlUtils() {

    }

    public static SatelliteData getSatelliteData(ResultSet rs, String name) throws SQLException {

        return getSatelliteData(rs, rs.findColumn(name));
    }

    public static SatelliteData getSatelliteData(ResultSet rs, int fieldIndex) throws SQLException {

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
