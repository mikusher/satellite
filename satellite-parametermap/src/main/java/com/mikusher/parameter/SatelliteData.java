package com.mikusher.parameter;

import com.mikusher.error.IncorrectTypeException;
import com.mikusher.error.SatelliteException;
import com.mikusher.error.UnknownParameterException;

import java.util.Map;

/**
 * Primary dynamic-data API for Satellite 2.x.
 *
 * <p>This class preserves the mature ParameterMap behavior while giving the
 * API a product-level name that is not tied to request parameters.</p>
 */
public class SatelliteData extends ParameterMap {

    public SatelliteData() {
        super();
    }

    public SatelliteData(int initialCapacity) {
        super(initialCapacity);
    }

    public SatelliteData(DataDefinition dataDefinition) {
        super(dataDefinition);
    }

    /**
     * @deprecated Use {@link #SatelliteData(DataDefinition)}.
     */
    @Deprecated
    public SatelliteData(ParameterInfoMap dataDefinition) {
        super(dataDefinition);
    }

    @SuppressWarnings("rawtypes")
    public SatelliteData(Map map) {
        super(map);
    }

    public SatelliteData(Map<String, Object> map, boolean copyInputMap) {
        super(map, copyInputMap);
    }

    public SatelliteData(Map<String, Object> map,
                         DataDefinition dataDefinition) throws SatelliteException {
        super(map, dataDefinition);
    }

    /**
     * @deprecated Use {@link #SatelliteData(Map, DataDefinition)}.
     */
    @Deprecated
    public SatelliteData(Map<String, Object> map,
                         ParameterInfoMap dataDefinition) throws SatelliteException {
        super(map, dataDefinition);
    }

    @Override
    public SatelliteData clone() {
        return (SatelliteData) super.clone();
    }

    /**
     * Returns a nested value using the Satellite 2.x name.
     *
     * <p>Legacy nested ParameterMap instances are exposed as a SatelliteData
     * view over the same backing map, so migration does not require copying
     * the nested content.</p>
     */
    public SatelliteData getData(String name)
            throws UnknownParameterException, IncorrectTypeException {
        ParameterMap nested = super.getMap(name);
        if (nested instanceof SatelliteData) {
            return (SatelliteData) nested;
        }

        SatelliteData view = new SatelliteData(nested._params, false);
        view._paramInfoMap = nested._paramInfoMap;
        return view;
    }

    public static SatelliteData merge(SatelliteData first,
                                      SatelliteData second) throws SatelliteException {
        ParameterMap merged = ParameterMap.merge(first, second);
        return merged == null ? null : (SatelliteData) merged;
    }
}
