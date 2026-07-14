# datachange-example-app

Beispielanwendung zur Demonstration der Framework-JAR `datachange-framework`.

## Demo
- Entity `Customer` ist mit Envers (`@Audited`) versehen.
- ChangeSet `src/main/resources/datachange/001-customer-seed.json` wird beim Start ausgefuehrt.
- Beim zweiten Lauf wird das ChangeSet idempotent uebersprungen.
- Komplexes Modell mit Beziehungen:
  - `PersonProfile` (`@OneToOne` -> `Person`)
  - `Address` (`@ManyToOne` -> `Person`, inverse `@OneToMany` auf `Person`)
  - `BankAccount` (`@ManyToOne` -> `Person`, inverse `@OneToMany` auf `Person`)
- ChangeSet `src/main/resources/datachange/003-person-relationships.json` zeigt Insert/Update ueber mehrere abhaengige Entities mit `saveAs`/`${ref('...')}`.
- ChangeSet `src/main/resources/datachange/004-person-re-assign.json` zeigt Re-Assign auf der Owning Side (ManyToOne/OneToOne), also das Umhaengen bestehender `Address`-, `BankAccount`- und `PersonProfile`-Beziehungen auf eine andere `Person`.
- ChangeSet `src/main/resources/datachange/005-customer-crud.json` zeigt alle CRUD-Bausteine im Framework:
  - Create ueber `insert`
  - Read ueber `preConditions`/`where`
  - Update ueber `update`
  - Delete ueber `delete`
- ChangeSet `src/main/resources/datachange/006-customer-lookup-copy.json` zeigt Cross-Entity-Value-Resolution ueber `${lookup('Entity','whereField','whereValue','selectField')}`.
- ChangeSet `src/main/resources/datachange/007-person-lookup-numeric.json` zeigt `lookup` mit numerischem Filterwert (z. B. `age` als Zahl statt String).
- ChangeSet `src/main/resources/datachange/008-person-lookup-complex.json` zeigt `lookup` mit beliebig komplexen Bedingungen (`and`/`or`/`not`, Klammern, mehrere Vergleiche).
- ChangeSet `src/main/resources/datachange/009-customer-in-operators.json` zeigt `where`-Ausdruecke mit `in` und `not in` in Kombination mit weiteren Bedingungen.
- Ergebnis eines deployten ChangeSets abrufen:
  ```bash
  curl http://localhost:8080/datachange/audit/changeset/customer-crud-005
  ```
  Der Endpunkt ist read-only und zeigt Status, Dauer und Aenderungszahlen eines bereits deployten ChangeSets.

## Start
```zsh
./mvnw -pl datachange-example-app spring-boot:run
```
