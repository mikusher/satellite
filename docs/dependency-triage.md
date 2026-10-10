# Dependency update triage — 2026-10-10

This is a **review record**, not evidence that any upgrade is installed or safe. Changes must pass relevant CI/security workflows on their final merge commit. Latest reviewed open Dependabot PRs: [#23–#32](https://github.com/mikusher/satellite/pulls?q=is%3Apr+is%3Aopen+author%3Aapp%2Fdependabot).

## Java library patch updates

| PR | Upgrade | Recommendation |
| --- | --- | --- |
| [#24](https://github.com/mikusher/satellite/pull/24) | Jackson 2.22.2 → 2.22.3 | Review changelog, then test all modules and Jackson serialization/schema/adversarial behavior |
| [#27](https://github.com/mikusher/satellite/pull/27) | Commons Lang 3.20.0 → 3.21.0 | Review changelog, then build on Java 11/17/21 |
| [#28](https://github.com/mikusher/satellite/pull/28) | Guava 33.7.1-jre → 33.7.2-jre | Review changelog/transitives, then run full matrix and consumer tests |

All three patches touch **only version properties in the root POM**. They are good candidates for a targeted maintenance PR, but are not approved for automatic merge based only on version numbers.

## GitHub Actions updates

| PR | Upgrade | Recommended verification |
| --- | --- | --- |
| [#23](https://github.com/mikusher/satellite/pull/23) | setup-java v4 → v6 in CodeQL workflow | Verify Node runtime support and CodeQL build |
| [#30](https://github.com/mikusher/satellite/pull/30) | checkout v4 → v7 in CodeQL and dependency review | Verify both workflows; overlaps the files in #23 |
| [#29](https://github.com/mikusher/satellite/pull/29) | dependency-review-action v4 → v5 | Check permissions and dependency-review outcome |
| [#26](https://github.com/mikusher/satellite/pull/26) | first-interaction v1 → v3 | Check greeting job on first issues/PRs |
| [#25](https://github.com/mikusher/satellite/pull/25) | SonarQube scan action v8.2.1 → v8.3.0 | Requires real SonarQube configuration/secrets; CI alone may not exercise it |
| [#31](https://github.com/mikusher/satellite/pull/31) | TruffleHog v3.97.5 → v3.99.0 | Verify scan scope and workflow output on a test PR |
| [#32](https://github.com/mikusher/satellite/pull/32) | Checkmarx action 2.3.42 → 2.3.45 | Check provider compatibility, credentials and scanner results |

Some workflow PRs were originally opened against an older base commit and may need an update/rebase. **Do not merge them indiscriminately**: a green generic Maven build does not validate an externally configured scanner action.

## Release relevance

- Before candidate publication, re-evaluate open security alerts and dependency-review findings. Access to actual private alerts is not established by this document.
- Merge low-risk, tested maintenance changes before freezing the candidate version; rerun all release readiness checks on the resulting commit.
- Never silently interpret a pending Dependabot PR as an applied fix.
- No publishing or version bump is authorized by this review.

See also [release-readiness.md](release-readiness.md).
