# DataChange Audit Log

Das Framework speichert automatisch alle ausgeführten ChangeSets in der Tabelle `DATA_CHANGELOG` für Audit-Zwecke und um Doppelausführungen zu verhindern (Idempotenz).

## Tabellenstruktur

| Spalte | Typ | Beschreibung |
|--------|-----|-------------|
| `id` | VARCHAR(200) | ChangeSet-Identifier (Primary Key) |
| `checksum` | VARCHAR(128) | SHA-256 Hash des ChangeSet-Inhalts zur Detektion von Änderungen |
| `author` | VARCHAR(200) | Autor aus ChangeSet-Definition |
| `git_commit` | VARCHAR(64) | Optional: Git Commit SHA |
| `status` | VARCHAR(32) | SUCCESS, FAILED, SKIPPED |
| `executed_at` | TIMESTAMP | Zeitstempel der Ausführung |
| `duration_ms` | BIGINT | Ausführungsdauer in Millisekunden |
| `inserts` | BIGINT | Anzahl eingefügter Records |
| `updates` | BIGINT | Anzahl aktualisierter Records |
| `deletes` | BIGINT | Anzahl gelöschter Records |
| `error_message` | VARCHAR(1024) | Fehlermeldung bei Fehler (gekürzt auf 1024 Zeichen) |
| `application_version` | VARCHAR(64) | Version der Applikation während Ausführung |
| `environment` | VARCHAR(64) | Umgebung (z.B. prod, staging, dev) |
| `hostname` | VARCHAR(128) | Hostname des ausführenden Servers |

## Idempotenz-Prüfung

Das Framework verhindert Doppelausführungen:

1. **Beim Startup**: Vor der Ausführung wird geprüft, ob ein ChangeSet bereits erfolgreich ausgeführt wurde.
2. **Checksummen-Vergleich**: Die Checksumme wird abgeglichen. Bei Änderungen am ChangeSet-Inhalt wird ein Fehler geworfen.
3. **Status-Prüfung**: Nur ChangeSets mit Status `SUCCESS` werden als bereits ausgeführt behandelt.

## REST API für Audit-Log-Abfrage

### Alle Ausführungen abrufen

```
GET /datachange/audit/history?limit=100&status=SUCCESS
```

**Parameter:**
- `limit` (optional, default: 100): Max. Anzahl Einträge
- `status` (optional): Filter nach SUCCESS oder FAILED

**Response:**
```json
[
  {
    "id": "001-customer-seed",
    "checksum": "abc123def456...",
    "author": "admin",
    "status": "SUCCESS",
    "executedAt": "2026-07-02T05:30:00Z",
    "durationMs": 1234,
    "inserts": 5,
    "updates": 0,
    "deletes": 0,
    "errorMessage": null,
    "applicationVersion": "1.0.0",
    "environment": "prod",
    "hostname": "app-server-01"
  }
]
```

### Einzelnes ChangeSet abrufen

```
GET /datachange/audit/changeset/001-customer-seed
```

## Konfiguration

**in application.yaml:**

```yaml
info:
  app:
    version: "1.0.0"
spring:
  profiles:
    active: "prod"
```

Diese Werte werden automatisch in das Audit-Log geschrieben.

## Best Practices

1. **Checksummen-Konflikte**: Bei Änderungen am ChangeSet-Inhalt muss die ID erhöht werden (z.B. `002-...` statt `001-...`).
2. **Fehlerbehandlung**: Fehlgeschlagene ChangeSets können manuell korrigiert und neu ausgeführt werden (mit neuer ID).
3. **Audit-Abfragen**: Nutzen Sie die REST API zur Überprüfung von Ausführungshistorien.
4. **Monitoring**: Überwachen Sie fehlgeschlagene ChangeSets (Status = FAILED).

