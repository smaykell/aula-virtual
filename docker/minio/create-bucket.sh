#!/bin/sh
set -e

mc alias set local http://minio:9000 aula_virtual aula_virtual
mc mb --ignore-existing local/aula-virtual
mc anonymous set none local/aula-virtual
mc ilm import local/aula-virtual < /setup/lifecycle.json
