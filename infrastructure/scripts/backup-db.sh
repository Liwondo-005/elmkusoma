#!/bin/bash
# ELMKUSOMA database backup — writes real status JSON consumed by Platform Admin (spec §58)
set -uo pipefail

TIMESTAMP=$(date +%Y%m%d_%H%M%S)
RUN_AT=$(date +%Y-%m-%dT%H:%M:%S)
BACKUP_DIR="${BACKUP_DIR:-./backups}"
STATUS_FILE="${STATUS_FILE:-$BACKUP_DIR/last_backup_status.json}"
HISTORY_FILE="$BACKUP_DIR/backup_history.log"
DUMP_FILE="$BACKUP_DIR/elmkusoma_$TIMESTAMP.dump"
DB_NAME="${DB_NAME:-elmkusoma}"
DB_USER="${DB_USER:-elmkusoma}"
DB_HOST="${DB_HOST:-localhost}"

mkdir -p "$BACKUP_DIR"

echo "Backing up ELMKUSOMA database..."

EXIT_CODE=0
pg_dump -U "$DB_USER" -h "$DB_HOST" -d "$DB_NAME" -F c -f "$DUMP_FILE" || EXIT_CODE=$?

SIZE=0
if [ -f "$DUMP_FILE" ]; then
  SIZE=$(stat -c%s "$DUMP_FILE" 2>/dev/null || stat -f%z "$DUMP_FILE" 2>/dev/null || echo 0)
fi

INTEGRITY_OK=false
STATUS="FAILURE"
if [ $EXIT_CODE -eq 0 ] && [ -f "$DUMP_FILE" ] && [ "$SIZE" -gt 0 ]; then
  # verify archive is readable
  if pg_restore --list "$DUMP_FILE" > /dev/null 2>&1; then
    INTEGRITY_OK=true
    STATUS="SUCCESS"
    echo "Backup saved to $DUMP_FILE ($SIZE bytes)"
  else
    echo "Backup file failed integrity check (pg_restore --list)" >&2
    EXIT_CODE=1
  fi
else
  echo "pg_dump failed with exit code $EXIT_CODE" >&2
fi

cat > "$STATUS_FILE" <<EOF
{
  "status": "$STATUS",
  "runAt": "$RUN_AT",
  "file": "$(basename "$DUMP_FILE")",
  "sizeBytes": $SIZE,
  "integrityOk": $INTEGRITY_OK,
  "exitCode": $EXIT_CODE
}
EOF

echo "$RUN_AT $STATUS exit=$EXIT_CODE size=$SIZE file=$(basename "$DUMP_FILE")" >> "$HISTORY_FILE"

# Keep only last 7 backups
ls -t "$BACKUP_DIR"/elmkusoma_*.dump 2>/dev/null | tail -n +8 | xargs -r rm
echo "Old backups cleaned up."

exit $EXIT_CODE
