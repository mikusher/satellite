package com.mikusher.error;

import com.mikusher.constants.MessageCodes;

public class SatelliteException extends CoreException {

    public SatelliteException(String errorCode) {
        super(errorCode);
    }

    public SatelliteException(String errorCode, Object[] messageArguments) {

        super(errorCode, messageArguments);
    }

    public SatelliteException(String errorCode, Object[] messageArguments, Throwable thr) {

        super(errorCode, messageArguments, thr);
    }

    public SatelliteException(String errorCode, Object messageArgument) {

        super(errorCode, messageArgument);
    }


    public SatelliteException(String errorCode, Object messageArgument, Throwable thr) {

        super(errorCode, messageArgument, thr);
    }


    public SatelliteException(String errorCode, Throwable e) {

        super(errorCode, e);
    }


    public SatelliteException(MessageCodes errorCode, Object... messageArguments) {

        super(errorCode, messageArguments);
    }


    public SatelliteException(MessageCodes errorCode, Throwable thr, Object... messageArguments) {

        super(errorCode, thr, messageArguments);
    }

    public SatelliteException(CoreException ce) {

        super(ce);
    }

    public SatelliteException(CoreError se) {
        super(se);
    }


}
