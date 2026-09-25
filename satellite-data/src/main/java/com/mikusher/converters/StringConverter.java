package com.mikusher.converters;


import com.mikusher.error.IncorrectTypeException;
import com.mikusher.formats.StreamedPMapParser;
import com.mikusher.utils.DataMap;
import org.w3c.dom.DocumentFragment;

import javax.xml.stream.XMLStreamException;
import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Date;


public class StringConverter implements Converter<String> {

    @Override
    public final String cast(Object source) throws IncorrectTypeException {

        // Simple case conversion
        if (source instanceof CharSequence || source instanceof Boolean || source instanceof Long || source instanceof Integer) {
            return source.toString();
        }

        if (source instanceof Double) {
            double value = (Double) source;
            long lngValue = (long) value;
            if (lngValue == value) {
                return Long.toString(lngValue);
            }
            if (ConversionUtils.isFinite(value)) {
                return Double.toString(value);
            }
        }

        if (source instanceof Float) {
            float value = (Float) source;
            long lngValue = (long) value;
            if (lngValue == value) {
                return Long.toString(lngValue);
            }
            if (ConversionUtils.isFinite(value)) {
                return Float.toString(value);
            }
        }

        if (source instanceof Date) {
            return ConversionUtils.FastDateFormat.SECOND.date2String((Date) source);
        }

        if (source instanceof BigDecimal) {
            return ((BigDecimal) source).toPlainString();
        }

        if (source instanceof DataMap) {
            try {
                return StreamedPMapParser.getInstance().toXMLString((DataMap) source, StreamedPMapParser.SerializationType.PMAP2);
            } catch (XMLStreamException e) {
                throw new IncorrectTypeException(String.class, source.getClass(), e);
            }
        }

        if (source instanceof DocumentFragment) {
            throw new IncorrectTypeException(String.class, source.getClass(), source); // return XmlUtils.docToString((DocumentFragment) source);
        }

        throw new IncorrectTypeException(String.class, source.getClass(), source);
    }

    private String castFromZeroDecimalNumber(Object source) {

        String text = source.toString();
        if (text.endsWith(".0")) {
            return text.substring(0, text.length() - 2);
        } else {
            return text;
        }
    }


    @Override
    public String fromResultSet(ResultSet rs, int columnIndex) throws SQLException {

        return rs.getString(columnIndex);
    }


    @Override
    public String fromResultSet(ResultSet rs, String columnName) throws SQLException {

        return rs.getString(columnName);
    }

}
