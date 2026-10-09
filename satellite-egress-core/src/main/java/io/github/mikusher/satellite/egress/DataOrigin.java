package io.github.mikusher.satellite.egress;

/**
 * Origin of a value at runtime.
 */
public enum DataOrigin {
    APPLICATION,
    USER_INPUT,
    HTTP_HEADER,
    HTTP_BODY,
    QUERY_PARAMETER,
    COOKIE,
    DATABASE,
    ENVIRONMENT,
    CONFIGURATION,
    THIRD_PARTY,
    GENERATED,
    UNKNOWN
}
