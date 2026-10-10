package com.mikusher.parameter;


import com.mikusher.constants.Msg;
import com.mikusher.error.IncorrectTypeException;
import com.mikusher.error.SatelliteError;
import com.mikusher.error.SatelliteException;
import com.mikusher.error.UnknownParameterException;
import com.mikusher.utils.CloneableEntry;
import com.mikusher.utils.DataMap;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.math.BigDecimal;
import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Function;

public class SatelliteData implements DataMap {

    protected static final char SIMPLE = 'S';
    protected static final char MAP = 'M';
    protected static final char LIST = 'L';

    protected static final Object NOT_FOUND = new Object();

    protected Map<String, Object> valuesByName = null;
    protected DataDefinition definition = null;


    /***************************************************************************
     *
     * Initializes the object with no parameters. This <code>SatelliteData</code>
     * will not be restrained by a <code>{@link DataDefinition}</code> and can
     * store any parameter regardless of its name.
     *
     ***************************************************************************/
    public SatelliteData() {

        this(4);
    }


    public SatelliteData(int initialCapacity) {

        valuesByName = new HashMap<>(initialCapacity);
        definition = null;
    }


    public SatelliteData(DataDefinition constraints) {

        valuesByName = new HashMap<>();
        definition = constraints;

        reset();
    }


    /***************************************************************************
     *
     * Initializes this <code>SatelliteData</code> to use an external
     * <code>java.util.Map</code> as repository. No constraints will be
     * associated with this map.
     *
     * @param map
     *            A string-keyed map used as the backing store; mutations are shared.
     *
     ***************************************************************************/
    public SatelliteData(Map<String, Object> map) {
        this(map, false);
    }


    /***************************************************************************
     *
     * Initializes this <code>SatelliteData</code> to use an external
     * <code>java.util.Map</code> as repository. No constraints will be
     * associated with this map.
     *
     * @param map
     *            An external map used to store the elements.
     *
     * @param copyInputMap
     *            Whether to copy the map provided as input
     *
     ***************************************************************************/
    public SatelliteData(Map<String, Object> map, boolean copyInputMap) {

        if (copyInputMap) {
            valuesByName = new HashMap<>(map);
        } else {
            valuesByName = map;
        }
        definition = null;
    }


    /***************************************************************************
     *
     * Initializes this <code>SatelliteData</code> to use an external
     * <code>java.util.Map</code> as repository. The set of permissible entries
     * will be constrained by the <code>constraints</code> <code> {@link DataDefinition}</code>.
     *
     * <p>If a mandatory parameter is not present in <code>map</code> then a
     * <code>JafException</code> is thrown. If an optional parameter is missing
     * then one is created having the default value.</p>
     *
     * @param map
     *            An external map used to store the elements.
     *
     * @param constraints
     *            The set of constraints on the elements this
     *            <code>SatelliteData</code> may contain. If null there will be
     *            no constraint on the parameters.
     *
     * @exception SatelliteException
     *                Thrown when the siplied <code>map</code> does not contain
     *                a mandatory parameter as specified by
     *                <code>constraints</code>.
     *
     ***************************************************************************/
    public SatelliteData(Map<String, Object> map, DataDefinition constraints) throws SatelliteException {

        valuesByName = map;
        setConstraints(constraints);
    }

    protected static Object cloneObject(Object value) {

        if (value == null) {
            return null;
        } else if (value instanceof CloneableEntry) {
            return ((CloneableEntry) value).clone();
        } else if (value instanceof List) {
            return cloneArray((List) value);
        } else if (value instanceof Date) {
            return ((Date) value).clone();
        } else {
            return value;
        }
    }

    /***************************************************************************
     *
     * This method is responsiblefor cloning a parameter of type Array.
     *
     * @param array
     *            The <code>List</code> object to be cloned.
     *
     * @return List The <code>List</code> instance cloned from the argument
     *         object.
     *
     * @exception UnknownParameterException
     *
     **************************************************************************/
    static List<?> cloneArray(List<?> array) {
        List<Object> cloned = new ArrayList<>(array.size());
        for (Object value : array) {
            cloned.add(cloneObject(value));
        }
        return cloned;
    }

    /***************************************************************************
     *
     *
     *
     ***************************************************************************/
    /***************************************************************************
     *
     *
     *
     ***************************************************************************/
    public static SatelliteData merge(SatelliteData map1, SatelliteData map2) throws SatelliteException {

        if (map1 == null) {
            return map2;
        } else if (map2 == null) {
            return map1;
        }

        for (Map.Entry<String, Object> entry : map1.valuesByName.entrySet()) {

            Object mapObj1 = entry.getValue();
            Object mapObj2 = map2.getParameterNoCheck(entry.getKey());

            if (mapObj2 == null) {
                continue;
            }

            switch (getType(mapObj1)) {
                case SIMPLE:
                    break;
                case MAP:
                    if (getType(mapObj2) == MAP) {
                        merge((SatelliteData) mapObj1, (SatelliteData) mapObj2);
                    }
                    break;
                case LIST:
                    if (getType(mapObj2) == LIST) {
                        listMerge((List<Object>) mapObj1, (List<Object>) mapObj2);
                    }
                    break;
                default:
                    break;
            }
        }

        for (Map.Entry<String, Object> entry : map2.valuesByName.entrySet()) {
            final String paramName = entry.getKey();
            if (!map1.containsKey(paramName)) {
                map1.setParameter(paramName, entry.getValue());
            }
        }

        return map1;
    }

    /***************************************************************************
     *
     *
     *
     ***************************************************************************/
    protected static List<Object> listMerge(List<Object> list1, List<Object> list2) throws SatelliteException {

        final int lstSize1 = list1.size();
        final int lstSize2 = list2.size();
        final int minSize = Math.min(lstSize1, lstSize2);
        final ListIterator<Object> it1 = list1.listIterator(minSize);
        final ListIterator<Object> it2 = list2.listIterator(minSize);

        while (it1.hasPrevious()) {
            Object lstObj1 = it1.previous();
            Object lstObj2 = it2.previous();

            switch (getType(lstObj1)) {
                case SIMPLE:
                    break;
                case MAP:
                    if (getType(lstObj2) == MAP) {
                        ((SatelliteData) lstObj1).merge((SatelliteData) lstObj2);
                    }
                    break;
                case LIST:
                    if (getType(lstObj2) == LIST) {
                        listMerge((List<Object>) lstObj1, (List<Object>) lstObj2);
                    }
                    break;
                default:
                    break;
            }
        }

        if (lstSize2 > lstSize1) {
            final ListIterator<Object> it = list2.listIterator(Math.max(0, lstSize1 - 1));
            while (it.hasNext()) {
                list1.add(it.next());
            }
        }

        return list1;
    }

    /***************************************************************************
     *
     *
     *
     ***************************************************************************/
    protected static char getType(Object obj) {

        if (obj instanceof SatelliteData) {
            return MAP;
        } else if (obj instanceof List) {
            return LIST;
        } else {
            return SIMPLE;
        }
    }

