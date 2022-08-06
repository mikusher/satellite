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

public class ParameterMap implements DataMap {

    protected static final char SIMPLE = 'S';
    protected static final char MAP = 'M';
    protected static final char LIST = 'L';

    protected static final Object NOT_FOUND = new Object();

    protected Map<String, Object> _params = null;
    protected ParameterInfoMap _paramInfoMap = null;


    /***************************************************************************
     *
     * Initializes the object with no parameters. This <code>ParameterMap</code>
     * will not be restrained by a <code>{@link ParameterInfoMap}</code> and can
     * store any parameter regardless of its name.
     *
     ***************************************************************************/
    public ParameterMap() {

        this(4);
    }


    public ParameterMap(int initialCapacity) {

        _params = new HashMap<>(initialCapacity);
        _paramInfoMap = null;
    }


    public ParameterMap(ParameterInfoMap paramInfoMap) {

        _params = new HashMap<>();
        _paramInfoMap = paramInfoMap;

        reset();
    }


    /***************************************************************************
     *
     * Initializes this <code>ParameterMap</code> to use an external
     * <code>java.util.Map</code> as repository. No constraints will be
     * associated with this map.
     *
     * @param map
     *            An external map used to store the elements.
     *
     *
     ***************************************************************************/
    public ParameterMap(Map map) {

        // TODO: Change this constructor. At this moment this is kept as is to
        // give additional compatibility
        _params = map;
        _paramInfoMap = null;
    }


    /***************************************************************************
     *
     * Initializes this <code>ParameterMap</code> to use an external
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
    public ParameterMap(Map<String, Object> map, boolean copyInputMap) {

        if (copyInputMap) {
            _params = new HashMap<>(map);
        } else {
            _params = map;
        }
        _paramInfoMap = null;
    }


    /***************************************************************************
     *
     * Initializes this <code>ParameterMap</code> to use an external
     * <code>java.util.Map</code> as repository. The set of permissible entries
     * will be constrained by the <code>paramInfoMap</code> <code> {@link ParameterInfoMap}</code>.
     *
     * <p>If a mandatory parameter is not present in <code>map</code> then a
     * <code>JafException</code> is thrown. If an optional parameter is missing
     * then one is created having the default value.</p>
     *
     * @param map
     *            An external map used to store the elements.
     *
     * @param paramInfoMap
     *            The set of constraints on the elements this
     *            <code>ParameterMap</code> may contain. If null there will be
     *            no constraint on the parameters.
     *
     * @exception SatelliteException
     *                Thrown when the siplied <code>map</code> does not contain
     *                a mandatory parameter as specified by
     *                <code>paramInfoMap</code>.
     *
     ***************************************************************************/
    public ParameterMap(Map<String, Object> map, ParameterInfoMap paramInfoMap) throws SatelliteException {

        _params = map;
        setConstraints(paramInfoMap);
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

        List<Object> cloned;
        try {
            cloned = array.getClass().newInstance();
        } catch (InstantiationException ie) {
            cloned = new ArrayList<>(array.size());
        } catch (Exception ex) {
            try {
                cloned = new ArrayList<>(array.size());
            } catch (Exception e) {
                throw new SatelliteError(e.toString(), e);
            }
        }

        for (Object obj : array) {
            cloned.add(cloneObject(obj));
        }
        return cloned;
    }

    /***************************************************************************
     *
     *
     *
     ***************************************************************************/
    private static void prettyPrint(PrintWriter writer, Map<String, Object> value, int indent) {

        int nextIndent = indent + 4;

        writer.println();

        for (Map.Entry<String, Object> entry : value.entrySet()) {
            prettyPrint(writer, entry.getKey(), entry.getValue(), nextIndent);
        }
    }

    /***************************************************************************
     *
     *
     *
     ***************************************************************************/
    private static void prettyPrint(PrintWriter writer, List<?> value, int indent) {

        int nextIndent = indent + 4;

        writer.println();

        int index = 0;
        for (Iterator<?> i = value.iterator(); i.hasNext(); ) {
            String key = "(" + index++ + ")";
            Object parVal = i.next();
            prettyPrint(writer, key, parVal, nextIndent);
        }
    }

