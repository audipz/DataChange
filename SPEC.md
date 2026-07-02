# SPEC: GitOps Data Change Framework

## 1. Ziel
Das Framework fuehrt fachliche Datenaenderungen deklarativ aus JSON-ChangeSets aus. Es nutzt ausschliesslich JPA/Hibernate (`EntityManager`) fuer schreibende Operationen, damit Bean Validation, Entity Listener, Optimistic Locking und Hibernate Envers automatisch greifen.

## 2. Scope
- Declarative ChangeSets aus JSON
- Operationen: `insert`, `update`, `delete`, `upsert`, `merge`
- Bedingte Ausfuehrung (Condition Engine)
- Idempotenz ueber interne Changelog-Tabelle
- Ausfuehrungsmodi: Startup, REST, CLI, Spring Batch
- SPI fuer eigene Conditions, Operationen, Validatoren und Funktionen

## 3. Non-Goals
- Keine Schema-Migration (DDL)
- Kein Git-Cloning im Framework
- Keine SQL-DML (`insert/update/delete`) im Framework
- Keine direkten Schreibzugriffe in Envers `*_AUD`-Tabellen

## 4. Technische Basis
- Java 21+
- Spring Boot 3.x (Projektseitig pruefen)
- Hibernate 6 + Envers
- Jackson

## 5. Architekturmodule
1. **Parser**: JSON Parsing + Schema Validation + `specVersion` Mapping
2. **Loader**: Finden/Laden von ChangeSet-Dateien
3. **Validator**: Typ-, Feld-, Entity- und Referenzpruefung
4. **Execution Engine**: Orchestrierung, Reihenfolge, Reporting
5. **Entity Resolver**: Metamodel + Reflection + BeanWrapper
6. **Condition Engine**: Parsing und Auswertung von Bedingungen
7. **Expression Engine**: Built-in und Plugin-Funktionen
8. **Transaction Manager**: `CHANGESET`, `PER_CHANGE`, `CUSTOM`
9. **Audit Integration**: Envers-Kompatibilitaet sicherstellen
10. **GitOps Integration**: Konsumiert bereitgestellte JSON-Dateien
11. **Plugin API**: Erweiterungspunkte fuer Framework-Features

## 6. ChangeSet-Format (JSON v1)
Pflichtfelder eines ChangeSets:
- `id`
- `author`
- `description`
- `labels`
- `tags`
- `transactionMode`
- `preConditions`
- `changes`

Beispiel:

```json
{
  "specVersion": "1.0",
  "id": "customer-seed-001",
  "author": "team-data",
  "description": "Initial role setup",
  "labels": ["seed"],
  "tags": ["v1"],
  "transactionMode": "CHANGESET",
  "preConditions": {
    "expression": "not exists(Role where name='ADMIN')"
  },
  "changes": [
    {
      "id": "create-admin-role",
      "op": "insert",
      "entity": "Role",
      "values": {
        "name": "ADMIN",
        "active": true
      },
      "saveAs": "adminRole"
    }
  ]
}
```

## 7. Operationen
### Pflicht (MVP)
- `insert`
- `update`
- `delete`
- `upsert`
- `merge`

### Optional (Plugin)
- `copy`, `clone`, `replace`
- `executeBean`, `executeJavaMethod`, `executeScript`
- `executeSQLReadOnly` (nur lesend)

## 8. Condition Engine
### Unterstuetzte Operatoren
- `==`, `!=`, `>`, `<`, `>=`, `<=`
- `contains`, `matches`, `in`, `not in`, `startsWith`, `endsWith`

### Logische Operatoren
- `and`, `or`, `xor`, `not`

### Beispiele
- `entity(Customer).count() == 0`
- `exists(Customer where email='abc@test.de')`
- `not exists(Role where name='ADMIN')`
- `Customer[id=10].status == 'ACTIVE'`
- `application.version >= '3.5'`

## 9. Referenzen zwischen Aenderungen
Eine Aenderung kann Ergebnisse vorheriger Aenderungen referenzieren:
- generierte IDs
- UUIDs
- Revisionen

Mechanismus:
- `saveAs` speichert Ergebnis in Run-Context
- `ref('name')` liest gespeicherten Wert

## 10. Idempotenz
Interne Tabelle:

```text
DATA_CHANGELOG
--------------
id
checksum
author
gitCommit
executedAt
duration
status
applicationVersion
environment
hostname
```

Regeln:
- Erfolgreiche ChangeSets werden nicht erneut ausgefuehrt
- Checksum-Differenzen werden erkannt und konfigurierbar behandelt (`FAIL`, `WARN`)

## 11. Transaktionen
- `CHANGESET`: ein ChangeSet = eine Transaktion
- `PER_CHANGE`: jede Operation eigene Transaktion
- `CUSTOM`: konfigurierbare Gruppierung

## 12. Fehlerbehandlung
Konfigurierbare Strategien:
- `FAIL_FAST`
- `CONTINUE`
- `RETRY`
- `ROLLBACK`
- `COMPENSATION`

## 13. Envers-Anforderungen
- Nur JPA-basierte Schreiboperationen
- Keine direkten `*_AUD`-Writes
- Keine nativen SQL-DML-Shortcuts

## 14. Performance
- Batch Inserts/Updates
- Paging und Streaming grosser Datenmengen
- konfigurierbare Flush-Groesse
- Diff Engine: nur echte Feldaenderungen schreiben

## 15. Logging & Observability
Pro Run/ChangeSet:
- gestartet, uebersprungen, erfolgreich, fehlgeschlagen
- ausgewertete Bedingungen
- Anzahl Inserts/Updates/Deletes
- Dauer

## 16. Validierung vor Ausfuehrung
- JSON-Schema Validation
- Entity Validation
- Feld- und Typpruefung
- Referenzpruefung

## 17. Sicherheit
- Optionales Allowlisting fuer Entities
- Optionales Blocklisting sensibler Felder
- `execute*` Operationen standardmaessig deaktiviert

## 18. Testing-Anforderungen
- Unit Tests: Parser, Condition Engine, Diff Engine, Validatoren
- Integrationstests: JPA + Envers + Transaktionen + Idempotenz
- Contract Tests: SPI-Erweiterungen

## 19. Akzeptanzkriterien (Definition of Done)
- Keine SQL-DML im Framework fuer Datenaenderungen
- Envers erzeugt nachvollziehbare Revisionen
- Idempotenz und Checksum-Pruefung funktionieren
- Conditions mit DB-Funktionen sind testabgedeckt
- SPI erlaubt mindestens eine Custom Condition und eine Custom Operation

