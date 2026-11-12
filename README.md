# CVM Signals — Pipeline RAG de Extração de Sinais Financeiros

Artefato do TCC (Unisinos, 2026): pipeline de Retrieval-Augmented Generation
que extrai **sentimento, risco e eventos corporativos** de documentos
regulatórios da CVM (DFP, ITR, Fatos Relevantes) e compara três LLMs —
Claude Sonnet, GPT-4o e Gemma (local) — sob o mesmo contexto e os mesmos
prompts, com avaliação técnica (RAGAS) e de utilidade percebida (TAM).

## Mapa monografia → código

| Monografia | Implementação |
|---|---|
| §4.2 Arquitetura (6 bounded contexts, tópicos Kafka) | módulos `ingestion`, `indexing`, `retrieval`, `signal-extraction`, `market-data`, `evaluation`; `shared-kernel/Topics.java` |
| §4.3 Modelo de dados (Figura 2, vector(1024), JSONB, HNSW, Flyway) | `app/src/main/resources/db/migration/V1__schema.sql` |
| §4.4 Tecnologias (Spring AI, Tika, BGE-M3, pgvector, Kafka, Docker) | `AiConfig`, `TikaDocumentParser`, `docker-compose.yml` |
| §4.5 Avaliação (RAGAS + estudo TAM de 3 semanas, Quadro 4) | `ragas/`, endpoint `/api/evaluation/ragas-dataset`, módulo `evaluation` + UI `static/index.html` |

Decisões registradas em `docs/adr/`.

## Pré-requisitos
Docker + Docker Compose, JDK 21, Maven 3.9+, Node 20+ (interface React) e
Ollama instalado no host (recomendado no macOS, pelo suporte Metal).

---

# Subindo a aplicação — passo a passo

O caminho abaixo é o que foi executado e validado em **macOS Apple Silicon**
(Docker 29.7, Corretto 21.0.11, Maven 3.9.11). A infraestrutura roda em
contêiner, e a aplicação Spring roda **no host** para aproveitar o Ollama
nativo com Metal. Em Linux, veja a variação no passo 3.

## Passo 0 — Conferir o ambiente

Antes de subir qualquer coisa, confirme que as quatro dependências existem.
Cada comando deve responder sem erro:

```bash
docker info --format '{{.ServerVersion}}'   # daemon do Docker no ar
java -version                               # precisa ser 21
mvn -v                                      # 3.9+
node -v                                     # 20+ (build da interface React)
curl -s http://localhost:11434/api/tags     # Ollama respondendo no host
```

Se o Ollama não responder, abra o app do Ollama (macOS) ou `ollama serve`.

## Passo 1 — Configurar o `.env`

```bash
cp .env.example .env
```

Preencha as três credenciais obrigatórias — sem elas os braços de fronteira
não extraem sinal e a sincronização de cotações falha:

| Variável | Onde obter |
|---|---|
| `ANTHROPIC_API_KEY` | console.anthropic.com |
| `OPENAI_API_KEY` | platform.openai.com |
| `BRAPI_TOKEN` | https://brapi.dev/dashboard (plano gratuito exige token) |

O `Makefile` faz `-include .env` e exporta essas variáveis para a aplicação,
então **rodar via `make` é o que garante que as chaves chegam ao processo**.
Subir com `mvn spring-boot:run` direto, fora do `make`, sobe uma aplicação
sem chaves. Para conferir depois que a app estiver de pé:

```bash
ps eww $(lsof -ti tcp:8080) | tr ' ' '\n' | grep -E '^(ANTHROPIC|OPENAI|BRAPI)'
```

## Passo 2 — Baixar os modelos no Ollama

```bash
make models        # ollama pull bge-m3 + Gemma
```

Confira que os dois modelos que o pipeline usa aparecem na lista — `bge-m3`
(embeddings, 1024 dims) e o modelo de chat do braço local:

```bash
curl -s http://localhost:11434/api/tags | python3 -c \
  "import sys,json;[print(' -',m['name']) for m in json.load(sys.stdin)['models']]"
```

Se `gemma4:e4b` não existir no seu registry, ajuste `OLLAMA_CHAT_MODEL` no
`.env` (ex.: `gemma3n:e4b`).

