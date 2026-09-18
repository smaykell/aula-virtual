#!/usr/bin/env bash
set -euo pipefail

APP_USER="${APP_USER:-aula_virtual}"
APP_PASSWORD="${APP_PASSWORD:-aula_virtual}"
APP_DB="${APP_DB:-aula_virtual}"
TEST_DB="${TEST_DB:-aula_virtual_test}"
SUPER_USER="${SUPER_USER:-postgres}"
DB_HOST="${DB_HOST:-localhost}"
DB_PORT="${DB_PORT:-5432}"

if ! command -v psql >/dev/null 2>&1; then
  echo "No se encontro 'psql' en el PATH. Anade el directorio bin de PostgreSQL y vuelve a intentarlo." >&2
  exit 1
fi

script_dir="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

psql --host="$DB_HOST" --port="$DB_PORT" --username="$SUPER_USER" --dbname=postgres \
     --no-psqlrc --quiet --set=ON_ERROR_STOP=1 \
     --set=app_user="$APP_USER" --set=app_password="$APP_PASSWORD" \
     --set=app_db="$APP_DB" --set=test_db="$TEST_DB" \
     --file="$script_dir/db-bootstrap.sql"

cat <<EOF

Listo: rol $APP_USER, bases $APP_DB y $TEST_DB.
Si no son los valores por defecto, exportalos antes de arrancar:

export DB_URL=jdbc:postgresql://$DB_HOST:$DB_PORT/$APP_DB
export DB_USER=$APP_USER
export DB_PASSWORD=$APP_PASSWORD
EOF
