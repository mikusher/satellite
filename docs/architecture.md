# Satellite modular architecture

Satellite remains a single repository, but the codebase is intended to evolve as independent Maven modules.

## Module boundaries

- `satellite-parametermap`
  - Legacy and modern ParameterMap functionality.
  - Must remain independently usable.
  - Must not depend on any egress/privacy module.

- `satellite-egress-core`
  - Security and privacy metadata model.
  - Typed keys and data classification primitives.
  - Must not depend on ParameterMap.

- `satellite-egress-policy`
  - Policy evaluation and decisions.
  - ALLOW, REDACT, TOKENIZE and DENY decisions.
  - Depends only on egress core.

- `satellite-egress-observability`
  - Safe adapters for logging, tracing and serialization sinks.
  - No raw sensitive-data bypasses.

- `satellite-parametermap-egress-bridge`
  - Optional integration between ParameterMap and the egress model.
  - This is the only module allowed to depend on both sides.

## Dependency rules

```text
satellite-parametermap
        │
        │  no dependency
        ▼
   [independent]

satellite-egress-core
        │
        ▼
satellite-egress-policy
        │
        ▼
satellite-egress-observability

satellite-parametermap ─────┐
                           ├── satellite-parametermap-egress-bridge
satellite-egress-core ─────┘
```

ParameterMap must never depend on Egress.

Egress must never depend on ParameterMap.

Only the bridge may reference both.

## Delivery order

1. Physically modularize the Maven reactor while preserving Java 11 compatibility.
2. Preserve and test ParameterMap behavior.
3. Add typed egress metadata:
   - `Key<T>`
   - `DataClassification`
   - `DataCategory`
   - `DataOrigin`
   - `TrustLevel`
4. Add an explicit policy engine:
   - `ALLOW`
   - `REDACT`
   - `TOKENIZE`
   - `DENY`
5. Add security/privacy violation events.
6. Add deterministic pseudonymization/HMAC where correlation is required.
7. Add sink adapters such as SLF4J, OpenTelemetry and Jackson.
8. Add schema and JSON Schema interoperability.

## Security invariants

- Sensitive export must require an explicit policy decision.
- Security-sensitive operations fail closed.
- Adapters must not expose a generic raw-map escape hatch.
- Secrets and credentials must never be emitted by default.
- Privacy/security violations should be observable without leaking the protected value.
- Parser and serialization limits should be bounded and tested.

## Compatibility

The foundation line remains Java 11 compatible. A later 2.x release may raise the baseline only as an explicit breaking change.
