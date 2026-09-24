package io.github.mikusher.satellite.egress.policy;

public enum EgressSink {
    LOG,
    TRACE,
    METRIC,
    AUDIT,
    SERIALIZATION,
    NETWORK,
    STORAGE
}
