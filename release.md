# Dependency-resolver release history

## v1.1.1, In progress

## v1.1.0, 2026-02-12
- Add constructor overloads for `DependencyResolver` that accept a `ClassLoader` or `URLClassLoader` to allow more flexible usage outside GroovyShell contexts

## v1.0.2, 2024-02-10
- Upgrade dependencies
  - log4j 2.20.0 -> 2.25.2
  - maven-3.9.4-utils -> maven-utils
  - gradle 9.2.0 -> 9.3.1
- delegate latest artifact resolution to `maven-utils` `ArtifactLookup` to reduce duplicated logic
- Harden resolution internals (secure XML parsing, classloader guard fix, timeout-bounded and atomic pom downloads)
- split tests into default hermetic tests and opt-in `integrationTest` network tests

## v1.0.1, 2025-03-12
- Compile static for enhanced performance as no dynamic features are used.
- upgrade log4j 2.20.0 -> 2.24.3
- upgrade maven-3.9.4-utils -> 1.0.3
- Upgrade groovy 4.0.25 -> 4.0.26

## v1.0.0, 2024-02-24
- Initial release
