[CmdletBinding()]
param(
    [string]$AppUser = 'aula_virtual',
    [string]$AppPassword = 'aula_virtual',
    [string]$AppDb = 'aula_virtual',
    [string]$TestDb = 'aula_virtual_test',
    [string]$SuperUser = 'postgres',
    [string]$DbHost = 'localhost',
    [int]$Port = 5432
)

$ErrorActionPreference = 'Stop'

if (-not (Get-Command psql -ErrorAction SilentlyContinue)) {
    throw "No se encontro 'psql' en el PATH. Anade el directorio bin de PostgreSQL y vuelve a intentarlo."
}

$sqlFile = Join-Path $PSScriptRoot 'db-bootstrap.sql'

psql --host=$DbHost --port=$Port --username=$SuperUser --dbname=postgres `
     --no-psqlrc --quiet --set=ON_ERROR_STOP=1 `
     --set=app_user=$AppUser --set=app_password=$AppPassword `
     --set=app_db=$AppDb --set=test_db=$TestDb `
     --file=$sqlFile

if ($LASTEXITCODE -ne 0) {
    throw "psql termino con codigo $LASTEXITCODE; la base no quedo preparada."
}

@"

Listo. Copia estas lineas en tu .env:

DB_URL=jdbc:postgresql://${DbHost}:${Port}/${AppDb}
DB_USER=$AppUser
DB_PASSWORD=$AppPassword
"@