    /***************************************************************************
     *
     *
     *
     ***************************************************************************/
    private static void prettyPrint(PrintWriter writer, String key, Object value, int indent) {

        printIndent(writer, indent);
        if (key != null) {
            writer.print(key);
            writer.print(" ");
        }

        if (value instanceof CharSequence) {
            writer.print("(String)");
            writer.println(" \"" + value + "\"");
            return;
        }
        if (value instanceof Map) {
            writer.print("(Map)");
            prettyPrint(writer, (Map) value, indent);
            return;
        }
        if (value instanceof List) {
            writer.print("(Array)");
            prettyPrint(writer, (List) value, indent);
            return;
        }
        if (value instanceof BigDecimal) {
            writer.print("(Decimal)");
            writer.println(" \"" + value + "\"");
            return;
        }
        if (value == null) {
            writer.println("(NULL)");
            return;
        }

        writer.print("(" + getTypeName(value) + ")");
        writer.println(" " + value);
    }

    /***************************************************************************
     *
     *
     *
     ***************************************************************************/
    private static String getTypeName(Object obj) {

        String className = obj.getClass().getName();
        int lastDotIndex = className.lastIndexOf('.');
        String typeName = (lastDotIndex >= 0) ? className.substring(lastDotIndex + 1) : className;

        return typeName;
    }

    /***************************************************************************
     *
     *
     *
     ***************************************************************************/
    private static void printIndent(PrintWriter writer, int count) {

        for (int i = 0; i < count; i++) {
            writer.print(' ');
        }
    }

