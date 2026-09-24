package com.mikusher.parameter;

import com.mikusher.constants.Msg;
import com.mikusher.error.UnknownParameterException;

import java.util.*;

public class ParameterInfoMap {

    private final Map<String, ParameterInfo> _map;

    private String _name = "";
    private String _description = "";

    public ParameterInfoMap() {

        _map = new LinkedHashMap<>();
    }


    /***************************************************************************
     *
     *
     *
     ***************************************************************************/
    public ParameterInfoMap(ParameterInfoMap infoMap) {

        _map = new LinkedHashMap<>(infoMap._map);
    }


    /***************************************************************************
     *
     * Initializes the description from the <code>paramInfo</code> array
     * of <code>ParameterInfo </code>.
     *
     ***************************************************************************/
    public ParameterInfoMap(ParameterInfo[] paramInfo) {

        this();

        for (int i = paramInfo.length; (i--) >= 0; ) {
            add(paramInfo[i]);
        }
    }


    /***************************************************************************
     *
     * Initializes an empty description, providing a name and a description
     * for this <code>parameterInfoMap</code>.
     *
     ***************************************************************************/
    public ParameterInfoMap(String name, String description) {

        this();

        _name = name;
        _description = description;
    }


    /***************************************************************************
     *
     * Initializes the description from the <code>paramInfo</code> array
     * of <code>ParameterInfo</code>, providing a name and a description
     * for this <code>parameterInfoMap</code>.
     *
     ***************************************************************************/
    public ParameterInfoMap(String name, String description, ParameterInfo[] paramInfo) {

        this(name, description);

        for (int i = paramInfo.length; (i--) >= 0; ) {
            add(paramInfo[i]);
        }
    }


    /***************************************************************************
     *
     * Adds the description of a new parameter.
     *
     * @param paramInfo
     *            Information on the new parameter.
     *
     * @deprecated Use the <code>{@link #add(ParameterInfo)}</code>
     *             instead.
     *
     ***************************************************************************/
    @Deprecated
    public void addParameterInfo(ParameterInfo paramInfo) {

        add(paramInfo);
    }


    /***************************************************************************
     *
     *
     *
     ***************************************************************************/
    public void add(ParameterInfo paramInfo) {

        _map.put(paramInfo.getName(), paramInfo);
    }


    /***************************************************************************
     *
     * Adds the description of a new parameter.
     *
     * @param name
     *            The name of the parameter.
     *
     * @param type
     *            Identifier of the parameter type.
     *
     * @param description
     *            A short description.
     *
     * @param mandatory
     *            Signals if the parameter is mandatory.
     *
     * @param defaultValue
     *            The default value the parameter is initialized
     *            with inside a <code>ParameterMap</code>.
     *
     ***************************************************************************/
    public void add(String name, String type, String description, boolean mandatory, Object defaultValue) {

        add(new ParameterInfo(name, type, description, mandatory, defaultValue));
    }


    /***************************************************************************
     *
     * Adds the description of a new parameter.
     *
     * @param name
     *            The name of the parameter.
     *
     * @param type
     *            Identifier of the parameter type.
     *
     * @param description
     *            A short description.
     *
     * @param mandatory
     *            Signals if the parameter is mandatory.
     *
     * @param defaultValue
     *            The default value the parameter is initialized
     *            with inside a <code>ParameterMap</code>.
     *
     ***************************************************************************/
    public void add(String name, ParameterTypes type, String description, boolean mandatory, Object defaultValue) {

        add(new ParameterInfo(name, type, description, mandatory, defaultValue));
    }


    /***************************************************************************
     *
     * Adds the description of a new mandatory parameter of type "String".
     *
     ***************************************************************************/
    public void addString(String name, String description) {

        add(name, ParameterTypes.String, description, true, null);
    }


    /***************************************************************************
     *
     * Adds the description of a new optional parameter of type "String".
     *
     ***************************************************************************/
    public void addStringOpt(String name, String description, String defaultValue) {

        add(name, ParameterTypes.String, description, false, defaultValue);
    }


    /***************************************************************************
     *
     *
     ***************************************************************************/
    public void addInteger(String name, String description) {

        add(name, ParameterTypes.Integer, description, true, null);
    }


    /***************************************************************************
     *
     *
     ***************************************************************************/
    public void addIntegerOpt(String name, String description, int defaultValue) {

        addIntegerOpt(name, description, Integer.valueOf(defaultValue));
    }


    /***************************************************************************
     *
     *
     ***************************************************************************/
    public void addIntegerOpt(String name, String description, Integer defaultValue) {

        add(name, ParameterTypes.Integer, description, false, defaultValue);
    }


    /***************************************************************************
     *
     *
     ***************************************************************************/
    public void addFloat(String name, String description) {

        add(name, ParameterTypes.Float, description, true, null);
    }


    /***************************************************************************
     *
     *
     ***************************************************************************/
    public void addFloatOpt(String name, String description, float defaultValue) {

        addFloatOpt(name, description, Float.valueOf(defaultValue));
    }


    /***************************************************************************
     *
     *
     ***************************************************************************/
    public void addFloatOpt(String name, String description, Float defaultValue) {

        add(name, ParameterTypes.Float, description, false, defaultValue);
    }


    /***************************************************************************
     *
     *
     ***************************************************************************/
    public void addDouble(String name, String description) {

        add(name, ParameterTypes.Double, description, true, null);
    }


