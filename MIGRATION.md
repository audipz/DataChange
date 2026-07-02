# DataChange Framework – Migrations- & Integrationsleitfaden

## 🔄 Migration von Liquibase zu DataChange

### Szenario
Sie verwenden Liquibase für Schema-Migrationen und möchten auch fachliche Datenänderungen mit DataChange verwalten.

### Schritt 1: Liquibase behalten, DataChange hinzufügen

**`pom.xml`:**
```xml
<!-- Liquibase (für Schema) -->
<dependency>
    <groupId>org.liquibase</groupId>
    <artifactId>liquibase-core</artifactId>
</dependency>

<!-- DataChange (für Daten) -->
<dependency>
    <groupId>de.graube</groupId>
    <artifactId>datachange-framework</artifactId>
    <version>0.0.1-SNAPSHOT</version>
</dependency>
```

### Schritt 2: Konfiguration

**`application.yaml`:**
```yaml
# Liquibase Schema-Migrationen
spring:
  liquibase:
    enabled: true
    change-log: classpath:db/liquibase/changelog-master.xml

# DataChange: Fachliche Datenänderungen
datachange:
  enabled: true
  mode: STARTUP
  locations:
    - classpath*:datachange/*.json
```

**Reihenfolge:**
1. Spring startet
2. Liquibase: Schema-Migrations (Tabellen erstellen)
3. DataChange: Fachliche Daten-Changes (Datensätze einfügen)

### Schritt 3: Bestehende Liquibase-Änderungen zu DataChange migrieren

**Vorher (Liquibase XML):**
```xml
<changeSet id="20240601-seed-customers" author="dev-team">
    <insert tableName="customer">
        <column name="id" value="1"/>
        <column name="email" value="alice@x.de"/>
        <column name="status" value="ACTIVE"/>
    </insert>
    <insert tableName="customer">
        <column name="id" value="2"/>
        <column name="email" value="bob@x.de"/>
        <column name="status" value="ACTIVE"/>
    </insert>
</changeSet>
```

**Nachher (DataChange JSON):**
```json
{
  "specVersion": "1.0",
  "id": "seed-customers",
  "author": "dev-team",
  "description": "Seed customers",
  "transactionMode": "CHANGESET",
  "changes": [
    {
      "id": "insert-alice",
      "op": "insert",
      "entity": "Customer",
      "values": {
        "id": 1,
        "email": "alice@x.de",
        "status": "ACTIVE"
      }
    },
    {
      "id": "insert-bob",
      "op": "insert",
      "entity": "Customer",
      "values": {
        "id": 2,
        "email": "bob@x.de",
        "status": "ACTIVE"
      }
    }
  ]
}
```

---

## 🔄 Migration von Flyway zu DataChange

Analog zu Liquibase:

**Flyway-Change:**
```java
// V2024.06.01__SeedCustomers.java
public class V20240601__SeedCustomers implements BaseJavaMigration {
    public void migrate(Context context) throws Exception {
        context.getConnection()
            .createStatement()
            .execute("INSERT INTO customer (email, status) VALUES ('alice@x.de', 'ACTIVE')");
    }
}
```

**DataChange-Äquivalent:**
```json
{
  "id": "seed-customers",
  "changes": [
    {
      "op": "insert",
      "entity": "Customer",
      "values": { "email": "alice@x.de", "status": "ACTIVE" }
    }
  ]
}
```

**Vorteil:** 
- ✅ Keine Java-Kompilierung nötig
- ✅ Deklarativ, wartbar
- ✅ Automatisches Envers-Audit

---

## 🔄 Migration von direktem SQL zu DataChange

### Szenario
Sie haben bestehende SQL-Migration / Seed-Skripte, die Sie in DataChange konvertieren möchten.

### Conversion-Mapping

| SQL | DataChange |
|-----|-----------|
| `INSERT INTO ...` | `"op": "insert"` |
| `UPDATE ... SET` | `"op": "update"` |
| `DELETE FROM ... WHERE` | `"op": "delete"` |
| `INSERT ... ON CONFLICT UPDATE` | `"op": "upsert"` |

