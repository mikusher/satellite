# Security Policy

Satellite processes structured data and may be used near application trust boundaries. Security reports are treated as first-class engineering issues.

## Supported versions

Until the 2.x line is released, security fixes are delivered on the latest maintained release line. Older versions should not be assumed to receive fixes.

## Reporting a vulnerability

Please avoid publishing exploit details in a public issue before a fix is available.

Use GitHub Security Advisories or private vulnerability reporting when available for this repository. If that channel is unavailable, contact the maintainer privately at `mikusher@hotmail.com`.

Include, where possible:

- affected version or commit;
- minimal reproduction steps;
- security impact and required preconditions;
- whether untrusted input is required;
- suggested mitigation, if known.

## Security design goals

The project aims to make unsafe parser and serialization behavior opt-in rather than default.

The 2.x roadmap also includes:

- explicit input-size, collection-size and nesting-depth limits;
- policy-driven handling of sensitive values;
- deterministic serialization;
- fuzzing for parser boundaries;
- software bill of materials and release provenance.
