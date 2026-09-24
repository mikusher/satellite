# Satellite modular architecture

Satellite is a single repository containing independent Maven modules.

The design deliberately separates the generic ParameterMap library from the security/privacy Egress framework.

## Modules

### satellite-parametermap

Owns the existing ParameterMap data model, converters, PMAP/XML format and related utilities.

It has **no dependency on Egress**.

### satellite-legacy-logging

Contains the pre-v2 JSON/SLF4J logger for compatibility. Keeping it separate prevents the generic ParameterMap artifact from imposing a logging backend or logging API.

### satellite-egress-core

Dependency-light security/privacy model:

- `Key<T>`
- `SatelliteMap`
- `SatelliteEntry<T>`
- `DataClassification`
- `DataCategory`
- `DataOrigin`
- `TrustLevel`
- `ValueMetadata`
- `SatelliteSchema`

It has **no dependency on ParameterMap**.

### satellite-egress-policy

Policy enforcement:

- `EgressPolicyEngine`
- `PolicyRule`
- `DefaultEgressRule`
- `ALLOW / REDACT / TOKENIZE / DENY`
- `EgressProcessor`
- `PrivacyViolation`
- `HmacSha256Tokenizer`

Depends only on Egress Core.

### satellite-egress-observability

SLF4J adapter with mandatory policy processing and log-control-character escaping.

### satellite-egress-jackson

Policy-enforced Jackson serialization plus JSON Schema Draft 2020-12 export.

### satellite-egress-opentelemetry

Policy-enforced OpenTelemetry span attributes.

### satellite-parametermap-egress-bridge

Optional integration between ParameterMap and Egress.

This is the only module allowed to depend on both product lines.

## Dependency direction

```text
satellite-parametermap                 satellite-egress-core
        │                                      │
        │                                      ▼
        │                              satellite-egress-policy
        │                               /       |        \
        │                              /        |         \
        │                             ▼         ▼          ▼
        │                       observability  jackson  opentelemetry
        │
        └──────────────┐
                       ▼
          satellite-parametermap-egress-bridge
                       ▲
                       │
              satellite-egress-core
              satellite-egress-policy
```

The module reactor itself enforces most of this boundary: code cannot import another product line unless its Maven module declares that dependency.

## Security model

Static semantics belong to `Key<T>`:

- classification;
- categories;
- Java type;
- required/optional schema semantics.

Runtime semantics belong to each value:

- origin;
- trust level.

This is intentional. The same logical key may receive values from different origins or validation states across requests.

## Egress flow

```text
typed value
   │
   ▼
SatelliteMap
   │
   ▼
EgressContext (sink + purpose)
   │
   ▼
EgressPolicyEngine
   │
   ├── ALLOW ───────► original representation
   ├── REDACT ──────► safe marker/replacement
   ├── TOKENIZE ────► deterministic pseudonym
   └── DENY ────────► omitted + PrivacyViolation
   │
   ▼
sink-specific adapter
```

Adapters do not receive a raw-map escape hatch.

## ParameterMap bridge

The bridge treats legacy data as unclassified until the application supplies explicit `Key<?>` definitions.

Strict conversion rejects unknown ParameterMap fields. A separately named lenient migration method exists only for deliberate staged adoption.

## Parser boundary

PMAP/XML parsing:

- disables DTD processing;
- disables external entities;
- rejects external XML resolution;
- can bound input size, nesting depth, total entries, per-collection size and text length;
- preserves caller ownership of output streams.

## Build and supply chain

- Java 11 source/target.
- CI matrix: Java 11, 17, 21.
- Dependabot for Maven and GitHub Actions.
- CodeQL `security-extended`.
- pull-request dependency review.
- aggregate CycloneDX SBOM during `verify`.
- release workflow will not publish snapshot versions.

## Future boundaries

Features such as additional adapters or JSON Schema import should remain optional modules. The Egress Core should remain small and must not accumulate framework dependencies.