    /***************************************************************************
     *
     *
     ***************************************************************************/
    public void addDoubleOpt(String name, String description, double defaultValue) {

        addDoubleOpt(name, description, Double.valueOf(defaultValue));
    }


    /***************************************************************************
     *
     *
     ***************************************************************************/
    public void addDoubleOpt(String name, String description, Double defaultValue) {

        add(name, ParameterTypes.Double, description, false, defaultValue);
    }


    /***************************************************************************
     *
     *
     ***************************************************************************/
    public void addLong(String name, String description) {

        add(name, ParameterTypes.Long, description, true, null);
    }


    /***************************************************************************
     *
     *
     ***************************************************************************/
    public void addLongOpt(String name, String description, long defaultValue) {

        addLongOpt(name, description, Long.valueOf(defaultValue));
    }


    /***************************************************************************
     *
     *
     ***************************************************************************/
    public void addLongOpt(String name, String description, Long defaultValue) {

        add(name, ParameterTypes.Long, description, false, defaultValue);
    }


    /***************************************************************************
     *
     *
     ***************************************************************************/
    public void addBoolean(String name, String description) {

        add(name, ParameterTypes.Boolean, description, true, null);
    }


    /***************************************************************************
     *
     *
     ***************************************************************************/
    public void addBooleanOpt(String name, String description, boolean defaultValue) {

        addBooleanOpt(name, description, Boolean.valueOf(defaultValue));
    }


    /***************************************************************************
     *
     *
     ***************************************************************************/
    public void addBooleanOpt(String name, String description, Boolean defaultValue) {

        add(name, ParameterTypes.Boolean, description, false, defaultValue);
    }


    /***************************************************************************
     *
     *
     ***************************************************************************/
    public void addMap(String name, String description) {

        add(name, ParameterTypes.Map, description, true, null);
    }


    /***************************************************************************
     *
     *
     ***************************************************************************/
    public void addMapOpt(String name, String description, ParameterMap defaultValue) {

        add(name, ParameterTypes.Map, description, false, defaultValue);
    }


    /***************************************************************************
     *
     *
     ***************************************************************************/
    public void addArray(String name, String description) {

        add(name, ParameterTypes.Array, description, true, null);
    }


    /***************************************************************************
     *
     *
     ***************************************************************************/
    public void addArrayOpt(String name, String description, List<?> defaultValue) {

        add(name, ParameterTypes.Array, description, false, defaultValue);
    }


    /***************************************************************************
     *
     *
     ***************************************************************************/
    public void addDate(String name, String description) {

        add(name, ParameterTypes.Date, description, true, null);
    }


    /***************************************************************************
     *
     *
     ***************************************************************************/
    public void addDateOpt(String name, String description, Date defaultValue) {

        add(name, ParameterTypes.Date, description, false, defaultValue);
    }


    /***************************************************************************
     *
     * Fetches information on a parameter in this set.
     *
     * @param paramName
     *            The identifier of a previously registered
     *            parameter.
     *
     * @return The <code>ParameterInfo</code> corresponding to the
     *         parameter named <code>paramName</code>.
     *
     * @exception UnknownParameterException
     *                Thrown if no parameter named
     *                <code>paramName</code> has been previously registered.
     *
     ***************************************************************************/
    public ParameterInfo getParameterInfo(String paramName) throws UnknownParameterException {

        ParameterInfo info = get(paramName);
        if (info == null) {
            throw new UnknownParameterException(Msg.SAT_UT0001, paramName);
        }

        return info;
    }


    /***************************************************************************
     *
     * Retrieves the <code>ParameterInfo</code> for the specified parameter,
     * or <code>null</code> if none exists.
     *
     * @param paramName
     *            The identifier of a previously registered parameter.
     *
     * @return The <code>ParameterInfo</code> corresponding to the parameter
     *         named <code>paramName</code>, or <code>null</code>.
     *
     ***************************************************************************/
    ParameterInfo get(String paramName) {

        return _map.get(paramName);
    }


    /***************************************************************************
     *
     * Fetches an <code>Iterator</code> containing all the
     * <code>ParameterInfo</code> stored in this object.
     *
     ***************************************************************************/
    public Iterator<String> list() {

        return _map.keySet().iterator();
    }


    /***************************************************************************
     *
     * Fetches an <code>Iterator</code> containing all the
     * <code>ParameterInfo</code> instances stored in this object.
     *
     * @return An <code>Iterator</code> containing <the code>{@link ParameterInfo}</code> managed by this
     *         <code>ParameterInfoMap</code>.
     *
     ***************************************************************************/
    public Iterator<ParameterInfo> iterator() {

        return _map.values().iterator();
    }


    /***************************************************************************
     *
     * Checks if this set contains the named parameter.
     *
     * @param paramName
     *            The name of the parameter being checked.
     *
     * @return True if there is a parameter named <code>paramName</code>
     *         in this set.
     *
     ***************************************************************************/
    public boolean containsParameter(String paramName) {

        return _map.containsKey(paramName);
    }

    /***************************************************************************
     *
     *
     ***************************************************************************/
    public String getName() {

        return _name;
    }

    /***************************************************************************
     *
     *
     ***************************************************************************/
    public void setName(String name) {

        _name = name;
    }

    /***************************************************************************
     *
     *
     ***************************************************************************/
    public String getDescription() {

        return _description;
    }

    /***************************************************************************
     *
     *
     ***************************************************************************/
    public void setDescription(String description) {

        _description = description;
    }


}
