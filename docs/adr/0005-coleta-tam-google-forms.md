# ADR-0005 — Coleta do instrumento TAM no Google Forms

## Status
Aceito. Substitui a coleta embutida na aplicação.

## Contexto
A Seção 4.5 prevê que os participantes acessem a interface ao menos uma vez
por semana, durante três semanas, e respondam ao fim de cada semana o
instrumento TAM do Quadro 4.

A primeira implementação embutia o questionário na própria aplicação:
`POST /api/evaluation` gravava as respostas em `avaliacao_usuario`, e
`GET /api/evaluation/report` consolidava as médias por dimensão.

Isso conflita com a forma de distribuição escolhida. O site é publicado como
**build estático com dados congelados** — sem Postgres, sem chaves de API,
sem custo de inferência por acesso — justamente para que os participantes o
acessem de qualquer lugar sem depender da máquina do autor estar ligada.
Nessa configuração não existe backend para receber o POST. As alternativas
eram expor uma instância da aplicação na internet (com o banco e as chaves
que a publicação estática evita) ou pedir ao participante que salvasse um
arquivo JSON e o encaminhasse — frágil e com atrito.

## Decisão
Aplicar o instrumento por um formulário do Google, embutido na aba
Questionário via `<iframe>` (`web/src/components/GoogleForm.tsx`). As
respostas vão direto para a planilha do formulário.

## Consequências
- A coleta passa a funcionar independentemente de a aplicação estar no ar,
  que era o requisito da publicação estática.
- `POST /api/evaluation`, a tabela `avaliacao_usuario` e
  `GET /api/evaluation/report` continuam existindo e funcionando, mas a
  interface não os alimenta mais. **A consolidação das respostas passa a ser
  feita sobre a planilha do Google**, não pelo endpoint de relatório — a
  Seção 4.5 da monografia precisa refletir isso.
- O questionário embutido está preservado em
  `web/legacy/Questionnaire.tsx`; voltar atrás é reconectar o componente.
- Dependência de um serviço de terceiros para um instrumento de pesquisa. O
  embed traz `width=640` fixo, neutralizado por CSS, e há link para abrir o
  formulário em nova aba caso o navegador do participante bloqueie iframes.
