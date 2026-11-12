# Interface estática anterior

`index-estatico.html` é a primeira versão da interface do estudo — HTML e
JavaScript puros, servida em `/` antes da interface React em `web/`.

Está guardada aqui apenas como referência histórica do artefato. Não é
copiada para `app/src/main/resources/static/` nem servida pela aplicação:
quem ocupa `/` agora é o build de `web/`, instalado por `make web`.

## `Questionnaire.tsx`

Questionário TAM que rodava dentro da aplicação: as dez afirmações do
Quadro 4 em escala Likert, enviadas para `POST /api/evaluation` e gravadas
na tabela `avaliacao_usuario`.

Foi substituído pelo formulário do Google embutido em
`web/src/components/GoogleForm.tsx`. O componente segue aqui, junto com o
endpoint e a tabela que continuam existindo no backend, caso você queira
voltar a coletar as respostas dentro da própria aplicação.
