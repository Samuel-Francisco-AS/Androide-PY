# P1 — Checklist operacional de implementação

**Branch:** feat/p1-script-library  
**Base:** main  
**Marco:** P1 — Biblioteca e gerenciamento de scripts  
**Estado atual:** implementação publicada; validação automatizada, build e validação física ainda pendentes.

Este documento é a fonte de verdade do progresso da P1. Publicação no GitHub não equivale a build, teste ou validação física.

## Regras de execução

Para cada subetapa, seguir obrigatoriamente:

1. **CONFERE:** reler este item, inspecionar código/documentos/dependências e definir verificação.
2. **IMPLEMENTA:** alterar somente o escopo selecionado e dependências necessárias.
3. **CONFERE:** revisar arquivos e diff; executar testes disponíveis; registrar limitações.
4. **MARCA:** atualizar este checklist somente com evidência.
5. **CONFERE:** reler o estado atualizado antes de avançar.

Estados: PENDENTE, EM ANDAMENTO, BLOQUEADO, IMPLEMENTADO — TESTE PENDENTE, TESTADO, VALIDADO FISICAMENTE.

## P1.0 — Preparação e contratos

- [x] Criar branch dedicada a partir de main. Evidência: feat/p1-script-library.
- [x] Criar este checklist operacional.
- [x] Criar o relatório operacional P1.
- [x] Revisar README, AGENTS, visão, arquitetura, roadmap, persistência, executor, interface, testes e módulos Python existentes.
- [x] Definir contrato de scripts, parâmetros, versões, execuções e dependências.
- [x] Definir estratégia Room versão 2 com fallback destrutivo durante o desenvolvimento, conforme ADR-0007.
- [x] Definir limites de importação: UTF-8, arquivo de texto, máximo de 1 MiB; não executar no ato da importação.
- [x] Definir limite do gerenciamento de dependências: manifesto declarativo e validação local; sem instalação PyPI genérica em runtime.

## P1.1 — Modelo de dados e persistência

**Estado:** IMPLEMENTADO — TESTES JVM EXECUTADOS; validação física pendente.

- [x] Expandir ScriptEntity com descrição, tags, parâmetros, dependências e metadados.
- [x] Criar entidade de versões imutáveis do script.
- [x] Criar entidade de execuções com parâmetros, status, horários, saída e erro.
- [x] Implementar DAOs e operações transacionais para gravação de script/versão e exclusão.
- [x] Remover migração Room 1 → 2 e configurar `fallbackToDestructiveMigration()`; atualizações incompatíveis podem descartar o banco local.
- [x] Escrever testes de repositório, histórico e persistência de execução.
- [x] Executar `:app:testDebugUnitTest`; resultado relatado pelo usuário: `BUILD SUCCESSFUL`.
- [x] Desabilitar exportação de schemas históricos Room nesta etapa (`exportSchema = false`), pois não há migração retrocompatível a manter.
- [ ] Validar instalação/atualização P1 e confirmar inicialização do banco versão 2 no Moto G06; dados locais anteriores podem ser descartados.

## P1.2 — Biblioteca de scripts

**Estado:** IMPLEMENTADO — TESTE PENDENTE.

- [x] Listar scripts persistidos e selecionar script.
- [x] Criar script.
- [x] Renomear e editar metadados.
- [x] Duplicar script com novo identificador e primeira versão.
- [x] Excluir script com confirmação; histórico de execuções é preservado.
- [x] Pesquisar por nome, descrição e tags.
- [x] Importar arquivo Python UTF-8 pelo Storage Access Framework com limite de 1 MiB.
- [x] Exportar código Python pelo Storage Access Framework.
- [ ] Validar operações CRUD e importação/exportação no aparelho.

## P1.3 — Editor e parâmetros

**Estado:** IMPLEMENTADO — TESTE PENDENTE.

- [x] Implementar editor com numeração de linhas.
- [x] Implementar realce lexical básico de Python sem WebView ou dependência de IDE.
- [x] Preservar conteúdo e seleção nativa do campo de edição.
- [x] Editar descrição, tags e parâmetros.
- [x] Validar nomes, tipos, obrigatoriedade, defaults e valores extras.
- [x] Resolver parâmetros antes da execução e passá-los ao runner como dicionário params.
- [x] Documentar que o realce lexical não é parser/IDE completo.
- [ ] Validar usabilidade, teclado, rolagem e desempenho no Moto G06.

## P1.4 — Execução e histórico

**Estado:** IMPLEMENTADO — TESTE PENDENTE.

- [x] Associar execução ao script e à versão de origem.
- [x] Persistir QUEUED/RUNNING/estado terminal com timestamps.
- [x] Persistir parâmetros, stdout, stderr, erro, duração e flags de truncamento.
- [x] Exibir console da execução atual.
- [x] Consultar histórico de execuções do script com saída e falhas.
- [x] Preservar registros de execução após exclusão do script.
- [ ] Executar testes JVM e validar histórico após fechar/reabrir o app.
- [ ] Testar fila cheia e falhas de persistência.

## P1.5 — Biblioteca Python mobile

**Estado:** IMPLEMENTADO — TESTE PENDENTE.

- [x] Versionar pacote e contrato público (mobile 0.2.0; API_VERSION 1).
- [x] Adicionar mobile.capabilities.list_available() e is_implemented().
- [x] Catalogar somente device.get_info e apps.launch como implementados.
- [x] Documentar módulos futuros como planejados, não disponíveis.
- [x] Escrever teste host Python do catálogo.
- [x] Executar testes host Python: 13/13 aprovados, conforme saída relatada pelo usuário.
- [ ] Confirmar execução no runtime Android P1.

