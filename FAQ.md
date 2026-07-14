# DataChange Framework – FAQ & Häufige Fragen

## 🤔 Allgemein

### F: Was ist der Unterschied zwischen DataChange und Liquibase/Flyway?

**A:** 
| Aspekt | DataChange | Liquibase/Flyway |
|--------|-----------|-----------------|
| **Zweck** | Fachliche Datenänderungen | Schema-Migrationen |
| **Format** | JSON (deklarativ) | XML/SQL/YAML |
| **Ausführung** | Über JPA EntityManager | Direktes SQL |
| **Audit-Trail** | Automatisch via Envers | Manuelle Implementierung |
| **Entities** | Beliebige JPA-Entities | N/A |
| **Versionskontrolle** | Mit Business-Logik | Mit Schema-Änderungen |

**Empfehlungsmix:**
```
Schema-Migrationen → Flyway
Seed-Daten → DataChange
Status-Updates → DataChange
Bulk-Migration → DataChange
```

---

### F: Können mehrere ChangeSet-Dateien zur gleichen Zeit geladen werden?

**A:** Ja, DataChange sortiert automatisch nach Dateiname (alphanumerisch):
```
datachange/001-seed.json        ← Wird zuerst geladen
datachange/002-migrate.json     ← Dann dies
datachange/003-cleanup.json     ← Dann das
```

Alle werden sequenziell ausgeführt in einer Transaktion (bei `transactionMode: CHANGESET`).

---

### F: Was passiert bei einem Fehler während ChangeSet-Ausführung?

**A:** 
- Alle Änderungen werden zurückgerollt (ROLLBACK)
- ChangeSet wird als `FAILED` markiert
- Nächster Start versucht erneut
- Logs zeigen Fehlerdetails

```
Error: Could not find entity type 'InvalidEntity'
→ ROLLBACK
→ Changelog: status=FAILED
→ Nächster Start: Retry
```

---

## 🔐 Sicherheit & Datenschutz

### F: Können Passwörter/API-Keys in ChangeSet-Dateien hardcoded werden?

**A:** ❌ **NIEMALS!** 

Nutzen Sie Environment Variables:

```json
// ❌ FALSCH
{
  "values": {
    "password": "supersecret123"
  }
}

// ✅ RICHTIG
{
  "values": {
    "password": "${env('DB_USER_PASSWORD')}"
  }
}
```

Setzen Sie die Variable in:
- Docker: `ENV DB_USER_PASSWORD=...`
- Kubernetes: `env.DB_USER_PASSWORD`
- Application: System Properties

---

### F: Sollte die Changelog-Tabelle in Git versioniert sein?

**A:** ❌ **Nein!** Die Tabelle `DATA_CHANGELOG` ist nur für das System:

```sql
-- Diese Tabelle NICHT manuell bearbeiten
SELECT * FROM DATA_CHANGELOG;
-- Sie wird automatisch vom Framework verwaltet
```

---

## 🔄 Idempotenz & Wiederholung

### F: Was passiert, wenn ich ein ChangeSet zweimal ausführe?

**A:**
```bash
# 1. Ausführung
curl -X POST http://localhost:8080/datachange/execute?id=seed
# Status: SUCCESS

# 2. Ausführung (sofort danach)
curl -X POST http://localhost:8080/datachange/execute?id=seed
# Status: SKIPPED ← bereits erfolgreich ausgeführt
```

Gespeichert im Changelog:
```sql
SELECT * FROM DATA_CHANGELOG WHERE id='seed';
-- id='seed', status='SUCCESS', checksum=<hash>
```

### F: Wie bekomme ich das Ergebnis eines deployten ChangeSets?

**A:** Über den Audit-Endpunkt `GET /datachange/audit/changeset/{id}`. Dort sehen Sie Status, Laufzeit, Inserts/Updates/Deletes und Fehlerdetails.

```bash
curl http://localhost:8080/datachange/audit/changeset/seed-customers
```

Der Request übermittelt keine Änderungen. Er liest nur den auditierten Ausführungsstatus eines bereits deployten ChangeSets.

### F: Wie führe ich dasselbe ChangeSet erneut aus?

**A:** Ein ChangeSet mit identischer `id` und identischem Inhalt wird beim zweiten Lauf als `SKIPPED` behandelt. Wenn Sie es erneut ausführen möchten, müssen Sie die Definition ändern, z. B.:

