#!/usr/bin/env bash
set -euo pipefail

# ---------------------------------------------------------------------------
# release.sh – Setzt Version in allen POMs, erstellt einen Release-Commit,
# taggt diesen Commit und pusht alles.
# Verwendung:  ./release.sh 1.2.3
# ---------------------------------------------------------------------------

if [ $# -ne 1 ]; then
  echo "Verwendung: $0 <version>  (z.B. $0 1.0.0)"
  exit 1
fi

VERSION="$1"
TAG="v${VERSION}"

# Sicherstellen, dass die Version dem Format X.Y.Z entspricht
if ! echo "$VERSION" | grep -qE '^[0-9]+\.[0-9]+\.[0-9]+$'; then
  echo "Fehler: Version muss im Format X.Y.Z sein (z.B. 1.0.0)"
  exit 1
fi

# Sicherstellen, dass wir auf main sind
BRANCH=$(git rev-parse --abbrev-ref HEAD)
if [ "$BRANCH" != "main" ]; then
  echo "Fehler: Release nur vom main-Branch erlaubt (aktuell: $BRANCH)"
  exit 1
fi

# Sicherstellen, dass der Arbeitsbereich sauber ist
if [ -n "$(git status --porcelain)" ]; then
  echo "Fehler: Es gibt uncommittete Änderungen. Bitte erst committen."
  git status --short
  exit 1
fi

# Sicherstellen, dass main aktuell ist
echo "Hole aktuellen Stand von origin/main..."
git pull --ff-only origin main

# Prüfen ob Tag bereits existiert
if git rev-parse "$TAG" >/dev/null 2>&1; then
  echo "Fehler: Tag '$TAG' existiert bereits lokal."
  exit 1
fi

echo ""
echo "==> Setze Version auf $VERSION in allen POMs..."
./mvnw -B versions:set -DnewVersion="$VERSION" -DgenerateBackupPoms=false

echo "==> Baue und teste..."
./mvnw -B clean verify

echo "==> Committe Release-Version $VERSION..."
git add -A
git commit -m "release: $VERSION"

echo "==> Erstelle Tag $TAG..."
git tag "$TAG"


echo "==> Pushe main und Tag zu origin..."
git push origin main
git push origin "$TAG"

echo ""
echo "✅ Release $TAG erfolgreich erstellt und gepusht!"
echo "   GitHub Actions Release-Workflow wird jetzt ausgelöst."

