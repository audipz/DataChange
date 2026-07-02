# MVP Roadmap

## Iteration 1 - Foundation
- Grundlegende Modulstruktur und API/SPI-Skelette anlegen
- ChangeSet-Domaenenmodell (`ChangeSetDefinition`, `ChangeDefinition`) einfuehren
- Basiskonfiguration (`datachange.*`) definieren
- Technische ADRs fuer Transaction- und Idempotenzstrategie dokumentieren

## Iteration 2 - Parsing & Validation
- JSON Parser mit Jackson implementieren
- JSON-Schema Validation fuer `specVersion=1.0`
- Semantische Validation (Entity/Feld/Typ/Referenzen)
- Erste Negativtests fuer ungueltige ChangeSets

## Iteration 3 - Execution Engine MVP
- Operationen `insert`, `update`, `delete`
- Idempotenz mit `DATA_CHANGELOG`
- Transaktionsmodus `CHANGESET` und `PER_CHANGE`
- Basales Run-Reporting und Logging

## Iteration 4 - Conditions & Expressions
- Condition Parser + Evaluator (`and/or/not`, Vergleichsoperatoren)
- DB-Funktionen (`exists`, `count`)
- Expression Functions (`uuid`, `now`, `property`, `env`)
- Referenzmechanismus `saveAs/ref`

## Iteration 5 - Envers, Diff, Performance
- Diff Engine fuer minimale Updates
- Batch/Flush/Paging-Konfiguration
- Integrations-Tests mit Envers-Revisionspruefung
- Performance-Benchmarks fuer groessere Datenmengen

## Iteration 6 - Integration Surfaces
- Startup Runner
- REST Trigger Endpoint
- CLI Einstiegspunkt
- Spring Batch Job/Step Integration

## Iteration 7 - Plugin Ecosystem
- SPI-Endpunkte finalisieren und dokumentieren
- Beispielplugins (Custom Condition, Custom Operation)
- Contract-Testkit fuer Plugin-Autoren
- Erweiterte Dokumentation (How-To + Best Practices)

## Milestones
- **M1:** Parser + Validator stabil
- **M2:** Engine MVP produktiv testbar
- **M3:** Condition/Expression Feature-Complete
- **M4:** Integrationsmodi und SPI release-ready