    /***************************************************************************
     *
     * Specifies the constraints for this <code>SatelliteData</code>. The
     * <code>constraints</code> constraints specify the keys and associated
     * values that may be stored in this map.
     *
     * <p>If a mandatory parameter is not present then a
     * <code>JafException</code> is thrown. If an optional parameter is missing
     * then one is created having the default value.</p>
     *
     * @param constraints
     *            The set of new constraints for this map.
     *
     ***************************************************************************/
    public void setConstraints(DataDefinition constraints) throws SatelliteException {

        definition = constraints;
        if (definition == null) {
            return;
        }

        for (Iterator<ParameterInfo> it = definition.iterator(); it.hasNext(); ) {
            ParameterInfo paramInfo = it.next();
            String paramName = paramInfo.getName();

            Object value = valuesByName.getOrDefault(paramName, NOT_FOUND);
            if (value != NOT_FOUND) {
                Object newValue = paramInfo.getParameterType().cast(value);
                if (value != newValue) {
                    valuesByName.put(paramName, newValue);
                }
            } else {
                if (paramInfo.isMandatory()) {
                    throw new UnknownParameterException(paramName);
                }

                valuesByName.put(paramName, paramInfo.getDefaultValue());
            }
        }
    }

    /***************************************************************************
     *
     * Specifies the constraints for this <code>SatelliteData</code> but does not
     * validate its current contents. Validation is delayed until one of the
     * <code>getXxx(...)</code> methods is invoked.
     *
     * @param constraints
     *            The new set of constraints for this map.
     *
     ***************************************************************************/
    public void assignConstraints(DataDefinition constraints) {

        definition = constraints;
    }

    /***************************************************************************
     *
     * All elements are reset to their respective default values. Mandatory
     * elements are not created. That means they must be explicitly set before
     * their values can be fetched with calls to <code>{@link #get(Object)} </code>.
     *
     ***************************************************************************/
    private void reset() {

        valuesByName.clear();
        if (definition == null) {
            return;
        }

        for (Iterator<ParameterInfo> i = definition.iterator(); i.hasNext(); ) {
            ParameterInfo paramInfo = i.next();
            if (!paramInfo.isMandatory()) {
                String name = paramInfo.getName();
                Object defVal = paramInfo.getDefaultValue();
                valuesByName.put(name, defVal);
            }
        }
    }

    /***************************************************************************
     *
     * This method returns a clone of the object. Implementation of the
     * CloneableEntry interface.
     *
     * @return The clone of the object
     *
     **************************************************************************/
    @Override
    public SatelliteData clone() {

        final SatelliteData cloned;
        try {
            cloned = this.getClass().getDeclaredConstructor().newInstance();
        } catch (Exception ex) {
            throw new SatelliteError(ex.toString(), ex);
        }

        for (Map.Entry<String, Object> entry : valuesByName.entrySet()) {
            cloned.put(entry.getKey(), cloneObject(entry.getValue()));
        }

        return cloned;
    }

    @Override
    public Object getParameter(String paramName) throws UnknownParameterException {

        Object value = getParameterOrNotFound(paramName);
        if (value != NOT_FOUND) {
            return value;
        }
        throw new UnknownParameterException(Msg.SAT_UT0001, paramName);
    }

    private Object getParameterOrNotFound(String paramName) {

        Entry entry = internalGet(paramName);
        if (entry != null) {
            return entry.getValue();
        }

        if (definition == null) {
            return NOT_FOUND;
        }

        ParameterInfo paramInfo = definition.get(paramName);
        return paramInfo == null || paramInfo.isMandatory() ? NOT_FOUND : paramInfo.getDefaultValue();
    }

    /***************************************************************************
     *
     *
     *
     ***************************************************************************/
    private Entry internalGet(String paramName) {
        if (paramName == null) {
            return null;
        }

        Object value = valuesByName.getOrDefault(paramName, NOT_FOUND);
        if (value != NOT_FOUND) {
            return new Entry(value);
        }

        char[] buffer = paramName.toCharArray();
        if (buffer.length < 3 || buffer[0] == '.' || buffer[0] == '('
                || buffer[buffer.length - 1] == '.'
                || buffer[buffer.length - 1] == '(') {
            return null;
        }

        Object holder = this;
        int last = 0;
        boolean inArray = false;
        boolean afterArray = false;

        try {
            for (int i = 1; i < buffer.length; i++) {
                switch (buffer[i]) {
                    case '(':
                    case '.':
                        if (!afterArray && (inArray || last == i)) {
                            return null;
                        }
                        if (!afterArray) {
                            if (!(holder instanceof SatelliteData)) {
                                return null;
                            }
                            holder = ((SatelliteData) holder).valuesByName.get(paramName.substring(last, i));
                        }
                        last = i + 1;
                        afterArray = false;
                        inArray = (buffer[i] == '(');
                        break;
                    case ')':
                        if (!inArray || last == i || !(holder instanceof List)) {
                            return null;
                        }
                        int index = Integer.parseInt(paramName.substring(last, i));
                        holder = ((List<?>) holder).get(index);
                        last = i + 1;
                        inArray = false;
                        afterArray = true;
                        break;
                    default:
                        if (afterArray) {
                            return null;
                        }
                }
            }

            if (inArray || last == 0) {
                return null;
            }

            if (last == buffer.length) {
                return new Entry(holder);
            }
            if (!(holder instanceof SatelliteData)) {
                return null;
            }

            value = ((SatelliteData) holder).valuesByName.getOrDefault(
                    paramName.substring(last), NOT_FOUND);
            return value == NOT_FOUND ? null : new Entry(value);
        } catch (NumberFormatException | IndexOutOfBoundsException invalidPath) {
            return null;
        }
    }

    /***************************************************************************
     *
     * Fetches a parameter value. If parameter identified by
     * <code>paramName</code> has never been set then a null is returned. A
     * parameter may have been set explicitly by calling <code> {@link #setParameter(String, Object)}</code> or implicitly in the
     * constructor when the paremeter value was initialized with a default value
     * given by the <code>{@link DataDefinition}</code> passed to the
     * constructor.
     *
     * @param paramName
     *            The name of the parameter whose value if being fetched.
     *
     * @return The value of the parameter or null if it has not been set.
     *
     ***************************************************************************/
    public Object getParameterNoCheck(String paramName) {

        return valuesByName.get(paramName);
    }

