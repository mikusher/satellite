package com.mikusher.error;

import com.mikusher.constants.MessageCodes;
import com.mikusher.constants.Msg;
import com.mikusher.parameter.ParameterTypes;

public class IncorrectTypeException extends SatelliteException {

    public IncorrectTypeException(String parameterName) {
        super(Msg.SAT_UT0007, parameterName);
    }

    public IncorrectTypeException(String messageCode, Throwable cause, String... args) {

        super(messageCode, args, cause);
    }

    public IncorrectTypeException(String parameterName, Class<?> expected, Class<?> actual) {

        super(Msg.SAT_UT0003, parameterName, expected.getName(), (actual == null) ? "Null" : actual.getName());
    }

    public IncorrectTypeException(String parameterName, Class<?> expected, Class<?> actual, Throwable error) {

        super(Msg.SAT_UT0003, parameterName, expected.getName(), (actual == null) ? "Null" : actual.getName());
        super.initCause(error);
    }

    public IncorrectTypeException(String field, Class<?> expected, Class<?> actual, String currentValue) {

        super(field + " (" + actual.getName() + "='" + currentValue + "' => " + expected.getName() + ")");
    }

    public IncorrectTypeException(Class<?> expected, Class<?> actual) {

        super(Msg.SAT_UT0007, expected.getName(), (actual == null) ? "Null" : actual.getName());
    }

    public IncorrectTypeException(Class<?> expected, Class<?> actual, Object value) {

        super(Msg.SAT_UT0004, expected.getName(), (actual == null) ? "Null" : actual.getName(), value);
    }

    public IncorrectTypeException(Class<?> expected, Class<?> actual, Throwable thr) {

        super(Msg.SAT_UT0002, expected.getName(), (actual == null) ? "Null" : actual.getName(), thr.toString());
        super.initCause(thr);
    }

    protected IncorrectTypeException(MessageCodes msg, ParameterTypes type, String value) {
        super(msg, null, type, value);
    }

    public IncorrectTypeException(MessageCodes errorCode, Object... messageArguments) {

        super(errorCode, messageArguments);
    }

    public IncorrectTypeException(MessageCodes errorCode, Throwable thr, Object... messageArguments) {

        super(errorCode, thr, messageArguments);
    }


}
