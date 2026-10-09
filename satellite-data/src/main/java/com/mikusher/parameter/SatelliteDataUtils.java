package com.mikusher.parameter;

import com.google.common.base.Function;
import com.mikusher.utils.DataMap;

import java.math.BigDecimal;
import java.util.*;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class SatelliteDataUtils {


    private static final String PARAM_UUID = "UUID";

    private SatelliteDataUtils() {

    }

    public static List<UUID> getUUIDList(String key, SatelliteData map) {

        Stream<UUID> stream = getUUIDStream(key, map);
        if (stream == null) {
            return null;
        }

        return stream.collect(Collectors.toList());
    }


    private static Stream<UUID> getUUIDStream(String key, SatelliteData map) {

        if (key == null || map == null) {
            return null;
        }

        final Object value = map.get(key);
        if (value == null || !(value instanceof List)) {
            return null;
        }

        return toUUIDStream(((List<?>) value).stream());
    }

    public static List<UUID> getUUIDListFromObjectList(String key, SatelliteData map) {

        Stream<UUID> stream = getUUIDStreamFromObjectList(key, map);
        return stream == null ? null : stream.collect(Collectors.toList());
    }


    private static Stream<UUID> getUUIDStreamFromObjectList(String key, SatelliteData map) {

        if (key == null || map == null) {
            return null;
        }

        Object value = map.get(key);
        if (value == null || !(value instanceof List)) {
            return null;
        }

        @SuppressWarnings("unchecked")
        List<SatelliteData> objLst = (List<SatelliteData>) value;
        return toUUIDStream(objLst.stream().map(pm -> {
            try {
                return pm.getString(PARAM_UUID);
            } catch (Exception e) {
                // Ignore different types
                return null;
            }
        }));
    }


    /**
     * Convert a given list of objects to a list of UUID.
     * <p>
     * List objects that are not strings in UUID format will be ignored.
     *
     * @param objList
     * @return
     */
    public static List<UUID> toUUIDList(Collection<?> objList) {

        return toUUIDStream(objList.stream()).collect(Collectors.toList());
    }


    /**
     * Convert a given list of objects to a Set of UUID.
     * <p>
     * List objects that are not strings in UUID format will be ignored.
     *
     * @param objList
     * @return
     */
    public static Set<UUID> toUUIDSet(Collection<?> objList) {

        return toUUIDStream(objList.stream()).collect(Collectors.toSet());
    }


    private static Stream<UUID> toUUIDStream(Stream<?> stream) {

        return stream.map(SatelliteDataUtils::toUUID).filter(Objects::nonNull);
    }


    /**
     * Retrieve an UUID set from a String list from a given map entry.
     * <p>
     * If <code>key</code> or <code>map</code> are <code>null</code>, then <code>null</code> is returned.
     * <p>
     * If the <code>map</code> has no <code>key</code>, or the value for the <code>key</code> is <code>null</code> or not a List, then <code>null</code> is returned.
     * <p>
     * Invalid UUID strings or <code>null</code> values are ignored.
     *
     * @param key Name of the map key that contains the String list.
     * @param map Map where to look for the specified key.
     * @return
     */
    public static Set<UUID> getUUIDSet(String key, SatelliteData map) {

        return getUUIDSet(key, map, null);
    }


    public static Set<UUID> getUUIDSet(String key, SatelliteData map, Set<UUID> defaultValue) {

        Stream<UUID> stream = getUUIDStream(key, map);
        if (stream == null) {
            return defaultValue;
        }

        return stream.collect(Collectors.toSet());
    }


    /**
     * Retrieve an UUID set from a String list or a Object List from a given map entry.
     * <p>
     * If <code>key</code> or <code>map</code> are <code>null</code>, then <code>null</code> is returned.
     * <p>
     * If the <code>map</code> has no <code>key</code>, or the value for the <code>key</code> is <code>null</code> or not a List, then <code>null</code> is returned.
     * <p>
     * Invalid UUID strings or <code>null</code> values are ignored.
     *
     * @param key Name of the map key that contains the String list.
     * @param map Map where to look for the specified key.
     * @return
     */
    public static Set<UUID> getUUIDSetFromUUIDListOrObjectList(String key, SatelliteData map) {

        Object value = map.get(key);
        if (value == null || !(value instanceof List)) {
            return null;
        }

        List<UUID> uuidList = getUUIDList(key, map);
        if (uuidList == null) {
            return null;
        }

        int listLenght = ((List<?>) value).size();
        if (listLenght > uuidList.size()) {
            Stream<UUID> stream = getUUIDStreamFromObjectList(key, map);
            return stream == null ? null : stream.collect(Collectors.toSet());
        }

        return new HashSet<>(uuidList);
    }


    /**
     * Retrieve an UUID set from a Object list from a given map entry.
     * <p>
     * If <code>key</code> or <code>map</code> are <code>null</code>, then <code>null</code> is returned.
     * <p>
     * If the <code>map</code> has no <code>key</code>, or the value for the <code>key</code> is <code>null</code> or not a List, then <code>null</code> is returned.
     * <p>
     * Invalid UUID strings or <code>null</code> values are ignored.
     *
     * @param key Name of the map key that contains the String list.
     * @param map Map where to look for the specified key.
     * @return
     */
    public static Set<UUID> getUUIDSetFromObjectList(String key, SatelliteData map) {

        Stream<UUID> stream = getUUIDStreamFromObjectList(key, map);
        return stream == null ? null : stream.collect(Collectors.toSet());
    }


    /**
     * Convert an UUID list to a String list and set it to the provided <code>map</code>.
     * <p>
     * If <code>key</code>, <code>uuidList</code> or <code>map</code> are <code>null</code>, nothing is done.
     *
     * <code>null</code> entries in the list are ignored.
     *
     * @param key      Name of the map key to be set.
     * @param uuidList UUID list to set in the specified map.
     * @param map      Map where to set the specified key.
     * @return
     */
    public static void setUUIDList(String key, Collection<UUID> uuidList, SatelliteData map) {

        if (key == null || map == null || uuidList == null) {
            return;
        }

        map.put(key, uuidList.stream().filter(Objects::nonNull).map(UUID::toString).collect(Collectors.toList()));
    }


    /**
     * Retrieve an UUID from a String parameter in a map.
     * <p>
     * If <code>key</code> or <code>map</code> are <code>null</code>, then <code>null</code> is returned.
     * <p>
     * If the <code>map</code> has no <code>key</code>, or the value for the <code>key</code> is <code>null</code> or not a String, then <code>null</code> is returned.
     * <p>
     * If the String contains an invalid UUID, then <code>null</code> is returned.
     *
     * @param key Name of the map key that contains the String.
     * @param map Map where to look for the specified key.
     * @return
     */
    public static UUID getUUID(String key, SatelliteData map) {

        return getUUID(key, map, null);
    }


    /**
     * Retrieve an UUID from a String parameter in a map.
     * <p>
     * If <code>key</code> or <code>map</code> are <code>null</code>, then <code>defaultUUID</code> is returned.
     * <p>
     * If the <code>map</code> has no <code>key</code>, or the value for the <code>key</code> is <code>null</code> or not a String, then <code>defaultUUID</code> is returned.
     * <p>
     * If the String contains an invalid UUID, then <code>defaultUUID</code> is returned.
     *
     * @param key Name of the map key that contains the String.
     * @param map Map where to look for the specified key.
     * @return
     */
    public static UUID getUUID(String key, SatelliteData map, UUID defaultUUID) {

        if (key == null || map == null) {
            return defaultUUID;
        }

        UUID result = toUUID(map.get(key));

        return result == null ? defaultUUID : result;
    }


    /**
     * Convert an UUID to a String and set it to the provided <code>map</code>.
     * <p>
     * If <code>key</code>, <code>uuid</code> or <code>map</code> are <code>null</code>, nothing is done.
     *
     * @param key  Name of the map key to be set.
     * @param uuid UUID to set in the specified map.
     * @param map  Map where to set the specified key.
     * @return
     */
    public static void setUUID(String key, UUID uuid, SatelliteData map) {

        if (key == null || map == null || uuid == null) {
            return;
        }

        map.put(key, uuid.toString());
    }


    /**
     * Convert a given Object to an UUID.
     * <p>
     * If the Object is not a String then <code>null</code> is returned.
     *
     * @param value
     * @return
     */
    public static UUID toUUID(Object value) {

        if (value == null || !(value instanceof String)) {
            return null;
        }

        try {
            return UUID.fromString((String) value);
        } catch (IllegalArgumentException e) {

            // Ignore invalid UUID strings.
        }

        return null;
    }


    /**
     * Retrieve a SatelliteData list from a given map entry.
     * <p>
     * If <code>key</code> or <code>map</code> are <code>null</code>, then <code>null</code> is returned.
     * <p>
     * If the <code>map</code> has no <code>key</code>, or the value for the <code>key</code> is <code>null</code> or not a List, then <code>null</code> is returned.
     * <p>
     * Invalid SatelliteData objects or <code>null</code> values are ignored.
     *
     * @param key Name of the map key that contains the List.
     * @param map Map where to look for the specified key.
     * @return
     */
    public static List<SatelliteData> getMapList(String key, SatelliteData map) {

        return getValueList(SatelliteData.class, key, map);
    }


    /**
     * Retrieve a String list from a given map entry.
     * <p>
     * If <code>key</code> or <code>map</code> are <code>null</code>, then <code>null</code> is returned.
     * <p>
     * If the <code>map</code> has no <code>key</code>, or the value for the <code>key</code> is <code>null</code> or not a List, then <code>null</code> is returned.
     * <p>
     * Invalid strings or <code>null</code> values are ignored.
     *
     * @param key Name of the map key that contains the List.
     * @param map Map where to look for the specified key.
     * @return
     */
    public static List<String> getStringList(String key, SatelliteData map) {

        return getValueList(String.class, key, map);
    }


    /**
     * Check if a given map value is a list of strings.
     * <p>
     * If <code>key</code> or <code>map</code> are <code>null</code>, then <code>false</code> is returned.
     * <p>
     * If the <code>map</code> has no <code>key</code>, or the value for the <code>key</code> is <code>null</code> or not a List, then <code>false</code> is returned.
     * <p>
     * If the List is empty, then <code>false</code> is returned.
     * <p>
     * Returns <code>true</code> if one of the elements in the list is a String.
     *
     * @param key Name of the map key that contains the List.
     * @param map Map where to look for the specified key.
     * @return
     */
    public static boolean isStringList(String key, SatelliteData map) {

        if (key == null || map == null) {
            return false;
        }

        Object value = map.get(key);
        if (value == null || !(value instanceof List)) {
            return false;
        }

        return ((List<?>) value).stream().filter(e -> e != null && e instanceof String).findAny().isPresent();
    }


    /**
     * Retrieve a Boolean value from a map.
     * <p>
     * If <code>key</code> or <code>map</code> are <code>null</code>, then <code>null</code> is returned.
     * <p>
     * If the <code>map</code> has no <code>key</code>, or the value for the <code>key</code> is <code>null</code> or not Boolean, then <code>null</code> is returned.
     *
     * @param key Name of the map key that contains the desired value.
     * @param map Map where to look for the specified key.
     * @return
     */
    public static Boolean getBoolean(String key, SatelliteData map) {

        return getBoolean(key, map, null);
    }


    /**
     * Retrieve a Boolean value from a map.
     * <p>
     * If <code>key</code> or <code>map</code> are <code>null</code>, then <code>defaultValue</code> is returned.
     * <p>
     * If the <code>map</code> has no <code>key</code>, or the value for the <code>key</code> is <code>null</code> or not Boolean, then <code>defaultValue</code> is returned.
     *
     * @param key Name of the map key that contains the desired value.
     * @param map Map where to look for the specified key.
     * @return
     */
    public static Boolean getBoolean(String key, SatelliteData map, Boolean defaultValue) {

        return getValue(ParameterTypes.Boolean, key, map, defaultValue);
    }


    /**
     * Set a Boolean value in a map.
     * <p>
     * If <code>key</code>, <code>value</code> or <code>map</code> are <code>null</code>, nothing is done.
     *
     * @param key   Name of the map key to be set.
     * @param value Value to be set in the specified map.
     * @param map   Map where to set the specified key.
     * @return
     */
    public static void setBoolean(String key, Boolean value, SatelliteData map) {

        setValue(key, value, map);
    }


    /**
     * If <code>keys</code>, <code>value</code> or <code>map</code> are <code>null</code>, nothing is done.
     *
     * <code>null</code> entries in the list are ignored.
     *
     * @param keys  Name of the sequence of keys to find object
     * @param value Boolean list to set in the specified map.
     * @param map   Map where to set the specified key.
     * @return
     */
    public static void setBoolean(Boolean value, SatelliteData map, String... keys) {

        if (keys.length == 0) {
            return;
        }

        setBoolean(keys[keys.length - 1], value, navigateMap(map, keys, 0, keys.length - 1));
    }


    /**
     * Retrieve a String value from a map.
     * <p>
     * If <code>key</code> or <code>map</code> are <code>null</code>, then <code>null</code> is returned.
     * <p>
     * If the <code>map</code> has no <code>key</code>, or the value for the <code>key</code> is <code>null</code> or not String, then <code>null</code> is returned.
     *
     * @param key Name of the map key that contains the desired value.
     * @param map Map where to look for the specified key.
     * @return
     */
    public static String getString(String key, SatelliteData map) {

        return getString(key, map, null);
    }


    /**
     * Retrieve a String value from a map.
     * <p>
     * If <code>key</code> or <code>map</code> are <code>null</code>, then <code>defaultValue</code> is returned.
     * <p>
     * If the <code>map</code> has no <code>key</code>, or the value for the <code>key</code> is <code>null</code> or not String, then <code>defaultValue</code> is returned.
     *
     * @param key Name of the map key that contains the desired value.
     * @param map Map where to look for the specified key.
     * @return
     */
    public static String getString(String key, SatelliteData map, String defaultValue) {

        return getValue(ParameterTypes.String, key, map, defaultValue);
    }


    public static void setString(String key, String value, SatelliteData map) {

        setValue(key, value, map);
    }


    public static Integer getInt(String key, SatelliteData map) {

        return getInt(key, map, null);
    }


    public static Integer getInt(String key, SatelliteData map, Integer defaultValue) {

        return getValue(ParameterTypes.Integer, key, map, defaultValue);
    }


    public static void setInt(String key, Integer value, SatelliteData map) {

        setValue(key, value, map);
    }


    public static void setInt(String key, Function<Integer, Integer> setter, SatelliteData map) {

        if (setter != null) {
            Integer value = getInt(key, map);
            if (value != null) {
                Integer finalValue = setter.apply(value);
                setInt(key, finalValue, map);
            }
        }
    }


    public static void setLong(String key, Function<Long, Long> setter, SatelliteData map) {

        if (setter != null) {
            Long value = getLong(key, map);
            if (value != null) {
                Long finalValue = setter.apply(value);
                setLong(key, finalValue, map);
            }
        }

    }


    public static Long getLong(String key, SatelliteData map) {

        return getLong(key, map, null);
    }


    public static Long getLong(String key, SatelliteData map, Long defaultValue) {

        return getValue(ParameterTypes.Long, key, map, defaultValue);
    }


    public static void setLong(String key, Long value, SatelliteData map) {

        setValue(key, value, map);
    }


    public static Float getFloat(String key, SatelliteData map) {

        return getFloat(key, map, null);
    }


    public static Float getFloat(String key, SatelliteData map, Float defaultValue) {

        return getValue(ParameterTypes.Float, key, map, defaultValue);
    }


    public static void setFloat(String key, Float value, SatelliteData map) {

        setValue(key, value, map);
    }


    public static Double getDouble(String key, SatelliteData map) {

        return getDouble(key, map, null);
    }


    public static Double getDouble(String key, SatelliteData map, Double defaultValue) {

        return getValue(ParameterTypes.Double, key, map, defaultValue);
    }


    public static void setDouble(String key, Double value, SatelliteData map) {

        setValue(key, value, map);
    }


    public static BigDecimal getDecimal(String key, SatelliteData map) {

        return getDecimal(key, map, null);
    }


    public static BigDecimal getDecimal(String key, SatelliteData map, BigDecimal defaultValue) {

        return getValue(ParameterTypes.Decimal, key, map, defaultValue);
    }


    public static void setDecimal(String key, BigDecimal value, SatelliteData map) {

        setValue(key, value, map);
    }


    public static Date getDate(String key, SatelliteData map) {

        return getDate(key, map, null);
    }


    public static Date getDate(String key, SatelliteData map, Date defaultValue) {

        return getValue(ParameterTypes.Date, key, map, defaultValue);
    }


    public static void setDate(String key, Date value, SatelliteData map) {

        setValue(key, value, map);
    }


    public static SatelliteData getMap(String key, SatelliteData map) {

        return getMap(key, map, null);
    }


    public static SatelliteData getMap(String key, SatelliteData map, SatelliteData defaultValue) {

        return getValue(ParameterTypes.Map, key, map, defaultValue);
    }


    public static void setMap(String key, SatelliteData value, SatelliteData map) {

        setValue(key, value, map);
    }


    public static List<?> getArray(String key, SatelliteData map) {

        return getArray(key, map, null);
    }


    public static List<?> getArray(String key, SatelliteData map, List<?> defaultValue) {

        Stream<?> value = getValueStream(key, map);
        if (value == null) {
            return defaultValue;
        }

        return value.collect(Collectors.toList());
    }


    public static void setArray(String key, List<?> value, SatelliteData map) {

        setValue(key, value, map);
    }


    public static Object getObject(String key, SatelliteData map) {

        return getObject(key, map, null);
    }


    public static Object getObject(String key, SatelliteData map, Object defaultValue) {

        return getValue(ParameterTypes.Unknown, key, map, defaultValue);
    }


    public static Object getObject(SatelliteData map, Object defaultValue, String... keys) {

        if (keys.length == 0) {
            return defaultValue;
        }

        return getObject(keys[keys.length - 1], navigateMap(map, keys, 0, keys.length - 1), defaultValue);
    }


    public static void setObject(String key, Object value, SatelliteData map) {

        setValue(key, value, map);
    }


    public static SatelliteData extend(SatelliteData... maps) {

        if (maps == null) {
            return null;
        }

        SatelliteData map = new SatelliteData();

        for (SatelliteData m : maps) {
            if (m != null) {
                map = extend(map, m);
            }
        }

        return map;
    }


    private static SatelliteData extend(SatelliteData map1, SatelliteData map2) {

        boolean map1IsInvalid = !isValidMap(map1);
        boolean map2IsInvalid = !isValidMap(map2);

        if (map1IsInvalid && map2IsInvalid) {
            return null;
        }

        if (map1IsInvalid) {
            return map2.clone();
        }

        if (map2IsInvalid) {
            return map1.clone();
        }

        SatelliteData map = map1.clone();

        for (Map.Entry<String, Object> entry : map2.entrySet()) {

            String key = entry.getKey();
            Object value = entry.getValue();
            Object existingValue = map.get(key);

            if (value instanceof SatelliteData) {

                // Keep extending the values if we found a SatelliteData.

                SatelliteData existingMap = (existingValue instanceof SatelliteData ? (SatelliteData) existingValue
                        : null);

                map.put(key, extend(existingMap, (SatelliteData) value));

            } else if (value instanceof List) {

                // Keep extending the values if we found a List.

                List<?> existingList = (existingValue instanceof List ? (List<?>) existingValue : null);

                map.put(key, extendList(existingList, (List<?>) value));

            } else {

                map.put(key, value);
            }
        }

        return map;
    }


    private static List<?> extendList(List<?> list1, List<?> list2) {

        if (list1 == null && list2 == null) {
            return null;
        }

        if (list1 == null) {
            return list2;
        }

        if (list2 == null) {
            return list1;
        }

        List<Object> list = new ArrayList<>();

        int list1Size = list1.size();
        int list2Size = list2.size();

        int maxSize = Math.max(list1Size, list2Size);

        for (int i = 0; i < maxSize; i++) {

            boolean hasValue1 = i < list1Size;
            boolean hasValue2 = i < list2Size;

            Object value1 = (hasValue1 ? list1.get(i) : null);
            Object value2 = (hasValue2 ? list2.get(i) : null);

            if (!hasValue2) {

                /*
                 * Second list reached its end so, we use the value from the first list.
                 * We also make sure that we extend the value if it's a SatelliteData or List.
                 */

                if (value1 instanceof SatelliteData) {
                    list.add(extend(null, (SatelliteData) value1));
                } else if (value1 instanceof List) {
                    list.add(extendList(null, (List<?>) value1));
                } else {
                    list.add(value1);
                }

                continue;
            }

            if (value2 instanceof SatelliteData) {

                // Keep extending the values if we found a SatelliteData.

                SatelliteData value1Map = (value1 instanceof SatelliteData ? (SatelliteData) value1 : null);

                list.add(extend(value1Map, (SatelliteData) value2));

            } else if (value2 instanceof List) {

                // Keep extending the values if we found a List.

                List<?> value1List = (value1 instanceof List ? (List<?>) value1 : null);

                list.add(extendList(value1List, (List<?>) value2));

            } else {

                list.add(value2);
            }
        }

        return list;
    }


    private static boolean isValidMap(SatelliteData map) {

        return (map != null && map instanceof SatelliteData);
    }


    public static boolean containsKeyValue(SatelliteData map, String key, Object value) {

        return value != null && map.containsKey(key) && value.equals(getObject(key, map));
    }


    public static boolean containsMap(SatelliteData map, SatelliteData map1) {

        for (String key1 : map1.keySet()) {
            Object value1 = getObject(key1, map1);
            if (!containsKeyValue(map, key1, value1)) {
                return false;
            }
        }

        return true;
    }


    @SuppressWarnings("unchecked")
    private static <T> T getValue(ParameterTypes type, String key, SatelliteData map, T defaultValue) {

        if (type == null || key == null || map == null) {
            return defaultValue;
        }

        /**
         * The String converter (in ParameterTypes.String) can convert DataMap representations to String
         * which is not the expected behavior in this utility method.
         *
         * We explicitly prevent any cast from DataMap to String by immediately returning the default value.
         */
        final Object value = map.get(key);
        if (value == null || (type.equals(ParameterTypes.String) && value instanceof DataMap)) {
            return defaultValue;
        }

        try {
            return (T) type.cast(value);
        } catch (Exception e) {
            // If the value cannot be cast we return the default value.
            return defaultValue;
        }
    }


    private static <T> List<T> getValueList(Class<T> cls, String key, SatelliteData map) {

        Stream<?> stream = getValueStream(key, map);
        if (stream == null) {
            return null;
        }

        return stream.filter(o -> cls.isAssignableFrom(o.getClass())).map(cls::cast).collect(Collectors.toList());
    }


    private static Stream<?> getValueStream(String key, SatelliteData map) {

        if (key == null || map == null) {
            return null;
        }

        final Object value = map.get(key);
        if (value == null || !(value instanceof List)) {
            return null;
        }

        return ((List<?>) value).stream().filter(Objects::nonNull);
    }


    private static void setValue(String key, Object value, SatelliteData map) {

        if (key == null || map == null || value == null) {
            return;
        }

        try {
            map.put(key, value);
        } catch (Exception e) {
            // If the value cannot be cast we ignore it.
        }
    }


    /**
     * If <code>key</code>, <code>mapList</code> or <code>map</code> are <code>null</code>, nothing is done.
     *
     * <code>null</code> entries in the list are ignored.
     *
     * @param key     Name of the map key to be set.
     * @param mapList SatelliteData list to set in the specified map.
     * @param map     Map where to set the specified key.
     * @return
     */
    public static void setMapList(String key, List<SatelliteData> mapList, SatelliteData map) {

        if (key == null || map == null || mapList == null) {
            return;
        }

        map.put(key, mapList);
    }


    /**
     * If <code>key</code>, <code>stringList</code> or <code>map</code> are <code>null</code>, nothing is done.
     *
     * <code>null</code> entries in the list are ignored.
     *
     * @param key        Name of the map key to be set.
     * @param stringList String list to set in the specified map.
     * @param map        Map where to set the specified key.
     * @return
     */
    public static void setStringList(String key, List<String> stringList, SatelliteData map) {

        if (key == null || map == null || stringList == null) {
            return;
        }

        map.put(key, stringList);
    }


    /**
     * If <code>key</code>, <code>setter</code> or <code>map</code> are <code>null</code>, nothing is done.
     *
     * <code>null</code> entries in the list are ignored.
     *
     * @param key    Name of the map key to be set.
     * @param setter function to apply in string list value
     * @param map    Map where to set the specified key.
     * @return
     */
    public static void setStringList(String key, Function<List<String>, List<String>> setter, SatelliteData map) {

        List<String> value = getStringList(key, map);
        List<String> finalValue = setter.apply(nvl(value, ArrayList::new));
        setStringList(key, finalValue, map);
    }


    /**
     * If <code>key</code>, <code>setter</code> or <code>map</code> are <code>null</code>, nothing is done.
     *
     * <code>null</code> entries in the list are ignored.
     *
     * @param key    Name of the map key to be set.
     * @param setter function to apply in map list value
     * @param map    Map where to set the specified key.
     * @return
     */
    public static void setMapList(String key, Function<List<SatelliteData>, List<SatelliteData>> setter,
                                  SatelliteData map) {

        List<SatelliteData> value = getMapList(key, map);
        List<SatelliteData> finalValue = setter.apply(nvl(value, ArrayList::new));
        setMapList(key, finalValue, map);
    }


    /**
     * If <code>keys</code>, <code>setter</code> or <code>map</code> are <code>null</code>, nothing is done.
     *
     * <code>null</code> entries in the list are ignored.
     *
     * @param keys   Name of the sequence of keys to find object
     * @param setter function to apply in string list value
     * @param map    Map where to set the specified key.
     * @return
     */
    public static void setStringList(Function<List<String>, List<String>> setter, SatelliteData map, String... keys) {

        if (keys.length == 0) {
            return;
        }

        setStringList(keys[keys.length - 1], setter, navigateMap(map, keys, 0, keys.length - 1));
    }


    public static void setInt(Function<Integer, Integer> setter, SatelliteData map, String... keys) {

        if (keys.length == 0) {
            return;
        }

        setInt(keys[keys.length - 1], setter, navigateMap(map, keys, 0, keys.length - 1));
    }



    public static void setLong(Function<Long, Long> setter, SatelliteData map, String... keys) {

        if (keys.length == 0) {
            return;
        }

        setLong(keys[keys.length - 1], setter, navigateMap(map, keys, 0, keys.length - 1));
    }


    public static void setStringList(List<String> stringList, SatelliteData map, String... keys) {

        if (keys.length == 0) {
            return;
        }

        setStringList(keys[keys.length - 1], stringList, navigateMap(map, keys, 0, keys.length - 1));
    }


    public static void setMap(SatelliteData mapValue, SatelliteData map, String... keys) {

        if (keys.length == 0) {
            return;
        }

        setMap(keys[keys.length - 1], mapValue, navigateMap(map, keys, 0, keys.length - 1));
    }


    public static void setString(String stringValue, SatelliteData map, String... keys) {

        if (keys.length == 0) {
            return;
        }

        setString(keys[keys.length - 1], stringValue, navigateMap(map, keys, 0, keys.length - 1));
    }


    public static void setLong(Long longValue, SatelliteData map, String... keys) {

        if (keys.length == 0) {
            return;
        }

        setLong(keys[keys.length - 1], longValue, navigateMap(map, keys, 0, keys.length - 1));
    }


    public static void setMapList(Function<List<SatelliteData>, List<SatelliteData>> setter, SatelliteData map,
                                  String... keys) {

        if (keys.length == 0) {
            return;
        }

        setMapList(keys[keys.length - 1], setter, navigateMap(map, keys, 0, keys.length - 1));
    }

    public static void setUUIDList(Function<List<UUID>, List<UUID>> setter, SatelliteData map, String... keys) {

        if (keys.length == 0) {
            return;
        }

        setUUIDList(keys[keys.length - 1], setter, navigateMap(map, keys, 0, keys.length - 1));
    }


    public static void setUUIDList(String key, Function<List<UUID>, List<UUID>> setter, SatelliteData map) {

        List<UUID> value = getUUIDList(key, map);
        List<UUID> finalValue = setter.apply(nvl(value, ArrayList::new));
        setUUIDList(key, finalValue, map);
    }


    private static SatelliteData navigateMap(SatelliteData map, String[] keys, int start, int end) {

        SatelliteData pointer = map;
        for (int i = start; i < end && pointer != null; i++) {
            pointer = getMap(keys[i], pointer);
        }

        return pointer;
    }


    private static <T> T nvl(T value, Supplier<T> defaultValue) {

        if (value != null) {
            return value;
        }

        return defaultValue.get();
    }


}
