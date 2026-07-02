# datachange-example-app

Beispielanwendung zur Demonstration der Framework-JAR `datachange-framework`.

## Demo
- Entity `Customer` ist mit Envers (`@Audited`) versehen.
- ChangeSet `src/main/resources/datachange/001-customer-seed.json` wird beim Start ausgefuehrt.
- Beim zweiten Lauf wird das ChangeSet idempotent uebersprungen.

## Start
```zsh
./mvnw -pl datachange-example-app spring-boot:run
```

