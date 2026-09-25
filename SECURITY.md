# Security Policy

## Supported code

The active 2.x work is developed on `satellite-v2-foundation` until it is ready to merge. Security fixes for compatibility APIs should avoid unnecessary breakage.

## Reporting a vulnerability

Do not open a public issue for a suspected vulnerability.

Use GitHub private vulnerability reporting when available. Include:

- affected module and version/commit;
- minimal reproduction steps;
- security impact;
- whether untrusted input is required;
- suggested mitigation when known.

If private reporting is unavailable, contact the repository owner through the project metadata and avoid publishing exploit details.

## Security model

Satellite treats outbound data movement as an explicit policy boundary.

The default Egress policy is fail-closed for credentials, secrets and restricted data.

Current guarantees include:

- denied values are omitted from sink output;
- privacy violations do not contain protected values;
- positive egress rules require sink + purpose;
- sensitive values cannot be emitted raw to observability sinks;
- HMAC tokenization requires at least 32 bytes of secret material;
- conflicting external key definitions are rejected;
- the Satellite Data bridge rejects unclassified fields by default;
- PMAP/XML parsing disables DTD/external entities and enforces finite resource budgets;
- release publishing does not run on ordinary pushes.

## Non-goals

Satellite is not a substitute for authorization, encryption, secrets management, endpoint DLP, consent management or full taint/data-flow analysis.

Classification is application-provided metadata and must be reviewed like any other security policy.

## Dependency and supply-chain security

CodeQL is configured for Java. The Maven reactor generates a CycloneDX SBOM during `verify`. Dependency review runs when GitHub Dependency Graph is available. Release publication refuses snapshot versions.
