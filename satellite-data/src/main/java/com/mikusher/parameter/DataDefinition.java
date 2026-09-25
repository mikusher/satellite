package com.mikusher.parameter;

/**
 * Defines allowed fields, types, required values and defaults for SatelliteData.
 *
 * <p>This is the Satellite 2.x name for the legacy ParameterInfoMap API.</p>
 */
public class DataDefinition extends ParameterInfoMap {

    public DataDefinition() {
        super();
    }

    public DataDefinition(DataDefinition definition) {
        super(definition);
    }

    public DataDefinition(ParameterInfoMap definition) {
        super(definition);
    }

    public DataDefinition(ParameterInfo[] fields) {
        super(fields);
    }

    public DataDefinition(String name, String description) {
        super(name, description);
    }

    public DataDefinition(String name,
                          String description,
                          ParameterInfo[] fields) {
        super(name, description, fields);
    }
}
