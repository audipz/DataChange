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

## Start
```zsh
./mvnw -pl datachange-example-app spring-boot:run
```