    /***************************************************************************
     *
     * Sets the value of a parameter. If this <code>SatelliteData</code> is
     * constrained by a <code>{@link DataDefinition}</code> then
     * <code>paramName</code> must refer to an existing parameter otherwise a
     * <code>UnknownParameterException</code> will be thrown.
     *
     * <p>If this <code>SatelliteData</code> is not constrained by a <code> {@link DataDefinition}</code> then the operation will succeed.</p>
     *
     * @param paramName
     *            The name of the parameter being set.
     *
     * @param paramValue
     *            The new value of the parameter.
     *
     *                Thrown if this <code>SatelliteData</code> is constrained by
     *                a <code>{@link DataDefinition}</code> and it does not
     *                contain the parameter named <code>paramName</code>.
     *
     ***************************************************************************/
    @Override
    public void setParameter(String paramName, Object paramValue) throws UnknownParameterException {

        if (definition != null && !definition.containsParameter(paramName)) {
            throw new UnknownParameterException(paramName);
        }

        Object value = valuesByName.getOrDefault(paramName, NOT_FOUND);
        if (value != NOT_FOUND) {
            valuesByName.put(paramName, paramValue);
            return;
        }

        char[] buffer = paramName.toCharArray();
        // if name can't be a valid nested expression, use the literal name directly
        if (buffer.length < 3 || buffer[0] == '.' || buffer[0] == '(' || buffer[buffer.length - 1] == '.' || buffer[buffer.length - 1] == '(') {
            valuesByName.put(paramName, paramValue);
            return;
        }

        Object holder = this;
        int last = 0;
        boolean inArrayIndex = false;
        boolean outArr = false;
        try {
            for (int i = 1; i < buffer.length; i++) {
                switch (buffer[i]) {
                    case '(':
                    case '.':
                        if (!outArr && (inArrayIndex || last == i)) {
                            throw new UnknownParameterException(paramName);
                        }

                        if (!outArr) {
                            if (!(holder instanceof SatelliteData)) {
                                throw new UnknownParameterException(paramName);
                            }
                            holder = ((SatelliteData) holder).valuesByName.get(paramName.substring(last, i));
                        }
                        last = i + 1;
                        outArr = false;
                        inArrayIndex = (buffer[i] == '(');
                        break;
                    case ')':

                        // If we were not in an array index, or the arrayIndex is empty the accessor
                        // is invalid so just use the literal value
                        if (!inArrayIndex || last == i) {
                            throw new UnknownParameterException(paramName);
                        }

                        if (!(holder instanceof List)) {
                            throw new UnknownParameterException(paramName);
                        }
                        int arrayIndexValue = Integer.parseInt(paramName.substring(last, i));

                        // if we're are the end of the string set the value, either by adding the new position
                        // to the end of the array, or replacing the existing value for that index.
                        if (i == buffer.length - 1) {
                            if (arrayIndexValue == ((List) holder).size()) {
                                ((List) holder).add(paramValue);
                            } else
                                ((List) holder).set(arrayIndexValue, paramValue);

                            return;
                        }

                        // Save the current position, for the next iteration
                        holder = ((List) holder).get(arrayIndexValue);
                        last = i + 1;
                        inArrayIndex = false;
                        outArr = true;
                        break;
                    default:
                        if (outArr) {
                            throw new UnknownParameterException(paramName);
                        }
                }
            }

            if (inArrayIndex || last == 0) {
                valuesByName.put(paramName, paramValue);
                return;
            }

            // put it in the structure
            if (holder != null) {
                ((SatelliteData) holder).valuesByName.put(paramName.substring(last), paramValue);
            } else {
                valuesByName.put(paramName, paramValue);
            }
        } catch (UnknownParameterException | ClassCastException
                 | NumberFormatException | IndexOutOfBoundsException invalidPath) {
            // Invalid paths remain literal keys; unrelated mutation failures propagate.
            valuesByName.put(paramName, paramValue);
        }
    }



    /***************************************************************************
     *
     * Checks if there is an entry identified by the specified name.
     *
     * @param paramName
     *            The name of the entry to check for existence.
     *
     * @return True if there is an entry identified by <code>paramName</code>.
     *         False otherwise.
     *
     ***************************************************************************/
    @Override
    public boolean containsKey(Object paramName) {

        String param = (String) paramName;
        Entry entry = internalGet(param);
        if (entry != null) {
            return true;
        }

        // no tree involved check for default values
        if (definition != null && definition.containsParameter(param)) {
            try {
                ParameterInfo paramInfo = definition.getParameterInfo(param);
                if (paramInfo.isMandatory()) {
                    return false;
                }
            } catch (UnknownParameterException e) {
            }

            return true;
        }

        return false;
    }

    /***************************************************************************
     *
     * Retrieves the names of all currently stored parameters.
     *
     * @return An <code>Iterator</code> containing strings representing the
     *         names of the stored parameters. There are no guarantees on the
     *         order the names are retrieved by the <code>Iterator</code>.
     *
     ***************************************************************************/
    @Override
    public Iterator<String> getParameterNames() {

        return valuesByName.keySet().iterator();
    }

    /***************************************************************************
     *
     * Fetches the value of one of this map elements as a <code>String</code>.
     * If the element identified by the <code>paramName</code> key is not a
     *
     * @param paramName
     *            The key associated with the value to retrieve.
     *
     * @return The <code>String</code> object which is the value associated with
     *         the <code>paramName</code> key.
     *
     * @exception UnknownParameterException
     *                Thrown if there is no element having
     *                <code>paramName</code> as key.
     *
     * @exception IncorrectTypeException
     *                Thrown if the value associated with the
     *                <code>paramName</code> key is not a string object.
     *
     ***************************************************************************/
    @Override
    public String getString(String paramName) throws UnknownParameterException, IncorrectTypeException {

        return getTypedParameter(ParameterTypes.String, paramName);
    }

    /***************************************************************************
     *
     * Fetches the value of one of this map elements as a <code>String</code>.
     * If the element identified by the <code>paramName</code> key is not a
     * <code>java.lang.String</code> then an <code> {@link IncorrectTypeException}</code> will be thrown.
     *
     * @param paramName
     *            The key associated with the value to retrieve.
     * @param defaultValue
     *            The default value if the key is not defined
     *
     * @return The <code>String</code> object which is the value associated with
     *         the <code>paramName</code> key or defaultValue if paramName does not exist.
     *
     * @exception IncorrectTypeException
     *                Thrown if the value associated with the
     *                <code>paramName</code> key is not a string object.
     *
     ***************************************************************************/
    public String getStringOrDefault(String paramName, String defaultValue) throws IncorrectTypeException {

        return getOrDefaultTypedParameter(ParameterTypes.String, paramName, defaultValue);
    }



    /***************************************************************************
     *
     * Checks whether or not a given value is null
     *
     * @param paramName
     *            The key associated with the value to retrieve.
     *
     * @return The <code>String</code> object which is the value associated with
     *         the <code>paramName</code> key.
     *
     * @exception UnknownParameterException
     *                Thrown if there is no element having
     *                <code>paramName</code> as key. *
     ***************************************************************************/
    @Override
    public boolean isNull(String paramName) throws UnknownParameterException {

        return getParameter(paramName) == null;
    }

    /***************************************************************************
     *
     * Sets the value of a parameter. If this map is constrained and none of its
     * keys may be <code>paramName</code> then an <code> {@link UnknownParameterException}</code> is thrown.
     *
     * @param paramName
     *            The name of the parameter to change.
     *
     * @param paramValue
     *            The new value to assign to the parameter.
     *
     * @exception UnknownParameterException
     *                Thrown if this map is constrained and it has no parameter
     *                named <code>paramName</code>.
     *
     ***************************************************************************/
    @Override
    public void setString(String paramName, String paramValue) throws UnknownParameterException {

        setParameter(paramName, paramValue);
    }

