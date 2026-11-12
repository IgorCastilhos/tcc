# ADR-0002 — Esquema próprio no pgvector em vez do PgVectorStore do Spring AI

## Status
Aceito.

## Contexto
O Spring AI oferece `PgVectorStore` com tabela genérica própria
(`vector_store`). A monografia (Seção 4.3, Figura 2), porém, define um modelo
de dados explícito: `chunk` com `vector(1024)`, `metadata JSONB`, índice HNSW
e vínculo forte com `documento`/`empresa`; a busca esparsa exige uma coluna
`tsvector` gerada sobre o conteúdo — inexistente no esquema genérico.

## Decisão
Persistência de chunks via JDBC (`PgVectorChunkRepository`) sobre o esquema
versionado por Flyway (V1__schema.sql), com casts explícitos `::vector` e
`::jsonb`. O Spring AI continua sendo a abstração de modelos (EmbeddingModel
e ChatClient); apenas o armazenamento é próprio.

## Consequências
+ O banco reflete 1:1 a Figura 2 da monografia — rastreabilidade acadêmica.
+ Coluna `content_tsv` gerada habilita o braço esparso (FTS 'portuguese').
+ Filtros combinados (cnpj, tipo, período, documento) em SQL direto.
− Abrimos mão da API `VectorStore`; o custo é um repositório JDBC pequeno e
  testado por integração real (Testcontainers).
