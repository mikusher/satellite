package com.mikusher.error;

import com.mikusher.constants.MessageCodes;

public class SatelliteError extends CoreError {

    public SatelliteError(String errorCode) {

        super(errorCode);
    }

    public SatelliteError(String errorCode, Object[] messageArguments) {

        super(errorCode, messageArguments);
    }


    public SatelliteError(String errorCode, Object[] messageArguments, Throwable thr) {

        super(errorCode, messageArguments, thr);
    }


    public SatelliteError(String errorCode, Object messageArgument) {

        super(errorCode, messageArgument);
    }


    public SatelliteError(String errorCode, Object messageArgument, Throwable thr) {

        super(errorCode, messageArgument, thr);
    }


    public SatelliteError(String errorCode, Throwable e) {

        super(errorCode, e);
    }


    public SatelliteError(MessageCodes errorCode, Object... messageArguments) {

        super(errorCode, messageArguments);
    }


    public SatelliteError(MessageCodes errorCode, Throwable thr, Object... messageArguments) {

        super(errorCode, thr, messageArguments);
    }

    public SatelliteError(CoreException ce) {

        super(ce);
    }

    public SatelliteError(CoreError cr) {

        super(cr);
    }

}