    /***************************************************************************
     *
     * Fetches the value of one of this map elements as an integer value. If the
     * element identified by <code>paramName</code> is not a
     * <code>java.lang.Integer</code> then an <code> {@link IncorrectTypeException}</code> will be thrown.
     *
     * @param paramName
     *            The key associated with the value to retrieve.
     *
     * @return The integer value which is the value associated with the
     *         <code>paramName</code> key.
     *
     * @exception UnknownParameterException
     *                Thrown if there is no element having
     *                <code>paramName</code> as key.
     *
     * @exception IncorrectTypeException
     *                Thrown if the value associated with the
     *                <code>paramName</code> key is not an int value or null
     *                value.
     *
     ***************************************************************************/
    @Override
    public int getInt(String paramName) throws UnknownParameterException, IncorrectTypeException {

        return getTypedParameter(ParameterTypes.Integer, paramName);
    }

    /***************************************************************************
     *
     * Fetches the value of one of this map elements as an integer value. If the
     * element identified by <code>paramName</code> is not a
     * <code>java.lang.Integer</code> then an <code> {@link IncorrectTypeException}</code> will be thrown.
     *
     * @param paramName
     *            The key associated with the value to retrieve.
     * @param defaultValue
     *            The default value if the key is not defined
     *
     * @return The <code>Integer</code> object which is the value associated with
     *         the <code>paramName</code> key or defaultValue if paramName does not exist.
     *
     * @exception IncorrectTypeException
     *                Thrown if the value associated with the
     *                <code>paramName</code> key is not a string object.
     *
     ***************************************************************************/
    public int getIntOrDefault(String paramName, int defaultValue) throws IncorrectTypeException {

        return getOrDefaultTypedParameter(ParameterTypes.Integer, paramName, defaultValue);
    }



    /***************************************************************************
     *
     * Sets the value of a parameter. If this map is constrained and none of its
     * keys may be <code>paramName</code> then an <code> {@link UnknownParameterException}</code> is thrown.
     *
     * @param paramName
     *            The name of the parameter to change.
     *
     * @param paramValue
     *            The new value to assign to the parameter.
     *
     * @exception UnknownParameterException
     *                Thrown if this map is constrained and it has no parameter
     *                named <code>paramName</code>.
     *
     ***************************************************************************/
    @Override
    public void setInt(String paramName, int paramValue) throws UnknownParameterException {

        setParameter(paramName, paramValue);
    }

    /***************************************************************************
     *
     * Fetches the value of one of this map elements as a long value. If the
     * element identified by the <code>paramName</code> key is not a
     * <code>java.lang.Long</code> then an <code>{@link IncorrectTypeException} </code> will be thrown.
     *
     * @param paramName
     *            The key associated with the value to retrieve.
     *
     * @return The <code>String</code> object which is the value associeted with
     *         the <code>paramName</code> key.
     *
     * @exception UnknownParameterException
     *                Thrown if there is no element having
     *                <code>paramName</code> as key.
     *
     * @exception IncorrectTypeException
     *                Thrown if the value associated with the
     *                <code>paramName</code> key is not a long value or null
     *                value.
     *
     ***************************************************************************/

    @Override
    public long getLong(String paramName) throws UnknownParameterException, IncorrectTypeException {

        return getTypedParameter(ParameterTypes.Long, paramName);
    }

    /***************************************************************************
     *
     * Fetches the value of one of this map elements as an integer value. If the
     * element identified by <code>paramName</code> is not a
     * <code>java.lang.Long</code> then an <code> {@link IncorrectTypeException}</code> will be thrown.
     *
     * @param paramName
     *            The key associated with the value to retrieve.
     * @param defaultValue
     *            The default value if the key is not defined
     *
     * @return The <code>Long</code> object which is the value associated with
     *         the <code>paramName</code> key or defaultValue if paramName does not exist.
     *
     * @exception IncorrectTypeException
     *                Thrown if the value associated with the
     *                <code>paramName</code> key is not a string object.
     *
     ***************************************************************************/
    public long getLongOrDefault(String paramName, long defaultValue) throws IncorrectTypeException {

        return getOrDefaultTypedParameter(ParameterTypes.Long, paramName, defaultValue);
    }



    /***************************************************************************
     *
     * Fetches the value of one of this map elements as a <code>T</code> value, for the specified <code>type</code>
     * parameter.
     *
     * If the element identified by the <code>paramName</code> key is not of the specified <code>type</code> then an
     * <code>{@link IncorrectTypeException} </code> will be thrown.
     *
     * @param type
     *            Type of the retrieved parameter.
     *
     * @param paramName
     *            The key associated with the value to retrieve.
     *
     * @param <T>
     *            Class type of the specified type parameter for which the returned value will be cast.
     *
     * @return The <code>T</code> object which is the value associated with the <code>paramName</code> key.
     *
     * @throws UnknownParameterException
     *             Thrown if there is no element having <code>paramName</code> as key.
     *
     * @throws IncorrectTypeException
     *             Thrown if the value associated with the <code>paramName</code> key is not a <code>T</code> value
     *             or null value.
     *
     * @throws ClassCastException
     *             If the parameter value is not assignable to the return type <code>T</code>.
     *
     ***************************************************************************/
    @SuppressWarnings("unchecked")
    public <T> T getTypedParameter(ParameterTypes type, String paramName)
            throws UnknownParameterException, IncorrectTypeException {

        return (T) type.cast(getParameter(paramName));
    }

    /***************************************************************************
     *
     * Fetches the value of one of this map elements as a <code>T</code> value, for the specified <code>type</code>
     * parameter.
     * If no element exists for the specified <code>paramName</code> the <code>defaultValue</code> will be
     * returned instead.
     *
     * If the element identified by the <code>paramName</code> key is not of the specified <code>type</code> then an
     * <code>{@link IncorrectTypeException} </code> will be thrown.
     *
     * @param type
     *            Type of the retrieved parameter.
     *
     * @param paramName
     *            The key associated with the value to retrieve.
     *
     * @param defaultValue
     *            The default <code>T</code> value to return.
     *
     * @param <T>
     *            Class type of the specified type parameter for which the returned value will be cast.
     *
     * @return The <code>T</code> object which is the value associated with the <code>paramName</code> key; or the
     *         <code>defaultValue</code>.
     *
     * @throws IncorrectTypeException
     *             Thrown if the value associated with the <code>paramName</code> key is not a <code>T</code> value
     *             or null value.
     *
     * @throws ClassCastException
     *             If the parameter value is not assignable to the return type <code>T</code>.
     *
     ***************************************************************************/
    @SuppressWarnings("unchecked")
    public <T> T getOrDefaultTypedParameter(ParameterTypes type, String paramName, T defaultValue)
            throws IncorrectTypeException {

        Object obj = getParameterOrNotFound(paramName);
        return obj == NOT_FOUND ? defaultValue : (T) type.cast(obj);
    }

