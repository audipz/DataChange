# DataChange Framework

**GitOps-fähiges, deklaratives Framework für fachliche Datenänderungen in Spring Boot / JPA / Hibernate Envers**

> Repository: https://github.com/audipz/Datachange
> 
> Lizenz: Apache License 2.0

## 🎯 Zielbild

Ein generisches Framework zur Verwaltung von **Datenänderungen** (nicht Schema-Migrationen) via JSON-ChangeSets:
- ✅ Deklarativ über **JSON**-Dateien
- ✅ Versioniert in **Git**
- ✅ **GitOps-kompatibel** (ArgoCD, FluxCD)
- ✅ **Generisch** für beliebige JPA-Entities
- ✅ **Idempotent** – jedes ChangeSet wird maximal einmal ausgeführt
- ✅ **Envers-integriert** – automatische Audit-Trails
- ✅ Mehrere **Betriebsmodi** (Startup, REST, CLI, Batch)

## 📚 Dokumentation

| Datei | Zweck | Leser |
|-------|-------|-------|
| **[QUICKSTART.md](QUICKSTART.md)** | 5-Minuten Einstieg | Anfänger |
| **[USAGE.md](USAGE.md)** | Vollständiges Handbuch | Entwickler |
| **[API.md](API.md)** | REST API Referenz | Integratoren |
| **[ARCHITECTURE.md](ARCHITECTURE.md)** | Architektur-Diagramm & Schichten | Architekten |
| **[FAQ.md](FAQ.md)** | Häufige Fragen & Lösungen | Troubleshooter |
| **[MIGRATION.md](MIGRATION.md)** | Von Liquibase/SQL migrieren | Migration |
| **[SPEC.md](SPEC.md)** | Technische Spezifikation | Architekten |
| **[ROADMAP.md](ROADMAP.md)** | Development-Plan | Beitragende |

## 🚀 Quickstart (3 Minuten)

### 1. Dependency

```xml
<dependency>
    <groupId>io.github.audipz</groupId>
    <artifactId>datachange-framework</artifactId>
    <version>0.0.1-SNAPSHOT</version>
</dependency>
```

### 2. Entity mit @Audited

```java
@Audited  // ← Wichtig!
@Entity
public class Customer { ... }
```

### 3. ChangeSet JSON

**`src/main/resources/datachange/001-seed.json`:**
```json
{
  "specVersion": "1.0",
  "id": "seed-customers",
  "author": "dev-team",
  "description": "Seed demo customers",
  "preConditions": { "expression": "count(Customer) == 0" },
  "changes": [{
    "op": "insert",
    "entity": "Customer",
    "values": { "email": "alice@x.de", "status": "ACTIVE" }
  }]
}
```

### 4. Config

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
# → ChangeSet wird automatisch geladen & ausgeführt
```

✅ **Fertig!** Customer wurde eingefügt und von Envers auditiert.

---

## 📦 Modul-Struktur

```
DataChange/
├── datachange-framework/          ← Die Framework-JAR
│   ├── src/main/java/
│   │   ├── api/                  ← Public API (DataChangeExecutor, ...)
│   │   ├── model/                ← ChangeSet Domain Models
│   │   ├── engine/               ← Execution Engine
│   │   ├── parser/               ← JSON Parser
│   │   ├── loader/               ← ChangeSet Loader
│   │   ├── validation/           ← Validator
│   │   ├── condition/            ← Condition Evaluator
│   │   ├── log/                  ← Changelog & Idempotenz
│   │   ├── rest/                 ← REST Controller
│   │   ├── boot/                 ← Spring Boot Integration
│   │   └── spi/                  ← Plugin API
│   └── pom.xml
│
├── datachange-example-app/        ← Beispiel-Anwendung
│   ├── src/main/java/
│   │   ├── DataChangeApplication.java
│   │   └── example/              ← Demo Entities
│   ├── src/main/resources/
│   │   ├── application.yaml
│   │   └── datachange/           ← Demo ChangeSets
│   └── pom.xml
│
├── QUICKSTART.md                  ← 5-Min Einstieg
├── USAGE.md                       ← Vollständiges Handbuch
├── API.md                         ← REST API Docs
├── FAQ.md                         ← FAQ & Troubleshooting
├── MIGRATION.md                   ← Migration Guide
├── SPEC.md                        ← Technical Spec
└── ROADMAP.md                     ← Development Plan
```

## 🎓 Konzepte

### ChangeSet
Atomare Einheit von Datenänderungen:
```json
{
  "id": "unique-id",
  "author": "dev-team",
  "description": "What this does",
  "preConditions": { "expression": "..." },
  "changes": [
    { "op": "insert", "entity": "Customer", ... },
    { "op": "update", "entity": "Order", ... }
  ]
}
```

### Idempotenz
Jedes ChangeSet wird **maximal einmal** ausgeführt:
- ✅ 1. Lauf: SUCCESS
- ⊝ 2. Lauf: SKIPPED
- ⊝ 3. Lauf: SKIPPED

### Preconditions
Bedingung vor Ausführung prüfen:
```json
{
  "preConditions": {
    "expression": "not exists(Customer where email='duplicate@x.de') and count(Customer) == 0"
  }
}
```

### Envers-Integration
Alle Changes gehen über EntityManager → automatisches Audit-Trail:
```
DataChangeEngine
    ↓
