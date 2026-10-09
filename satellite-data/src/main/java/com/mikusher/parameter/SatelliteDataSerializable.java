package com.mikusher.parameter;

import com.mikusher.error.SatelliteException;

public interface SatelliteDataSerializable {

    SatelliteData toSatelliteData() throws SatelliteException;

    void fromSatelliteData(SatelliteData pm) throws SatelliteException;

}
