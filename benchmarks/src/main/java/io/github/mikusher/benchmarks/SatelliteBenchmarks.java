package io.github.mikusher.benchmarks;

import com.mikusher.parameter.SatelliteData;
import io.github.mikusher.satellite.bridge.DataBridgeResult;
import io.github.mikusher.satellite.bridge.SatelliteDataEgressBridge;
import io.github.mikusher.satellite.egress.DataClassification;
import io.github.mikusher.satellite.egress.DataOrigin;
import io.github.mikusher.satellite.egress.EgressEnvelope;
import io.github.mikusher.satellite.egress.Key;
import io.github.mikusher.satellite.egress.SatelliteSchema;
import io.github.mikusher.satellite.egress.TrustLevel;
import io.github.mikusher.satellite.egress.policy.EgressProcessor;
import io.github.mikusher.satellite.egress.policy.EgressReport;
import io.github.mikusher.satellite.egress.policy.EgressSink;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;

import java.util.concurrent.TimeUnit;

/**
 * Baseline throughput/latency only: compare runs on the same JVM/hardware.
 * No hard-coded performance thresholds or claims about production latency.
 */
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MICROSECONDS)
@State(Scope.Thread)
public class SatelliteBenchmarks {

    private EgressEnvelope publicEnvelope;
    private EgressEnvelope mixedEnvelope;
    private EgressProcessor processor;
    private SatelliteData data;
    private SatelliteSchema schema;

    @Setup
    public void setup() {
        Key<String> orderId = Key.string("order.id")
                .classifiedAs(DataClassification.PUBLIC);
        Key<String> email = Key.string("customer.email")
                .classifiedAs(DataClassification.CONFIDENTIAL);

        publicEnvelope = EgressEnvelope.builder()
                .put(orderId, "ORDER-9001").build();
        mixedEnvelope = EgressEnvelope.builder()
                .put(orderId, "ORDER-9001")
                .put(email, "alice@example.com")
                .build();

        data = new SatelliteData();
        data.put("order.id", "ORDER-9001");
        data.put("customer.email", "alice@example.com");
        schema = SatelliteSchema.builder("Order")
                .required(orderId).optional(email).build();

        processor = EgressProcessor.secureDefaults();
    }

    @Benchmark
    public EgressReport publicLog() {
        return processor.process(publicEnvelope, EgressSink.LOG, "request-log");
    }

    @Benchmark
    public EgressReport mixedLog() {
        return processor.process(mixedEnvelope, EgressSink.LOG, "request-log");
    }

    @Benchmark
    public DataBridgeResult strictBridge() {
        return SatelliteDataEgressBridge.toEnvelope(
                data, schema, DataOrigin.APPLICATION, TrustLevel.VALIDATED);
    }
}
