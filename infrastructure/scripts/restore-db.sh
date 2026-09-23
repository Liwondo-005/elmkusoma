#!/bin/bash
# ELMKUSOMA database restore — documented recovery path (spec §58)
# Usage: ./restore-db.sh <dump-file> [--no-clean]
set -euo pipefail

if [ $# -lt 1 ]; then
  echo "Usage: $0 <dump-file> [--no-clean]" >&2
  exit 2
fi

DUMP_FILE="$1"
DB_NAME="${DB_NAME:-elmkusoma}"
DB_USER="${DB_USER:-elmkusoma}"
DB_HOST="${DB_HOST:-localhost}"
CLEAN="--clean --if-exists"
if [ "${2:-}" = "--no-clean" ]; then
  CLEAN=""
fi

if [ ! -f "$DUMP_FILE" ]; then
  echo "Dump file not found: $DUMP_FILE" >&2
  exit 1
fi

echo "Verifying dump integrity..."
if ! pg_restore --list "$DUMP_FILE" > /dev/null 2>&1; then
  echo "Dump failed integrity check — aborting restore" >&2
  exit 1
fi

echo "Restoring $DUMP_FILE into $DB_NAME on $DB_HOST..."
# shellcheck disable=SC2086
pg_restore -U "$DB_USER" -h "$DB_HOST" -d "$DB_NAME" $CLEAN --no-owner "$DUMP_FILE"

echo "Restore complete. Next steps:"
echo "  1. Run infrastructure/scripts/health-check.sh"
echo "  2. Verify login + dashboards in Platform Admin"
echo "  3. Record the recovery event in the audit log"
