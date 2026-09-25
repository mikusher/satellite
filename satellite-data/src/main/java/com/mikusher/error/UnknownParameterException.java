package com.mikusher.error;

import com.mikusher.constants.MessageCodes;
import com.mikusher.constants.Msg;

public class UnknownParameterException extends SatelliteException {


    public UnknownParameterException(String parameterName) {

        super(Msg.SAT_UT0001, parameterName);
    }


    public UnknownParameterException(String errorCode, Object object) {

        super(errorCode, object);
    }


    public UnknownParameterException(String errorCode, Object[] objs) {

        super(errorCode, objs);
    }


    public UnknownParameterException(MessageCodes errorCode, Object... args) {

        super(errorCode, args);
    }

    public UnknownParameterException(String errorCode, Exception e) {

        super(errorCode, e);
    }

    public UnknownParameterException(MessageCodes errorCode, Exception e) {

        super(errorCode, e);
    }


}
