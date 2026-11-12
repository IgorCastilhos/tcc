#!/usr/bin/env bash
# Instala o build da interface React em app/src/main/resources/static/,
# de onde o Spring a serve na raiz. Limpa o destino antes para não deixar
# bundles antigos (os nomes dos assets carregam hash e se acumulariam).
set -euo pipefail

root="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
dist="$root/web/dist"
static="$root/app/src/main/resources/static"

if [[ ! -d "$dist" ]]; then
  echo "erro: $dist não existe — rode 'npm run build' em web/ antes." >&2
  exit 1
fi

rm -rf "$static"
mkdir -p "$static"
cp -R "$dist"/. "$static"/

# O spring-boot:run serve de target/classes, não de src/main/resources.
# Sem limpar antes, os bundles antigos (nome com hash) se acumulam ali.
classes="$root/app/target/classes/static"
if [[ -d "$(dirname "$classes")" ]]; then
  rm -rf "$classes"
  mkdir -p "$classes"
  cp -R "$dist"/. "$classes"/
  echo "interface também copiada para target/classes (app em execução já serve a nova)"
fi

echo "interface instalada em app/src/main/resources/static/"
find "$static" -type f | sed "s|$static|  static|"
