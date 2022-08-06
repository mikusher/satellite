package com.mikusher.error;

import com.mikusher.constants.MessageCodes;

import java.util.Locale;

public class CoreError extends RuntimeException implements LocalizedMessage {
    private String _errorCode = null;
    private MessageCodes _errorObject = null;
    private Object[] _msgArgs = null;

    public CoreError(CoreException st) {

        this(st.getErrorObject(), st.getErrorCode(), st.getMessageArguments(), st);
    }


    protected CoreError(MessageCodes rbe, String errorCode, Object[] args, Throwable thr) {

        super();

        _errorObject = rbe;
        _errorCode = errorCode;

        _msgArgs = args == null ? new Object[0] : args;
        if (thr != null) {
            initCause(thr);
        }
    }


    public CoreError(CoreError se) {

        this(se.getErrorObject(), se.getErrorCode(), se.getMessageArguments(), se);
    }


    public CoreError(String errorObj, Object messageArgument) {

        this(errorObj, new Object[]{messageArgument}, null);
    }


    public CoreError(String errorObj, Object messageArgument, Throwable thr) {

        this(errorObj, new Object[]{messageArgument}, thr);
    }


    public CoreError(String errorObj, Object[] messageArguments) {

        this(errorObj, messageArguments, null);
    }


    public CoreError(String errorObj, Object[] messageArguments, Throwable thr) {

        this(null, errorObj, messageArguments, thr);
    }


    public CoreError(String errorObj) {

        this(errorObj, new Object[]{});
    }


    public CoreError(String errorCode, Throwable e) {

        this(errorCode, new Object[]{e.getClass().getName(), e}, e);
    }


    public CoreError(MessageCodes errorObj, Object... messageArgument) {

        this(errorObj, null, messageArgument);
    }


    public CoreError(MessageCodes errorObj, Throwable thr, Object... messageArgument) {

        this(errorObj, null, messageArgument, thr);

        if (thr != null) {
            Object[] tmp = new Object[_msgArgs.length + 2];
            System.arraycopy(_msgArgs, 0, tmp, 0, _msgArgs.length);
            _msgArgs = tmp;
            _msgArgs[_msgArgs.length - 2] = thr.getClass().getName();
            _msgArgs[_msgArgs.length - 1] = thr;
        }
    }

    public static CoreError toSatelliteError(Exception e) {

        return new CoreError(e.getLocalizedMessage());
    }

    public String getErrorCode() {

        if (_errorCode == null) {
            return String.valueOf(_errorObject);
        }
        return _errorCode;
    }

    public MessageCodes getErrorObject() {

        return _errorObject;
    }

    @Override
    public String getMessage() {

        return getLocalizedMessage(Locale.getDefault());
    }

    @Override
    public String getLocalizedMessage(Locale loc) {

        if (_errorObject == null && _errorCode == null && _msgArgs == null) {
            return super.getMessage();
        }

        if (_errorObject != null) {
            // Use new resources mechanism
            return null;
            //MessageManager.getInstance().format(_errorObject, loc, _msgArgs);
        }
        return _errorCode;
        //MessageManager.getInstance().format(_errorCode, _msgArgs);
    }

    public Object[] getMessageArguments() {

        return _msgArgs;
    }

    @Override
    public synchronized Throwable initCause(Throwable cause) {

        Throwable oldCause = getCause();

        if (oldCause == this || oldCause == null) {
            return super.initCause(cause);
        }
        return this;
    }

}
