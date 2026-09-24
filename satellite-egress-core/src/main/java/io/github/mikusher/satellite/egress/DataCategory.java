package io.github.mikusher.satellite.egress;

/**
 * Semantic categories used by egress policies.
 */
public enum DataCategory {
    GENERAL,
    PERSONAL_DATA,
    CREDENTIAL,
    SECRET,
    FINANCIAL,
    HEALTH,
    LOCATION,
    DEVICE_IDENTIFIER,
    NETWORK_IDENTIFIER
}
