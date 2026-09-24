# Satellite

**Typed dynamic data, validation and structured observability for Java.**

Satellite is a Java library built around `ParameterMap`: a dynamic data container with typed conversion, schema-like constraints, serialization support and structured logging utilities.

The current 1.x codebase is being hardened while the project evolves toward a smaller, security-aware 2.x core.

## Why Satellite?

Java applications often need to move data that is more dynamic than a domain POJO but should be safer and more predictable than an untyped `Map<String, Object>`. Satellite provides a bridge between those models.

Current capabilities include:

- typed parameter access and conversion;
- optional parameter constraints through `ParameterInfoMap`;
- nested maps and lists;
- PMAP/XML streaming serialization;
- structured JSON logging on top of SLF4J;
- JDBC-oriented type conversion helpers.

## Example

```java
ParameterMap context = new ParameterMap();
context.put("caseNumber", "C12.12343");
context.put("step", "Assignment");
context.put("department", "BPM");

Logger logger = LoggerFactory.getLogger(MyService.class);
logger.info()
        .message("Processing case")
        .map("context", context)
        .log();
```

## Security posture

Satellite treats parsing and serialization as security-sensitive functionality.

The current hardening work includes:

- disabling XML DTD and external entity processing by default;
- parser security regression tests;
- dependency monitoring;
- dedicated CI verification across supported JDKs.

See [SECURITY.md](SECURITY.md) for vulnerability reporting.

## 2.x direction

The next major version is planned around a security-aware typed data envelope rather than a logging framework.

A future API may look like this:

```java
Key<String> userId = Key.string("user.id").required();
Key<String> accessToken = Key.string("auth.access_token").sensitive();

SatelliteMap data = SatelliteMap.builder()
        .put(userId, "user-123")
        .put(accessToken, token)
        .build();
```

The design goals are:

- immutable data by default;
- typed keys and schema validation;
- explicit origin, trust and sensitivity metadata;
- policy-driven redaction and serialization;
- Jackson and JSON Schema interoperability;
- adapters for SLF4J 2 and OpenTelemetry;
- bounded parsing with depth, entry and input-size limits.

The 2.x API shown above is a roadmap and is not part of the current release.

## Build

```bash
mvn verify
```

The project currently targets Java 11. CI also verifies compatibility on Java 17 and 21.

## License

MIT