    /***************************************************************************
     *
     * Fetches the value of one of this map elements as a long value. If the
     * element identified by the <code>paramName</code> key is not a
     * <code>java.lang.BigDecimal</code> then an <code> {@link IncorrectTypeException} </code> will be thrown.
     *
     * @param paramName
     *            The key associated with the value to retrieve.
     *
     * @return The <code>String</code> object which is the value associeted with
     *         the <code>paramName</code> key.
     *
     * @exception UnknownParameterException
     *                Thrown if there is no element having
     *                <code>paramName</code> as key.
     *
     * @exception IncorrectTypeException
     *                Thrown if the value associated with the
     *                <code>paramName</code> key is not a long value or null
     *                value.
     *
     ***************************************************************************/
    @Override
    public BigDecimal getDecimal(String paramName) throws UnknownParameterException, IncorrectTypeException {

        return getTypedParameter(ParameterTypes.Decimal, paramName);
    }

    /***************************************************************************
     *
     * Fetches the value of one of this map elements as a decimal value. If the
     * element identified by <code>paramName</code> is not a
     * <code>java.lang.Decimal</code> then an <code> {@link IncorrectTypeException}</code> will be thrown.
     *
     * @param paramName
     *            The key associated with the value to retrieve.
     * @param defaultValue
     *            The default value if the key is not defined
     *
     * @return The <code>Decimal</code> object which is the value associated with
     *         the <code>paramName</code> key or defaultValue if paramName does not exist.
     *
     * @exception IncorrectTypeException
     *                Thrown if the value associated with the
     *                <code>paramName</code> key is not a string object.
     *
     ***************************************************************************/
    public BigDecimal getDecimalOrDefault(String paramName, BigDecimal defaultValue) throws IncorrectTypeException {

        return getOrDefaultTypedParameter(ParameterTypes.Decimal, paramName, defaultValue);
    }



    /***************************************************************************
     *
     * Sets the value of a parameter. If this map is constrained and none of its
     * keys may be <code>paramName</code> then an <code> {@link UnknownParameterException}</code> is thrown.
     *
     * @param paramName
     *            The name of the parameter to change.
     *
     * @param paramValue
     *            The new value to assign to the parameter.
     *
     * @exception UnknownParameterException
     *                Thrown if this map is constrained and it has no parameter
     *                named <code>paramName</code>.
     *
     ***************************************************************************/
    @Override
    public void setLong(String paramName, long paramValue) throws UnknownParameterException {

        setParameter(paramName, paramValue);
    }

    /***************************************************************************
     *
     * Fetches the value of one of this map elements as a float value. If the
     * element identified by the <code>paramName</code> key is not a
     * <code>java.lang.Float</code> then an <code>{@link IncorrectTypeException} </code> will be thrown.
     *
     * @param paramName
     *            The key associated with the value to retrieve.
     *
     * @return The <code>String</code> object which is the value associeted with
     *         the <code>paramName</code> key.
     *
     * @exception UnknownParameterException
     *                Thrown if there is no element having
     *                <code>paramName</code> as key.
     *
     * @exception IncorrectTypeException
     *                Thrown if the value associated with the
     *                <code>paramName</code> key is not a float value or null
     *                value.
     *
     ***************************************************************************/
    @Override
    public float getFloat(String paramName) throws UnknownParameterException, IncorrectTypeException {
        return (Float) ParameterTypes.Float.cast(getParameter(paramName));
    }

    /***************************************************************************
     *
     * Fetches the value of one of this map elements as a float value. If the
     * element identified by <code>paramName</code> is not a
     * <code>java.lang.Float</code> then an <code> {@link IncorrectTypeException}</code> will be thrown.
     *
     * @param paramName
     *            The key associated with the value to retrieve.
     * @param defaultValue
     *            The default value if the key is not defined
     *
     * @return The <code>Float</code> object which is the value associated with
     *         the <code>paramName</code> key or defaultValue if paramName does not exist.
     *
     * @exception IncorrectTypeException
     *                Thrown if the value associated with the
     *                <code>paramName</code> key is not a string object.
     *
     ***************************************************************************/
    public float getFloatOrDefault(String paramName, float defaultValue) throws IncorrectTypeException {

        return getOrDefaultTypedParameter(ParameterTypes.Float, paramName, defaultValue);
    }



    /***************************************************************************
     *
     * Sets the value of a parameter. If this map is constrained and none of its
     * keys may be <code>paramName</code> then an <code> {@link UnknownParameterException}</code> is thrown.
     *
     * @param paramName
     *            The name of the parameter to change.
     *
     * @param paramValue
     *            The new value to assign to the parameter.
     *
     * @exception UnknownParameterException
     *                Thrown if this map is constrained and it has no parameter
     *                named <code>paramName</code>.
     *
     ***************************************************************************/
    @Override
    public void setFloat(String paramName, float paramValue) throws UnknownParameterException {

        setParameter(paramName, paramValue);
    }

    /***************************************************************************
     *
     * Fetches the value of one of this map elements as a double value. If the
     * element identified by the <code>paramName</code> key is not a
     * <code>java.lang.Double</code> then an <code> {@link IncorrectTypeException}</code> will be thrown.
     *
     * @param paramName
     *            The key associated with the value to retrieve.
     *
     * @return The <code>String</code> object which is the value associeted with
     *         the <code>paramName</code> key.
     *
     * @exception UnknownParameterException
     *                Thrown if there is no element having
     *                <code>paramName</code> as key.
     *
     * @exception IncorrectTypeException
     *                Thrown if the value associated with the
     *                <code>paramName</code> key is not a double value or null
     *                value.
     *
     ***************************************************************************/
    @Override
    public double getDouble(String paramName) throws UnknownParameterException, IncorrectTypeException {

        return getTypedParameter(ParameterTypes.Double, paramName);
    }

    /***************************************************************************
     *
     * Fetches the value of one of this map elements as a double value. If the
     * element identified by <code>paramName</code> is not a
     * <code>java.lang.Double</code> then an <code> {@link IncorrectTypeException}</code> will be thrown.
     *
     * @param paramName
     *            The key associated with the value to retrieve.
     * @param defaultValue
     *            The default value if the key is not defined
     *
     * @return The <code>Double</code> object which is the value associated with
     *         the <code>paramName</code> key or defaultValue if paramName does not exist.
     *
     * @exception IncorrectTypeException
     *                Thrown if the value associated with the
     *                <code>paramName</code> key is not a string object.
     *
     ***************************************************************************/
    public double getDoubleOrDefault(String paramName, double defaultValue) throws IncorrectTypeException {

        return getOrDefaultTypedParameter(ParameterTypes.Double, paramName, defaultValue);
    }



    /***************************************************************************
     *
     * Sets the value of a parameter. If this map is constrained and none of its
     * keys may be <code>paramName</code> then an <code> {@link UnknownParameterException}</code> is thrown.
     *
     * @param paramName
     *            The name of the parameter to change.
     *
     * @param paramValue
     *            The new value to assign to the parameter.
     *
     * @exception UnknownParameterException
     *                Thrown if this map is constrained and it has no parameter
     *                named <code>paramName</code>.
     *
     ***************************************************************************/
    @Override
    public void setDouble(String paramName, double paramValue) throws UnknownParameterException {

        setParameter(paramName, paramValue);
    }

