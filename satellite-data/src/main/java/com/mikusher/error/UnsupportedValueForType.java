package com.mikusher.error;

import com.mikusher.constants.Msg;
import com.mikusher.parameter.ParameterTypes;

public class UnsupportedValueForType extends CoreError {

    public UnsupportedValueForType(ParameterTypes type, Object value) {
        super(Msg.SAT_UT0004, type, String.valueOf(value));
    }
}
