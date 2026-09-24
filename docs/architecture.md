# Satellite modular architecture

Satellite is a single repository containing independently usable Maven modules.

## Dependency boundaries

```text
satellite-parametermap                 satellite-egress-core
        │                                      │
        │                              satellite-egress-policy
        │                                      │
        │                    ┌─────────────────┼──────────────────┐
        │                    ▼                 ▼                  ▼
        │          egress-observability  egress-jackson  egress-opentelemetry
        │
        └──────────────┐
                       ▼
          satellite-parametermap-egress-bridge
                       ▲
                       │
               satellite-egress-core
               satellite-egress-policy
```

`satellite-legacy-logging` is intentionally isolated from both product lines.

## Rules

1. `satellite-parametermap` must never depend on an Egress module.
2. `satellite-egress-core` must never depend on ParameterMap.
3. Egress policy/adapters may depend only on Egress modules plus their external integration API.
4. Only `satellite-parametermap-egress-bridge` may depend on both ParameterMap and Egress.
5. Legacy logging remains isolated and is not a dependency of the new Egress adapters.

## ParameterMap responsibility

ParameterMap remains a general dynamic-data utility:

- typed conversion and retrieval;
- `ParameterInfoMap` constraints;
- nested maps/lists;
- PMAP/XML interoperability;
- JDBC/conversion utilities.

Parser security is part of this module, including DTD/external-entity blocking and configurable resource limits.

## Egress responsibility

The Egress framework controls application data at output boundaries.

Static key metadata:

- `DataClassification`;
- `DataCategory`;
- type information;
- required/optional schema metadata.

Runtime value metadata:

- `DataOrigin`;
- `TrustLevel`.

This separation matters because one logical key may receive values from different origins and with different trust levels.

## Policy flow

```text
SatelliteMap
    │
    ▼
EgressPolicyEngine
    │
    ├── ALLOW ──────► original value
    ├── REDACT ─────► redacted representation
    ├── TOKENIZE ───► deterministic pseudonym
    └── DENY ───────► no output + privacy violation
    │
    ▼
EgressReport
    │
    ▼
approved sink adapter
```

The engine is fail-closed. If no rule matches, egress is denied.

## Security invariants

- secrets/credentials are denied by the default policy;
- restricted data requires explicit application policy;
- adapters do not receive or expose raw maps;
- violations contain metadata and reason codes, never the protected value;
- duplicate external key names with conflicting security metadata are rejected;
- HMAC tokenization uses key-name domain separation;
- output serialization/logging happens only after policy evaluation;
- ParameterMap bridge rejects unclassified source fields by default.

## Current adapters

- SLF4J;
- Jackson;
- OpenTelemetry spans.

Jackson additionally exports `SatelliteSchema` as JSON Schema 2020-12.

## Compatibility

The foundation remains Java 11 source-compatible and is verified on Java 11, 17 and 21.

The repository version is currently `2.0.0-SNAPSHOT`. Publication is gated to release/manual workflows and snapshots are rejected by the release job.
