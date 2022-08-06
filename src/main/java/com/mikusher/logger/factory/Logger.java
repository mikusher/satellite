package com.mikusher.logger.factory;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.mikusher.logger.JsonLogger;
import com.mikusher.logger.tracker.*;
import org.apache.commons.lang3.time.FastDateFormat;

/**
 * Wrapper for slf4j Logger that enables a builder pattern and JSON layout
 */
public class Logger {
    private final org.slf4j.Logger slf4jLogger;

    private final Gson gson = new GsonBuilder().disableHtmlEscaping().enableComplexMapKeySerialization().serializeNulls().create();
    private final FastDateFormat formatter;
    private final boolean includeLoggerName;

    private final boolean justJsonLogger;

    private final NoopLogger noopLogger = new NoopLogger();

    public Logger(org.slf4j.Logger slf4jLogger, FastDateFormat formatter, boolean includeLoggerName, boolean justJsonLogger) {
        this.slf4jLogger = slf4jLogger;
        this.formatter = formatter;
        this.includeLoggerName = includeLoggerName;
        this.justJsonLogger = justJsonLogger;
    }

    public JsonLogger trace() {
        if (slf4jLogger.isTraceEnabled()) {
            return new TraceLogger(slf4jLogger, formatter, gson, includeLoggerName, justJsonLogger);
        }

        return noopLogger;
    }

    public JsonLogger debug() {
        if (slf4jLogger.isDebugEnabled()) {
            return new DebugLogger(slf4jLogger, formatter, gson, includeLoggerName, justJsonLogger);
        }

        return noopLogger;
    }

    public JsonLogger info() {
        if (slf4jLogger.isInfoEnabled()) {
            return new InfoLogger(slf4jLogger, formatter, gson, includeLoggerName, justJsonLogger);
        }

        return noopLogger;
    }

    public JsonLogger warn() {
        if (slf4jLogger.isWarnEnabled()) {
            return new WarnLogger(slf4jLogger, formatter, gson, includeLoggerName, justJsonLogger);
        }

        return noopLogger;
    }

    public JsonLogger error() {
        if (slf4jLogger.isErrorEnabled()) {
            return new ErrorLogger(slf4jLogger, formatter, gson, includeLoggerName, justJsonLogger);
        }

        return noopLogger;
    }
}
