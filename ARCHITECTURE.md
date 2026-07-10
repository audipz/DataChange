# Architecture

## Overview

DataChange is organized as a small public framework API with a Spring Boot integration layer and a set of internal execution components.

```mermaid
flowchart TD
    A[Spring Boot App] --> B[DataChangeAutoConfiguration]
    B --> C[DataChangeStartupRunner]
    C --> D[ChangeSetSource]
    C --> E[DataChangeExecutor]

    D --> F[ResourceChangeSetSource]
    F --> G[ChangeSetParser SPI]
    G --> H[JsonChangeSetParser]

    E --> I[DataChangeEngine]
    I --> J[ConditionEvaluator SPI]
    I --> K[OperationHandler SPI]
    I --> L[DataChangeLogService]
    I --> M[JpaOperationExecutor]

    M --> N[EntityResolver SPI]
    M --> O[WhereClauseParser]
    M --> P[PropertyApplier]
    M --> Q[OperationValueResolver]

    J --> R[DefaultConditionEvaluator]
    R --> S[ConditionExpressionParser]

    L --> T[DATA_CHANGELOG]
```

## Layers

### Public API
- `io.github.audipz.datachange.framework.api`
- `io.github.audipz.datachange.framework.model`
- `io.github.audipz.datachange.framework.spi`

### Spring Integration
- `io.github.audipz.datachange.framework.boot`
- `io.github.audipz.datachange.framework.rest`
- `io.github.audipz.datachange.framework.config`

### Internal Implementation
- `io.github.audipz.datachange.framework.engine`
- `io.github.audipz.datachange.framework.parser`
- `io.github.audipz.datachange.framework.loader`
- `io.github.audipz.datachange.framework.condition`
- `io.github.audipz.datachange.framework.validation`
- `io.github.audipz.datachange.framework.log`
- `io.github.audipz.datachange.framework.internal`

## Extension Points

- **Conditions** via `ConditionProvider` and `ConditionEvaluator`
- **Operations** via `OperationHandler`
- **Validation** via `ChangeSetSemanticValidator`
- **Parsing** via `ChangeSetParser` and `ChangeSetSerializer`
- **Entity resolution** via `EntityResolver`

## Release Packaging

The framework module is prepared for Maven Central with:
- source JAR
- javadoc JAR
- GPG signing in release profile
- SCM metadata
- license metadata
- GitHub Actions workflows