### Beispiel: SQL zu DataChange

**Vorher (SQL-Skript):**
```sql
-- Seed Rollen
INSERT INTO role (name, description) VALUES ('ADMIN', 'Administrator');
INSERT INTO role (name, description) VALUES ('USER', 'Regular User');

-- Seed Benutzer
INSERT INTO user (email, role_id, active) 
SELECT 'alice@x.de', id, true FROM role WHERE name='ADMIN';

-- Update alte Statüsse
UPDATE customer SET status='ARCHIVED' WHERE status='OLD';

-- Cleanup
DELETE FROM old_table WHERE archived=true;
```

**Nachher (DataChange):**
```json
{
  "id": "migrate-legacy-sql",
  "changes": [
    {
      "id": "insert-admin-role",
      "op": "insert",
      "entity": "Role",
      "values": { "name": "ADMIN", "description": "Administrator" },
      "saveAs": "admin_role"
    },
    {
      "id": "insert-user-role",
      "op": "insert",
      "entity": "Role",
      "values": { "name": "USER", "description": "Regular User" }
    },
    {
      "id": "insert-alice",
      "op": "insert",
      "entity": "User",
      "values": { 
        "email": "alice@x.de",
        "role": "${ref('admin_role')}",
        "active": true
      }
    },
    {
      "id": "update-old-statuses",
      "op": "update",
      "entity": "Customer",
      "where": "status == 'OLD'",
      "set": { "status": "ARCHIVED" }
    },
    {
      "id": "cleanup-old-table",
      "op": "delete",
      "entity": "OldTable",
      "where": "archived == true"
    }
  ]
}
```

---

## 🔄 Bestehende Daten in Envers aktivieren

### Szenario
Sie haben alte Tabellen ohne Envers-Audit und möchten historische Daten jetzt tracken.

### Lösung

**1. Envers auf Entity aktivieren:**
```java
@Audited  // ← Hinzufügen
@Entity
public class Customer {
    // ...
}
```

**2. Externe Audit-Tabelle initialisieren:**

Erstellen Sie ein ChangeSet, das alte Daten in die `*_aud`-Tabelle migriert:

```json
{
  "id": "init-envers-history",
  "description": "Populate initial envers audit records",
  "changes": [
    {
      "id": "create-revinfo-records",
      "op": "insert",
      "entity": "RevisionEntity",
      "values": {
        "revisionDate": "2024-01-01T00:00:00",
        "revisionNumber": 0
      }
    }
  ]
}
```

**3. Ab sofort:**
- Alle neuen Changes werden getracked
- Alte Daten werden als "Basis" angesehen
- Historical Queries funktionieren ab jetzt

---

## 🔄 Integration mit Bestehenden Systemen

### Option 1: Parallel zu Spring Boot Actuator

```yaml
management:
  endpoints:
    web:
      exposure:
        include: datachange, health, metrics

# Custom Actuator Endpoint
spring:
  application:
    name: my-data-app
```

**Endpunkte:**
```bash
curl http://localhost:8080/actuator/datachange/changesets
curl http://localhost:8080/actuator/datachange/execute?id=seed
```

---

### Option 2: Als Spring Batch Job (künftig)

```java
@Configuration
public class DataChangeJobConfiguration {
    
    @Bean
    public Job dataChangeJob(JobRepository jobRepository, 
                            DataChangeExecutor executor) {
        return new JobBuilder("dataChangeJob", jobRepository)
            .start(dataChangeStep(executor))
            .build();
    }
    
    @Bean
    public Step dataChangeStep(DataChangeExecutor executor) {
        return new StepBuilder("dataChangeStep", jobRepository)
            .tasklet((contribution, chunkContext) -> {
                // Laden und Ausführen aller ChangeSets
                // ...
                return RepeatStatus.FINISHED;
            })
            .build();
    }
}
```

---

### Option 3: CLI-Integration (künftig)

