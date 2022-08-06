package com.mikusher.logger.factory;

import org.apache.commons.lang3.time.FastDateFormat;

import java.util.Optional;

public class LoggerFactory {
    public static boolean justJsonLogger = false;
    private static String dateFormatString = "yyyy-MM-dd HH:mm:ss.SSSZ"; // yyyy-MM-dd HH:mm:ss.SSSZ || yyyy-MM-dd'T'HH:mm:ss.SSSXXX
    private static FastDateFormat formatter = FastDateFormat.getInstance(dateFormatString);
    private static boolean includeLoggerName = true;

    public static Logger getLogger(String name) {
        org.slf4j.Logger slf4jLogger = org.slf4j.LoggerFactory.getLogger(name);
        return new Logger(slf4jLogger, formatter, includeLoggerName, justJsonLogger);
    }

    public static Logger getLogger(Class<?> clazz) {
        org.slf4j.Logger slf4jLogger = org.slf4j.LoggerFactory.getLogger(clazz);
        return new Logger(slf4jLogger, formatter, includeLoggerName, justJsonLogger);
    }

    public static Logger getLogger(boolean justJsonLogger, Optional<String> name, Optional<Class<?>> clazz) {
        setJustJsonLogger(justJsonLogger);
        if (name.isPresent()) {
            return getLogger(name.get());
        } else if (clazz.isPresent()) {
            return getLogger(clazz.get());
        } else {
            return getLogger("");
        }
    }

    public static void setDateFormatString(String dateFormatString) {
        LoggerFactory.dateFormatString = dateFormatString;
        LoggerFactory.formatter = FastDateFormat.getInstance(dateFormatString);
    }

    public static void setIncludeLoggerName(boolean includeLoggerName) {
        LoggerFactory.includeLoggerName = includeLoggerName;
    }

    public static void setJustJsonLogger(boolean justJsonLogger) {
        LoggerFactory.justJsonLogger = justJsonLogger;
    }
}