1. die ChangeSet-ID anpassen,
2. den Inhalt/Checksum ändern, oder
3. den Changelog-Eintrag gezielt entfernen.

---

### F: Kann ich ein ChangeSet "rückgängig machen"?

**A:** ❌ Nicht direkt. Aber:

1. **Neues kompensierendes ChangeSet erstellen:**
```json
{
  "id": "compensation-undo-seed",
  "description": "Undo: remove seed customers",
  "changes": [
    {
      "op": "delete",
      "entity": "Customer",
      "where": "email == 'alice@x.de'"
    }
  ]
}
```

2. **Oder: Manuell Changelog-Eintrag löschen + ChangeSet modifizieren:**
```sql
DELETE FROM DATA_CHANGELOG WHERE id='seed';
-- Beim nächsten Start wird 'seed' erneut ausgeführt
```

---

## ⚙️ Performance & Optimierung

### F: Wie schnell ist die Ausführung?

**A:** Abhängig von Datenmenge:
- **Insert 1 Datensatz**: ~10-50ms
- **Insert 1.000 Datensätze**: ~200-500ms
- **Update 10.000 Datensätze**: ~1-3s

**Verbesserungen:**
```json
{
  "transactionMode": "PER_CHANGE"  // ← Parallele Verarbeitung (später)
}
```

---

### F: Gibt es Batch-Optimierungen?

**A:** Ja, via Hibernate:
```yaml
spring:
  jpa:
    properties:
      hibernate:
        jdbc:
          batch_size: 20
          fetch_size: 20
        order_inserts: true
        order_updates: true
```

Für sehr große Datenmengen: Mehrere kleine ChangeSets statt eines großen.

---

## 🐛 Debugging & Troubleshooting

### F: Wie kann ich ein ChangeSet debuggen?

**A:**
1. **Logs verbosen machen:**
```yaml
logging:
  level:
    io.github.audipz.datachange: DEBUG
    org.hibernate: DEBUG
```

2. **SQL anzeigen:**
```yaml
spring:
  jpa:
    properties:
      hibernate:
        format_sql: true
        show_sql: true
```

3. **ChangeSet-Validierung prüfen:**
```bash
# Logs sollten zeigen:
# - JSON parsing
# - Precondition evaluation
# - Entity metadata lookup
# - SQL generation
```

---

### F: Mein ChangeSet wird nicht geladen. Was tun?

**A:** Debug-Schritte:

1. **Standort prüfen:**
```yaml
# ✅ Korrekt
datachange:
  locations:
    - classpath*:datachange/*.json

# ❌ Falsch
datachange:
  locations:
    - datachange/*.json
```

2. **Logs prüfen:**
```
DEBUG: Scanning for ChangeSets in classpath*:datachange/*.json
DEBUG: Found: 001-seed.json, 002-migrate.json
```

3. **Datei-Encoding:**
```bash
# Muss UTF-8 sein
file -i src/main/resources/datachange/001-seed.json
```

---

## 🌍 GitOps & Kubernetes

### F: Wie funktioniert DataChange mit ArgoCD?

**A:** Workflow:

```
1. Git Commit (ChangeSet-JSON) → Push
        ↓
2. ArgoCD erkennt Änderung
        ↓
3. Pod wird deployed
        ↓
4. Spring App startet
        ↓
5. DataChangeEngine: Mode=STARTUP
        ↓
6. Neue ChangeSets werden automatisch ausgeführt
```

Keine zusätzliche Konfiguration nötig!

---

### F: Kann ich ChangeSets von mehreren Repos laden?

**A:** Ja:

```yaml
datachange:
  enabled: true
  locations:
    - classpath*:datachange/*.json           # Projekt-intern
    - file:/mnt/gitops/changesets/*.json     # GitOps Volume
    - file:/etc/datachange/*.json            # ConfigMap
```

---

## 📊 Monitoring & Observability

### F: Wie kann ich Ausführungen monitoren?

**A:** Über Logs + Metrics:

```bash
# Logs live anschauen
kubectl logs -f deployment/my-app | grep DataChange

# Changelog-Tabelle queryen
SELECT 
  id, status, executed_at, duration_ms 
FROM DATA_CHANGELOG 
ORDER BY executed_at DESC;
```

**Grafana Dashboard:**
```sql
SELECT 
  DATE_TRUNC('day', executed_at) as day,
  COUNT(*) as changesets_run,
  AVG(duration_ms) as avg_duration,
  SUM(CASE WHEN status='FAILED' THEN 1 ELSE 0 END) as failures
FROM DATA_CHANGELOG
GROUP BY day
ORDER BY day DESC;
```

