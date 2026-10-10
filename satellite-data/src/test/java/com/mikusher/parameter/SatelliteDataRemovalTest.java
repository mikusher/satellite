package com.mikusher.parameter;

import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;

public class SatelliteDataRemovalTest {

    @Test
    public void removesNestedFieldsAndMutableListEntries() throws Exception {
        SatelliteData nested = new SatelliteData();
        nested.put("id", "42");
        nested.put("email", "user@example.com");

        List<Object> list = new ArrayList<>();
        list.add("first");
        list.add("second");

        SatelliteData data = new SatelliteData();
        data.put("nested", nested);
        data.put("items", list);

        data.remove("nested.id");
        assertFalse(nested.containsKey("id"));
        assertEquals("user@example.com", nested.getString("email"));

        data.remove("items(0)");
        assertEquals(Collections.singletonList("second"), list);
    }

    @Test
    public void removesLiteralNullValuedKeysWithoutTouchingOtherFields() {
        SatelliteData data = new SatelliteData();
        data.put("nested.field", null);
        data.put("other", 7);

        data.remove("nested.field");

        assertFalse(data.containsKey("nested.field"));
        assertEquals(7, data.get("other"));
    }

    @Test
    public void ignoresInvalidOrNonexistentPaths() throws Exception {
        SatelliteData data = new SatelliteData();
        data.put("items", new ArrayList<>(Arrays.asList("first")));
        data.put("number", 7);

        data.remove("items(not-a-number)");
        data.remove("items(99)");
        data.remove("number.field");
        data.remove("missing.field");

        assertEquals(1, ((List<?>) data.get("items")).size());
        assertEquals(7, data.get("number"));
    }

    @Test(expected = UnsupportedOperationException.class)
    public void doesNotHideUnexpectedImmutableListFailure() throws Exception {
        SatelliteData data = new SatelliteData();
        data.put("items", Collections.unmodifiableList(Arrays.asList("one")));

        data.remove("items(0)");
    }

    @Test
    public void cloningListsDoesNotDependOnReflectiveConstructors() {
        List<String> original = Arrays.asList("a", "b");
        List<?> clone = SatelliteData.cloneArray(original);

        assertEquals(original, clone);
        clone.clear();
        assertEquals(Arrays.asList("a", "b"), original);
    }
}
