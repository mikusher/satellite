package com.mikusher.converters;


import com.mikusher.error.IncorrectTypeException;

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;


public class BooleanConverter implements Converter<Boolean> {

    public static final String TRUE = "TRUE";
    public static final String FALSE = "FALSE";


    /**
     *
     */
    @Override
    public final Boolean cast(Object source) throws IncorrectTypeException {


        if (source instanceof Integer || source instanceof Long || source instanceof Byte) {
            int value = ((Number) source).intValue();
            if (value == 0) {
                return false;
            } else if (value == 1) {

                return true;
            }
        } else if (source instanceof CharSequence) {
            CharSequence charSeq = (CharSequence) source;
            if (charSeq.length() == 1) {
                // Check for a value of 0 = false and non zero true
                char ch = charSeq.charAt(0);
                if (ch == '0') {
                    return false;
                } else if (ch == '1') {
                    return true;
                }
            } else {
                // TODO: Improve performance
                if (checkCaseInsensitiveMatch(charSeq, TRUE)) {
                    return true;
                }
                if (checkCaseInsensitiveMatch(charSeq, FALSE)) {
                    return false;
                }
            }
        } else if (source instanceof Float || source instanceof Double || source instanceof BigDecimal) {
            double value = ((Number) source).doubleValue();
            if (!ConversionUtils.hasDecimalPart(value)) {
                if (value == 0) {
                    return false;
                } else if (value == 1) {
                    return true;
                }
            }
        }

        throw new IncorrectTypeException(Boolean.class, source.getClass(), source);
    }


    private final boolean checkCaseInsensitiveMatch(CharSequence cs, CharSequence referenceValue) {

        if (cs.length() != referenceValue.length()) {
            return false;
        }
        for (int i = 0; i < cs.length(); i++) {
            if (Character.toUpperCase(cs.charAt(i)) != referenceValue.charAt(i)) {
                return false;
            }
        }

        // Equal
        return true;
    }


    @Override
    public Boolean fromResultSet(ResultSet rs, int columnIndex) throws SQLException {

        return rs.getBoolean(columnIndex);
    }


    @Override
    public Boolean fromResultSet(ResultSet rs, String columnName) throws SQLException {

        return rs.getBoolean(columnName);
    }

}