    /***************************************************************************
     *
     * Fetches the value of one of this map elements as a boolean value. If the
     * element identified by the <code>paramName</code> key is not a
     * <code>java.lang.Boolean</code> then an <code> {@link IncorrectTypeException}</code> will be thrown.
     *
     * @param paramName
     *            The key associated with the value to retrieve.
     *
     * @return The <code>String</code> object which is the value associeted with
     *         the <code>paramName</code> key.
     *
     * @exception UnknownParameterException
     *                Thrown if there is no element having
     *                <code>paramName</code> as key.
     *
     * @exception IncorrectTypeException
     *                Thrown if the value associated with the
     *                <code>paramName</code> key is not a boolean value or null
     *                value.
     *
     ***************************************************************************/
    @Override
    public boolean getBoolean(String paramName) throws UnknownParameterException, IncorrectTypeException {

        return getTypedParameter(ParameterTypes.Boolean, paramName);

    }

    /***************************************************************************
     *
     * Fetches the value of one of this map elements as a boolean value. If the
     * element identified by <code>paramName</code> is not a
     * <code>java.lang.Boolean</code> then an <code> {@link IncorrectTypeException}</code> will be thrown.
     *
     * @param paramName
     *            The key associated with the value to retrieve.
     * @param defaultValue
     *            The default value if the key is not defined
     *
     * @return The <code>Boolean</code> object which is the value associated with
     *         the <code>paramName</code> key or defaultValue if paramName does not exist.
     *
     * @exception IncorrectTypeException
     *                Thrown if the value associated with the
     *                <code>paramName</code> key is not a string object.
     *
     ***************************************************************************/
    public boolean getBooleanOrDefault(String paramName, boolean defaultValue) throws IncorrectTypeException {

        return getOrDefaultTypedParameter(ParameterTypes.Boolean, paramName, defaultValue);
    }



    /***************************************************************************
     *
     * Sets the value of a parameter. If this map is constrained and none of its
     * keys may be <code>paramName</code> then an <code> {@link UnknownParameterException}</code> is thrown.
     *
     * @param paramName
     *            The name of the parameter to change.
     *
     * @param paramValue
     *            The new value to assign to the parameter.
     *
     * @exception UnknownParameterException
     *                Thrown if this map is constrained and it has no parameter
     *                named <code>paramName</code>.
     *
     ***************************************************************************/
    @Override
    public void setBoolean(String paramName, boolean paramValue) throws UnknownParameterException {

        setParameter(paramName, paramValue);
    }



    /***************************************************************************
     *
     * Fetches the value of one of this map elements as a
     * <code>SatelliteData</code> object. If the element identified by the
     * <code>paramName</code> key is not a <code>SatelliteData</code> then an
     * <code>{@link IncorrectTypeException}</code> will be thrown.
     *
     * @param paramName
     *            The key associated with the value to retrieve.
     *
     * @return The <code>String</code> object which is the value associeted with
     *         the <code>paramName</code> key.
     *
     * @exception UnknownParameterException
     *                Thrown if there is no element having
     *                <code>paramName</code> as key.
     *
     * @exception IncorrectTypeException
     *                Thrown if the value associated with the
     *                <code>paramName</code> key is not a
     *                <code>SatelliteData</code> object.
     *
     ***************************************************************************/
    public SatelliteData getData(String paramName)
            throws UnknownParameterException, IncorrectTypeException {

        return getTypedParameter(ParameterTypes.Map, paramName);
    }

    @Override
    public SatelliteData getMap(String paramName)
            throws UnknownParameterException, IncorrectTypeException {

        return getData(paramName);
    }

    /***************************************************************************
     *
     * Fetches the value of one of this map elements as a map value. If the
     * element identified by <code>paramName</code> is not a
     * <code>SatelliteData</code> then an <code> {@link IncorrectTypeException}</code> will be thrown.
     *
     * @param paramName
     *            The key associated with the value to retrieve.
     * @param defaultValue
     *            The default value if the key is not defined
     *
     * @return The <code>SatelliteData</code> object which is the value associated with
     *         the <code>paramName</code> key or defaultValue if paramName does not exist.
     *
     * @exception IncorrectTypeException
     *                Thrown if the value associated with the
     *                <code>paramName</code> key is not a SatelliteData object.
     *
     ***************************************************************************/
    public SatelliteData getDataOrDefault(String paramName, SatelliteData defaultValue) throws IncorrectTypeException {

        return getOrDefaultTypedParameter(ParameterTypes.Map, paramName, defaultValue);
    }

    /***************************************************************************
     *
     * Sets the value of a parameter. If this map is constrained and none of its
     * keys may be <code>paramName</code> then an <code> {@link UnknownParameterException}</code> is thrown.
     *
     * @param paramName
     *            The name of the parameter to change.
     *
     * @param paramValue
     *            The new value to assign to the parameter.
     *
     * @exception UnknownParameterException
     *                Thrown if this map is constrained and it has no parameter
     *                named <code>paramName</code>.
     *
     ***************************************************************************/
    @Override
    public void setMap(String paramName, DataMap paramValue) throws UnknownParameterException {

        setParameter(paramName, paramValue);
    }



    /***************************************************************************
     *
     * Fetches the value of one of this map elements as a
     * <code>java.util.List</code> object. If the element identified by the
     * <code>paramName</code> key is not a <code>java.util.List</code> then an
     * <code>{@link IncorrectTypeException}</code> will be thrown.
     *
     * @param paramName
     *            The key associated with the value to retrieve.
     *
     * @return The <code>String</code> object which is the value associated with
     *         the <code>paramName</code> key.
     *
     * @exception UnknownParameterException
     *                Thrown if there is no element having
     *                <code>paramName</code> as key.
     *
     * @exception IncorrectTypeException
     *                Thrown if the value associated with the
     *                <code>paramName</code> key is not a
     *                <code>java.util.List</code> object.
     *
     ***************************************************************************/
    @Override
    public List getArray(String paramName) throws UnknownParameterException, IncorrectTypeException {

        return getTypedParameter(ParameterTypes.Array, paramName);
    }

    /***************************************************************************
     *
     * Fetches the value of one of this map elements as a array value. If the
     * element identified by <code>paramName</code> is not a
     * <code>SatelliteData</code> then an <code> {@link IncorrectTypeException}</code> will be thrown.
     *
     * @param paramName
     *            The key associated with the value to retrieve.
     * @param defaultValue
     *            The default value if the key is not defined
     *
     * @return The <code>SatelliteData</code> object which is the value associated with
     *         the <code>paramName</code> key or defaultValue if paramName does not exist.
     *
     * @exception IncorrectTypeException
     *                Thrown if the value associated with the
     *                <code>paramName</code> key is not a string object.
     *
     ***************************************************************************/
    public List getArrayOrDefault(String paramName, List defaultValue) throws IncorrectTypeException {

        return getOrDefaultTypedParameter(ParameterTypes.Array, paramName, defaultValue);
    }

