package com.mikusher.parameter;

import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class SatelliteUuidSupportTest {

    private static final UUID FIRST = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private static final UUID SECOND = UUID.fromString("00000000-0000-0000-0000-000000000002");
    private static final UUID THIRD = UUID.fromString("00000000-0000-0000-0000-000000000003");

    @Test
    public void mixedListNeverThrowsAndKeepsValidIdentifiers() {
        SatelliteData nested = new SatelliteData();
        nested.put("UUID", SECOND.toString());

        SatelliteData data = new SatelliteData();
        data.put("ids", Arrays.asList(
                FIRST.toString(), "not-a-uuid", nested, THIRD, null, 42));

        Set<UUID> result = SatelliteDataUtils.getUUIDSetFromUUIDListOrObjectList("ids", data);

        assertTrue(result.contains(FIRST));
        assertTrue(result.contains(SECOND));
        assertTrue(result.contains(THIRD));
        assertEquals(3, result.size());
    }

    @Test
    public void objectListFiltersUnexpectedElementTypes() {
        SatelliteData valid = new SatelliteData();
        valid.put("UUID", FIRST.toString());
        SatelliteData bad = new SatelliteData();
        bad.put("UUID", "invalid");

        SatelliteData data = new SatelliteData();
        data.put("objects", Arrays.asList(valid, bad, "not a data instance", null));

        assertEquals(
                Collections.singletonList(FIRST),
                SatelliteDataUtils.getUUIDListFromObjectList("objects", data));
        assertEquals(
                Collections.singleton(FIRST),
                SatelliteDataUtils.getUUIDSetFromObjectList("objects", data));
    }

    @Test
    public void stringsRemainTheOnlyValuesAcceptedByToUUID() {
        assertEquals(FIRST, SatelliteDataUtils.toUUID(FIRST.toString()));
        assertNull(SatelliteDataUtils.toUUID(FIRST));
        assertNull(SatelliteDataUtils.toUUID("invalid"));
        assertNull(SatelliteDataUtils.toUUID(null));
    }

    @Test
    public void absentListsKeepNullAndFallbackSemantics() {
        SatelliteData data = new SatelliteData();
        Set<UUID> fallback = new HashSet<>(Collections.singleton(FIRST));

        assertNull(SatelliteDataUtils.getUUIDList("missing", data));
        assertNull(SatelliteDataUtils.getUUIDListFromObjectList("missing", data));
        assertNull(SatelliteDataUtils.getUUIDSetFromUUIDListOrObjectList("missing", data));
        assertEquals(fallback, SatelliteDataUtils.getUUIDSet("missing", data, fallback));
        assertNull(SatelliteDataUtils.getUUIDSetFromUUIDListOrObjectList("missing", null));
    }

    @Test
    public void stringListRoundTripsAndSkipsNull() {
        SatelliteData data = new SatelliteData();
        SatelliteDataUtils.setUUIDList("ids", Arrays.asList(FIRST, null, SECOND), data);

        List<UUID> restored = SatelliteDataUtils.getUUIDList("ids", data);

        assertEquals(Arrays.asList(FIRST, SECOND), restored);
        assertEquals(new HashSet<>(restored), SatelliteDataUtils.getUUIDSet("ids", data));
        SatelliteDataUtils.setUUID("id", FIRST, data);
        assertEquals(FIRST, SatelliteDataUtils.getUUID("id", data));
    }
}
