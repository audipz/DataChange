# DataChange Framework – Quickstart (5 Min)

## 🚀 Installation in 3 Schritten

### 1. Dependency hinzufügen

**`pom.xml`:**
```xml
<dependency>
    <groupId>de.graube</groupId>
    <artifactId>datachange-framework</artifactId>
    <version>0.0.1-SNAPSHOT</version>
</dependency>

<dependency>
    <groupId>org.hibernate.orm</groupId>
    <artifactId>hibernate-envers</artifactId>
</dependency>
```

### 2. Entity mit @Audited

```java
import org.hibernate.envers.Audited;

@Audited  // ← Das ist wichtig!
@Entity
public class Customer {
    @Id
    @GeneratedValue
    private Long id;
    
    @Column(unique = true)
    private String email;
    
    private String firstName;
    private String lastName;
    
    @Enumerated(EnumType.STRING)
    private CustomerStatus status;
}
```

### 3. ChangeSet JSON erstellen

**`src/main/resources/datachange/001-seed.json`:**
```json
{
  "specVersion": "1.0",
  "id": "seed-customers",
  "author": "dev-team",
  "description": "Seed demo customers",
  "transactionMode": "CHANGESET",
  "preConditions": {
    "expression": "count(Customer) == 0"
  },
  "changes": [
    {
      "id": "insert-alice",
      "op": "insert",
      "entity": "Customer",
      "values": {
        "email": "alice@example.com",
        "firstName": "Alice",
        "lastName": "Smith",
        "status": "ACTIVE"
      }
    }
  ]
}
```

### 4. Config

**`application.yaml`:**
```yaml
datachange:
  enabled: true
  mode: STARTUP
  locations:
    - classpath*:datachange/*.json

spring:
  jpa:
    properties:
      hibernate:
        envers:
          autoRegisterListeners: true
```

### 5. Starten

```bash
mvn spring-boot:run
```

**Logs sollten zeigen:**
```
DataChangeStartupRunner: ChangeSet seed-customers -> SUCCESS (inserts=1)
```

✅ **Fertig!** Customer wurde eingefügt und von Envers auditiert.

---

## 📝 Häufige Operationen

### Insert (Neue Daten einfügen)
```json
{
  "id": "insert-user",
  "op": "insert",
  "entity": "User",
  "values": {
    "email": "bob@x.de",
    "role": "USER"
  }
}
```

### Update (Daten ändern)
```json
{
  "id": "update-alice",
  "op": "update",
  "entity": "Customer",
  "where": "email == 'alice@x.de'",
  "set": {
    "status": "INACTIVE"
  }
}
```

### Delete (Daten löschen)
```json
{
  "id": "delete-old",
  "op": "delete",
  "entity": "Customer",
  "where": "status == 'ARCHIVED'"
}
```

### Upsert (Insert oder Update)
```json
{
  "id": "upsert-bob",
  "op": "upsert",
  "entity": "Customer",
  "where": "email == 'bob@x.de'",
  "values": {
    "email": "bob@x.de",
    "status": "ACTIVE"
  }
}
```

---

## 🎯 Bedingte Ausführung

### Nur wenn Tabelle leer
```json
{
  "preConditions": {
    "expression": "count(Customer) == 0"
  }
}
```

### Nur wenn Datensatz existiert
```json
{
  "preConditions": {
    "expression": "exists(Customer where email='bob@x.de')"
  }
}
```

### Nur wenn Bedingung NICHT erfüllt
```json
{
  "preConditions": {
    "expression": "not exists(Role where name='ADMIN')"
  }
}
```

### Mehrere Bedingungen kombinieren
```json
{
  "preConditions": {
    "expression": "count(Customer where active=true) > 0 and not exists(Customer where email='duplicate@x.de')"
  }
}
```

---

## 🔧 REST API (Mode: MANUAL)

**Config ändern zu MANUAL:**
```yaml
datachange:
  mode: MANUAL
```

**ChangeSets auflisten:**
```bash
curl http://localhost:8080/datachange/changesets
```

**ChangeSet manuell ausführen:**
```bash
curl -X POST http://localhost:8080/datachange/execute?id=seed-customers
```

---

## 📊 Auditing mit Envers

Alle Änderungen werden automatisch getracked:

```sql
-- Zeige ursprüngliche Daten
SELECT * FROM customer WHERE id=1;

-- Zeige Audit-Trail
SELECT * FROM customer_aud WHERE id=1;

-- Zeige Revisionen
SELECT * FROM revinfo;
```

**Die Tabelle `customer_aud` enthält:**
- Alle bisherigen Werte (History)
- Revision ID (wann geändert)
- Revtype (0=INSERT, 1=UPDATE, 2=DELETE)
- Änderungsdatum

---

## ✅ Tipps & Tricks

### Tip 1: Numerische Dateinamen
```
datachange/001-seed.json          ← Wird zuerst geladen
datachange/002-migrate-v1-v2.json ← Dann dies
datachange/003-cleanup.json       ← Dann das
```

### Tip 2: Saubere IDs
```json
{
  "id": "prod-migrate-customer-status",  // ← Prod-Kontext
  "tags": ["v2.0", "prod"]               // ← Versionierung
}
```

### Tip 3: Precondition als Safeguard
```json
{
  "preConditions": {
    "expression": "not exists(Customer where email='${systemProperty}') and count(Customer) < 10000"
  }
}
```

### Tip 4: Fehler? Logs anschauen
```bash
# In application.yaml:
logging:
  level:
    de.graube.datachange: DEBUG
```

---

## 🎓 Lernen Sie mehr

- **Vollständiges Handbuch**: [USAGE.md](USAGE.md)
- **Spezifikation**: [SPEC.md](SPEC.md)
- **Beispiele**: `datachange-example-app/`