EntityManager.persist()
    ↓
Hibernate Listeners
    ↓
✅ Main Table (customer)
✅ Audit Table (customer_aud)
✅ Revision Meta (revinfo)
```

## 🔧 Betriebsmodi

| Mode | Verhalten | Use Case |
|------|-----------|----------|
| **STARTUP** | Auto-Load beim Start | Standard, Prod-Ready |
| **MANUAL** | REST-Trigger | Admin-Tools, Batch-Jobs |

## 💻 REST API

```bash
# Mode: MANUAL aktivieren
# Dann:

# Verfügbare ChangeSets anzeigen
curl http://localhost:8080/datachange/changesets

# ChangeSet ausführen
curl -X POST http://localhost:8080/datachange/execute?id=seed-customers
```

## 🧪 Tests & Demo

```bash
# Alle Tests ausführen
mvn -q test

# Nur Framework-Tests
mvn -pl datachange-framework test

# Nur Example-App
mvn -pl datachange-example-app test

# Example-App starten
mvn -pl datachange-example-app spring-boot:run
```

## 🌍 GitOps Integration

**ArgoCD / FluxCD:**
```
Git Commit (ChangeSet JSON)
    ↓
ArgoCD Sync
    ↓
Pod deployed
    ↓
Spring App startet
    ↓
DataChangeEngine: Mode=STARTUP
    ↓
✅ Neue ChangeSets werden automatisch geladen & ausgeführt
```

Keine zusätzliche Konfiguration nötig!

## 🚫 Was DataChange NICHT macht

- ❌ Schema-Migrationen (dafür: Flyway/Liquibase)
- ❌ Direktes SQL (nur JPA EntityManager)
- ❌ Verteilte Transaktionen über mehrere DBs
- ❌ Zero-Downtime Deployments (nur Standard-Migration)

## 📋 Tech Stack

- **Spring Boot 4.1+**
- **Java 21+**
- **Hibernate 7.4+**
- **Hibernate Envers**
- **Jackson 3.x** (JSON)
- **JPA/Jakarta Persistence**

## 🤝 Beitragen

### Release-Regeln

- Releases nach Maven Central laufen nur ueber Versionstags `v*`.
- Ein Release-Tag wird nur akzeptiert, wenn der getaggte Commit aus `main` erreichbar ist.
- Nicht-`main` Branches fuehren nur Build/Test plus lokales SNAPSHOT-Install aus.

```bash
# Fork & Clone
git clone https://github.com/audipz/Datachange.git

# Feature Branch erstellen
git checkout -b feature/my-feature

# Test schreiben & Code implementieren
mvn test

# Commit & Push
git push origin feature/my-feature

# Pull Request erstellen
```

## 📄 Lizenz

Apache License 2.0 (siehe `LICENSE`)

---

**Dokumentation starten:** [QUICKSTART.md](QUICKSTART.md) oder [USAGE.md](USAGE.md)
