#!/bin/bash
TIMESTAMP=$(date +%Y%m%d_%H%M%S)
BACKUP_DIR="./backups"
mkdir -p $BACKUP_DIR

echo "Backing up ELMKUSOMA database..."
pg_dump -U elmkusoma -h localhost -d elmkusoma -F c -f "$BACKUP_DIR/elmkusoma_$TIMESTAMP.dump"

echo "Backup saved to $BACKUP_DIR/elmkusoma_$TIMESTAMP.dump"

# Keep only last 7 backups
ls -t $BACKUP_DIR/elmkusoma_*.dump | tail -n +8 | xargs -r rm
echo "Old backups cleaned up."