```bash
# DataChange CLI-Tool
java -jar datachange-cli.jar \
  --config application.yaml \
  --execute seed-customers

# Oder: Dry-Run
java -jar datachange-cli.jar \
  --config application.yaml \
  --dry-run
```

---

## 🔄 Bestehende Repository-Klassen verwenden

### Szenario
Sie haben bereits Spring Data JPA Repositories, möchten diese aber nicht verlieren.

**Es ist nicht nötig! DataChange funktioniert ohne Repositories:**

```java
// DataChange braucht nur JPA Metamodel
public class JpaOperationExecutor {
    
    private Object instantiate(String entityName, EntityManager em) {
        EntityType<?> type = em.getMetamodel()
            .getEntities()
            .stream()
            .filter(e -> e.getName().equals(entityName))
            .findFirst()
            .get();
        
        return type.getJavaType()
            .getDeclaredConstructor()
            .newInstance();
    }
}
```

**Aber Sie können weiterhin Repositories verwenden:**

```java
@Service
public class CustomerService {
    
    @Autowired
    private CustomerRepository repository;  // ← Weiterhin nutzbar
    
    public void processCustomers() {
        List<Customer> all = repository.findAll();
        // Ihre Business-Logik
    }
}

// DataChange nutzt einen anderen Pfad:
// EntityManager → DataChangeEngine → JPA Persistence
// (nicht über Repositories)
```

---

## 🔄 Bestehende Envers-Audit erweitern

### Szenario
Envers ist bereits aktiviert, Sie möchten DataChange-Changes auch auditieren.

**Gute Nachricht:** Automatisch! DataChange nutzt EntityManager, der mit Envers integriert ist:

```
DataChangeEngine
    ↓
EntityManager.persist()
    ↓
Hibernate Event Listener (Envers)
    ↓
✅ customer_aud updated automatically
```

**Abfragen:**

```sql
-- Zeige alle Änderungen einer Entity
SELECT 
  revisions.rev as revision,
  revisions.revtype,
  revisions.revchanges.timestamp,
  customer_aud.*
FROM customer_aud
JOIN revisions ON customer_aud.rev = revisions.rev
WHERE customer_aud.id = 1
ORDER BY revisions.rev DESC;

-- Zeige spezifische Änderung
SELECT * FROM customer_aud WHERE id=1 AND rev=10;
```

---

## 📋 Checkliste: Migration durchführen

- [ ] DataChange Dependency hinzufügen
- [ ] Framework-Konfiguration in `application.yaml` einstellen
- [ ] Bestehende Entities mit `@Audited` markieren
- [ ] Liquibase/Flyway Config überprüfen (wenn vorhanden)
- [ ] Erste ChangeSet-JSON erstellen & testen
- [ ] In DEV-Umgebung validieren
- [ ] Logs prüfen
- [ ] Preconditions definieren
- [ ] In QA-Umgebung testen
- [ ] Audit-Trail überprüfen
- [ ] Production-Rollout planen

---

## 🆘 Häufige Migrations-Fehler

### Fehler 1: Envers nicht aktiviert

```
ERROR: No audit table for entity Customer
```

**Lösung:**
```java
@Audited  // ← Hinzufügen
@Entity
public class Customer { }
```

---

### Fehler 2: Falsche Entity-Namen in ChangeSet

```json
{
  "entity": "customer"  // ❌ Falsch (Kleinbuchstaben)
}
```

**Lösung:**
```json
{
  "entity": "Customer"  // ✅ JPA-Entity-Name
}
```

---

### Fehler 3: Liquibase + DataChange Konflikte

```
ERROR: Table customer_aud doesn't exist
```

**Ursache:** Liquibase löscht die Envers-Tabellen bei `drop=true`.

**Lösung:**
```yaml
spring:
  liquibase:
    drop-first: false  // ← Nicht löschen!
```

---

## 📚 Weitere Ressourcen

- [USAGE.md](USAGE.md) – Vollständiges Handbuch
- [QUICKSTART.md](QUICKSTART.md) – 5-Minuten Einstieg
- [API.md](API.md) – REST API Dokumentation
- [FAQ.md](FAQ.md) – Häufige Fragen

