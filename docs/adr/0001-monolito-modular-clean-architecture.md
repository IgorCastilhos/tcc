# ADR-0001 — Monólito modular Maven com Clean Architecture

## Status
Aceito.

## Contexto
A monografia (Seção 4.2) define seis bounded contexts — Ingestion, Indexing,
Retrieval, SignalExtraction, MarketData e Evaluation — com dependências
apontando sempre para o domínio, livre de frameworks. O artefato é executado
por um único pesquisador em hardware local (MacBook M3, 18 GB).

## Decisão
Um módulo Maven por bounded context, mais `shared-kernel` (eventos de
integração e tópicos Kafka) e `app` (composition root Spring Boot). Dentro de
cada módulo, três pacotes: `domain` (JDK puro), `application` (portas e casos
de uso, sem Spring) e `infrastructure` (adapters). Os casos de uso são POJOs
instanciados em `UseCasesConfig`; o `ArchitectureTest` (ArchUnit) falha o
build se domínio ou aplicação importarem Spring, JPA, Kafka, Tika ou Jackson.

## Consequências
+ Fronteiras de contexto verificadas em tempo de build, não por convenção.
+ Deploy e operação de um único processo — adequado à escala do experimento.
+ Kafka desacopla as etapas sem exigir microsserviços.
− Disciplina extra de wiring manual dos casos de uso no módulo app.
