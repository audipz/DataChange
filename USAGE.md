# DataChange Framework – Benutzerhandbuch

## Inhaltsverzeichnis
1. [Überblick](#überblick)
2. [Quickstart](#quickstart)
3. [Konzepte](#konzepte)
4. [ChangeSet Format](#changeset-format)
5. [Installation & Konfiguration](#installation--konfiguration)
6. [Betriebsmodi](#betriebsmodi)
7. [Bedingungen (Condition Engine)](#bedingungen-condition-engine)
8. [Praktische Beispiele](#praktische-beispiele)
9. [Troubleshooting](#troubleshooting)
10. [Best Practices](#best-practices)

---

## Überblick

Das **DataChange Framework** ist eine deklarative, GitOps-fähige Lösung für fachliche Datenänderungen in Spring-Boot/JPA-Anwendungen.

**Hauptmerkmale:**
- ✅ **JSON-basierte ChangeSets** statt imperativer SQL-Migration
- ✅ **Vollständige JPA/Hibernate/Envers-Integration** – automatische Audit-Trails
- ✅ **Idempotente Ausführung** – gleiche ChangeSet wird niemals zweimal ausgeführt
- ✅ **Bedingte Ausführung** – Preconditions vor Datenaenderung prüfen
- ✅ **Mehrere Betriebsmodi** – Startup, REST API, CLI, Spring Batch
- ✅ **Generisch** – funktioniert mit beliebigen JPA-Entities, keine Repository-Zwangsbindung

**Workflow:**
```
Git Repository (JSON-ChangeSets)
    ↓
ArgoCD/Flux (GitOps Sync)
    ↓
Spring Boot App (DataChange Runner)
    ↓
DataChangeEngine (Parser → Validator → Executor)
    ↓
EntityManager (JPA)
    ↓
Hibernate + Envers (Datenbankänderung + Audit)
```

---

## Quickstart

### 1. Dependency hinzufügen (Maven)

**`pom.xml`:**
```xml
<dependency>
    <groupId>de.graube</groupId>
    <artifactId>datachange-framework</artifactId>
    <version>0.0.1-SNAPSHOT</version>
</dependency>
```

### 2. Entity mit Envers vorbereiten

```java
import jakarta.persistence.*;
import org.hibernate.envers.Audited;

@Audited  // ← Wichtig! Aktiviert automatisches Audit-Trail
@Entity
@Table(name = "customer")
public class Customer {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false, unique = true)
    private String email;
    
    @Enumerated(EnumType.STRING)
    private CustomerStatus status;
    
    // Getters/Setters...
}
```

### 3. ChangeSet JSON erstellen

**`src/main/resources/datachange/001-seed-customers.json`:**
```json
{
  "specVersion": "1.0",
  "id": "seed-customers-001",
  "author": "data-team",
  "description": "Seed initial customers",
  "labels": ["seed"],
  "tags": ["v1.0"],
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
        "status": "ACTIVE"
      }
    },
    {
      "id": "insert-bob",
      "op": "insert",
      "entity": "Customer",
      "values": {
        "email": "bob@example.com",
        "status": "ACTIVE"
      }
    }
  ]
}
```

### 4. Konfigurieren

**`src/main/resources/application.yaml`:**
```yaml
spring:
  jpa:
    hibernate:
      ddl-auto: update
    properties:
      hibernate:
        envers:
          autoRegisterListeners: true

datachange:
  enabled: true
  mode: STARTUP  # oder MANUAL für REST-Trigger
  locations:
    - classpath*:datachange/*.json
```

### 5. App starten

```bash
mvn spring-boot:run
```

**Ergebnis:**
- ✅ ChangeSet "seed-customers-001" wird geladen
- ✅ Precondition geprüft: `count(Customer) == 0` ✓
- ✅ Beide Customers eingefügt
- ✅ Envers speichert Audit-Trail automatisch
- ✅ Interner Changelog aktualisiert (nächster Start: SKIPPED)

---

## Konzepte

### ChangeSets
Ein **ChangeSet** ist eine atomare Einheit von Datenänderungen:
- Hat eindeutige `id` + `author`
- Enthält 1+ Operations (`insert/update/delete/upsert/merge`)
- Wird genau einmal pro Lauf ausgeführt (Idempotenz)
- Optional mit Preconditions und Transaktionsmodi

### Idempotenz
Nach erfolgreicher Ausführung wird das ChangeSet im internen Changelog gespeichert:

| Feld | Wert |
|------|------|
| id | changeset-id |
| checksum | Hash des JSON |
| status | SUCCESS / FAILED |
| executedAt | Timestamp |
| duration | ms |
| hostname | Server, auf dem es lief |

Beim nächsten Lauf: **SKIPPED** (bereits erfolgreich ausgeführt).

### Transaktionsmodi
- **CHANGESET** (Standard): Alle Ops in einer Transaktion
- **PER_CHANGE**: Jede Operation in eigener Transaktion
- **CUSTOM**: Konfigurierbare Grenzen (künftig)

### Envers-Integration
Alle Änderungen gehen durch `EntityManager`:
```
Customer { id=1, email="alice@x.de", status=ACTIVE }
                           ↓
                    EntityManager.persist()
                           ↓
                       Hibernate Listener
                           ↓
                    ✅ customer (Main Table)
                    ✅ customer_aud (Audit Table)
                    ✅ revinfo (Revision Meta)
```

---

## ChangeSet Format

### Struktur (v1.0)

```json
{
  "specVersion": "1.0",                    // ← Pflicht: Format-Version
  "id": "unique-changeset-id",           // ← Pflicht: Eindeutige ID
  "author": "team-name",                  // ← Pflicht: Verantwortliche
  "description": "What this does",        // ← Pflicht: Beschreibung
  "labels": ["tag1", "tag2"],            // Optional
  "tags": ["v1.0", "prod"],              // Optional
  "transactionMode": "CHANGESET",         // Optional, default=CHANGESET
  "preConditions": {                      // Optional: vor Ausführung prüfen
    "expression": "count(Customer) == 0"
  },
  "changes": [                            // ← Pflicht: Mindestens 1 Operation
    {
      "id": "op-1",                       // ← Eindeutig im ChangeSet
      "op": "insert",                     // insert|update|delete|upsert|merge
      "entity": "Customer",               // JPA Entity-Name
      "values": {                         // Werte für insert/upsert
        "email": "alice@x.de",
        "status": "ACTIVE"
      },
      "where": "email == 'bob@x.de'",    // Filter für update/delete
      "set": { "status": "INACTIVE" },   // Neue Werte für update
      "saveAs": "customer_id"             // Speichere Ergebnis in Context
    }
  ]
}
```

### Field-Typen

**Primitive:**
```json
{
  "values": {
    "name": "Alice",              // String
    "age": 30,                    // Number
    "active": true,               // Boolean
    "joinDate": "2024-01-15"      // Date (ISO 8601)
  }
}
```

**Enums & Conversions:**
```json
{
  "values": {
    "status": "ACTIVE",           // Automatisch zu CustomerStatus.ACTIVE
    "role": "USER"                // Zu Role Enum konvertiert
  }
}
```

**Referenzen auf frühere Ops:**
```json
{
  "changes": [
    {
      "id": "create-admin-role",
      "op": "insert",
      "entity": "Role",
      "values": { "name": "ADMIN" },
      "saveAs": "adminRoleId"   // ← Speichere unter "adminRoleId"
    },
    {
      "id": "assign-role",
      "op": "update",
      "entity": "User",
      "where": "email == 'alice@x.de'",
      "set": {
        "role": "${ref('adminRoleId')}"  // ← Nutze gespeicherten Wert
      }
    }
  ]
}
```

---

## Installation & Konfiguration

### Maven Setup

**1. Dependency hinzufügen**
```xml
<dependency>
    <groupId>de.graube</groupId>
    <artifactId>datachange-framework</artifactId>
    <version>0.0.1-SNAPSHOT</version>
</dependency>
```

**2. Spring Boot Starters (empfohlen)**
```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-data-jpa</artifactId>
</dependency>
<dependency>
    <groupId>org.hibernate.orm</groupId>
    <artifactId>hibernate-envers</artifactId>
</dependency>
```

### Application Properties

**`application.yaml`:**
```yaml
datachange:
  enabled: true                          # Framework global aktivieren?
  mode: STARTUP                          # STARTUP | MANUAL
  locations:
    - classpath*:datachange/*.json      # Wo sind die ChangeSet-Dateien?
    - file:/etc/datachange/*.json        # Auch externe Pfade möglich

spring:
  jpa:
    hibernate:
      ddl-auto: update                   # oder validate in Prod
    properties:
      hibernate:
        format_sql: true
        show_sql: false
        envers:
          autoRegisterListeners: true    # ← Wichtig für Audit!
```

### Entities mit Envers

**Minimal:**
```java
import org.hibernate.envers.Audited;

@Audited
@Entity
public class Customer {
    @Id
    @GeneratedValue
    private Long id;
    
    private String email;
    // ...
}
```

**Mit Custom Audit-Verhalten:**
```java
import org.hibernate.envers.Audited;
import org.hibernate.envers.NotAudited;

@Audited
@Entity
public class Customer {
    @Id
    private Long id;
    
    @Audited  // Explizit auditiert
    private String email;
    
    @NotAudited  // Ausgenommen vom Audit
    private LocalDateTime lastLogin;
}
```

---

## Betriebsmodi

### Mode: STARTUP (Standard)

**Verhalten:**
- ChangeSet-Dateien werden beim App-Start automatisch geladen
- Werden sequenziell ausgeführt
- Nur für noch nicht ausgeführte ChangeSets

**Konfiguration:**
```yaml
datachange:
  enabled: true
  mode: STARTUP
  locations:
    - classpath*:datachange/*.json
```

**Logs:**
```
2026-07-01T10:00:00 INFO DataChangeStartupRunner: Starting DataChange execution
2026-07-01T10:00:05 INFO DataChangeEngine: ChangeSet seed-customers-001 -> SUCCESS (inserts=2, duration=245ms)
2026-07-01T10:00:06 INFO DataChangeEngine: ChangeSet init-roles-001 -> SUCCESS (inserts=5, duration=156ms)
```

---

### Mode: MANUAL

**Verhalten:**
- ChangeSet-Dateien werden NICHT automatisch geladen
- REST-Endpunkte stehen zur Verfügung für manuellen Trigger
- Ideal für Admin-Tools oder Batch-Jobs

**Konfiguration:**
```yaml
datachange:
  enabled: true
  mode: MANUAL
  locations:
    - classpath*:datachange/*.json
```

**REST-Endpunkte:**
```bash
# ChangeSets anzeigen
curl http://localhost:8080/datachange/changesets

# Spezifisches ChangeSet ausführen
curl -X POST http://localhost:8080/datachange/execute?id=seed-customers-001
```

**Response:**
```json
{
  "changeSetId": "seed-customers-001",
  "status": "SUCCESS",
  "inserts": 2,
  "updates": 0,
  "deletes": 0,
  "duration": "00:00:00.245",
  "message": "executed"
}
```

---

## Bedingungen (Condition Engine)

### Preconditions

**Syntax:**
```json
{
  "preConditions": {
    "expression": "<condition>"
  }
}
```

Wenn Ausdruck zu `false` evaluiert → ChangeSet wird **SKIPPED**, nicht ausgeführt.

### Grundlegende Operatoren

**Literal:**
```json
{
  "expression": "true"   // Immer ausführen
}
{
  "expression": "false"  // Immer überspringen
}
```

**Logik:**
```json
{
  "expression": "not false"  // ✓ true
}
{
  "expression": "true and true"   // ✓ true
}
{
  "expression": "true or false"   // ✓ true
}
{
  "expression": "not (false or false)"  // ✓ true
}
```

### DB-Funktionen

**Existiert Datensatz?**
```json
{
  "expression": "exists(Customer where email='alice@x.de')"  // Datensatz vorhanden?
}
```

**Zähle Datensätze:**
```json
{
  "expression": "count(Customer where status='ACTIVE') == 2"
}
{
  "expression": "count(Customer) >= 5"
}
{
  "expression": "count(Order where state='OPEN') > 0"
}
```

**Listen-Operationen:**
```json
{
  "expression": "Customer.status in ('ACTIVE', 'PENDING')"
}
{
  "expression": "User.role not in ('ADMIN', 'ROOT')"
}
```

### Komplexe Bedingungen

```json
{
  "expression": "not exists(Customer where email='new@x.de') and count(Customer where active=true) > 0"
}
{
  "expression": "count(Role) == 0 or (not exists(User where admin=true) and count(User) > 1)"
}
```

---

## Praktische Beispiele

### Beispiel 1: Seed-Daten

**Datei:** `datachange/001-seed-customers.json`
```json
{
  "specVersion": "1.0",
  "id": "seed-customers-prod",
  "author": "ops-team",
  "description": "Populate initial customers for production",
  "labels": ["seed", "prod"],
  "tags": ["v1.0"],
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
        "email": "alice@mycompany.com",
        "firstName": "Alice",
        "lastName": "Smith",
        "status": "ACTIVE",
        "createdDate": "2024-01-01"
      },
      "saveAs": "alice_id"
    },
    {
      "id": "insert-bob",
      "op": "insert",
      "entity": "Customer",
      "values": {
        "email": "bob@mycompany.com",
        "firstName": "Bob",
        "lastName": "Johnson",
        "status": "ACTIVE"
      },
      "saveAs": "bob_id"
    }
  ]
}
```

**Ausführungs-Szenario:**
```
Beim 1. Start: ✓ Precondition erfüllt (0 Customers)
              ✓ Beide Customers eingefügt
              ✓ Changelog: SUCCESS
              
Beim 2. Start: ✗ Precondition nicht erfüllt (2 Customers vorhanden)
              → SKIPPED
```

---

### Beispiel 2: Bedingte Updates

**Datei:** `datachange/002-update-status.json`
```json
{
  "specVersion": "1.0",
  "id": "migrate-customer-status",
  "author": "data-team",
  "description": "Migrate inactive customers from OLD to ARCHIVED",
  "tags": ["v1.1"],
  "transactionMode": "CHANGESET",
  "preConditions": {
    "expression": "exists(Customer where status='OLD')"
  },
  "changes": [
    {
      "id": "update-status",
      "op": "update",
      "entity": "Customer",
      "where": "status == 'OLD'",
      "set": {
        "status": "ARCHIVED",
        "archivedDate": "2024-06-01"
      }
    }
  ]
}
```

---

### Beispiel 3: Mit Referenzen

**Datei:** `datachange/003-assign-roles.json`
```json
{
  "specVersion": "1.0",
  "id": "create-roles-and-assign",
  "author": "admin-team",
  "description": "Create roles and assign to admin user",
  "transactionMode": "CHANGESET",
  "changes": [
    {
      "id": "create-admin-role",
      "op": "insert",
      "entity": "Role",
      "values": {
        "name": "ADMIN",
        "description": "Full system access"
      },
      "saveAs": "admin_role"
    },
    {
      "id": "create-user-role",
      "op": "insert",
      "entity": "Role",
      "values": {
        "name": "USER",
        "description": "Limited user access"
      },
      "saveAs": "user_role"
    },
    {
      "id": "assign-admin",
      "op": "update",
      "entity": "User",
      "where": "email == 'admin@example.com'",
      "set": {
        "role": "${ref('admin_role')}"
      }
    }
  ]
}
```

---

### Beispiel 4: Bedingte Datenlöschung

**Datei:** `datachange/004-cleanup-old-orders.json`
```json
{
  "specVersion": "1.0",
  "id": "cleanup-old-orders",
  "author": "operations",
  "description": "Delete orders older than 2 years",
  "preConditions": {
    "expression": "count(Order where createdDate < '2022-01-01') > 0"
  },
  "changes": [
    {
      "id": "delete-old",
      "op": "delete",
      "entity": "Order",
      "where": "createdDate < '2022-01-01'"
    }
  ]
}
```

---

## Troubleshooting

### Problem: ChangeSet wird nicht ausgeführt

**Symptom:** ChangeSet-Datei existiert, wird aber nicht geladen.

**Ursache 1: Standort stimmt nicht**
```yaml
# ❌ Falsch
datachange:
  locations:
    - datachange/*.json

# ✅ Richtig
datachange:
  locations:
    - classpath*:datachange/*.json
    - classpath*:changesets/*.json
```

**Ursache 2: Framework deaktiviert**
```yaml
# ❌ Falsch
datachange:
  enabled: false

# ✅ Richtig
datachange:
  enabled: true
```

**Ursache 3: Mode ist MANUAL**
```yaml
# Wenn Mode=MANUAL:
datachange:
  mode: MANUAL

# → Manuell über REST Endpoint ausführen:
curl -X POST http://localhost:8080/datachange/execute?id=your-changeset-id
```

---

### Problem: Precondition schlägt fehl, ChangeSet wird skipped

**Log:**
```
ChangeSet seed-customers-001 -> SKIPPED (precondition is false)
```

**Debug:**
```json
{
  "preConditions": {
    "expression": "count(Customer) == 0"
  }
}
```

1. Prüfe, ob bereits Customers in der DB existieren:
   ```bash
   SELECT COUNT(*) FROM customer;
   ```

2. Falls ja, ändere die Precondition oder lösche manuell alte Daten

---

### Problem: Envers-Audit funktioniert nicht

**Ursache:** `@Audited` fehlt auf Entity

```java
// ❌ Falsch
@Entity
public class Customer { ... }

// ✅ Richtig
@Audited
@Entity
public class Customer { ... }
```

**Und in `application.yaml`:**
```yaml
spring:
  jpa:
    properties:
      hibernate:
        envers:
          autoRegisterListeners: true
```

---

### Problem: ChangeSet wird zweimal ausgeführt

**Ursache:** Changelog-Tabelle wurde manuell geleert oder `id` wurde geändert

**Lösung:**
```sql
-- Prüfe Changelog
SELECT * FROM DATA_CHANGELOG WHERE id = 'your-changeset-id';

-- Falls Fehler: Handhuelle Bereinigung
DELETE FROM DATA_CHANGELOG WHERE id = 'your-changeset-id';

-- Beim nächsten Start wird es erneut ausgeführt
```

---

## Best Practices

### 1. Versionierung

**Nutze klare Naming-Konventionen:**
```
datachange/001-seed-customers.json
datachange/002-seed-roles.json
datachange/003-migrate-status-v1-to-v2.json
datachange/004-cleanup-old-records.json
```

Vorteile:
- ✅ Numerische Ordnung → Reihenfolge klar
- ✅ Beschreibend → sofort verständlich
- ✅ Versionierung → Änderungen nachvollziehbar

---

### 2. Transaktionalität

**Nutze CHANGESET für atomare Ops:**
```json
{
  "transactionMode": "CHANGESET",  // ← Alles oder nichts
  "changes": [
    { "op": "insert", ... },
    { "op": "update", ... },
    { "op": "delete", ... }
    // Alle 3 erfolgreich → commit
    // Eine fehlgeschlagen → rollback aller
  ]
}
```

---

### 3. Preconditions

**Immer safeguards nutzen:**
```json
{
  "preConditions": {
    "expression": "not exists(Customer where email='duplicate@x.de') and count(Customer) < 1000"
  },
  "changes": [...]
}
```

Verhindert:
- ✅ Duplikate
- ✅ Überschreitung von Limits
- ✅ Akkidentelle Mehrfach-Ausführung

---

### 4. Datensicherheit

**Sensible Daten nicht im Git hardcoden:**
```json
// ❌ Falsch
{
  "changes": [{
    "values": {
      "password": "supersecret123",
      "apiKey": "sk-1234567890abcdef"
    }
  }]
}

// ✅ Richtig: Nutze Environment Variables
{
  "changes": [{
    "values": {
      "password": "${env('CUSTOMER_PASSWORD')}",
      "apiKey": "${env('API_KEY')}"
    }
  }]
}
```

---

### 5. Monitoring

**Nutze Logs zur Diagnose:**
```yaml
logging:
  level:
    de.graube.datachange: DEBUG
```

Logs zeigen:
- ✅ Welche ChangeSet geladen
- ✅ Precondition Evaluierungen
- ✅ SQL-Ausführungen
- ✅ Fehler & Rollbacks

---

### 6. GitOps Integration

**Workflow mit ArgoCD:**

1. **Repo-Struktur:**
```
my-app-repo/
├── src/
├── pom.xml
├── datachange/
│   ├── 001-seed.json
│   ├── 002-migrate.json
│   └── 003-cleanup.json
└── k8s/
    └── deployment.yaml
```

2. **ArgoCD synct**:
```bash
argocd app create my-data-app \
  --repo https://github.com/myorg/my-app-repo.git \
  --path . \
  --dest-server https://kubernetes.default.svc
```

3. **App startet, Framework lädt ChangeSets auto**:
```
ArgoCD Sync → Pod erstellt
           ↓
Spring App startet
           ↓
DataChangeEngine: Mode=STARTUP
           ↓
Lädt *.json aus /resources/datachange
           ↓
Führt noch-nicht-ausgeführte ChangeSets aus
```

---

## Zusammenfassung

| Aspekt | Antwort |
|--------|--------|
| **Wofür?** | Fachliche Datenänderungen deklarativ verwalten |
| **Wie?** | JSON-ChangeSets + JPA EntityManager |
| **Wann?** | Bei App-Start (STARTUP) oder manuell (MANUAL) |
| **Garantien?** | Idempotenz, Transaktionen, Audit (Envers) |
| **Fehler?** | Rollback, Retry, Logging, Changelog Tracking |

**Nächste Schritte:**
1. Framework als Maven-Dep einbinden
2. Entity mit `@Audited` markieren
3. ChangeSet JSON erstellen
4. Precondition definieren
5. App starten → erledigt! ✅

