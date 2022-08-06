package com.mikusher.utils;

import com.mikusher.error.UnknownParameterException;

import java.math.BigDecimal;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public interface DataMap extends Map<String, Object>, CloneableEntry<DataMap> {

    void setString(String paramName, String paramValue) throws UnknownParameterException;


    void setInt(String paramName, int paramValue) throws UnknownParameterException;


    void setLong(String paramName, long paramValue) throws UnknownParameterException;


    void setFloat(String paramName, float paramValue) throws UnknownParameterException;


    void setDouble(String paramName, double paramValue) throws UnknownParameterException;


    void setBoolean(String paramName, boolean paramValue) throws UnknownParameterException;


    void setDate(String paramName, Date paramValue) throws UnknownParameterException;

    void setArray(String paramName, List<?> paramValue) throws UnknownParameterException;


    void setMap(String paramName, DataMap paramValue) throws UnknownParameterException;


    String getString(String paramName) throws Exception;


    int getInt(String paramName) throws Exception;


    long getLong(String paramName) throws Exception;


    float getFloat(String paramName) throws Exception;


    double getDouble(String paramName) throws Exception;


    boolean getBoolean(String paramName) throws Exception;


    Date getDate(String paramName) throws Exception;


    <T> List<T> getArray(Class<T> classObj, String paramName) throws Exception;


    List getArray(String paramName) throws Exception;


    DataMap getMap(String paramName) throws Exception;


    Object getParameter(String paramName) throws Exception;


    DataMap clone();


    DataMap newMap();


    List<Object> newArray();


    BigDecimal getDecimal(String paramName) throws Exception;


    boolean isNull(String paramName) throws Exception;

    void setNull(String paramName) throws Exception;

    void setDecimal(String paramName, BigDecimal paramValue) throws Exception;

    Iterator<String> getParameterNames();

    void setParameter(String paramName, Object value) throws Exception;

}
