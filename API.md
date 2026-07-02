# DataChange REST API

## Base URL
```
http://localhost:8080/datachange
```

## Prerequisites
Framework muss mit `mode: MANUAL` konfiguriert sein:
```yaml
datachange:
  mode: MANUAL
  enabled: true
  locations:
    - classpath*:datachange/*.json
```

---

## Endpoints

### 1. List Available ChangeSets

**GET /changesets**

```bash
curl http://localhost:8080/datachange/changesets
```

**Response (200 OK):**
```json
[
  {
    "id": "seed-customers",
    "author": "dev-team",
    "description": "Seed demo customers"
  },
  {
    "id": "migrate-status",
    "author": "data-team",
    "description": "Migrate inactive customers"
  }
]
```

---

### 2. Execute ChangeSet

**POST /execute?id={changeSetId}**

```bash
curl -X POST http://localhost:8080/datachange/execute?id=seed-customers
```

**Response (200 OK):**
```json
{
  "changeSetId": "seed-customers",
  "status": "SUCCESS",
  "inserts": 5,
  "updates": 0,
  "deletes": 0,
  "duration": "00:00:00.234",
  "message": "executed"
}
```

**Mögliche Status:**
- `SUCCESS` – Erfolgreich ausgeführt
- `SKIPPED` – Vorbedingung nicht erfüllt oder bereits ausgeführt
- `FAILED` – Fehler bei Ausführung

---

## Example Workflows

### Workflow 1: Seed-Daten in DEV-Umgebung

```bash
# 1. Verfügbare ChangeSets anschauen
curl http://localhost:8080/datachange/changesets

# 2. Seed-ChangeSet ausführen
curl -X POST http://localhost:8080/datachange/execute?id=seed-dev-data

# 3. Bei Bedarf wiederholen
curl -X POST http://localhost:8080/datachange/execute?id=seed-dev-data
# → Status: SKIPPED (bereits ausgeführt)
```

### Workflow 2: Data Migration in PROD

```bash
# 1. Vorbedingung prüfen
curl http://localhost:8080/datachange/changesets

# 2. Migration ausführen
curl -X POST http://localhost:8080/datachange/execute?id=migrate-customer-v1-to-v2

# 3. Auditing in DB prüfen
SELECT * FROM customer_aud ORDER BY rev DESC LIMIT 5;
```

### Workflow 3: Automation mit Shell-Script

```bash
#!/bin/bash

API="http://localhost:8080/datachange"
CHANGESETS=("seed-roles" "seed-users" "init-permissions")

for cs in "${CHANGESETS[@]}"; do
  echo "Executing: $cs"
  result=$(curl -s -X POST "$API/execute?id=$cs")
  status=$(echo "$result" | jq -r '.status')
  
  if [ "$status" = "SUCCESS" ]; then
    echo "✓ $cs: SUCCESS"
  elif [ "$status" = "SKIPPED" ]; then
    echo "⊝ $cs: SKIPPED (already executed)"
  else
    echo "✗ $cs: FAILED"
    exit 1
  fi
done

echo "All changesets processed successfully!"
```

---

## Error Handling

### ChangeSet Not Found (404)

```bash
curl -X POST http://localhost:8080/datachange/execute?id=non-existent
```

**Response (400 Bad Request):**
```json
{
  "error": "ChangeSet not found: non-existent"
}
```

### Precondition Failed (Status: SKIPPED)

```json
{
  "changeSetId": "seed-customers",
  "status": "SKIPPED",
  "message": "precondition is false"
}
```

**Bedeutet:** Vorbedingung war nicht erfüllt, ChangeSet wurde übersprungen.

### Execution Error (Status: FAILED)

```json
{
  "changeSetId": "bad-changeset",
  "status": "FAILED",
  "message": "Unknown entity: BadEntity"
}
```

**Bedeutet:** Fehler bei Ausführung. Prüfen Sie:
1. Logs in der Anwendung
2. ChangeSet-Definition (JSON)
3. Entity-Namen

---

## Integration mit Monitoring

### Prometheus Metrics

```yaml
# application.yaml
management:
  endpoints:
    web:
      exposure:
        include: metrics, prometheus
```

### Example: Custom Metrics

```bash
# Prüfe Metrics nach Ausführung
curl http://localhost:8080/actuator/metrics | grep datachange
```

---

## Best Practices

### 1. Immer Preconditions nutzen

```json
{
  "preConditions": {
    "expression": "not exists(Customer where email='duplicate@x.de')"
  }
}
```

### 2. Nachrichtenstrom logging

```bash
# Monitor Logs in Echtzeit
tail -f application.log | grep DataChange
```

### 3. Idempotenz-Checks

```bash
# Zweiter Aufruf sollte SKIPPED sein
curl -X POST http://localhost:8080/datachange/execute?id=seed-customers
# Status: SUCCESS

curl -X POST http://localhost:8080/datachange/execute?id=seed-customers
# Status: SKIPPED ✓
```

---

## Integration mit ArgoCD/FluxCD

### Via Webhook

```yaml
# ArgoCD Application
apiVersion: argoproj.io/v1alpha1
kind: Application
metadata:
  name: my-data-app
spec:
  project: default
  source:
    repoURL: https://github.com/myorg/repo
    path: datachange/
  destination:
    server: https://kubernetes.default.svc
  syncPolicy:
    automated:
      prune: true
      selfHeal: true
    syncOptions:
    - CreateNamespace=true
  # Nach Sync ChangeSets triggern
  notifications:
    - name: datachange-sync
      action: webhook
      webhook: http://my-app:8080/datachange/execute
```

### Via CronJob

```yaml
apiVersion: batch/v1
kind: CronJob
metadata:
  name: datachange-runner
spec:
  schedule: "0 2 * * *"  # 2 Uhr morgens täglich
  jobTemplate:
    spec:
      template:
        spec:
          containers:
          - name: curl
            image: curlimages/curl:latest
            command:
            - /bin/sh
            - -c
            - |
              curl -X POST http://my-app-service:8080/datachange/execute?id=daily-cleanup
          restartPolicy: OnFailure
```