## Passo 3 — Subir a infraestrutura

**No macOS** o Docker não expõe a GPU, e a imagem do TEI (reranker) é
CPU-bound e específica por arquitetura. Por isso a infra sobe **sem** o TEI,
que vem no passo 4 em modo nativo:

```bash
make infra-no-tei      # Postgres+pgvector, Kafka, Prometheus, Grafana
```

**No Linux**, suba tudo junto — inclusive o TEI em contêiner — e pule o
passo 4:

```bash
make infra             # idem + tei-reranker (porta 8081)
# ou 'make infra-linux' para incluir também o Ollama containerizado
```

Espere o Postgres ficar `healthy` antes de seguir:

```bash
docker ps --format 'table {{.Names}}\t{{.Status}}\t{{.Ports}}'
```

Esperado: `cvm-postgres` (healthy), `cvm-kafka`, `cvm-prometheus`,
`cvm-grafana`.

## Passo 4 — Subir o reranker TEI nativo (só macOS)

O reranker cross-encoder BGE-reranker-v2-m3 é obrigatório para
`/api/retrieval/search`: o `TeiRerankAdapter` **não tem fallback**, então
sem ele a busca falha. Instale uma vez e deixe rodando em um terminal
separado:

```bash
brew install text-embeddings-inference     # uma única vez
make tei-native                            # ocupa o terminal, porta 8081
```

Ou, em segundo plano:

```bash
nohup text-embeddings-router --model-id BAAI/bge-reranker-v2-m3 \
  --port 8081 > /tmp/tei.log 2>&1 &
```

O carregamento leva alguns segundos (baixa o modelo na primeira vez e faz
warm-up na GPU). O log deve mostrar `Starting Bert model on Metal(...)`.
Valide com um teste de sanidade — o trecho relevante precisa pontuar muito
acima do irrelevante:

```bash
curl -s http://localhost:8081/health

curl -s -X POST http://localhost:8081/rerank -H 'Content-Type: application/json' \
  -d '{"query":"dividendos","texts":["A companhia aprovou pagamento de dividendos.","O clima estava bom ontem."]}'
# [{"index":0,"score":0.43},{"index":1,"score":0.0000163}]  ✅
```

## Passo 5 — Subir a aplicação

```bash
make run       # compila a interface, os módulos Java e sobe o composition root
```

O alvo depende de `make web`, que roda `npm install` e `npm run build` em
`web/` e instala o bundle em `app/src/main/resources/static/` — é de lá que o
Spring serve a interface React na raiz. Em seguida vêm
`mvn -DskipTests -pl app -am install` (build dos seis módulos Java) e
`mvn -pl app spring-boot:run`. No boot, o Flyway aplica as migrations
— `V1__schema.sql` cria as seis tabelas de domínio (`empresa`, `documento`,
`chunk`, `sinal`, `serie_preco`, `avaliacao_usuario`) mais a
`flyway_schema_history`; `V2__mapa_ticker_b3.sql` cria o mapa CNPJ → código
B3 (ver *Interface do estudo*); e `V3__corrige_ticker_copel.sql` corrige um
erro da semente. Aguarde a aplicação responder:

```bash
curl -s --retry 30 --retry-delay 2 --retry-connrefused \
  http://localhost:8080/actuator/health
# {"status":"UP"}
```

## Passo 6 — Validar a stack de ponta a ponta

Todos devem retornar **200**:

```bash
B=http://localhost:8080
for u in "/" "/api/retrieval/search?q=dividendos&topK=5" "/api/evaluation/signals" \
         "/api/evaluation/report" "/api/evaluation/ragas-dataset" \
         "/actuator/health" "/actuator/prometheus"; do
  printf "%-45s -> HTTP %s\n" "$u" "$(curl -s -o /dev/null -w '%{http_code}' -m 90 "$B$u")"
done
```

Confirme também que o Prometheus está coletando a aplicação (target
`cvm-signals` com health `up`):

```bash
curl -s 'http://localhost:9090/api/v1/targets?state=active' | python3 -c \
  "import sys,json;[print(t['labels'].get('job'),t['health']) for t in json.load(sys.stdin)['data']['activeTargets']]"
```

