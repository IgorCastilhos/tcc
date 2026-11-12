# ADR-0004 — Read model do painel e mapa CNPJ → código B3

## Status
Aceito.

## Contexto
A interface do estudo (Seção 4.5) precisa mostrar, numa linha só: a
companhia, a ação, a cotação e a leitura dos três LLMs sobre o mesmo
documento. Esses dados nascem em quatro contextos distintos — `ingestion`
(empresa e documento), `signal-extraction` (sinal), `market-data`
(cotação) — e o endpoint existente, `GET /api/evaluation/signals`, devolve
uma lista plana de sinais, sem ticker e sem preço.

Havia ainda uma lacuna de dados: os datasets da CVM não trazem o código de
negociação. `DiscoverDocumentsService` grava `empresa.ticker` como `NULL`, e
`serie_preco` é indexada por ticker. Sem uma ponte entre os dois, não existe
como pôr o preço ao lado do sinal.

## Decisão
1. **Um read model dedicado**, `GET /api/panel`, servido por
   `PanelViewPort` / `JdbcPanelAdapter`, no módulo `evaluation` — o mesmo que
   já fazia leitura cruzada para a exportação RAGAS. Duas consultas planas
   (sinais e cotações) agregadas em memória; o volume é o de um estudo com
   poucas dezenas de documentos, e agregar em Java sai mais legível do que
   construir o aninhamento em SQL.
2. O payload é **autocontido** — sinais, cotações, origem dos documentos e o
   Quadro 4 juntos —, para que o mesmo JSON sirva à aplicação ao vivo e ao
   snapshot estático publicado aos participantes (ADR-0005).
3. **Tabela de referência `mapa_ticker`** (migration V2) com uma semente
   curada de 41 companhias da B3, marcada com `origem = 'SEED'`, e correção
   manual via `PUT /api/panel/empresas/{cnpj}/ticker`, que grava
   `origem = 'MANUAL'`.
4. O painel resolve o ticker com `COALESCE(mapa_ticker.ticker,
   empresa.ticker)`: o mapa é a fonte de verdade. Sem isso, companhias
   ingeridas depois da migration entrariam com ticker nulo e apareceriam sem
   código B3 e sem cotação.

## Consequências
- A leitura cruza contextos, o que um purista de DDD evitaria. É uma
  concessão consciente ao formato de monólito modular do ADR-0001: uma
  projeção somente-leitura, isolada num adaptador, sem escrita e sem regra de
  negócio.
- A semente do mapa é curada à mão e **precisa ser conferida**: um ticker
  errado associa a cotação de outra companhia aos sinais exibidos. Cada linha
  guarda `nome_referencia` justamente para permitir a auditoria contra o nome
  vindo da CVM.
- A validação empírica dos 41 códigos contra a API de cotações encontrou três
  sem preço: `CPLE6` era erro de mapeamento (a Copel migrou ao Novo Mercado e
  as preferenciais viraram ordinárias — corrigido para `CPLE3` na migration
  V3), enquanto `AZUL4` e `GOLL4` são companhias que seguem registradas na
  CVM mas cujos papéis não têm cotação disponível. A interface exibe os sinais
  dessas empresas sem preço.
