package com.mikusher.parameter;

import com.mikusher.error.SatelliteException;

public interface ParameterMapSerializable {

    ParameterMap toParameterMap() throws SatelliteException;

    void fromParameterMap(ParameterMap pm) throws SatelliteException;

}
