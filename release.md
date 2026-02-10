# Dependency-resolver release history

## v1.0.2, in progress
- upgrade log4j 2.20.0 -> 2.25.2
- upgrade maven-3.9.4-utils -> maven-3.9.11-utils
- upgrade to gradle 9.2.0
- delegate latest artifact resolution to `maven-utils` `ArtifactLookup` to reduce duplicated logic
- harden resolution internals (secure XML parsing, classloader guard fix, timeout-bounded and atomic pom downloads)
- split tests into default hermetic tests and opt-in `integrationTest` network tests
## v1.0.1, 2025-03-12
- Compile static for enhanced performance as no dynamic features are used.
- upgrade log4j 2.20.0 -> 2.24.3
- upgrade maven-3.9.4-utils -> 1.0.3
- Upgrade groovy 4.0.25 -> 4.0.26

## v1.0.0, 2024-02-24
- Initial release
