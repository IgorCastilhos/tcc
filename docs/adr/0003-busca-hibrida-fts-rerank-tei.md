# ADR-0003 — Busca híbrida: pgvector + FTS 'portuguese' + rerank via TEI

## Status
Aceito.

## Contexto
Chen et al. (2024) recomendam, para o BGE-M3, recuperação híbrida
(densa + esparsa) seguida de reranking. O Ollama expõe embeddings densos do
BGE-M3, mas não expõe os pesos esparsos (lexical weights) do modelo nem uma
API de reranking cross-encoder para o BGE-reranker-v2-m3.

## Decisão
1. Braço denso: cosseno no pgvector (`<=>`) com índice HNSW.
2. Braço esparso: full-text search nativo do PostgreSQL com dicionário
   'portuguese' (`websearch_to_tsquery` + `ts_rank_cd`) — recuperação lexical
   com stemming, cumprindo o papel do componente esparso sem infraestrutura
   adicional.
3. Fusão: Reciprocal Rank Fusion (k=60), imune à diferença de escala entre
   cosseno e ts_rank.
4. Reranking: BGE-reranker-v2-m3 servido pelo HuggingFace Text Embeddings
   Inference (endpoint `/rerank`), mesmo modelo da família BGE definido na
   monografia — apenas o servidor difere, pela limitação do Ollama.

## Consequências
+ Estratégia recomendada pela literatura do BGE-M3, 100% local e sem custo
  por token.
+ Trocar o braço esparso por outra implementação (ex.: SPLADE) exige apenas
  um novo adapter de `SparseSearchPort`.
− Mais um container (TEI) no compose; mitigado por imagem oficial e cache de
  modelo em volume.

## Nota de implementação — arquitetura do host (ARM64)

A HuggingFace publica manifestos Docker distintos por arquitetura para o TEI:
`cpu-1.9` (x86_64) e `cpu-arm64-1.9` (aarch64). O ambiente de desenvolvimento
deste trabalho é um Apple Silicon (M3), de modo que a imagem aarch64 é o
default do `docker-compose.yml`, parametrizada por `TEI_IMAGE` para permitir
execução em hosts x86_64 sem alteração de código.

O Docker no macOS não expõe a GPU ao contêiner: dentro do contêiner a
inferência do reranker é CPU-bound. Como alternativa, o TEI oferece binário
nativo via Homebrew com aceleração Metal, servindo a mesma API `/rerank` na
mesma porta — decisão simétrica à adotada para o Ollama (nativo no macOS,
contêiner sob o profile `linux`). A troca é de infraestrutura apenas: o
adapter `TeiRerankAdapter` e o restante do código permanecem inalterados.

Consequência para a reprodutibilidade: o `docker-compose.yml` segue como
caminho canônico e portátil (um comando, qualquer arquitetura); o binário
nativo é otimização opcional de desempenho no ambiente do autor.
