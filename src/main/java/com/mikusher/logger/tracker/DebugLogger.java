package com.mikusher.logger.tracker;


import com.google.gson.Gson;
import com.mikusher.logger.AbstractJsonLogger;
import org.apache.commons.lang3.time.FastDateFormat;

public class DebugLogger extends AbstractJsonLogger {

    public static final String LOG_LEVEL = "DEBUG";

    public DebugLogger(org.slf4j.Logger slf4jLogger, FastDateFormat formatter, Gson gson, boolean includeLoggerName, boolean justJsonLogger) {
        super(slf4jLogger, formatter, gson, includeLoggerName, justJsonLogger);
    }

    @Override
    public void log() {
        slf4jLogger.debug(formatMessage(LOG_LEVEL));
    }

    public String toString() {
        return formatMessage(LOG_LEVEL);
    }
}
