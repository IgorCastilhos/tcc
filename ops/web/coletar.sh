#!/usr/bin/env bash
# Amplia o painel: descobre um Fato Relevante por companhia mapeada na B3,
# recupera o que o ENET recusou e sincroniza as cotações.
#
# Não extrai sinais — isso consome chamadas pagas de LLM e fica a seu
# critério (POST /api/signals/extract/{documentId}).
set -euo pipefail

base="${BASE:-http://localhost:8080}"
ano="${ANO:-$(date +%Y)}"

echo "== catálogo B3 =="
cnpjs=$(curl -sf "$base/api/panel/tickers" \
  | python3 -c "import sys,json;print('&'.join('cnpj='+x['cnpj'] for x in json.load(sys.stdin)))")

echo "== descobrindo Fatos Relevantes de $ano (1 por empresa) =="
curl -sf -X POST "$base/api/ingestion/discover?type=FATO_RELEVANTE&year=$ano&limit=60&maxPorEmpresa=1&$cnpjs"
echo

echo "== recuperando os que o ENET recusou (leva alguns minutos) =="
curl -sf -m 2400 -X POST "$base/api/ingestion/reparse-failed?limit=60"
echo

echo "== cotações das empresas com ticker =="
ok=0; fail=0
for t in $(curl -sf "$base/api/panel/tickers" | python3 -c "import sys,json;[print(x['ticker']) for x in json.load(sys.stdin) if x['ingested']]"); do
  if curl -sf -m 90 -X POST "$base/api/market-data/sync/$t?range=3mo" >/dev/null 2>&1; then
    ok=$((ok+1))
  else
    fail=$((fail+1)); echo "  sem cotação: $t"
  fi
  sleep 3
done
echo "cotações: $ok sincronizadas, $fail sem dados"

echo
echo "Documentos indexados aguardando extração de sinais:"
curl -sf "$base/api/panel" | python3 -c "
import sys,json
d=json.load(sys.stdin)
print(f\"  painel atual: {len(d['companies'])} empresas com sinais\")
print('  rode POST /api/signals/extract/{documentId} para incluir as demais')"
