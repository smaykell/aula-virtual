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

Listo: rol $AppUser, bases $AppDb y $TestDb.
Si no son los valores por defecto, exportalos antes de arrancar:

`$env:DB_URL = 'jdbc:postgresql://${DbHost}:${Port}/${AppDb}'
`$env:DB_USER = '$AppUser'
`$env:DB_PASSWORD = '$AppPassword'
"@
