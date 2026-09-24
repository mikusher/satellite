package io.github.mikusher.satellite.egress;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class KeyTest {

    @Test
    public void keepsClassificationAndCategoriesSeparate() {
        Key<String> key = Key.string("user.email")
                .classifiedAs(DataClassification.CONFIDENTIAL)
                .category(DataCategory.PERSONAL_DATA)
                .required();

        assertEquals(DataClassification.CONFIDENTIAL, key.getClassification());
        assertTrue(key.getCategories().contains(DataCategory.PERSONAL_DATA));
        assertFalse(key.getCategories().contains(DataCategory.GENERAL));
        assertTrue(key.isRequired());
    }

    @Test(expected = IllegalArgumentException.class)
    public void rejectsWrongRuntimeType() {
        Key<Integer> age = Key.integer("age");
        age.cast("not-an-integer");
    }
}
