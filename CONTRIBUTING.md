# Contributing

Vielen Dank fuer dein Interesse an DataChange.

## Schnellstart

1. Fork des Repositories erstellen
2. Branch anlegen: `feature/<kurzbeschreibung>`
3. Tests lokal ausfuehren: `./mvnw test`
4. Pull Request erstellen

## Lokale Entwicklung

```bash
./mvnw clean test
./mvnw -pl datachange-framework test
./mvnw -pl datachange-example-app spring-boot:run
```

## Richtlinien
- Kleine, nachvollziehbare Pull Requests
- Tests fuer neue Funktionalitaet
- Oeffentliche APIs moeglichst abwaertskompatibel halten
- Dokumentation mitziehen, wenn sich Verhalten aendert

## Commit-Nachrichten

Bitte verwende aussagekraeftige Commits, z. B.:
- `refactor: extract where clause parser`
- `docs: update architecture guide`
- `test: add parser coverage`

## Release-Hinweis

Releases werden ueber GitHub Actions und Maven Release / Deploy vorbereitet. Fuer Maven Central werden signierte Artefakte, Source-JARs und Javadocs erwartet.

## Branch- und Release-Policy

- Releases nach Maven Central erfolgen nur ueber Versionstags `v*` und nur, wenn der Tag auf einem Commit aus `main` liegt.
- Auf `main` selbst wird kein automatischer Release-Deploy ausgefuehrt.
- Auf Nicht-`main` Branches wird nur gebaut und ein lokales SNAPSHOT (`./mvnw clean install`) erzeugt.
- CI-Logik liegt in `.github/workflows/release.yml` und `.github/workflows/snapshot.yml`.

