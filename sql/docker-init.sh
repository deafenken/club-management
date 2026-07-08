#!/bin/bash
# Docker-entrypoint wrapper for init.sql + migration_v2.sql
# The MySQL Docker image has already created MYSQL_DATABASE and selected it
# as the default database. init.sql starts with DROP DATABASE / CREATE DATABASE
# / USE statements that would fail inside the container. We strip those first 3
# lines so the schema + seed data load cleanly.
set -e
sed '1,3d' /tmp/init.sql | mysql -u root -p"${MYSQL_ROOT_PASSWORD}"
# Apply v2 migration (new tables: application_draft, approval_record, activity_closure,
# venue_damage_record, resource_damage_record; new columns for existing tables)
if [ -f /tmp/migration_v2.sql ]; then
  echo "Applying migration_v2.sql..."
  mysql -u root -p"${MYSQL_ROOT_PASSWORD}" club_management < /tmp/migration_v2.sql
fi