---

## 🎯 Best Practices

### F: Sollte ich ein ChangeSet für jede kleine Änderung erstellen?

**A:** Nein, nutze Gruppierung:

```json
// ❌ Zu granular
{
  "id": "insert-customer-alice",
  "changes": [{ "op": "insert", "entity": "Customer", ... }]
}
{
  "id": "insert-customer-bob",
  "changes": [{ "op": "insert", "entity": "Customer", ... }]
}

// ✅ Besser
{
  "id": "seed-initial-customers",
  "changes": [
    { "op": "insert", "entity": "Customer", ... },  // Alice
    { "op": "insert", "entity": "Customer", ... }   // Bob
  ]
}
```

---

### F: Wann sollte ich `transactionMode: PER_CHANGE` nutzen?

**A:** Seltene Fälle:
- ✅ Wenn einzelne Ops fehlschlagen dürfen
- ✅ Wenn Operationen unabhängig sind
- ❌ Nicht default!

Empfehlung: Nutze `CHANGESET` (Alles-oder-Nichts).

---

### F: Wie vermeide ich Duplikate?

**A:** Immer Precondition verwenden:

```json
{
  "preConditions": {
    "expression": "not exists(Customer where email='alice@x.de')"
  },
  "changes": [...]
}
```

---

### F: Wie modelliere ich `@OneToOne`, `@OneToMany` und `@ManyToOne` in ChangeSets?

**A:** Setze Beziehungen ueber die **Owning Side** und nutze `saveAs`/`ref()` fuer Abhaengigkeiten.

```json
{
  "changes": [
    {
      "id": "insert-person",
      "op": "insert",
      "entity": "Person",
      "values": { "firstName": "Max", "lastName": "Mustermann", "age": 34 },
      "saveAs": "person"
    },
    {
      "id": "insert-address",
      "op": "insert",
      "entity": "Address",
      "values": {
        "street": "Hauptstrasse 1",
        "city": "Hamburg",
        "person": "${ref('person')}"
      }
    }
  ]
}
```

Praxisregeln:
- Bei `@OneToMany` wird die Relation meist ueber die Child-Entity (`@ManyToOne` + `@JoinColumn`) gesetzt.
- Bei `@OneToOne` wird die Seite mit `@JoinColumn` aktualisiert.
- Re-Assign ist ein normales `update` auf dem Relationsfeld, z. B. `"person": "${ref('targetPerson')}"`.
- Verwende in Conditions moeglichst einfache Ausdruecke; komplexe `and`-Kombinationen direkt im `where` koennen parserabhaengig sein.

Typische Fehler:
- Nur die inverse Collection (`Person.addresses`) setzen, aber nicht die Owning Side (`Address.person`).
- Referenzen auf IDs/Felder annehmen, obwohl `saveAs` ein Objekt speichert (besser: direkt als Relationsobjekt verwenden).
- Reihenfolge missachten (Child einfuegen, bevor Parent im Context ist).

---

### F: Kann ich einen Feldwert aus einer anderen Entity uebernehmen?

**A:** Ja, mit `lookup(...)`.

```json
{
  "set": {
    "status": "${lookup('Customer','email','source@test.de','status')}"
  }
}
```

Regeln:
- Syntax: `${lookup('Entity','whereField','whereValue','selectField')}`
- Erweiterte Syntax: `${lookup('Entity',"<whereExpression>",'selectField')}`
- `whereValue` unterstuetzt Strings (`'abc'`) und Literale (`31`, `true`, `false`, `null`)
- `whereExpression` kann beliebig viele Bedingungen enthalten (mit `and`, `or`, `not`, Klammern)
- Auch Mengenoperatoren sind moeglich: `in (...)`, `not in (...)`
- Das Lookup muss genau **einen** Treffer liefern
- Bei 0 oder mehreren Treffern wird die Operation mit Fehler beendet

Hinweis: Fuer Relationen (z. B. `person`) bleibt `saveAs` + `${ref('...')}` der bevorzugte Weg.

---

## 📞 Support

**Dokumentation:** [USAGE.md](USAGE.md)
**Quickstart:** [QUICKSTART.md](QUICKSTART.md)
**REST API:** [API.md](API.md)
**Spezifikation:** [SPEC.md](SPEC.md)