E inspecione o estado do banco:

```bash
docker exec cvm-postgres psql -U cvm -d cvmsignals -tAc \
  "select 'documento='||(select count(*) from documento)
       ||' chunk='||(select count(*) from chunk)
       ||' sinal='||(select count(*) from sinal)
       ||' avaliacao='||(select count(*) from avaliacao_usuario)"
```

## Passo 7 — Popular o banco (primeira execução)

Em um banco vazio a UI não mostra nada. Rode o pipeline uma vez:

O CSV da CVM vem agrupado por companhia: uma descoberta sem filtro esgota o
limite nas primeiras empresas da lista. Para o painel ter **várias ações**,
restrinja aos CNPJs do catálogo B3 e limite a um documento por empresa.

O caminho curto é `make coletar`, que faz os três passos de uma vez —
descoberta ampla, recuperação dos downloads recusados e cotações:

```bash
make coletar          # leva alguns minutos: o ENET só entrega em ritmo lento
```

Passo a passo, se preferir controlar cada etapa:

```bash
# 1. descobre uma amostra ampla — um Fato Relevante por companhia mapeada na B3
CNPJS=$(curl -s localhost:8080/api/panel/tickers \
  | python3 -c "import sys,json;print('&'.join('cnpj='+x['cnpj'] for x in json.load(sys.stdin)))")
curl -X POST "localhost:8080/api/ingestion/discover?type=FATO_RELEVANTE&year=2026&limit=40&maxPorEmpresa=1&$CNPJS"

# 1b. o ENET recusa rajadas e marca documentos como FAILED; repita devagar
curl -X POST "localhost:8080/api/ingestion/reparse-failed?limit=40"

# 2. confirme que os chunks foram indexados
curl -s "localhost:8080/api/retrieval/search?q=dividendos&topK=3"

# 3. extraia os sinais de um documento nos três braços (Claude, GPT-4o, Gemma)
curl -X POST "localhost:8080/api/signals/extract/{documentId}"

# 4. cotações das empresas com ticker mapeado (3mo é o teto do plano free do brapi)
curl -X POST "localhost:8080/api/market-data/sync/BBAS3?range=3mo"
```

`EXTRACTION_AUTO=false` no `.env` significa que **indexar não dispara
extração** — o comando 3 acima é manual. Para automatizar, mude para `true`
e reinicie a aplicação.

## Derrubando tudo

```bash
docker compose down          # infra (adicione -v para apagar os volumes)
# Ctrl+C nos terminais do 'make run' e do 'make tei-native'
```

---

# Interface do estudo (React)

A interface que os participantes usam vive em `web/` — Vite + React +
TypeScript, sem framework de UI. Ela é o que a Seção 4.5 da monografia chama
de *interface web que apresenta os sinais extraídos pela pipeline*.

## O que ela mostra

Uma linha por companhia com **código B3, razão social, CNPJ, cotação e
variação do dia**, e — ao abrir o card — a leitura dos **três modelos lado a
lado sobre o mesmo documento**: sentimento, risco, eventos corporativos, a
confiança declarada por cada modelo e os trechos do documento que sustentam
cada classificação.

Em *Ver trechos citados do documento* a cadeia de evidência fica completa:
uma linha de **Fonte** identifica o documento (tipo, data de referência,
protocolo de entrega) e leva ao arquivo original publicado na CVM, e cada
trecho vem rotulado com o sinal que ele sustenta — Sentimento, Risco ou o
evento específico. Assim o participante consegue sair da conclusão do modelo
e chegar ao texto que a originou, que é o que permite julgar se a
classificação faz sentido.

**Não há coluna de recomendação, e isso é deliberado.** O pipeline não produz
esse juízo: os prompts pedem classificação de sentimento
(`POSITIVO`/`NEGATIVO`/`NEUTRO`), de risco (`ALTO`/`MEDIO`/`BAIXO`) e a lista
de eventos corporativos — nada mais. Some-se a isso o escopo declarado no
TCC, que exclui "a recomendação de estratégias de investimento aos
participantes", e a Seção 4.5, que afirma que "nenhuma recomendação de
investimento é feita aos participantes". A interface exibe o **veredito de
cada modelo** e carrega o aviso no topo de cada tela.

