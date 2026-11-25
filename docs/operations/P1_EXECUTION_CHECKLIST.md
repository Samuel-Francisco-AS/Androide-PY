# P1 — Checklist operacional de implementação

**Branch:** feat/p1-script-library  
**Base:** main  
**Marco:** P1 — Biblioteca e gerenciamento de scripts  
**Estado atual (25/11/2026):** testes host Python 13/13 PASS; testes JVM :app:testDebugUnitTest BUILD SUCCESSFUL; build debug :app:assembleDebug concluído e APK (~32 MB) transferido/instalado no Moto G06. Validação física em andamento: P1-TF01 a P1-TF11 concluídos conforme relatos do usuário; P1-TF12 em diante pendentes.

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

**Estado:** IMPLEMENTADO — TESTES JVM EXECUTADOS; inicialização do app/biblioteca no Moto G06 confirmada. Comportamento específico do fallback destrutivo ainda não isolado como teste.

- [x] Expandir ScriptEntity com descrição, tags, parâmetros, dependências e metadados.
- [x] Criar entidade de versões imutáveis do script.
- [x] Criar entidade de execuções com parâmetros, status, horários, saída e erro.
- [x] Implementar DAOs e operações transacionais para gravação de script/versão e exclusão.
- [x] Remover migração Room 1 → 2 e configurar `fallbackToDestructiveMigration()`; atualizações incompatíveis podem descartar o banco local.
- [x] Escrever testes de repositório, histórico e persistência de execução.
- [x] Executar `:app:testDebugUnitTest`; resultado relatado pelo usuário: `BUILD SUCCESSFUL`.
- [x] Desabilitar exportação de schemas históricos Room nesta etapa (`exportSchema = false`), pois não há migração retrocompatível a manter.
- [x] Confirmar que o APK P1 inicia e carrega a biblioteca no Moto G06; a inicialização do banco em uso foi observada pelo funcionamento da biblioteca.
- [ ] Isolar/confirmar o comportamento de fallback destrutivo numa atualização incompatível; não presumir quais dados foram descartados sem observação específica.

## P1.2 — Biblioteca de scripts

**Estado:** IMPLEMENTADO — VALIDAÇÃO FÍSICA PARCIAL (biblioteca, criação, persistência e duplicação aprovadas; importação/exportação, busca, renomeação e exclusão pendentes).

- [x] Listar scripts persistidos e selecionar script.
- [x] Criar script.
- [x] Renomear e editar metadados.
- [x] Duplicar script com novo identificador e primeira versão.
- [x] Excluir script com confirmação; histórico de execuções é preservado.
- [x] Pesquisar por nome, descrição e tags.
- [x] Importar arquivo Python UTF-8 pelo Storage Access Framework com limite de 1 MiB.
- [x] Exportar código Python pelo Storage Access Framework.
- [x] Validar abertura da biblioteca, criação de script, persistência após encerramento/reabertura e duplicação no Moto G06.
- [ ] Validar renomeação, exclusão, pesquisa e importação/exportação no aparelho.

## P1.3 — Editor e parâmetros

**Estado:** IMPLEMENTADO — VALIDAÇÃO FÍSICA PARCIAL (edição de código, descrição, tags e configuração JSON de parâmetros persistem e geram versões; usabilidade/realce e fluxo de parâmetros ainda pendentes).

- [x] Implementar editor com numeração de linhas.
- [x] Implementar realce lexical básico de Python sem WebView ou dependência de IDE.
- [x] Preservar conteúdo e seleção nativa do campo de edição.
- [x] Editar descrição, tags e parâmetros.
- [x] Validar nomes, tipos, obrigatoriedade, defaults e valores extras.
- [x] Resolver parâmetros antes da execução e passá-los ao runner como dicionário params.
- [x] Documentar que o realce lexical não é parser/IDE completo.
- [x] Validar edição de código e geração de versão; edição de descrição (v6), tags (v7) e definição JSON de parâmetros; configuração de parâmetros persiste ao sair e reabrir a tela.
- [ ] Validar usabilidade, teclado, rolagem, numeração de linhas e realce no Moto G06.
- [ ] Validar valores de execução e recebimento no Python por params.

## P1.4 — Execução e histórico

**Estado:** IMPLEMENTADO — VALIDAÇÃO FÍSICA PARCIAL (execução básica e histórico persistente observados; passagem de parâmetros, falhas e cenários de fila pendentes).