    @Override
    public <T> List<T> getArray(Class<T> classObj, String paramName) throws UnknownParameterException, IncorrectTypeException {

        Object value = getParameter(paramName);
        if (value == null) {
            return null;
        }
        List<?> convListValue = (List<?>) ParameterTypes.Array.cast(value);
        List<T> copyList = new ArrayList<>(convListValue.size());
        ParameterTypes ptype = ParameterTypes.matchType(classObj);
        for (Object elem : convListValue) {
            copyList.add((T) ptype.cast(elem));
        }

        return copyList;
    }

    public <T> List<T> getAsArray(Class<T> classObj, String paramName) throws UnknownParameterException, IncorrectTypeException {

        return getArray(classObj, paramName);
    }



    /***************************************************************************
     *
     * Sets the value of a parameter. If this map is constrained and none of its
     * keys may be <code>paramName</code> then an <code> {@link UnknownParameterException}</code> is thrown.
     *
     * @param paramName
     *            The name of the parameter to change.
     *
     * @param paramValue
     *            The new value to assign to the parameter.
     *
     * @exception UnknownParameterException
     *                Thrown if this map is constrained and it has no parameter
     *                named <code>paramName</code>.
     *
     ***************************************************************************/
    @Override
    public void setArray(String paramName, List<?> paramValue) throws UnknownParameterException {

        setParameter(paramName, paramValue);
    }



    /***************************************************************************
     *
     * Fetches the value of one of this map elements as a <code>Date</code>. If
     * the element identified by the <code>paramName</code> key is not a
     * <code>java.util.Date</code> then an <code>{@link IncorrectTypeException} </code> will be thrown.
     *
     * @param paramName
     *            The key associated with the value to retrieve.
     *
     * @return The <code>String</code> object which is the value associated with
     *         the <code>paramName</code> key.
     *
     * @exception UnknownParameterException
     *                Thrown if there is no element having
     *                <code>paramName</code> as key.
     *
     * @exception IncorrectTypeException
     *                Thrown if the value associated with the
     *                <code>paramName</code> key is not a
     *                <code>java.util.Date</code> object.
     *
     ***************************************************************************/
    @Override
    public Date getDate(String paramName) throws UnknownParameterException, IncorrectTypeException {

        return getTypedParameter(ParameterTypes.Date, paramName);
    }

    /***************************************************************************
     *
     * Fetches the value of one of this map elements as a date value. If the
     * element identified by <code>paramName</code> is not a
     * <code>Date</code> then an <code> {@link IncorrectTypeException}</code> will be thrown.
     *
     * @param paramName
     *            The key associated with the value to retrieve.
     * @param defaultValue
     *            The default value if the key is not defined
     *
     * @return The <code>Date</code> object which is the value associated with
     *         the <code>paramName</code> key or defaultValue if paramName does not exist.
     *
     * @exception IncorrectTypeException
     *                Thrown if the value associated with the
     *                <code>paramName</code> key is not a string object.
     *
     ***************************************************************************/
    public Date getDateOrDefault(String paramName, Date defaultValue) throws IncorrectTypeException {

        return getOrDefaultTypedParameter(ParameterTypes.Date, paramName, defaultValue);
    }

    /***************************************************************************
     *
     * Sets the value of a parameter. If this map is constrained and none of its
     * keys may be <code>paramName</code> then an <code> {@link UnknownParameterException}</code> is thrown.
     *
     * @param paramName
     *            The name of the parameter to change.
     *
     * @param paramValue
     *            The new value to assign to the parameter.
     *
     * @exception UnknownParameterException
     *                Thrown if this map is constrained and it has no parameter
     *                named <code>paramName</code>.
     *
     ***************************************************************************/
    @Override
    public void setDate(String paramName, Date paramValue) throws UnknownParameterException {

        setParameter(paramName, paramValue);
    }

    /***************************************************************************
     *
     * Sets the value of a parameter to null. If this map is constrained and
     * none of its keys may be <code>paramName</code> then an <code> {@link UnknownParameterException}</code> is thrown.
     *
     * @param paramName
     *            The name of the parameter to change. *
     * @exception UnknownParameterException
     *                Thrown if this map is constrained and it has no parameter
     *                named <code>paramName</code>.
     *
     ***************************************************************************/
    @Override
    public void setNull(String paramName) throws UnknownParameterException {

        setParameter(paramName, null);
    }


    /***************************************************************************
     *
     *
     *
     ***************************************************************************/

    /***************************************************************************
     *
     * Sets the value of a parameter. If this map is constrained and none of its
     * keys may be <code>paramName</code> then an <code> {@link UnknownParameterException}</code> is thrown.
     *
     * @param paramName
     *            The name of the parameter to change.
     *
     * @param paramValue
     *            The new value to assign to the parameter.
     *
     * @exception UnknownParameterException
     *                Thrown if this map is constrained and it has no parameter
     *                named <code>paramName</code>.
     *
     ***************************************************************************/
    @Override
    public void setDecimal(String paramName, BigDecimal paramValue) throws UnknownParameterException {

        setParameter(paramName, paramValue);
    }

    /***************************************************************************
     *
     * Sets the values of parameters taken from another
     * <code>SatelliteData</code>. All the parameters contained in
     * <code>params</code> are set in this <code>SatelliteData</code> with the
     * values taken from <code>params</code>. If this <code>SatelliteData</code>
     * is constrained by a <code>{@link DataDefinition}</code> then if
     * <code>params</code> contains a paremeter that does not exist in this
     * <code>SatelliteData</code> a <code>{@link UnknownParameterException} </code> will be thrown.
     *
     * @param params
     *            The <code>SatelliteData</code> from which the parameters will
     *            be taken.
     *
     *                Thrown if this <code>SatelliteData</code> is constrained by
     *                a <code>{@link DataDefinition}</code> and
     *                <code>params</code> contains a parameter that does not
     *                exist in this <code>SatelliteData</code>.
     *
     ***************************************************************************/
    public void setParameters(SatelliteData params) throws UnknownParameterException {

        for (Iterator<String> i = params.getParameterNames(); i.hasNext(); ) {
            String paramName = i.next();
            Object paramValue = params.getParameter(paramName);

            setParameter(paramName, paramValue);
        }
    }

