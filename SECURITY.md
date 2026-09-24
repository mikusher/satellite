# Security Policy

## Supported code

The active 2.x work is developed on `satellite-v2-foundation` until it is ready to merge. Security fixes for the legacy API should avoid breaking compatibility where practical.

## Reporting a vulnerability

Please do not open a public issue for a suspected vulnerability.

Use GitHub's private vulnerability reporting for this repository when available. Include:

- affected module and version/commit;
- minimal reproduction steps;
- security impact;
- whether untrusted input is required;
- any suggested mitigation.

If private vulnerability reporting is unavailable, contact the repository owner through the contact information in the project metadata and avoid including exploit details in public channels.

## Security model

Satellite treats outbound data movement as an explicit policy boundary. The default egress policy is fail-closed for credentials, secrets and restricted data.

Important guarantees of the current 2.x foundation:

- denied values are omitted from sink output;
- violation reports do not contain the protected value;
- HMAC tokenization requires at least 32 bytes of secret material;
- conflicting external key definitions are rejected;
- the legacy PMAP/XML parser disables DTD/external entity processing;
- PMAP parsing has finite resource budgets;
- release publishing does not run on ordinary pushes to the default branch.

## Non-goals

Satellite is not a substitute for authorization, encryption, secrets management, endpoint DLP, consent management or full taint/data-flow analysis. Classification is application-provided metadata and must be reviewed like any other security policy.

## Dependency and supply-chain security

Pull requests are subject to dependency review. CodeQL is configured for Java. The Maven reactor generates a CycloneDX SBOM during `verify`. Release publication refuses snapshot versions.
