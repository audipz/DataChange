# 📚 DataChange Framework – Dokumentations-Index

**Willkommen! Hier finden Sie Einstiegspunkte für alle Dokumentationen des DataChange Frameworks.**

---

## 🚀 **Ich möchte schnell starten**

👉 **Start hier:** [QUICKSTART.md](QUICKSTART.md) (5 Min)

- ✅ Installation
- ✅ Erstes ChangeSet
- ✅ Config
- ✅ App starten

---

## 📖 **Ich möchte alles verstehen**

👉 **Lesen Sie:** [USAGE.md](USAGE.md) (umfassendes Handbuch)

**Themen:**
- Überblick & Konzepte
- ChangeSet-Format im Detail
- Bedingungen (Condition Engine)
- Betriebsmodi (STARTUP, MANUAL)
- 10+ praktische Beispiele
- Troubleshooting & Best Practices

---

## 🔌 **Ich möchte die REST API nutzen**

👉 **Lesen Sie:** [API.md](API.md)

**Themen:**
- GET `/datachange/changesets` – ChangeSet auflisten
- POST `/datachange/execute?id=...` – ChangeSet ausführen
- Fehlerbehandlung
- Integration mit Monitoring (Prometheus)
- ArgoCD/FluxCD Integration

---

## ❓ **Ich habe eine Frage**

👉 **Lesen Sie:** [FAQ.md](FAQ.md)

**Häufige Fragen:**
- Unterschied zu Liquibase/Flyway?
- Sicherheit & Passwörter?
- Idempotenz – wie funktioniert es?
- Envers-Integration?
- Performance-Optimierung?
- Debugging-Tipps?

---

## 🔄 **Ich migriere von Liquibase/SQL**

👉 **Lesen Sie:** [MIGRATION.md](MIGRATION.md)

**Themen:**
- Schritt-für-Schritt Migration
- SQL zu DataChange Konvertierung
- Parallel-Betrieb mit Liquibase/Flyway
- Bestehende Daten in Envers aktivieren
- Checkliste & häufige Fehler

---

## 🏗️ **Ich bin Architekt/Entwickler**

👉 **Lesen Sie:** [SPEC.md](SPEC.md)
👉 **Architekturdiagramm:** [ARCHITECTURE.md](ARCHITECTURE.md)

**Themen:**
- Technische Spezifikation
- Modulare Architektur
- SPI (Service Provider Interface)
- JPA/Hibernate Integration

---

## 🧩 **Ich möchte beitragen**

👉 **Lesen Sie:** [ROADMAP.md](ROADMAP.md)

**Themen:**
- Geplante Features
- Refactoring-Schritte
- Release-Ziele
- Offene Punkte für Beitragende

---

## 📦 **Alle Dokumente im Überblick**

| Datei | Inhalt | Zielgruppe |
|-------|--------|------------|
| [README.md](README.md) | Projektübersicht | Alle |
| [QUICKSTART.md](QUICKSTART.md) | Schnellstart | Neue Nutzer |
| [USAGE.md](USAGE.md) | Vollständige Nutzung | Entwickler |
| [API.md](API.md) | REST API Referenz | Integratoren |
| [ARCHITECTURE.md](ARCHITECTURE.md) | Architektur & Diagramm | Architekten |
| [FAQ.md](FAQ.md) | Fragen & Antworten | Support |
| [MIGRATION.md](MIGRATION.md) | Migration von Liquibase/Flyway | Teams |
| [SPEC.md](SPEC.md) | Technische Spezifikation | Architekten |
| [ROADMAP.md](ROADMAP.md) | Roadmap | Beitragende |

---

**Hinweis:** Die Dokumentation ist bewusst ausführlich, damit das Framework als Open-Source-Projekt, für GitHub und für Maven Central gut verständlich bleibt.