    /***************************************************************************
     *
     * Removes the entry identified by the given name.
     *
     * @param paramName
     *            The identifier of the entry to remove.
     *
     *
     ***************************************************************************/
    public void remove(String paramName) throws UnknownParameterException {
        Objects.requireNonNull(paramName, "paramName");

        if (valuesByName.containsKey(paramName)) {
            valuesByName.remove(paramName);
            return;
        }

        char[] buffer = paramName.toCharArray();
        if (buffer.length < 3 || buffer[0] == '.' || buffer[0] == '('
                || buffer[buffer.length - 1] == '.'
                || buffer[buffer.length - 1] == '(') {
            return;
        }

        Object holder = this;
        int last = 0;
        boolean inArray = false;
        boolean afterArray = false;

        for (int i = 1; i < buffer.length; i++) {
            switch (buffer[i]) {
                case '(':
                case '.':
                    if (!afterArray && (inArray || last == i)) {
                        return;
                    }
                    if (!afterArray) {
                        if (!(holder instanceof SatelliteData)) {
                            return;
                        }
                        holder = ((SatelliteData) holder).valuesByName.get(paramName.substring(last, i));
                    }
                    last = i + 1;
                    afterArray = false;
                    inArray = (buffer[i] == '(');
                    break;
                case ')':
                    if (!inArray || last == i || !(holder instanceof List)) {
                        return;
                    }
                    int index;
                    try {
                        index = Integer.parseInt(paramName.substring(last, i));
                    } catch (NumberFormatException invalidIndex) {
                        return;
                    }

                    List<?> values = (List<?>) holder;
                    if (index < 0 || index >= values.size()) {
                        return;
                    }
                    if (i == buffer.length - 1) {
                        values.remove(index);
                        return;
                    }

                    holder = values.get(index);
                    last = i + 1;
                    inArray = false;
                    afterArray = true;
                    break;
                default:
                    if (afterArray) {
                        return;
                    }
            }
        }

        if (!inArray && last > 0 && holder instanceof SatelliteData) {
            ((SatelliteData) holder).valuesByName.remove(paramName.substring(last));
        }
    }

    /***************************************************************************
     *
     * Resets all parameters to their respective default values. Mandatory
     * parameters are not created. That means they must be explicitly set before
     * their values can be fetched with calls to <code>{@link #get(Object)} </code>.
     *
     ***************************************************************************/
    @Override
    public void clear() {

        reset();
    }

    /***************************************************************************
     *
     * Produces a human readable representation of the contents of this
     * <code>SatelliteData</code>. This is mainly used for debugging purposes.
     * The output is sent to the <code>java.io.PrintWriter</code> given as
     * argument.
     *
     * <p>The output produced by this method is supposed to be read by humans.
     * It is not meant to be parsed by other programs.</p>
     *
     * @param writer
     *            The <code>java.io.PrintStream</code> where output is sent to.
     *
     ***************************************************************************/
    public void prettyPrint(PrintWriter writer) {

        SatelliteDataPrinter.print(writer, this);
    }

    /***************************************************************************
     *
     * Produces a human readable representation of the contents of this
     * <code>SatelliteData</code>. This is mainly used for debugging purposes.
     * The output is sent to the <code>java.io.PrintWriter</code> given as
     * argument.
     *
     * <p>The string produced by this method is meant for humans. It is not
     * meant to be parsed by other programs.</p>
     *
     * @return A string with a human readable representation of this
     *         <code>SatelliteData</code>.
     *
     ***************************************************************************/
    public String prettyPrint() {

        StringWriter buffer = new StringWriter();
        try (PrintWriter writer = new PrintWriter(buffer)) {
            prettyPrint(writer);
        }

        return buffer.toString();
    }



    /***************************************************************************
     *
     * Fetches a string representation for this map. The returned string is only
     * usefull for debugging or information purposes.
     *
     * @return A string with a representation of contents of this map.
     *
     ***************************************************************************/
    @Override
    public String toString() {

        return prettyPrint();
    }

    /***************************************************************************
     *
     *
     *
     ***************************************************************************/
    @Override
    public boolean containsValue(Object value) {

        return valuesByName.containsValue(value);
    }

    /***************************************************************************
     *
     *
     *
     ***************************************************************************/
    @Override
    public Set<Map.Entry<String, Object>> entrySet() {

        return valuesByName.entrySet();
    }

    /***************************************************************************
     *
     *
     *
     ***************************************************************************/
    @Override
    public boolean equals(Object o) {

        return (o == this) || valuesByName.equals(o);
    }

    /***************************************************************************
     *
     *
     *
     ***************************************************************************/
    @Override
    public Object get(Object key) {

        return valuesByName.get(key);
    }

    /***************************************************************************
     *
     *
     *
     ***************************************************************************/
    @Override
    public int hashCode() {

        return valuesByName.hashCode();
    }

    /***************************************************************************
     *
     *
     *
     ***************************************************************************/
    @Override
    public boolean isEmpty() {

        return valuesByName.isEmpty();
    }

    /***************************************************************************
     *
     *
     *
     ***************************************************************************/
    @Override
    public Set<String> keySet() {

        return valuesByName.keySet();
    }

    /***************************************************************************
     *
     *
     *
     ***************************************************************************/
    @Override
    public Object put(String key, Object value) {

        return valuesByName.put(key, value);
    }

    /***************************************************************************
     *
     *
     *
     ***************************************************************************/
    @Override
    public Object remove(Object key) {

        return valuesByName.remove(key);
    }

    /***************************************************************************
     *
     *
     *
     ***************************************************************************/
    @Override
    public int size() {

        return valuesByName.size();
    }

    /***************************************************************************
     *
     *
     *
     ***************************************************************************/
    @Override
    public Collection<Object> values() {

        return valuesByName.values();
    }

    @Override
    public Object getOrDefault(Object key, Object defaultValue) {

        return valuesByName.getOrDefault(key, defaultValue);
    }

    @Override
    public void forEach(BiConsumer<? super String, ? super Object> action) {

        valuesByName.forEach(action);
    }

    @Override
    public void replaceAll(BiFunction<? super String, ? super Object, ?> function) {

        valuesByName.replaceAll(function);
    }

    @Override
    public Object putIfAbsent(String key, Object value) {

        return valuesByName.putIfAbsent(key, value);
    }

    @Override
    public boolean remove(Object key, Object value) {

        return valuesByName.remove(key, value);
    }

    @Override
    public boolean replace(String key, Object oldValue, Object newValue) {

        return valuesByName.replace(key, oldValue, newValue);
    }

    @Override
    public Object replace(String key, Object value) {

        return valuesByName.replace(key, value);
    }

    @Override
    public Object computeIfAbsent(String key, Function<? super String, ?> mappingFunction) {

        return valuesByName.computeIfAbsent(key, mappingFunction);
    }

    @Override
    public Object computeIfPresent(String key, BiFunction<? super String, ? super Object, ?> remappingFunction) {

        return valuesByName.computeIfPresent(key, remappingFunction);
    }

    @Override
    public Object compute(String key, BiFunction<? super String, ? super Object, ?> remappingFunction) {

        return valuesByName.compute(key, remappingFunction);
    }

    @Override
    public Object merge(String key, Object value, BiFunction<? super Object, ? super Object, ?> remappingFunction) {

        return valuesByName.merge(key, value, remappingFunction);
    }

    /***************************************************************************
     *
     *
     *
     ***************************************************************************/
    public SatelliteData merge(SatelliteData map) throws SatelliteException {

        return merge(this, map);
    }

    @Override
    public void putAll(Map<? extends String, ? extends Object> map) {

        valuesByName.putAll(map);
    }

    @Override
    public DataMap newMap() {

        return new SatelliteData();
    }

    @Override
    public List<Object> newArray() {

        return new ArrayList<>();
    }

    /***************************************************************************
     *
     *
     *
     ***************************************************************************/
    private static final class Entry {

        private final Object _value;


        private Entry(Object value) {

            _value = value;
        }


        private Object getValue() {

            return _value;
        }
    }

}