    /***************************************************************************
     *
     *
     *
     ***************************************************************************/
    public static ParameterMap merge(ParameterMap map1, ParameterMap map2) throws SatelliteException {

        if (map1 == null) {
            return map2;
        } else if (map2 == null) {
            return map1;
        }

        for (Map.Entry<String, Object> entry : map1._params.entrySet()) {

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
                        merge((ParameterMap) mapObj1, (ParameterMap) mapObj2);
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

        for (Map.Entry<String, Object> entry : map2._params.entrySet()) {
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
                        ((ParameterMap) lstObj1).merge((ParameterMap) lstObj2);
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

        if (obj instanceof ParameterMap) {
            return MAP;
        } else if (obj instanceof List) {
            return LIST;
        } else {
            return SIMPLE;
        }
    }

    /***************************************************************************
     *
     * Specifies the constraints for this <code>ParameterMap</code>. The
     * <code>paramInfoMap</code> constraints specify the keys and associated
     * values that may be stored in this map.
     *
     * <p>If a mandatory parameter is not present then a
     * <code>JafException</code> is thrown. If an optional parameter is missing
     * then one is created having the default value.</p>
     *
     * @param paramInfoMap
     *            The set of new constraints for this map.
     *
     ***************************************************************************/
    public void setConstraints(ParameterInfoMap paramInfoMap) throws SatelliteException {

        _paramInfoMap = paramInfoMap;
        if (_paramInfoMap == null) {
            return;
        }

        for (Iterator<ParameterInfo> it = _paramInfoMap.iterator(); it.hasNext(); ) {
            ParameterInfo paramInfo = it.next();
            String paramName = paramInfo.getName();

            Object value = _params.getOrDefault(paramName, NOT_FOUND);
            if (value != NOT_FOUND) {
                Object newValue = paramInfo.getParameterType().cast(value);
                if (value != newValue) {
                    _params.put(paramName, newValue);
                }
            } else {
                if (paramInfo.isMandatory()) {
                    throw new UnknownParameterException(paramName);
                }

                _params.put(paramName, paramInfo.getDefaultValue());
            }
        }
    }

    /***************************************************************************
     *
     * Specifies the constraints for this <code>ParameterMap</code> but does not
     * validate its current contents. Validation is delayed until one of the
     * <code>getXxx(...)</code> methods is invoked.
     *
     * @param paramInfoMap
     *            The new set of constraints for this map.
     *
     ***************************************************************************/
    public void assignConstraints(ParameterInfoMap paramInfoMap) {

        _paramInfoMap = paramInfoMap;
    }

    /***************************************************************************
     *
     * All elements are reset to their respective default values. Mandatory
     * elements are not created. That means they must be explicitly set before
     * their values can be fetched with calls to <code>{@link #get(Object)} </code>.
     *
     ***************************************************************************/
    private void reset() {

        _params.clear();
        if (_paramInfoMap == null) {
            return;
        }

        for (Iterator<ParameterInfo> i = _paramInfoMap.iterator(); i.hasNext(); ) {
            ParameterInfo paramInfo = i.next();
            if (!paramInfo.isMandatory()) {
                String name = paramInfo.getName();
                Object defVal = paramInfo.getDefaultValue();
                _params.put(name, defVal);
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
    public ParameterMap clone() {

        final ParameterMap cloned;
        try {
            cloned = this.getClass().newInstance();
        } catch (Exception ex) {
            throw new SatelliteError(ex.toString(), ex);
        }

        for (Map.Entry<String, Object> entry : _params.entrySet()) {
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

        if (_paramInfoMap == null) {
            return NOT_FOUND;
        }

        ParameterInfo paramInfo = _paramInfoMap.get(paramName);
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

        // must verify if the paramName is present on the keys (containsKey)
        Object value = _params.getOrDefault(paramName, NOT_FOUND);
        if (value != NOT_FOUND) {
            return new Entry(value);
        }

        char[] buffer = paramName.toCharArray();
        if (buffer.length < 3 || buffer[0] == '.' || buffer[0] == '(' || buffer[buffer.length - 1] == '.' || buffer[buffer.length - 1] == '(') {
            return null;
        }

        Object holder = this;
        int last = 0;
        boolean arr = false;
        boolean outArr = false;
        try {
            for (int i = 1; i < buffer.length; i++) {
                switch (buffer[i]) {
                    case '(':
                    case '.':
                        if (!outArr && (arr || last == i)) {
                            return null;
                        }

                        if (!outArr) {
                            holder = ((ParameterMap) holder)._params.get(paramName.substring(last, i));
                        }

                        last = i + 1;
                        outArr = false;
                        arr = (buffer[i] == '(');
                        break;
                    case ')':
                        if (!arr || last == i) {
                            return null;
                        }

                        holder = ((List) holder).get(Integer.parseInt(paramName.substring(last, i)));
                        last = i + 1;
                        arr = false;
                        outArr = true;
                        break;
                    default:
                        if (outArr) {
                            return null;
                        }
                }
            }

            if (arr || last == 0) {
                return null;
            }

            if (last < buffer.length) {
                String tmp = paramName.substring(last);
                if (holder != null) {
                    value = ((ParameterMap) holder)._params.getOrDefault(tmp, NOT_FOUND);
                    if (value != NOT_FOUND) {
                        return new Entry(value);
                    }
                }
            } else {
                return new Entry(holder);
            }
        } catch (Exception e) {
        }

        return null;
    }

    /***************************************************************************
     *
     * Fetches a parameter value. If parameter identified by
     * <code>paramName</code> has never been set then a null is returned. A
     * parameter may have been set explicitly by calling <code> {@link #setParameter(String, Object)}</code> or implicitly in the
     * constructor when the paremeter value was initialized with a default value
     * given by the <code>{@link ParameterInfoMap}</code> passed to the
     * constructor.
     *
     * @param paramName
     *            The name of the parameter whose value if being fetched.
     *
     * @return The value of the parameter or null if it has not been set.
     *
     ***************************************************************************/
    public Object getParameterNoCheck(String paramName) {

        return _params.get(paramName);
    }

    /***************************************************************************
     *
     * Sets the value of a parameter. If this <code>ParameterMap</code> is
     * constrained by a <code>{@link ParameterInfoMap}</code> then
     * <code>paramName</code> must refer to an existing parameter otherwise a
     * <code>UnknownParameterException</code> will be thrown.
     *
     * <p>If this <code>ParameterMap</code> is not constrained by a <code> {@link ParameterInfoMap}</code> then the operation will succeed.</p>
     *
     * @param paramName
     *            The name of the parameter being set.
     *
     * @param paramValue
     *            The new value of the parameter.
     *
     *                Thrown if this <code>ParameterMap</code> is constrained by
     *                a <code>{@link ParameterInfoMap}</code> and it does not
     *                contain the parameter named <code>paramName</code>.
     *
     ***************************************************************************/
    @Override
    public void setParameter(String paramName, Object paramValue) throws UnknownParameterException {

        if (_paramInfoMap != null && !_paramInfoMap.containsParameter(paramName)) {
            throw new UnknownParameterException(paramName);
        }

        Object value = _params.getOrDefault(paramName, NOT_FOUND);
        if (value != NOT_FOUND) {
            _params.put(paramName, paramValue);
            return;
        }

        char[] buffer = paramName.toCharArray();
        // if name can't be a valid nested expression, use the literal name directly
        if (buffer.length < 3 || buffer[0] == '.' || buffer[0] == '(' || buffer[buffer.length - 1] == '.' || buffer[buffer.length - 1] == '(') {
            _params.put(paramName, paramValue);
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
                            holder = ((ParameterMap) holder)._params.get(paramName.substring(last, i));
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
                _params.put(paramName, paramValue);
                return;
            }

            // put it in the structure
            if (holder != null) {
                ((ParameterMap) holder)._params.put(paramName.substring(last), paramValue);
            } else {
                _params.put(paramName, paramValue);
            }
        } catch (Exception e) {
            // parameter not in structure so keep it
            _params.put(paramName, paramValue);
        }
    }

    /***************************************************************************
     *
     * Sets the value of a parameter.
     *
     * @param paramName
     *            The name of the parameter being set.
     *
     * @param paramValue
     *            The new value of the parameter.
     *
     *
     ***************************************************************************/
    @Deprecated
    public void setParameterNoCheck(String paramName, Object paramValue) {

        _params.put(paramName, paramValue);
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
        if (_paramInfoMap != null && _paramInfoMap.containsParameter(param)) {
            try {
                ParameterInfo paramInfo = _paramInfoMap.getParameterInfo(param);
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

        return _params.keySet().iterator();
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
     * Fetches the value of one of this map elements as a <code>String</code>.
     * If the element identified by the <code>paramName</code> key is not a
     * <code>java.lang.String</code> then an <code> {@link IncorrectTypeException}</code> will be thrown.
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
     *                <code>paramName</code> key is not a string object.
     *
     ***************************************************************************/
    @Deprecated
    public String getAsString(String paramName) throws UnknownParameterException, IncorrectTypeException {

        return getTypedParameter(ParameterTypes.String, paramName);
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

    @Deprecated
    public Integer getAsInteger(String paramName) throws UnknownParameterException, IncorrectTypeException {

        return getTypedParameter(ParameterTypes.Integer, paramName);
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
    @Deprecated
    public Long getAsLong(String paramName) throws UnknownParameterException, IncorrectTypeException {

        return getTypedParameter(ParameterTypes.Long, paramName);
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
    @Deprecated
    public BigDecimal getAsDecimal(String paramName) throws UnknownParameterException, IncorrectTypeException {

        return getTypedParameter(ParameterTypes.Decimal, paramName);
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

        return getAsFloat(paramName);
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
     * Fetches the value of one of this map elements as a float value. If the
     * element identified by the <code>paramName</code> key is not a
     * <code>java.lang.Float</code> then an <code>{@link IncorrectTypeException} </code> will be thrown.
     *
     * @param paramName
     *            The key associated with the value to retrieve.
     *
     * @return The <code>Float</code> object which is the value associeted with
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
     * @deprecated
     ***************************************************************************/
    @Deprecated
    public Float getAsFloat(String paramName) throws UnknownParameterException, IncorrectTypeException {

        Object value = getParameter(paramName);
        return (Float) ParameterTypes.Float.cast(value);
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
    @Deprecated
    public Double getAsDouble(String paramName) throws UnknownParameterException, IncorrectTypeException {

        return getTypedParameter(ParameterTypes.Double, paramName);
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
    @Deprecated
    public Boolean getAsBoolean(String paramName) throws UnknownParameterException, IncorrectTypeException {

        return getTypedParameter(ParameterTypes.Boolean, paramName);
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
     * <code>ParameterMap</code> object. If the element identified by the
     * <code>paramName</code> key is not a <code>ParameterMap</code> then an
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
     *                <code>ParameterMap</code> object.
     *
     ***************************************************************************/
    @Deprecated
    public ParameterMap getAsMap(String paramName) throws UnknownParameterException, IncorrectTypeException {

        return getTypedParameter(ParameterTypes.Map, paramName);
    }

    /***************************************************************************
     *
     * Fetches the value of one of this map elements as a
     * <code>ParameterMap</code> object. If the element identified by the
     * <code>paramName</code> key is not a <code>ParameterMap</code> then an
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
     *                <code>ParameterMap</code> object.
     *
     ***************************************************************************/
    @Override
    public ParameterMap getMap(String paramName) throws UnknownParameterException, IncorrectTypeException {

        return getTypedParameter(ParameterTypes.Map, paramName);
    }

    /***************************************************************************
     *
     * Fetches the value of one of this map elements as a map value. If the
     * element identified by <code>paramName</code> is not a
     * <code>ParameterMap</code> then an <code> {@link IncorrectTypeException}</code> will be thrown.
     *
     * @param paramName
     *            The key associated with the value to retrieve.
     * @param defaultValue
     *            The default value if the key is not defined
     *
     * @return The <code>ParameterMap</code> object which is the value associated with
     *         the <code>paramName</code> key or defaultValue if paramName does not exist.
     *
     * @exception IncorrectTypeException
     *                Thrown if the value associated with the
     *                <code>paramName</code> key is not a string object.
     *
     ***************************************************************************/
    public ParameterMap getDoubleOrDefault(String paramName, ParameterMap defaultValue) throws IncorrectTypeException {

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
     *                NOTE: This is only needed to ensure binary compatibility
     *                with previous code
     ***************************************************************************/
    public void setMap(String paramName, ParameterMap paramValue) throws UnknownParameterException {

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
     * <code>ParameterMap</code> then an <code> {@link IncorrectTypeException}</code> will be thrown.
     *
     * @param paramName
     *            The key associated with the value to retrieve.
     * @param defaultValue
     *            The default value if the key is not defined
     *
     * @return The <code>ParameterMap</code> object which is the value associated with
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
     * Fetches the value of one of this map elements as a
     * <code>java.util.List</code> object. If the element identified by the
     * <code>paramName</code> key is not a <code>java.util.List</code> then an
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
     *                <code>java.util.List</code> object.
     *
     ***************************************************************************/
    @Deprecated
    public List getAsArray(String paramName) throws UnknownParameterException, IncorrectTypeException {

        return getTypedParameter(ParameterTypes.Array, paramName);
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
     *                <code>java.util.Date</code> object.
     *
     ***************************************************************************/
    @Deprecated
    public Date getAsDate(String paramName) throws UnknownParameterException, IncorrectTypeException {

        return getTypedParameter(ParameterTypes.Date, paramName);
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
     * <code>ParameterMap</code>. All the parameters contained in
     * <code>params</code> are set in this <code>ParameterMap</code> with the
     * values taken from <code>params</code>. If this <code>ParameterMap</code>
     * is constrained by a <code>{@link ParameterInfoMap}</code> then if
     * <code>params</code> contains a paremeter that does not exist in this
     * <code>ParameterMap</code> a <code>{@link UnknownParameterException} </code> will be thrown.
     *
     * @param params
     *            The <code>ParameterMap</code> from which the parameters will
     *            be taken.
     *
     *                Thrown if this <code>ParameterMap</code> is constrained by
     *                a <code>{@link ParameterInfoMap}</code> and
     *                <code>params</code> contains a parameter that does not
     *                exist in this <code>ParameterMap</code>.
     *
     ***************************************************************************/
    public void setParameters(ParameterMap params) throws UnknownParameterException {

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

        if (_params.remove(paramName) != null) {
            return;
        }

        Object holder = this;
        char[] buffer = paramName.toCharArray();
        int last = 0;

        try {
            if (buffer.length < 3 || buffer[0] == '.' || buffer[0] == '(' || buffer[buffer.length - 1] == '.' || buffer[buffer.length - 1] == '(')
                return;

            holder = this;

            boolean arr = false;
            boolean outArr = false;

            for (int i = 1; i < buffer.length; i++) {
                switch (buffer[i]) {
                    case '(':
                    case '.':
                        if (!outArr && (arr || last == i)) {
                            return;
                        }

                        if (!outArr) {
                            holder = ((ParameterMap) holder)._params.get(paramName.substring(last, i));
                        }
                        last = i + 1;
                        outArr = false;
                        arr = (buffer[i] == '(');
                        break;
                    case ')':
                        if (!arr || last == i) {
                            throw new UnknownParameterException(paramName);
                        }
                        int tmp = Integer.parseInt(paramName.substring(last, i));

                        if (i == buffer.length - 1) {
                            // the end
                            ((List) holder).remove(tmp);

                            return;
                        }

                        holder = ((List) holder).get(tmp);
                        last = i + 1;
                        arr = false;
                        outArr = true;
                        break;
                    default:
                        if (outArr) {
                            throw new UnknownParameterException(paramName);
                        }

                }
            }

            if (arr || last == 0) {
                return;
            }

            // remove it from the structure
            ((ParameterMap) holder)._params.remove(paramName.substring(last));
        } catch (Exception e) {
            // parameter not in structure so nothing to do
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
     * <code>ParameterMap</code>. This is mainly used for debugging purposes.
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

        prettyPrint(writer, this, -4);
    }

    /***************************************************************************
     *
     * Produces a human readable representation of the contents of this
     * <code>ParameterMap</code>. This is mainly used for debugging purposes.
     * The output is sent to the <code>java.io.PrintWriter</code> given as
     * argument.
     *
     * <p>The string produced by this method is meant for humans. It is not
     * meant to be parsed by other programs.</p>
     *
     * @return A string with a human readable representation of this
     *         <code>ParameterMap</code>.
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
     * Fetches the number of elements in this map.
     *
     * @return The number of current elements.
     *
     * @deprecated Use <code>{@link #size()}</code> instead.
     *
     ***************************************************************************/
    @Deprecated
    public int count() {

        return (_params != null) ? _params.size() : 0;
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

        return _params.containsValue(value);
    }

    /***************************************************************************
     *
     *
     *
     ***************************************************************************/
    @Override
    public Set<Map.Entry<String, Object>> entrySet() {

        return _params.entrySet();
    }

    /***************************************************************************
     *
     *
     *
     ***************************************************************************/
    @Override
    public boolean equals(Object o) {

        return (o == this) || _params.equals(o);
    }

    /***************************************************************************
     *
     *
     *
     ***************************************************************************/
    @Override
    public Object get(Object key) {

        return _params.get(key);
    }

    /***************************************************************************
     *
     *
     *
     ***************************************************************************/
    @Override
    public int hashCode() {

        return _params.hashCode();
    }

    /***************************************************************************
     *
     *
     *
     ***************************************************************************/
    @Override
    public boolean isEmpty() {

        return _params.isEmpty();
    }

    /***************************************************************************
     *
     *
     *
     ***************************************************************************/
    @Override
    public Set<String> keySet() {

        return _params.keySet();
    }

    /***************************************************************************
     *
     *
     *
     ***************************************************************************/
    @Override
    public Object put(String key, Object value) {

        return _params.put(key, value);
    }

    /***************************************************************************
     *
     *
     *
     ***************************************************************************/
    @Override
    public Object remove(Object key) {

        return _params.remove(key);
    }

    /***************************************************************************
     *
     *
     *
     ***************************************************************************/
    @Override
    public int size() {

        return _params.size();
    }

    /***************************************************************************
     *
     *
     *
     ***************************************************************************/
    @Override
    public Collection<Object> values() {

        return _params.values();
    }

    @Override
    public Object getOrDefault(Object key, Object defaultValue) {

        return _params.getOrDefault(key, defaultValue);
    }

    @Override
    public void forEach(BiConsumer<? super String, ? super Object> action) {

        _params.forEach(action);
    }

    @Override
    public void replaceAll(BiFunction<? super String, ? super Object, ?> function) {

        _params.replaceAll(function);
    }

    @Override
    public Object putIfAbsent(String key, Object value) {

        return _params.putIfAbsent(key, value);
    }

    @Override
    public boolean remove(Object key, Object value) {

        return _params.remove(key, value);
    }

    @Override
    public boolean replace(String key, Object oldValue, Object newValue) {

        return _params.replace(key, oldValue, newValue);
    }

    @Override
    public Object replace(String key, Object value) {

        return _params.replace(key, value);
    }

    @Override
    public Object computeIfAbsent(String key, Function<? super String, ?> mappingFunction) {

        return _params.computeIfAbsent(key, mappingFunction);
    }

    @Override
    public Object computeIfPresent(String key, BiFunction<? super String, ? super Object, ?> remappingFunction) {

        return _params.computeIfPresent(key, remappingFunction);
    }

    @Override
    public Object compute(String key, BiFunction<? super String, ? super Object, ?> remappingFunction) {

        return _params.compute(key, remappingFunction);
    }

    @Override
    public Object merge(String key, Object value, BiFunction<? super Object, ? super Object, ?> remappingFunction) {

        return _params.merge(key, value, remappingFunction);
    }

    /***************************************************************************
     *
     *
     *
     ***************************************************************************/
    public ParameterMap merge(ParameterMap map) throws SatelliteException {

        return merge(this, map);
    }

    @Override
    public void putAll(Map<? extends String, ? extends Object> map) {

        _params.putAll(map);
    }

    @Override
    public DataMap newMap() {

        return new ParameterMap();
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
