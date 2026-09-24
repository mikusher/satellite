package io.github.mikusher.satellite.egress.policy;

import io.github.mikusher.satellite.egress.DataCategory;
import io.github.mikusher.satellite.egress.DataClassification;
import io.github.mikusher.satellite.egress.DataOrigin;
import io.github.mikusher.satellite.egress.TrustLevel;

import java.util.Collections;
import java.util.EnumSet;
import java.util.Set;

public final class PrivacyViolation {
    private final String keyName;
    private final EgressSink sink;
    private final String reasonCode;
    private final DataClassification classification;
    private final Set<DataCategory> categories;
    private final DataOrigin origin;
    private final TrustLevel trustLevel;

    PrivacyViolation(String keyName,
                     EgressSink sink,
                     String reasonCode,
                     DataClassification classification,
                     Set<DataCategory> categories,
                     DataOrigin origin,
                     TrustLevel trustLevel) {
        this.keyName = keyName;
        this.sink = sink;
        this.reasonCode = reasonCode;
        this.classification = classification;
        this.categories = categories.isEmpty()
                ? Collections.emptySet()
                : Collections.unmodifiableSet(EnumSet.copyOf(categories));
        this.origin = origin;
        this.trustLevel = trustLevel;
    }

    public String getKeyName() {
        return keyName;
    }

    public EgressSink getSink() {
        return sink;
    }

    public String getReasonCode() {
        return reasonCode;
    }

    public DataClassification getClassification() {
        return classification;
    }

    public Set<DataCategory> getCategories() {
        return categories;
    }

    public DataOrigin getOrigin() {
        return origin;
    }

    public TrustLevel getTrustLevel() {
        return trustLevel;
    }

    @Override
    public String toString() {
        return "PrivacyViolation{key='" + keyName + "', sink=" + sink
                + ", reasonCode='" + reasonCode + "', classification=" + classification
                + ", categories=" + categories + ", origin=" + origin
                + ", trustLevel=" + trustLevel + '}';
    }
}
