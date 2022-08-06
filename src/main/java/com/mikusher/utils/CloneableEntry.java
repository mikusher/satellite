package com.mikusher.utils;

/**
 * @param <T>
 */
public interface CloneableEntry<T> extends Cloneable {
    T clone();
}
