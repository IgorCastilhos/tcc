# Fluxo de trabalho do pipeline. Requer: Docker, JDK 21, Maven, Ollama (host).
SHELL := /bin/bash
ENV_FILE := .env
-include $(ENV_FILE)
export

.PHONY: infra infra-linux infra-no-tei tei-native down models build test run \
        web web-deps web-dev web-snapshot web-publish coletar ragas-dataset ragas clean

## Sobe Postgres+pgvector, Kafka, TEI (reranker), Prometheus e Grafana.
infra:
	docker compose up -d postgres kafka tei-reranker prometheus grafana

## Igual, incluindo Ollama containerizado (para Linux; no macOS use o app nativo).
infra-linux:
	docker compose --profile linux up -d

## Infra sem o reranker — use junto com 'make tei-native'.
infra-no-tei:
	docker compose up -d postgres kafka prometheus grafana

## TEI nativo no macOS com aceleração Metal — mesma API /rerank, sem contêiner.
## Requer: brew install text-embeddings-inference
tei-native:
	text-embeddings-router --model-id BAAI/bge-reranker-v2-m3 --port 8081

down:
	docker compose down

## Baixa BGE-M3 e o modelo Gemma no Ollama do host.
models:
	./ops/ollama/pull-models.sh

build:
	mvn -q clean verify

test:
	mvn -q test

## Sobe a aplicação (composition root) no host, já com a interface React.
run: web
	mvn -q -DskipTests -pl app -am install
	mvn -q -pl app spring-boot:run

## ---------------------------------------------------------------------------
## Interface React (web/). O bundle é instalado em app/src/main/resources/static,
## de onde o Spring o serve na raiz.
## ---------------------------------------------------------------------------

web-deps:
	cd web && npm install

## Compila a interface e instala em static/. É dependência de 'make run'.
web: web-deps
	cd web && npm run build
	./ops/web/install.sh

## Servidor de desenvolvimento com hot reload (porta 5173, /api encaminhado
## para o Spring em 8080). Requer 'make run' num outro terminal.
web-dev: web-deps
	cd web && npm run dev

## Amplia o painel: descobre Fatos Relevantes de várias companhias da B3,
## recupera downloads recusados pelo ENET e sincroniza cotações.
## Não extrai sinais (chamadas pagas de LLM ficam a seu critério).
coletar:
	./ops/web/coletar.sh

## Congela o painel atual em web/public/data/panel.json (app precisa estar no ar).
web-snapshot:
	./ops/web/snapshot.sh

## Build para publicar aos participantes: dados congelados, sem backend.
## O resultado em web/dist é o que sobe para a Vercel/Render.
web-publish: web-deps web-snapshot
	cd web && VITE_SNAPSHOT=1 npm run build
	@echo "pronto: web/dist — publique este diretório"

## Exporta o dataset RAGAS a partir dos sinais persistidos.
ragas-dataset:
	curl -sf http://localhost:8080/api/evaluation/ragas-dataset -o ragas/data/dataset.jsonl
	@wc -l ragas/data/dataset.jsonl

## Roda a avaliação RAGAS no container Python (usa OPENAI_API_KEY como juiz).
ragas: ragas-dataset
	docker compose --profile eval run --rm ragas /data/dataset.jsonl /data/report

clean:
	mvn -q clean
	rm -rf web/dist app/src/main/resources/static
