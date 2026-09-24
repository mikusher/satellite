package io.github.mikusher.satellite.egress.jackson;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.mikusher.satellite.egress.SatelliteMap;
import io.github.mikusher.satellite.egress.policy.EgressContext;
import io.github.mikusher.satellite.egress.policy.EgressProcessor;
import io.github.mikusher.satellite.egress.policy.EgressReport;
import io.github.mikusher.satellite.egress.policy.EgressSink;

import java.util.Objects;

/**
 * Policy-enforced Jackson serialization for SatelliteMap.
 */
public final class JacksonEgressSerializer {
    private final ObjectMapper objectMapper;
    private final EgressProcessor processor;

    public JacksonEgressSerializer(ObjectMapper objectMapper, EgressProcessor processor) {
        this.objectMapper = Objects.requireNonNull(objectMapper, "objectMapper");
        this.processor = Objects.requireNonNull(processor, "processor");
    }

    public JacksonEgressResult toJsonNode(SatelliteMap map, String purpose) {
        EgressReport report = processor.process(
                Objects.requireNonNull(map, "map"),
                EgressContext.of(EgressSink.SERIALIZATION, purpose));
        JsonNode node = objectMapper.valueToTree(report.getOutput());
        return new JacksonEgressResult(node, report);
    }

    public String toJson(SatelliteMap map, String purpose) throws JsonProcessingException {
        JacksonEgressResult result = toJsonNode(map, purpose);
        return objectMapper.writeValueAsString(result.getJson());
    }
}
