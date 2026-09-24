package com.mikusher.error;

import com.mikusher.constants.MessageCodes;

import java.util.Locale;

public class CoreException extends Exception implements LocalizedMessage {

    private String _errorCode = null;
    private MessageCodes _errorObject = null;
    private Object[] _msgArgs = null;


    public CoreException(CoreException we) {

        this(we.getErrorObject(), we.getErrorCode(), we, we.getMessageArguments());
    }


    protected CoreException(MessageCodes rbe, String errorCode, Throwable thr, Object... args) {

        super();

        _errorObject = rbe;
        _errorCode = errorCode;

        _msgArgs = args == null ? new Object[0] : args;
        if (thr != null) {
            initCause(thr);
        }
    }


    public CoreException(CoreError we) {

        this(we.getErrorObject(), we.getErrorCode(), we, we.getMessageArguments());
    }


    public CoreException(String errorObj, Object messageArgument) {

        this(errorObj, new Object[]{messageArgument}, null);
    }


    public CoreException(String errorObj, Object messageArgument, Throwable thr) {

        this(errorObj, new Object[]{messageArgument}, thr);
    }


    public CoreException(String errorObj, Object[] messageArguments) {

        this(errorObj, messageArguments, null);
    }


    public CoreException(String errorObj, Object[] messageArguments, Throwable thr) {

        this(null, errorObj, thr, messageArguments);
    }


    public CoreException(String errorObj) {

        this(errorObj, new Object[]{});
    }


    public CoreException(String errorCode, Throwable e) {

        this(errorCode, new Object[]{e.getClass().getName(), e}, e);
    }


    public CoreException(MessageCodes errorObj, Object... messageArguments) {

        this(errorObj, null, messageArguments);
    }


    public CoreException(MessageCodes errorObj, Throwable thr, Object... messageArguments) {

        this(errorObj, null, thr, messageArguments);

        if (thr != null) {
            Object[] tmp = new Object[_msgArgs.length + 2];
            System.arraycopy(_msgArgs, 0, tmp, 0, _msgArgs.length);
            _msgArgs = tmp;
            _msgArgs[_msgArgs.length - 2] = thr.getClass().getName();
            _msgArgs[_msgArgs.length - 1] = thr;
        }
    }

    public static Exception suppress(Exception initial, Exception suppressed) {

        if (initial == null) {
            return suppressed;
        }

        initial.addSuppressed(suppressed);

        return initial;
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
            return null;
            //MessageManager.getInstance().format(_errorObject, loc, _msgArgs);
        }

        if (_errorCode == null) {
            return super.getMessage();
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
