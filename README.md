# Feature Flags API

## CI/CD com GitHub Actions

O workflow [ci-cd.yml](.github/workflows/ci-cd.yml) compila e executa os testes com Java 21, publica o JAR e simula um deploy em dev com health check. Veja o [roteiro da demonstração](.github/CI-CD.md) para executar e apresentar o pipeline.

Java 21 e Spring Boot. Execute `./mvnw.cmd spring-boot:run` e teste com `./mvnw.cmd test`.

## Contrato para o frontend

Base URL: `http://localhost:8080/api/v1`. Corpos e respostas usam JSON. CORS permite `http://localhost:3000` e `http://localhost:5173`; configure outros via `app.cors.allowed-origins` (valores separados por vírgula).

| Método | Rota | Resposta |
|---|---|---|
| GET | /flags | 200, array de flags ordenado pela chave |
| GET | /flags/{key} | 200, flag; 404 se inexistente |
| POST | /flags | 201, flag e cabeçalho Location; 409 se chave duplicada |
| PUT | /flags/{key} | 200, flag atualizada; 404 se inexistente |
| PATCH | /flags/{key}/status | 200, flag atualizada; 404 se inexistente |
| DELETE | /flags/{key} | 204 sem corpo; 404 se inexistente |


POST /flags:
```json
{"key":"welcome-banner","name":"Banner de boas-vindas","description":"Mensagem de exemplo","enabled":false}
```
A chave é imutável: 1 a 64 caracteres, começa com letra minúscula e aceita letras minúsculas, números e hífen. Nome obrigatório (até 100 caracteres), descrição opcional (até 500) e enabled boolean obrigatório.

PUT /flags/{key}: envie `name`, `description` e `enabled`, com as mesmas regras do cadastro. Substitui esses campos; descrição omitida vira null.

PATCH /flags/{key}/status:
```json
{"enabled":true}
```

Objeto de flag retornado por cadastro, consulta e alteração:
```json
{"key":"welcome-banner","name":"Banner de boas-vindas","description":"Mensagem de exemplo","enabled":true,"createdAt":"2026-11-09T12:00:00Z","updatedAt":"2026-11-09T12:01:00Z"}
```
Datas são ISO 8601 UTC. A listagem retorna um array desses objetos (sem paginação).

## Consumo das flags

A API gerencia apenas flags. O frontend consulta GET /flags e usa enabled em condicionais para exibir seus componentes. GET /demo/home foi removido. Consulte PROMPT-FRONTEND-CODEX.md para a migração.

Erros usam Problem Details (application/problem+json), com status, title e detail. Validação retorna 400 e uma lista `errors` com `field` e `message`. JSON malformado também retorna 400. Falha na persistência retorna 500 sem expor detalhes internos.

## Persistência e operação

Dados ficam em `./data/flags.bin` e sobrevivem a reinicializações. Altere com `flags.storage-path`. Use apenas uma instância por arquivo; este armazenamento local atende a demonstração, sem banco externo. Não edite ou substitua o arquivo por conteúdo não confiável. A aplicação falha ao iniciar se o arquivo existente estiver inválido.

API sem autenticação, adequada para desenvolvimento local. Antes de exposição pública, implemente autenticação/autorização e armazenamento compartilhado. Health check: GET /actuator/health.

Instalações novas incluem welcome-banner e special-offers desativadas como exemplos. Dados existentes são preservados; cadastre as chaves ausentes pelo CRUD. A API não associa comportamento às chaves. O consumidor implementa essa associação.
