#!/bin/bash
# =====================================================
# Docker-entrypoint DB init: schema + demo seed data.
# Runs once, on first container init (empty data dir).
# =====================================================
set -e
DB="${MYSQL_DATABASE:-club_management}"
# --default-character-set=utf8mb4 is REQUIRED: init.sql / seed_users.sql have no
# `SET NAMES`, so without it the client connects as latin1 and double-encodes the
# UTF-8 Chinese into mojibake.
MYSQL="mysql --default-character-set=utf8mb4 -u root -p${MYSQL_ROOT_PASSWORD}"

# 1) Base schema + baseline seed (init.sql contains its own
#    DROP/CREATE/USE database statements, so run without -D).
echo "Applying init.sql (schema + baseline seed)..."
$MYSQL < /tmp/init.sql

# 2) v2 migration: application_draft, approval_record, activity_closure,
#    venue_damage_record, resource_damage_record + new columns.
echo "Applying migration_v2.sql..."
$MYSQL "$DB" < /tmp/migration_v2.sql

# 3) Notification subsystem (rebuilds sys_notification with the full schema,
#    creates sys_notification_preference / sys_notification_log). REQUIRED —
#    every login poll and approval notify() depends on these tables.
echo "Applying upgrade_notification.sql..."
$MYSQL "$DB" < /tmp/upgrade_notification.sql

# 4) Demo seed data (best-effort — a seed error must NOT abort DB init).
set +e
for f in seed_more seed_users seed_approved_data seed_demo_applications fix_phones; do
  if [ -f "/tmp/${f}.sql" ]; then
    echo "Applying ${f}.sql (demo data, best-effort)..."
    $MYSQL "$DB" < "/tmp/${f}.sql" || echo "  WARN: ${f}.sql reported errors (continuing)"
  fi
done
set -e

echo "DB init complete."
