#!/usr/bin/env bash
# Congela o painel num JSON estático para a publicação aos participantes.
#
# O site publicado não fala com o Postgres nem carrega chaves de API: lê
# este arquivo. Rode com a aplicação no ar, depois de ingerir e extrair os
# sinais que os participantes devem ver na semana.
set -euo pipefail

base="${1:-http://localhost:8080}"
root="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
out="$root/web/public/data/panel.json"

mkdir -p "$(dirname "$out")"
curl -sf "$base/api/panel" -o "$out"

python3 - "$out" <<'PY'
import json, sys
path = sys.argv[1]
with open(path, encoding='utf-8') as f:
    panel = json.load(f)
companies = panel.get('companies', [])
docs = sum(len(c.get('documents', [])) for c in companies)
quotes = sum(1 for c in companies if c.get('quote'))
print(f"snapshot: {len(companies)} empresas, {docs} documentos, {quotes} com cotação")
print(f"gerado em {panel.get('generatedAt')}")
if not companies:
    print("aviso: nenhum sinal no snapshot — os participantes veriam a tela vazia.", file=sys.stderr)
PY

echo "gravado em web/public/data/panel.json"
