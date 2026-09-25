package com.mikusher.converters;


import com.mikusher.error.IncorrectTypeException;
import com.mikusher.formats.StreamedPMapParser;
import com.mikusher.parameter.SatelliteData;
import com.mikusher.utils.SqlUtils;

import javax.xml.stream.XMLStreamException;
import java.io.StringReader;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Map;

public class SatelliteDataConverter implements Converter<SatelliteData> {

    @Override
    public final SatelliteData cast(Object source) throws IncorrectTypeException {


        if (source instanceof Map) {
            return new SatelliteData((Map) source);
        }

        if (source instanceof String) {
            try {

                return StreamedPMapParser.getInstance().getData(new StringReader(source.toString()));

            } catch (XMLStreamException ev2) {

                throw new IncorrectTypeException(SatelliteData.class, String.class, ev2);
            }
        }

        throw new IncorrectTypeException(SatelliteData.class, source.getClass(), source);
    }


    @Override
    public SatelliteData fromResultSet(ResultSet rs, int columnIndex) throws SQLException {

        return SqlUtils.getSatelliteData(rs, columnIndex);

    }


    @Override
    public SatelliteData fromResultSet(ResultSet rs, String columnName) throws SQLException {

        return SqlUtils.getSatelliteData(rs, columnName);

    }
}