## Apresentação guiada no primeiro acesso

Quem abre o site pela primeira vez recebe um tour de nove passos com balões
ancorados nos elementos reais da tela: a busca, a identidade da empresa, a
cotação, os pontos de consenso dos três modelos, o comparativo completo, os
trechos citados, o questionário e o aviso de que nada ali é recomendação. O
passo do comparativo abre o primeiro card sozinho, para o participante ver o
que está sendo explicado.

O tour aparece **uma única vez por navegador** — a marca fica em
`localStorage`, na chave `cvm-signals:tour:v1`. Depois disso ele só reabre
pelo botão **Como usar**, no topo. Mudou o roteiro de forma relevante? Suba a
versão da chave em `web/src/lib/tour.ts` e todo mundo vê o tour novo.

Os passos que falam de empresa são omitidos automaticamente quando o painel
ainda está vazio, então a apresentação nunca aponta para o nada.

A aba **Questionário** embute o formulário do Google que aplica o
instrumento TAM do Quadro 4. As respostas vão direto para a planilha do
formulário, o que resolve a coleta no site publicado com dados congelados —
ali não há backend para receber um POST.

Trocar de formulário é trocar uma constante: `FORM_ID`, no topo de
`web/src/components/GoogleForm.tsx`. O embed que o Google gera vem com
`width=640` fixo, que estoura em telas estreitas e sobra nas largas; o CSS da
interface o converte para largura fluida, acompanhando o painel. Há também um
link para abrir o formulário em nova aba, caso o navegador do participante
bloqueie iframes de terceiros.

> **O que isso significa para o backend.** `POST /api/evaluation`, a tabela
> `avaliacao_usuario` e `GET /api/evaluation/report` continuam existindo, mas
> a interface não os alimenta mais — a consolidação das respostas passa a ser
> feita sobre a planilha do Google. O questionário embutido original está
> preservado em `web/legacy/Questionnaire.tsx` para quem quiser voltar a
> coletar dentro da aplicação.

## Ticker e cotação: como o vínculo foi feito

Os datasets da CVM não trazem o código de negociação — `DiscoverDocumentsService`
grava `empresa.ticker` como `NULL` —, e `serie_preco` é indexada por ticker.
Sem uma ponte, não há como pôr o preço ao lado do sinal.

A migration `V2__mapa_ticker_b3.sql` cria a tabela `mapa_ticker` com uma
**semente curada** de 41 companhias líquidas da B3 (`origem = 'SEED'`) e
preenche `empresa.ticker` onde o CNPJ bate.

> **Confira a semente antes de rodar o estudo.** Um ticker errado associa a
> cotação de outra companhia aos sinais exibidos. Cada linha guarda o
> `nome_referencia` usado ao montá-la, justamente para permitir a conferência
> contra o nome que veio da CVM.
>
> Os 41 códigos foram validados um a um contra a API de cotações: 38
> respondem com preço. `CPLE6` era erro de mapeamento — a Copel migrou ao
> Novo Mercado e as preferenciais viraram ordinárias —, corrigido para
> `CPLE3` na migration V3. `AZUL4` e `GOLL4` seguem registradas na CVM e
> publicam documentos, mas os papéis não têm cotação disponível; o painel
> mostra os sinais dessas companhias sem preço.

Para corrigir uma associação:

```bash
curl -X PUT localhost:8080/api/panel/empresas/00000000000191/ticker \
  -H 'Content-Type: application/json' \
  -d '{"ticker":"BBAS3","referenceName":"BANCO DO BRASIL S.A."}'
```

Isso grava `origem = 'MANUAL'` e propaga para `empresa`. Depois, sincronize a
série com `POST /api/market-data/sync/{ticker}`.

## Desenvolvendo a interface

```bash
make run                  # aplicação + interface compilada, em localhost:8080
make web-dev              # hot reload em localhost:5173 (proxy /api → 8080)
```

`make web-dev` exige a aplicação no ar em outro terminal: o Vite encaminha
`/api` para `localhost:8080`, o que evita CORS sem afrouxar a configuração do
Spring.

## Publicando para os participantes

