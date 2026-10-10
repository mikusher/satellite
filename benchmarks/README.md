# Satellite JMH baseline

This is an **independent, non-reactor** JMH project. It does not ship with Satellite and does not add runtime dependencies to any of the seven modules.

## Run locally

Requires Maven and a Java 21 runtime (the benchmark sources target Java 11):

```bash
mvn --batch-mode --no-transfer-progress -DskipTests install
mvn --batch-mode --no-transfer-progress -f benchmarks/pom.xml package
java -jar benchmarks/target/satellite-benchmarks.jar \
  -f 1 -wi 2 -i 3 -w 1s -r 1s -t 1 \
  -rf json -rff benchmarks/target/jmh-results.json
```

Or run GitHub Actions **JMH baseline (non-release)** manually. Its PR trigger covers changes to the benchmark implementation.

Measurements cover the default-policy public log path, the mixed public/confidential log path, and strict schema-backed bridge conversion. All benchmark state is prepared outside the timed methods.

JMH outputs **microseconds per operation**. These CI timings are *diagnostic baselines, not performance guarantees*, and cannot establish production latency. For comparisons, use the same Java version, host class, options and input dataset; use more forks and longer iterations for stable measurements.

The benchmark does not measure actual network, SLF4J backends, OpenTelemetry exporters or disk I/O.
