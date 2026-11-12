-- Ligação empresa (CNPJ da CVM) → código de negociação na B3.
--
-- Os datasets da CVM não trazem o ticker: DiscoverDocumentsService grava
-- empresa.ticker como NULL. Sem essa ponte não há como exibir a cotação ao
-- lado dos sinais, porque serie_preco é indexada por ticker.
--
-- O mapeamento abaixo é uma SEMENTE curada das companhias mais líquidas da
-- B3, marcada com origem='SEED'. Confira antes de rodar o estudo com
-- participantes: um ticker errado associa a cotação de outra companhia aos
-- sinais exibidos. Correções entram via
-- PUT /api/panel/empresas/{cnpj}/ticker, que grava origem='MANUAL'.

CREATE TABLE mapa_ticker
(
    cnpj            VARCHAR(14) PRIMARY KEY,
    ticker          VARCHAR(10) NOT NULL,
    nome_referencia VARCHAR(255) NOT NULL, -- nome usado ao montar a semente, para auditoria
    origem          VARCHAR(10) NOT NULL DEFAULT 'SEED' CHECK (origem IN ('SEED', 'MANUAL')),
    atualizado_em   TIMESTAMPTZ NOT NULL DEFAULT now()
);

INSERT INTO mapa_ticker (cnpj, ticker, nome_referencia) VALUES
    ('33000167000101', 'PETR4',  'PETROLEO BRASILEIRO S.A. PETROBRAS'),
    ('33592510000154', 'VALE3',  'VALE S.A.'),
    ('60872504000123', 'ITUB4',  'ITAU UNIBANCO HOLDING S.A.'),
    ('00000000000191', 'BBAS3',  'BANCO DO BRASIL S.A.'),
    ('60746948000112', 'BBDC4',  'BANCO BRADESCO S.A.'),
    ('90400888000142', 'SANB11', 'BANCO SANTANDER (BRASIL) S.A.'),
    ('07526557000100', 'ABEV3',  'AMBEV S.A.'),
    ('09346601000125', 'B3SA3',  'B3 S.A. - BRASIL, BOLSA, BALCAO'),
    ('84429695000111', 'WEGE3',  'WEG S.A.'),
    ('16404287000155', 'SUZB3',  'SUZANO S.A.'),
    ('02916265000160', 'JBSS3',  'JBS S.A.'),
    ('33611500000119', 'GGBR4',  'GERDAU S.A.'),
    ('33042730000104', 'CSNA3',  'COMPANHIA SIDERURGICA NACIONAL'),
    ('60894730000105', 'USIM5',  'USINAS SIDERURGICAS DE MINAS GERAIS S.A.'),
    ('42150391000170', 'BRKM5',  'BRASKEM S.A.'),
    ('00001180000126', 'ELET3',  'CENTRAIS ELETRICAS BRASILEIRAS S.A.'),
    ('17155730000164', 'CMIG4',  'CIA ENERGETICA DE MINAS GERAIS - CEMIG'),
    ('76483817000120', 'CPLE6',  'COPEL - CIA PARANAENSE DE ENERGIA'),
    ('43776517000180', 'SBSP3',  'CIA SANEAMENTO BASICO EST SAO PAULO - SABESP'),
    ('03220438000173', 'EQTL3',  'EQUATORIAL ENERGIA S.A.'),
    ('02558157000162', 'VIVT3',  'TELEFONICA BRASIL S.A.'),
    ('02421421000111', 'TIMS3',  'TIM S.A.'),
    ('92754738000162', 'LREN3',  'LOJAS RENNER S.A.'),
    ('47960950000121', 'MGLU3',  'MAGAZINE LUIZA S.A.'),
    ('16670085000155', 'RENT3',  'LOCALIZA RENT A CAR S.A.'),
    ('61585865000151', 'RADL3',  'RAIA DROGASIL S.A.'),
    ('71673990000177', 'NTCO3',  'NATURA &CO HOLDING S.A.'),
    ('89637490000145', 'KLBN11', 'KLABIN S.A.'),
    ('07689002000189', 'EMBR3',  'EMBRAER S.A.'),
    ('01838723000127', 'BRFS3',  'BRF S.A.'),
    ('03853896000140', 'MRFG3',  'MARFRIG GLOBAL FOODS S.A.'),
    ('50746577000115', 'CSAN3',  'COSAN S.A.'),
    ('02387241000160', 'RAIL3',  'RUMO S.A.'),
    ('02846056000197', 'CCRO3',  'CCR S.A.'),
    ('06047087000139', 'RDOR3',  'REDE DOR SAO LUIZ S.A.'),
    ('05197443000138', 'HAPV3',  'HAPVIDA PARTICIPACOES E INVESTIMENTOS S.A.'),
    ('06057223000171', 'ASAI3',  'SENDAS DISTRIBUIDORA S.A. (ASSAI)'),
    ('47508411000156', 'PCAR3',  'CIA BRASILEIRA DE DISTRIBUICAO (GPA)'),
    ('09305994000129', 'AZUL4',  'AZUL S.A.'),
    ('06164253000187', 'GOLL4',  'GOL LINHAS AEREAS INTELIGENTES S.A.'),
    ('00000208000100', 'BSLI4',  'BRB - BANCO DE BRASILIA S.A.');

-- Preenche o ticker das empresas já ingeridas.
UPDATE empresa e
SET ticker = m.ticker
FROM mapa_ticker m
WHERE m.cnpj = e.cnpj
  AND e.ticker IS DISTINCT FROM m.ticker;

CREATE INDEX idx_serie_preco_ticker_data ON serie_preco (ticker, data DESC);