O site publicado roda com **dados congelados**: nem Postgres, nem chaves de
API, nem custo de inferência por acesso.

```bash
# 1. com a aplicação no ar e os sinais da semana já extraídos:
make web-snapshot         # congela /api/panel em web/public/data/panel.json

# 2. gera o build que lê o snapshot em vez da API:
make web-publish          # resultado em web/dist
```

Na Vercel, o projeto já está vinculado (`web/.vercel`), então publicar é:

```bash
cd web && vercel deploy --prod
```

**No ar em https://pipeline-sinais-financeiros.vercel.app** — é este o link
que vai para os participantes. O `web/vercel.json` fixa `VITE_SNAPSHOT=1`, o
comando de build, a saída e os cabeçalhos de cache (bundles com hash
imutáveis, `index.html` e o snapshot sempre revalidados).

Repita o ciclo a cada semana do estudo para atualizar os sinais:

```bash
make web-snapshot     # com a aplicação no ar, congela o painel da semana
make web-publish      # rebuild com os dados novos
cd web && vercel deploy --prod
```

> A URL de preview (`vercel deploy` sem `--prod`) fica atrás da proteção SSO
> da Vercel e pede login — serve para você conferir, não para divulgar. Quem
> vai para os participantes é a URL de produção acima, que é pública.

> **Coleta de respostas no site publicado.** Resolvida pelo Google Forms: o
> questionário é servido pelo Google, então as respostas chegam à planilha
> independentemente de a aplicação estar no ar. O build congelado precisa
> cuidar apenas dos sinais.

---

# Links disponíveis

## Aplicação — `http://localhost:8080`

### Navegáveis (GET)

| Link | O que é |
|---|---|
| http://localhost:8080/ | **Interface React do estudo** — empresas, cotação, veredito dos 3 modelos e questionário do Quadro 4 |
| http://localhost:8080/api/retrieval/search?q=dividendos&topK=5 | Busca híbrida (pgvector + FTS `portuguese`), fusão RRF e rerank |
| http://localhost:8080/api/panel | **Read model da interface** — empresas, cotações, leitura dos 3 modelos e Quadro 4, num payload só |
| http://localhost:8080/api/panel/tickers | Catálogo CNPJ → código B3, com marca de quem já foi ingerido |
| http://localhost:8080/api/evaluation/signals | Todos os sinais persistidos, em JSON (lista plana) |
| http://localhost:8080/api/evaluation/report | Consolidação descritiva das respostas TAM |
| http://localhost:8080/api/evaluation/ragas-dataset | Dataset JSONL (`question`/`answer`/`contexts`) para o RAGAS |
| `http://localhost:8080/api/signals/document/{documentId}` | Sinais de um documento, um por modelo e tipo |
| `http://localhost:8080/api/market-data/series/{ticker}` | Série histórica de preços (ex.: `.../series/PETR4`) |
| http://localhost:8080/actuator/health | Saúde da aplicação |
| http://localhost:8080/actuator/info | Metadados do build |
| http://localhost:8080/actuator/metrics | Índice de métricas Micrometer |
| http://localhost:8080/actuator/prometheus | Métricas no formato Prometheus (raspado pelo Prometheus) |

Parâmetros de `/api/retrieval/search`: `q` (obrigatório), `topK` (default 5),
`cnpj` e `tipo` (filtros opcionais).

### Ações (POST — via `curl`, não navegáveis)

| Endpoint | Parâmetros |
|---|---|
| `POST /api/ingestion/discover` | `type=DFP\|ITR\|FATO_RELEVANTE`, `year`, `limit` (default 50), `cnpj` (repetível) e `maxPorEmpresa` |
| `POST /api/ingestion/reparse-failed` | `limit` — nova tentativa de download/parse dos documentos marcados FAILED |
| `POST /api/indexing/reindex/{documentId}` | reindexa um documento |
| `POST /api/indexing/reindex-pending` | reindexa tudo que ficou pendente |
| `POST /api/signals/extract/{documentId}` | RAG restrito ao documento → 3 LLMs |
| `POST /api/market-data/sync/{ticker}` | `range` (default `1y`; no plano free do brapi use `1d`, `5d`, `1mo` ou `3mo`) — cotações brapi.dev |
| `POST /api/evaluation` | corpo JSON: `participantCode`, `week`, `answers` (mapa `1..10` → `1..5`), `comments` — não é mais usado pela interface, ver *Interface do estudo* |
| `PUT /api/panel/empresas/{cnpj}/ticker` | corpo JSON: `ticker`, `referenceName` — corrige o mapa CNPJ → B3 |