- [x] Associar execução ao script e à versão de origem.
- [x] Persistir QUEUED/RUNNING/estado terminal com timestamps.
- [x] Persistir parâmetros, stdout, stderr, erro, duração e flags de truncamento.
- [x] Exibir console da execução atual.
- [x] Consultar histórico de execuções do script com saída e falhas.
- [x] Preservar registros de execução após exclusão do script.
- [x] Testes JVM executados: :app:testDebugUnitTest — BUILD SUCCESSFUL (relato do usuário).
- [x] Validar execução básica, saída no console e registro no histórico; fechar/reabrir o app e confirmar persistência do script e conseguir executar novamente.
- [ ] Validar execução com parâmetros, falhas, fila cheia e falhas de persistência.

## P1.5 — Biblioteca Python mobile

**Estado:** IMPLEMENTADO — TESTE HOST PASS; runtime Android ainda pendente.

- [x] Versionar pacote e contrato público (mobile 0.2.0; API_VERSION 1).
- [x] Adicionar mobile.capabilities.list_available() e is_implemented().
- [x] Catalogar somente device.get_info e apps.launch como implementados.
- [x] Documentar módulos futuros como planejados, não disponíveis.
- [x] Escrever teste host Python do catálogo.
- [x] Executar testes host Python: 13/13 aprovados, conforme saída relatada pelo usuário.
- [ ] Confirmar execução no runtime Android P1.

## P1.6 — Dependências Python compatíveis

**Estado:** IMPLEMENTADO — TESTE HOST PASS; validação física do manifesto/dependências ainda pendente, com limite explícito.

- [x] Persistir manifesto declarativo por script.
- [x] Validar nome de pacote e restrição simples de versão.
- [x] Exibir declarações no editor e explicar inclusão no build.
- [x] Documentar requisitos Chaquopy/Python 3.13/ABI arm64-v8a.
- [x] Não prometer instalação dinâmica de qualquer pacote PyPI.
- [ ] Executar testes de validação do manifesto.
- [ ] Validar pelo menos uma dependência real apenas se for escolhida e incluída explicitamente no build.

## P1.7 — Revisão, testes e gate

**Estado:** TESTES AUTOMATIZADOS E BUILD EXECUTADOS; validação física em andamento (P1-TF01–TF11).

- [x] Revisar estaticamente os 36 arquivos do diff completo; build APK e validação física continuam pendentes.
- [x] Executar testes Python host: 13/13 PASS, conforme saída relatada pelo usuário.
- [x] Executar `:app:testDebugUnitTest`: `BUILD SUCCESSFUL`, conforme saída relatada pelo usuário.
- [x] Executar :app:assembleDebug; APK debug de aproximadamente 32 MB gerado (relato do usuário).
- [x] Transferir pelo My-CT, instalar e abrir o APK no Moto G06; APK associado ao commit f403049b1a3fda0c5638133298a877aceaa47d30.
- [x] Registrar resultados físicos P1-TF01 a P1-TF11 como relatos do usuário (detalhes na seção Evidências físicas).
- [ ] Validar ciclo completo de parâmetros: fornecer valores JSON, executar e confirmar recebimento no Python por params.
- [ ] Validar importação/exportação, renomeação, exclusão, pesquisa, editor (teclado/linhas/realce) e manifesto de dependências no aparelho.
- [ ] Isolar e confirmar o comportamento do fallback destrutivo em atualização incompatível; dados P0 não precisam ser preservados conforme ADR-0007.
- [x] Atualizar README, docs/README, visão, roadmap, arquitetura, ADRs e contrato mobile.
- [x] Criar PR em draft para revisão/validação: PR #2 (https://github.com/Samuel-Francisco-AS/Androide-PY/pull/2).
- [ ] Marcar PR pronta para revisão após build e validação física do fallback.
- [ ] Fazer Squash Merge somente após revisão final e autorização explícita.

## Evidências da validação física — Moto G06

Os resultados abaixo foram relatados pelo usuário durante a conversa “Validar P1 Fisicamente”. São evidências de uso observado, não logs coletados diretamente pelo assistente.

