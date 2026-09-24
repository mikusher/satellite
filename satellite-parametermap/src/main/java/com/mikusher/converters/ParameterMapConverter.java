package com.mikusher.converters;


import com.mikusher.error.IncorrectTypeException;
import com.mikusher.formats.StreamedPMapParser;
import com.mikusher.parameter.ParameterMap;
import com.mikusher.utils.SqlUtils;

import javax.xml.stream.XMLStreamException;
import java.io.StringReader;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Map;

public class ParameterMapConverter implements Converter<ParameterMap> {

    @Override
    public final ParameterMap cast(Object source) throws IncorrectTypeException {


        if (source instanceof Map) {
            return new ParameterMap((Map) source);
        }

        if (source instanceof String) {
            try {

                return StreamedPMapParser.getInstance().getMap(new StringReader(source.toString()));

            } catch (XMLStreamException ev2) {

                throw new IncorrectTypeException(ParameterMap.class, String.class, ev2);
            }
        }

        throw new IncorrectTypeException(ParameterMap.class, source.getClass(), source);
    }


    @Override
    public ParameterMap fromResultSet(ResultSet rs, int columnIndex) throws SQLException {

        return SqlUtils.getParameterMap(rs, columnIndex);

    }


    @Override
    public ParameterMap fromResultSet(ResultSet rs, String columnName) throws SQLException {

        return SqlUtils.getParameterMap(rs, columnName);

    }
}