Exemplo do envio de uma resposta TAM:

```bash
curl -X POST localhost:8080/api/evaluation -H 'Content-Type: application/json' -d '{
  "participantCode": "P01",
  "week": 1,
  "answers": {"1":4,"2":5,"3":4,"4":4,"5":3,"6":5,"7":4,"8":4,"9":5,"10":4},
  "comments": "..."
}'
```

## Infraestrutura

| Link | Serviço | Credenciais |
|---|---|---|
| http://localhost:3000 | **Grafana** | `admin` / `admin` |
| http://localhost:9090 | **Prometheus** — target `cvm-signals` | — |
| http://localhost:8081/health | **TEI reranker** BGE-reranker-v2-m3 (`POST /rerank`) | — |
| http://localhost:5173 | **Vite dev server** da interface (`make web-dev`) | — |
| http://localhost:11434 | **Ollama** — `bge-m3` + Gemma | — |
| `localhost:5432` | **Postgres + pgvector**, db `cvmsignals` | `cvm` / `cvm` |
| `localhost:9092` | **Kafka** (listener externo) | — |

O Grafana vem apenas com o **datasource** do Prometheus provisionado
(`ops/grafana/provisioning/datasources/datasource.yml`) — não há dashboard
pronto. Use *Explore* e consulte as métricas com o label
`application="cvm-signals"`.

---

## Fluxo do pipeline

```
POST /api/ingestion/discover?type=FATO_RELEVANTE&year=2026&limit=20
   └─ cvm.docs.discovered → download + Tika → cvm.docs.parsed
        └─ chunking + BGE-M3 → pgvector → cvm.chunks.embedded
             └─ (extraction.auto=true) ou POST /api/signals/extract/{documentId}
                  └─ RAG restrito ao documento → 3 LLMs → sinal → cvm.signals.extracted
```

## Avaliação

```bash
make ragas          # exporta dataset dos sinais persistidos e roda o container RAGAS
```
`context_recall` exige `ground_truth` (anotação manual) em cada linha do
JSONL; sem ele, o script calcula as demais métricas e avisa.

O estudo TAM roda na própria aplicação: participantes (código pseudonimizado,
TCLE/CNS 510/2016) acessam `http://localhost:8080/`, leem os sinais e
respondem às 10 afirmações Likert do Quadro 4 ao fim de cada semana.

## Testes

```bash
make test
```
- Unitários de domínio e de casos de uso: `ChunkingPolicyTest`,
  `ReciprocalRankFusionTest`, `PriceSeriesTest`, `UserEvaluationTest`,
  `ExtractSignalsServiceTest`, `DiscoverDocumentsServiceTest` (filtro por
  CNPJ, teto por empresa e idempotência da descoberta) e
  `RetryFailedDocumentsServiceTest` (uma URL que segue recusando não aborta o
  lote) — todos com fakes em memória, sem mocks de infraestrutura.
- Integração real (Testcontainers, sem mocks): `PgVectorHybridSearchIntegrationTest`
  valida migration, HNSW, FTS 'portuguese', filtros e fusão RRF contra
  `pgvector/pgvector:pg16`.
- `ArchitectureTest` (ArchUnit): quebra o build se domínio/aplicação
  importarem framework.

## Avisos honestos

- **A aplicação foi executada e validada de ponta a ponta** em macOS Apple
  Silicon (24/08/2026): todos os GETs da tabela acima responderam 200 com
  dado real, o reranker pontuou corretamente e os três braços gravaram
  sinais para o mesmo documento. O que **não** foi executado nessa validação
  foi a suíte de testes (`mvn clean verify`) — rode-a localmente antes do
  primeiro uso em outra máquina e trate eventuais ajustes de API do
  Spring AI 1.0.x como revisão de código normal.