| ID | Verificação | Resultado observado |
|---|---|---|
| P1-TF01 | Carregamento da biblioteca | Aprovado: biblioteca abriu; script existente apareceu com nome legível e ações de interface. A presença dos botões não comprova, por si só, todas as suas funções. |
| P1-TF02 | Criar e salvar script | Aprovado: criado Teste P1 com código que imprime “Androide PY - P1 funcionando!”; script apareceu na biblioteca. |
| P1-TF03 | Persistência após encerramento | Aprovado: app encerrado pelo seletor de recentes e reaberto; script permaneceu na biblioteca. |
| P1-TF04 | Execução e histórico | Aprovado: execução bem-sucedida; histórico exibiu SUCCEEDED - versão 2, ID e saída do script. |
| P1-TF05 | Edição do código | Aprovado: edição do código gerou versão 3. |
| P1-TF06 | Restaurar versões | Aprovado: restauração da v2 criou v4; restauração da v3 criou v5. Versões anteriores permaneceram no histórico. |
| P1-TF07 | Duplicação | Aprovado: criada Cópia de Teste P1; código atual foi copiado e a cópia iniciou histórico próprio na v1. Alteração independente posterior ainda não testada. |
| P1-TF08 | Inspeção dos campos do editor | Concluído: identificados Nome, Descrição, Tags, Parâmetros, Dependências, Código, Valores para esta execução — JSON, Histórico de Versões, Console e Histórico de Execuções. Inspeção visual não valida o comportamento de todos os campos. |
| P1-TF09 | Edição da descrição | Aprovado: descrição salva; nova versão 6; card de Teste P1 passou ao primeiro lugar da biblioteca. |
| P1-TF10 | Edição das tags | Aprovado: tag validacao-p1 salva; nova versão 7 criada. |
| P1-TF11 | Definição JSON de parâmetros | Aprovado: JSON com parâmetro obrigatório cidade do tipo string aceito; salvar criou nova versão; após sair e voltar à tela do script, a definição persistiu. Número dessa versão não foi informado. |

### Sequência de versões observada

- v2: código original executado.
- v3: edição do código.
- v4: restauração da v2.
- v5: restauração da v3.
- v6: alteração da descrição.
- v7: alteração das tags.
- Versão seguinte: configuração JSON de parâmetros; número não informado.

### Próximos testes físicos

1. P1-TF12 — fornecer {"cidade":"Recife"} em Valores para esta execução, ajustar o código para imprimir params["cidade"], executar e conferir saída/histórico.
2. Depois, validar separadamente parâmetro obrigatório ausente e tipo inválido, sem misturar cenários.
3. Validar importação/exportação, pesquisa, renomeação e exclusão.
4. Validar editor no aparelho (teclado, numeração de linhas, realce, rolagem e desempenho).
5. Validar manifesto de dependências e catálogo mobile no runtime Android.
6. Verificar especificamente o fallback destrutivo em atualização incompatível, se necessário para o gate final.
## Critérios de aceite P1

1. Scripts podem ser criados, importados, renomeados, duplicados, editados, exportados e excluídos.
2. Scripts e metadados persistem após encerrar/reabrir o aplicativo.
3. Editor apresenta numeração de linhas e realce lexical básico.
4. Parâmetros declarados podem ser fornecidos e recebidos pelo script como valores Python em params.
5. Cada execução fica associada ao script/versão e possui histórico consultável com saída e falhas.
6. Dependências são declaradas e validadas com limites claros de compatibilidade Chaquopy; nenhuma instalação genérica é prometida.
7. Biblioteca mobile possui contrato versionado e introspecção honesta das capacidades implementadas.
8. O banco versão 2 inicializa corretamente; atualizações incompatíveis podem recriar o banco conforme ADR-0007.
9. Testes automatizados passam e o ciclo principal é validado no Moto G06; os testes físicos são registrados individualmente e não se presume cobertura de funcionalidades ainda não exercitadas.

## Evidências

| Item | Evidência |
|---|---|
| Branch | feat/p1-script-library criada a partir de main |
| Base inicial | a68ea3bca2e5c8c8fa8fed0c7bba768acb21821f |
| Implementação | Commits incrementais publicados nesta branch |
| Testes host Python | 13/13 PASS (relato do usuário) |
| Testes JVM/Kotlin | :app:testDebugUnitTest — BUILD SUCCESSFUL (relato do usuário) |
| Build APK P1 | :app:assembleDebug — BUILD SUCCESSFUL; APK ~32 MB (relato do usuário) |
| Instalação | APK do commit f403049b1a3fda0c5638133298a877aceaa47d30, transferido via My-CT e instalado no Moto G06 |
| Validação física | P1-TF01–TF11 concluídos conforme relatos do usuário; P1-TF12+ pendentes |
| Fallback destrutivo | Inicialização do app/biblioteca confirmada; cenário específico de migração incompatível ainda precisa de evidência isolada |
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
