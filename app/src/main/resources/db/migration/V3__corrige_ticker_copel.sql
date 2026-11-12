-- Correção da semente de V2__mapa_ticker_b3.sql.
--
-- A semente foi validada ticker a ticker contra a API de cotações: 38 dos 41
-- códigos respondem com preço. Os três que não respondem:
--
--   CPLE6  erro de mapeamento — a Copel migrou ao Novo Mercado e as ações
--          preferenciais foram convertidas em ordinárias. O código vigente
--          é CPLE3. Corrigido abaixo.
--   AZUL4  a companhia segue registrada na CVM e publica documentos, mas o
--          papel não tem cotação disponível. Mantido: o vínculo CNPJ →
--          companhia continua correto e o painel exibe os sinais sem preço.
--   GOLL4  mesma situação da AZUL4.
--
-- A alteração vai numa migration nova, e não na V2, porque o Flyway valida
-- o checksum das migrations já aplicadas.

UPDATE mapa_ticker
SET ticker = 'CPLE3',
    atualizado_em = now()
WHERE cnpj = '76483817000120'
  AND ticker = 'CPLE6';

UPDATE empresa
SET ticker = 'CPLE3'
WHERE cnpj = '76483817000120'
  AND ticker IS DISTINCT FROM 'CPLE3';

-- A série gravada sob o código antigo, se existir, não serve para nada.
DELETE FROM serie_preco WHERE ticker = 'CPLE6';