- DFP/ITR: a descoberta usa os CSVs de metadados da CVM cujo `LINK_DOC`
  aponta para o pacote ENET (ZIP); o Tika extrai o texto recursivamente.
  Fatos Relevantes usam o dataset IPE. Valide amostras antes de coletas
  grandes — os layouts da CVM mudam entre anos.
- **O ENET limita rajadas de download.** Sob muitas requisições seguidas ele
  responde HTTP 200 com a página intermediária do ASP.NET em vez do arquivo;
  o Tika extrai zero caractere e o documento vira FAILED. O downloader já
  manda User-Agent de navegador e repete até 4 vezes, mas numa coleta de
  algumas dezenas de documentos ainda sobra uma parcela para recuperar depois
  com `reparse-failed`, em lotes pequenos e espaçados. Conte com isso ao
  planejar a coleta: não é bug do pipeline, é o servidor da CVM se defendendo.
- `gemma4:e4b` é o default do braço local; se a tag não existir no seu
  registry Ollama, ajuste `OLLAMA_CHAT_MODEL` (ex.: `gemma3n:e4b`).

## Solução de problemas

**`no matching manifest for linux/arm64/v8` ao subir o TEI.** A imagem do
reranker tem manifestos separados por arquitetura. Em Apple Silicon use
`cpu-arm64-1.9` (default); em x86_64 defina no `.env`
`TEI_IMAGE=ghcr.io/huggingface/text-embeddings-inference:cpu-1.9`.
Quando um pull falha, o compose interrompe os demais — os outros serviços não
estão quebrados; basta repetir `make infra`.

**Reranker lento no macOS.** O Docker não expõe a GPU no macOS. Para usar
Metal: `brew install text-embeddings-inference`, depois `make tei-native` em
um terminal separado (porta 8081, mesma API) e `make infra-no-tei` para o
restante da infraestrutura (passos 3 e 4 acima).

**`/api/retrieval/search` retorna erro 500.** Quase sempre é o reranker fora
do ar — o `TeiRerankAdapter` não tem fallback. Cheque
`curl -s localhost:8081/health` e o valor de `TEI_URL` no `.env`.

**A UI em `/` abre vazia.** O banco não tem sinais ainda. Rode o passo 7
(ingestão + extração); confirme com
`curl -s localhost:8080/api/evaluation/signals`.

**Porta 8080 já em uso.** Uma instância anterior ficou rodando. Descubra e
finalize com `lsof -ti tcp:8080 | xargs kill`.

**A aplicação nem sobe: `Providers em extraction.providers sem credencial`.**
As chaves de fronteira não chegaram ao processo — quase sempre porque a
aplicação foi subida com `mvn spring-boot:run` direto, fora do `make`, que é
quem exporta o `.env`. O `AiConfig` faz um *readiness check* no boot e falha
de propósito: melhor não subir do que rodar o experimento com um braço
silenciosamente ausente. Use `make run` (passo 1) ou exporte você mesmo:
`set -a; source .env; set +a; mvn -pl app spring-boot:run`.

**`POST /api/market-data/sync/...` responde 500 com `INVALID_RANGE`.** O plano
gratuito do brapi.dev só libera os ranges `1d`, `5d`, `1mo` e `3mo` — o
default `1y` do endpoint exige o plano pago. Passe `?range=3mo`.

**O card da empresa mostra "Sem código B3 associado a este CNPJ".** O CNPJ não
está em `mapa_ticker`. Acrescente com
`PUT /api/panel/empresas/{cnpj}/ticker` e depois sincronize a série.

**A interface abre com o layout antigo (HTML puro).** O bundle React não foi
instalado em `static/`. Rode `make web` e reinicie a aplicação — o
`spring-boot:run` serve de `target/classes`, então o build precisa acontecer
antes do boot. (O `ops/web/install.sh` já copia para os dois lugares e limpa
o destino antes, para não acumular bundles antigos.)

**Mudei a interface e o navegador insiste na versão anterior.**
`StaticResourcesConfig` marca `index.html` e `/data/` como `no-cache` e os
bundles com hash como imutáveis, então isso não deve mais acontecer em
navegador limpo. Se acontecer, é cache anterior a essa configuração: recarga
forçada (⇧⌘R) resolve.
