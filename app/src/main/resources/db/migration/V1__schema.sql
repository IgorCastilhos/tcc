-- Esquema inicial do pipeline (Figura 2 da monografia).
-- Seis entidades: Empresa, Documento, Chunk, Sinal, SeriePreco, AvaliacaoUsuario.

CREATE EXTENSION IF NOT EXISTS vector;

CREATE TABLE empresa
(
    id         UUID PRIMARY KEY,
    cnpj       VARCHAR(14)  NOT NULL UNIQUE,
    codigo_cvm VARCHAR(20),
    nome       VARCHAR(255) NOT NULL,
    ticker     VARCHAR(10)
);

CREATE TABLE documento
(
    id              UUID PRIMARY KEY,
    empresa_id      UUID        NOT NULL REFERENCES empresa (id),
    tipo            VARCHAR(20) NOT NULL,        -- DFP | ITR | FATO_RELEVANTE
    data_referencia DATE        NOT NULL,
    url_origem      TEXT        NOT NULL UNIQUE, -- idempotência da descoberta
    protocolo       VARCHAR(40),
    status          VARCHAR(20) NOT NULL,        -- DISCOVERED | PARSED | FAILED
    texto           TEXT,
    criado_em       TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_documento_empresa ON documento (empresa_id);
CREATE INDEX idx_documento_tipo_data ON documento (tipo, data_referencia);

CREATE TABLE chunk
(
    id           UUID PRIMARY KEY,
    documento_id UUID         NOT NULL REFERENCES documento (id) ON DELETE CASCADE,
    seq          INT          NOT NULL,
    conteudo     TEXT         NOT NULL,
    embedding    vector(1024) NOT NULL,              -- BGE-M3: 1024 dimensões
    metadata     JSONB        NOT NULL DEFAULT '{}', -- cnpj, tipo, dataReferencia
    content_tsv  tsvector GENERATED ALWAYS AS (to_tsvector('portuguese', conteudo)) STORED,
    UNIQUE (documento_id, seq)
);

-- Busca densa: HNSW sobre cosseno (operador <=>).
CREATE INDEX idx_chunk_embedding_hnsw ON chunk USING hnsw (embedding vector_cosine_ops);
-- Busca esparsa: full-text search em português.
CREATE INDEX idx_chunk_content_tsv ON chunk USING gin (content_tsv);
-- Filtros combinados por metadados (empresa, período, tipo).
CREATE INDEX idx_chunk_metadata ON chunk USING gin (metadata);

CREATE TABLE sinal
(
    id            UUID PRIMARY KEY,
    documento_id  UUID             NOT NULL REFERENCES documento (id) ON DELETE CASCADE,
    modelo        VARCHAR(30)      NOT NULL, -- CLAUDE_SONNET | GPT_4O | GEMMA
    tipo          VARCHAR(30)      NOT NULL, -- SENTIMENTO | RISCO | EVENTO_CORPORATIVO
    rotulo        VARCHAR(60)      NOT NULL,
    confianca     DOUBLE PRECISION NOT NULL,
    justificativa TEXT,
    evidencia     TEXT,
    contextos     JSONB,                     -- trechos usados (insumo do RAGAS)
    criado_em     TIMESTAMPTZ      NOT NULL
);

CREATE INDEX idx_sinal_documento ON sinal (documento_id);
CREATE INDEX idx_sinal_modelo_tipo ON sinal (modelo, tipo);

CREATE TABLE serie_preco
(
    id         BIGSERIAL PRIMARY KEY,
    ticker     VARCHAR(10)    NOT NULL,
    data       DATE           NOT NULL,
    fechamento NUMERIC(14, 4) NOT NULL,
    volume     BIGINT         NOT NULL DEFAULT 0,
    UNIQUE (ticker, data)
);

CREATE TABLE avaliacao_usuario
(
    id           UUID PRIMARY KEY,
    participante VARCHAR(40) NOT NULL, -- código pseudonimizado (TCLE / CNS 510/2016)
    semana       INT         NOT NULL CHECK (semana BETWEEN 1 AND 3),
    respostas    JSONB       NOT NULL, -- {"1":4, ..., "10":5} — Quadro 4
    comentario   TEXT,
    criado_em    TIMESTAMPTZ NOT NULL
);
