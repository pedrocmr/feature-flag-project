# Demonstração de CI/CD

O workflow `ci-cd.yml` tem dois jobs em sequência:

```text
push / pull request / execução manual
                 |
          build-test (Java 21)
          clean verify -> testes -> JAR
                 |
          deploy-dev (exceto pull requests)
          baixar JAR -> iniciar -> health check -> encerrar
```

## Como apresentar

1. Envie este projeto para um repositório GitHub com Actions habilitado. O workflow deve estar na branch padrão para aparecer a opção de execução manual.
2. Faça um push em `main`, `master` ou `develop`, ou abra **Actions > CI/CD - Build, testes e dev > Run workflow**.
3. Abra o job **Build e testes**: ele configura Java 21, usa o cache das dependências e executa `bash ./mvnw --batch-mode --no-transfer-progress clean verify`. O Maven compila, executa os testes existentes e gera o JAR executável. Uma falha interrompe o fluxo e impede o deploy.
4. Mostre os artefatos `test-reports` e `application` na execução. Os relatórios também são publicados em caso de falha, quando disponíveis.
5. Abra **Deploy simulado em dev**. `needs: build-test` garante que ele só começa após sucesso no CI. O job baixa o JAR produzido, sem recompilar, registra seu checksum e inicia a API temporariamente no runner.
6. Mostre a consulta a `/actuator/health`, que precisa retornar `status: UP`, o resumo da execução e o artefato `deploy-dev-evidence` com logs, checksum e resposta do health check.
7. Abra um pull request para uma das branches configuradas: somente o build e os testes executam; o deploy aparece como ignorado.

## Ambiente dev

O job declara o GitHub Environment `dev`. Ele não precisa de secrets, conta de nuvem ou servidor. Em **Settings > Environments > dev**, você pode configurar revisores obrigatórios, se disponíveis no seu plano, para demonstrar uma aprovação antes do deploy. Sem essa regra, o job continua automaticamente.

Esta simulação inicia uma aplicação real, mas somente dentro do runner temporário. O processo é encerrado ao final do passo e os dados são descartados ao final do job. Não existe URL pública nem ambiente persistente. O workflow não envia o JAR para um servidor externo.

Os artefatos ficam disponíveis por sete dias. Os jobs têm limites de duração e os deploys em dev são serializados para evitar execuções simultâneas nesse ambiente.

## Verificação local

Execute `./mvnw.cmd clean verify` no Windows ou `bash ./mvnw clean verify` no Linux. Isso executa a mesma fase Maven do CI. O build usa os testes existentes; atualmente a suíte verifica a inicialização do contexto Spring. O health check do deploy adiciona a verificação do JAR empacotado em execução.

Referência: [GitHub Actions com Java e Maven](https://docs.github.com/en/actions/tutorials/build-and-test-code/java-with-maven).