## P1.6 — Dependências Python compatíveis

**Estado:** IMPLEMENTADO — TESTE PENDENTE, com limite explícito.

- [x] Persistir manifesto declarativo por script.
- [x] Validar nome de pacote e restrição simples de versão.
- [x] Exibir declarações no editor e explicar inclusão no build.
- [x] Documentar requisitos Chaquopy/Python 3.13/ABI arm64-v8a.
- [x] Não prometer instalação dinâmica de qualquer pacote PyPI.
- [ ] Executar testes de validação do manifesto.
- [ ] Validar pelo menos uma dependência real apenas se for escolhida e incluída explicitamente no build.

## P1.7 — Revisão, testes e gate

**Estado:** PENDENTE.

- [x] Revisar estaticamente os 35 arquivos do diff completo; build e testes continuam pendentes.
- [x] Executar testes Python host: 13/13 PASS, conforme saída relatada pelo usuário.
- [x] Executar `:app:testDebugUnitTest`: `BUILD SUCCESSFUL`, conforme saída relatada pelo usuário.
- [ ] Executar build debug localmente pelo usuário.
- [ ] Validar ciclo biblioteca → editor → salvar → executar com parâmetros → histórico no Moto G06.
- [ ] Validar importação/exportação no aparelho.
- [ ] Validar comportamento do banco após atualização incompatível no aparelho; não exigir preservação de dados P0.
- [ ] Registrar resultados físicos como relato do usuário, sem atribuir logs não fornecidos.
- [x] Atualizar README, docs/README, visão, roadmap, arquitetura, ADRs e contrato mobile.
- [x] Criar PR em draft para revisão/validação: PR #2 (https://github.com/Samuel-Francisco-AS/Androide-PY/pull/2).
- [ ] Marcar PR pronta para revisão após build e validação física do fallback.
- [ ] Fazer Squash Merge somente após revisão final e autorização explícita.

## Critérios de aceite P1

1. Scripts podem ser criados, importados, renomeados, duplicados, editados, exportados e excluídos.
2. Scripts e metadados persistem após encerrar/reabrir o aplicativo.
3. Editor apresenta numeração de linhas e realce lexical básico.
4. Parâmetros declarados podem ser fornecidos e recebidos pelo script como valores Python em params.
5. Cada execução fica associada ao script/versão e possui histórico consultável com saída e falhas.
6. Dependências são declaradas e validadas com limites claros de compatibilidade Chaquopy; nenhuma instalação genérica é prometida.
7. Biblioteca mobile possui contrato versionado e introspecção honesta das capacidades implementadas.
8. O banco versão 2 inicializa corretamente; atualizações incompatíveis podem recriar o banco conforme ADR-0007.
9. Testes automatizados passam e o ciclo principal é validado no Moto G06.

## Evidências

| Item | Evidência |
|---|---|
| Branch | feat/p1-script-library criada a partir de main |
| Base inicial | a68ea3bca2e5c8c8fa8fed0c7bba768acb21821f |
| Implementação | Commits incrementais publicados nesta branch |
| Testes host Python | Escritos; execução pendente |
| Testes JVM/Kotlin | Escritos; execução pendente |
| Build APK P1 | Pendente |
| Fallback destrutivo e validação física | Pendente |
| PR | #2 aberta em draft; aguarda validação |
| Merge | Não realizado; autorização específica P1 ainda não concedida |

## Bloqueios e desvios

- A compatibilidade real de dependências Python é específica por pacote, versão de CPython, ABI e distribuição Chaquopy; a P1 não executa instalação genérica de pacotes.
- A migração Room 1 → 2 foi removida conforme ADR-0007. `fallbackToDestructiveMigration()` permite recriar o banco quando a versão instalada não é compatível; scripts, versões e execuções locais podem ser descartados. A exportação de schemas históricos está desabilitada nesta etapa.
- O editor oferece realce lexical simples, sem autocomplete, lint, depuração ou análise semântica.
- Nenhum teste/build P1 deve ser marcado como aprovado antes de execução observável.


## Execução local após sincronização

Se ainda não existir uma branch local P1:

```bash
cd ~/Projetos/Androide-PY
git status --short
git fetch origin
git switch --track -c feat/p1-script-library origin/feat/p1-script-library
chmod +x android/gradlew
```

Se a branch local já existir:

```bash
cd ~/Projetos/Androide-PY
git status --short
git switch feat/p1-script-library
git pull --ff-only
chmod +x android/gradlew
```

Execute os testes Python host na raiz:

```bash
PYTHONPATH=android/app/src/main/python python3.13 -m unittest discover -s android/app/src/test/python -v
```

Execute testes JVM e build com recursos limitados:

```bash
cd android
./gradlew :app:testDebugUnitTest --no-daemon --max-workers=2
./gradlew :app:assembleDebug --no-daemon --max-workers=2
```

APK esperado: android/app/build/outputs/apk/debug/app-debug.apk. Transfira ao Moto G06 pelo fluxo já utilizado no My-CT; não é necessário ADB.

**Fallback destrutivo:** instalar o APK P1 como atualização sobre a instalação P0 pode acionar `fallbackToDestructiveMigration()` e apagar o banco local antigo. Não é necessário desinstalar o app nem limpar dados manualmente; considerar os scripts e registros locais descartáveis neste teste.

Na validação física, testar: abertura da biblioteca, CRUD, importação de .py UTF-8, exportação, editor/linhas/realce, parâmetros via params, versões/restauração, histórico após reabrir e inicialização do banco versão 2 após o fallback destrutivo (dados P0 podem ser descartados).
