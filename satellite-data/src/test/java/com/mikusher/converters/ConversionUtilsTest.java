package com.mikusher.converters;

import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class ConversionUtilsTest {

    @Test
    public void detectsFiniteDoubles() {
        assertTrue(ConversionUtils.isFinite(0.0d));
        assertTrue(ConversionUtils.isFinite(Double.MAX_VALUE));
        assertFalse(ConversionUtils.isFinite(Double.NaN));
        assertFalse(ConversionUtils.isFinite(Double.POSITIVE_INFINITY));
        assertFalse(ConversionUtils.isFinite(Double.NEGATIVE_INFINITY));
    }

    @Test
    public void detectsFiniteFloats() {
        assertTrue(ConversionUtils.isFinite(0.0f));
        assertTrue(ConversionUtils.isFinite(Float.MAX_VALUE));
        assertFalse(ConversionUtils.isFinite(Float.NaN));
        assertFalse(ConversionUtils.isFinite(Float.POSITIVE_INFINITY));
        assertFalse(ConversionUtils.isFinite(Float.NEGATIVE_INFINITY));
    }
}
