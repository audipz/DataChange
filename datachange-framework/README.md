# datachange-framework

Diese Library stellt ein generisches Data-Change-Framework auf Basis von JPA/Hibernate bereit.

## Kernfunktionen
- Laden von JSON-ChangeSets
- Semantische Basisvalidierung
- Ausfuehrung ueber `EntityManager` (kein SQL-DML)
- Idempotenz ueber `DATA_CHANGELOG`
- Startup-Autorun fuer Spring Boot

## Einbindung
Die Spring-Boot-Autokonfiguration wird ueber `AutoConfiguration.imports` geladen.

## Architektur

Das Modul ist in klare Schichten gegliedert:

- `api`: oeffentliche Einstiegspunkte fuer Anwender des Frameworks
- `model`: DSL-Domains und ChangeSet-Definitionen
- `spi`: stabile Erweiterungspunkte fuer Plugins und alternative Implementierungen
- `boot`: Spring-Boot-Auto-Konfiguration und Startausfuehrung
- `engine`, `parser`, `condition`, `loader`, `log`, `validation`: interne Umsetzung
- `internal`: kleinere Hilfsklassen, die bewusst nicht Teil der oeffentlichen API sind

## Oeffentliche API

Die folgenden Typen sind fuer Anwender vorgesehen:

- `de.graube.datachange.framework.api.DataChangeExecutor`
- `de.graube.datachange.framework.api.ChangeSetSource`
- `de.graube.datachange.framework.api.DataChangeExecutionResult`
- `de.graube.datachange.framework.api.ExecutionContext`
- `de.graube.datachange.framework.model.*`

## SPI / Erweiterungspunkte

Framework-Erweiterungen koennen ueber diese Schnittstellen erfolgen:

- `spi.OperationHandler` fuer neue Operationen
- `spi.ConditionProvider` fuer eigene Bedingungen
- `spi.ChangeSetSemanticValidator` fuer domae­nenspezifische Validierungen
- `spi.ChangeSetParser` und `spi.ChangeSetSerializer` fuer alternative DSL-Formate
- `spi.ConditionEvaluator` fuer alternative Evaluierungslogik
- `spi.EntityResolver` fuer alternative Entity-Aufloesung

## Konfiguration

Beispiel in `application.yaml`:

```yaml
datachange:
  enabled: true
  mode: STARTUP
  locations:
  - classpath*:datachange/*.json
  failure-strategy: FAIL_FAST # FAIL_FAST | CONTINUE
  dry-run: false
  include-ids: []
  exclude-ids: []
  include-labels: []
```

- `failure-strategy`: bestimmt das Verhalten, wenn ein ChangeSet fehlschlaegt.
- `dry-run`: laedt und prueft ChangeSets, fuehrt aber keine Aenderung aus.
- `include-ids`/`exclude-ids`: ID-basierte Selektion.
- `include-labels`: fuehrt nur ChangeSets mit mindestens einem passenden Label aus.

### Startup-Validierung

Im Modus `STARTUP` prueft das Framework die Konfiguration vor der Ausfuehrung:

- `datachange.locations` darf nicht leer sein und keine leeren Eintraege enthalten.
- `datachange.include-ids`, `datachange.exclude-ids`, `datachange.include-labels` duerfen keine leeren Eintraege enthalten.
- `datachange.include-ids` und `datachange.exclude-ids` duerfen sich nicht ueberschneiden.

