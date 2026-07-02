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
