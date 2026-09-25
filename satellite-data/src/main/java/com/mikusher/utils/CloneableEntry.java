package com.mikusher.utils;

/**
 * Cloneable implementation
 * @author <a href="mailto:mikusher@gmail.com">Mikusher</a>
 * @param <T> type of objects
 */
public interface CloneableEntry<T> extends Cloneable {
    T clone();
}
