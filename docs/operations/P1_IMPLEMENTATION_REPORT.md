# P1 — Relatório operacional de implementação

**Projeto:** Androide PY  
**Branch:** feat/p1-script-library  
**Base:** main  
**Marco:** P1 — Biblioteca e gerenciamento de scripts  
**Data do registro:** 2026-11-25  
**Estado:** implementação publicada; testes host Python 13/13 PASS, testes JVM `:app:testDebugUnitTest` BUILD SUCCESSFUL, build debug `:app:assembleDebug` concluído e APK (~32 MB) instalado no Moto G06. Validação física do escopo acordado concluída no Moto G06, incluindo as duas capacidades mobile; testes adicionais e TF13 permanecem pendentes/inconclusivos para fases posteriores, conforme checklist.

## 1. Objetivo

Evoluir a prova de conceito P0 para um ambiente local de criação e execução de scripts Python, com biblioteca organizada, editor utilizável, parâmetros, versões, histórico de execuções e gestão explícita de dependências.

## 2. Baseline conferida

A branch main foi conferida no commit a68ea3bca2e5c8c8fa8fed0c7bba768acb21821f.

Componentes existentes identificados:
- Kotlin + Jetpack Compose.
- Chaquopy 17.2.0 / Python 3.13.
- Room 2.8.5 e KSP 2.3.12.
- ScriptEntity, ScriptDao e ScriptRepository com script default.
- Executor Python serial com fila limitada e captura de stdout/stderr/exceções.
- Tela Compose inicial com editor simples e console.
- Biblioteca Python mobile com configure, device.get_info() e apps.launch().
- Testes host Python e JVM/Kotlin da P0.

A implementação inicial não continha histórico de versões, entidade de execução persistida, parâmetros declarativos, importação/exportação, editor com realce/linhas ou gerenciamento de dependências.

## 3. Decisões de implementação P1

1. **Persistência:** Room permanece a fonte de verdade dos metadados. Python não acessa diretamente Room/SQLite.
2. **Persistência:** schema Room versão 2 com `fallbackToDestructiveMigration()` durante o desenvolvimento; versões incompatíveis podem recriar o banco e descartar dados locais, conforme ADR-0007.
3. **Scripts:** metadados e código-fonte permanecem locais. Snapshots imutáveis são criados quando código ou metadados mudam.
4. **Execuções:** cada execução referencia script e versão; histórico armazena parâmetros, status, horários, stdout, stderr, erro estruturado e flags de truncamento.
5. **Exclusão:** excluir script remove metadados e versões em transação; execuções são preservadas como histórico.
6. **Parâmetros:** esquema declarativo JSON com nome, tipo, obrigatoriedade e default. O host valida os valores e o runner os expõe ao código como dicionário params.
7. **Editor:** Compose nativo com numeração e realce lexical básico; não é uma IDE completa, parser semântico ou depurador.
8. **Importação/exportação:** Storage Access Framework; importação exige texto UTF-8 e limita arquivo a 1 MiB; conteúdo importado não é executado automaticamente.
9. **Dependências:** manifesto declarativo por script e validação sintática local. Não há pip install genérico em runtime; pacotes precisam de compatibilidade Chaquopy/Python 3.13/ABI arm64-v8a e inclusão controlada no build.
10. **Biblioteca mobile:** versão pública 0.2.0, API_VERSION 1 e catálogo de capacidades implementadas. Apenas device.get_info e apps.launch são listadas; módulos futuros não são anunciados como prontos.
11. **Segurança:** scripts continuam sendo código confiável executado com permissões do app; importação não cria sandbox.

## 4. Implementação publicada

### 4.1 Dados e persistência

- ScriptEntity ganhou descrição, tags, definições de parâmetros e manifesto de dependências.
- ScriptVersionEntity registra snapshots imutáveis numerados.
- ScriptExecutionEntity registra execução e resultado.
- ScriptDao oferece gravações transacionais de script/versão e exclusão transacional.
- ScriptExecutionDao e ScriptExecutionRepository persistem ciclo de execução.
- AppDatabase usa Room schema versão 2 e `fallbackToDestructiveMigration()`; a migração 1 → 2 foi removida conforme ADR-0007.
- AndroidePyApplication expõe repositórios de scripts e execuções.

### 4.2 Biblioteca e editor

- Tela inicial evoluiu para biblioteca pesquisável.
- Criar, abrir, duplicar, excluir com confirmação, importar e exportar.
- Importação de arquivos UTF-8 limitada a 1 MiB; exportação por CreateDocument.
- Editor de metadados, tags, parâmetros e dependências.
- PythonCodeEditor usa BasicTextField, numeração de linhas e realce lexical leve.
- Histórico de versões com ação de restauração, que cria um novo snapshot.
- Histórico de execução apresenta status, versão, timestamp, stdout, stderr e erro.

### 4.3 Parâmetros e runner

- ScriptParameterResolver valida nomes, tipos, obrigatoriedade, defaults e valores não declarados.
- ScriptExecutionRequest transporta parâmetros JSON.
- ScriptExecutor envia parâmetros ao runner.
- runner.py valida que o payload é um objeto JSON e disponibiliza params no namespace do script.
- Testes Python foram ampliados para parâmetros válidos e payload inválido.

### 4.4 Biblioteca mobile

- mobile.__version__ = 0.2.0 e API_VERSION = 1.
- mobile.capabilities.list_available() lista capacidades implementadas.
- mobile.capabilities.is_implemented() consulta identificadores.
- Teste host verifica que apenas device.get_info e apps.launch aparecem como implementados.
- Contrato da Android Bridge foi estendido.

### 4.5 Documentação

- Checklist e relatório operacional P1 criados.
- README, docs/README, visão, roadmap, arquitetura e ADRs atualizados; ADR-0006 registra o modelo de versionamento e histórico.
- Contrato Android Bridge estendido com catálogo público.
- Limitações de dependências, editor e importação registradas.

## 5. Revisões e correções estáticas realizadas

Durante a revisão foram identificados e corrigidos:
- Semântica de newline no runner e nos testes Python.
- Persistência dos updates de execução movida para o ciclo de vida da tela principal, para não depender de permanecer no editor.
- Captura de falhas ao persistir updates para que um erro de histórico não encerre o coletor.
- Leitura do estado do editor movida para snapshot na thread principal antes de persistir em IO.
- Exclusão de script e versões tornada transacional.
- Feedback do editor ajustado para não afirmar criação de versão quando nada mudou.

Essas correções e os arquivos do diff foram revisados estaticamente. As APIs de Compose (BasicTextField/VisualTransformation), Storage Access Framework e migração Room foram conferidas na documentação oficial. Depois, o usuário executou os testes host Python (13/13 PASS), `:app:testDebugUnitTest` (BUILD SUCCESSFUL) e `:app:assembleDebug` (BUILD SUCCESSFUL; APK de aproximadamente 32 MB). O APK associado ao commit `f403049b1a3fda0c5638133298a877aceaa47d30` foi transferido pelo My-CT, instalado e aberto no Moto G06.

## 6. Testes e validações

Testes Python host (13/13 PASS), testes JVM (`:app:testDebugUnitTest` — BUILD SUCCESSFUL) e build debug (`:app:assembleDebug` — BUILD SUCCESSFUL; APK de aproximadamente 32 MB) foram executados pelo usuário e relatados nas conversas do projeto. O APK associado ao commit `f403049b1a3fda0c5638133298a877aceaa47d30` foi transferido pelo My-CT, instalado e aberto no Moto G06. A validação física do escopo acordado foi concluída conforme relatos do usuário: TF01–TF12, TF14–TF15, verificações selecionadas do editor e ambas as capacidades mobile (`mobile.device.get_info()` e `mobile.apps.launch("com.android.settings")`).

| Categoria | Estado |
|---|---|
| Revisão estática de código | Concluída; não substitui testes/build/validação física |
| Testes Python host | 13/13 PASS (relatado pelo usuário) |
| Testes JVM/Kotlin | `:app:testDebugUnitTest` — BUILD SUCCESSFUL (relatado pelo usuário) |
| Build APK P1 | `:app:assembleDebug` — BUILD SUCCESSFUL; APK ~32 MB (relatado pelo usuário) |
| Instalação | APK do commit `f403049b1a3fda0c5638133298a877aceaa47d30`, transferido via My-CT e instalado no Moto G06 |
| Validação física Moto G06 | Escopo acordado concluído: TF01–TF12, TF14–TF15, verificações selecionadas do editor e as duas capacidades mobile aprovadas. TF13 inconclusivo; demais cenários explicitamente adiados. |
| Fallback destrutivo | App/biblioteca inicializam; cenário específico de migração incompatível ainda não isolado como teste |
| PR | #2 aberta em draft; validação do escopo acordado concluída; revisão final/integração pendentes |

### Testes adicionados/atualizados

- ScriptRepositoryTest: script default, snapshots, ausência de versões duplicadas, duplicação, metadados e validação de manifestos.
- ScriptExecutionRepositoryTest: fila, início, resultado, stdout/stderr e erro estruturado.
- ScriptParameterResolverTest: required/default, tipos incorretos e valores não declarados.
- test_runner.py: dicionário params e payload de parâmetros inválido, além dos testes anteriores.
- test_mobile_api.py: versão e catálogo de capacidades.

## 6.1. Registro físico detalhado — Moto G06

Os resultados desta seção são relatos do usuário, não medições/logs obtidos diretamente pelo assistente.

| ID | Verificação | Evidência relatada |
|---|---|---|
| P1-TF01 | Biblioteca | Tela abriu e script existente apareceu. |
| P1-TF02 | Criar/salvar | Teste P1 criado com o código que imprime “Androide PY - P1 funcionando!” e listado. |
| P1-TF03 | Persistência | Após encerrar pelo seletor de recentes e reabrir o app, o script continuou na biblioteca. |
| P1-TF04 | Execução/histórico | Execução bem-sucedida; histórico exibiu SUCCEEDED - versão 2, ID e saída. |
| P1-TF05 | Editar código | Edição gerou v3. |
| P1-TF06 | Restaurar | Restaurar v2 gerou v4; restaurar v3 gerou v5. |
| P1-TF07 | Duplicar | Cópia Cópia de Teste P1 recebeu código atual e histórico próprio começando em v1. |
| P1-TF08 | Inspecionar campos | Nome, Descrição, Tags, Parâmetros, Dependências, Código, Valores para esta execução JSON, Histórico de Versões, Console e Histórico de Execuções identificados. |
| P1-TF09 | Descrição | Salvou descrição, criou v6 e moveu o card para o topo da biblioteca. |
| P1-TF10 | Tags | Salvou validacao-p1 e criou v7. |
| P1-TF11 | Parâmetros | JSON de cidade como string obrigatório aceito; nova versão criada; persistência confirmada após sair e voltar. Número da versão não informado. |
| P1-TF12 | Entrega de parâmetros | Valor `{"cidade":"Recife"}` informado no JSON de execução; `print(params["cidade"])` executado e `stdout` retornou `Recife`. Passagem básica de parâmetro string aprovada. |
| Mobile — `device.get_info()` | Execução no runtime Android retornou informações reais do Moto G06, conforme relato do usuário. **Aprovado.** |
| Mobile — `apps.launch("com.android.settings")` | Abriu o aplicativo Configurações do Android e retornou o resultado esperado no `stdout`, conforme relato do usuário. **Aprovado.** |

Sequência confirmada: v2 código original; v3 edição do código; v4 restauração de v2; v5 restauração de v3; v6 descrição; v7 tags; versão posterior para parâmetros (número não informado).

P1-TF12 aprovado: a passagem de `{"cidade":"Recife"}` ao Python foi confirmada por `print(params["cidade"])`, com `Recife` em stdout. Importação/exportação, busca, renomeação, exclusão e verificações selecionadas do editor também foram concluídas no escopo documentado. A biblioteca mobile foi validada fisicamente: `device.get_info()` e `apps.launch("com.android.settings")` aprovados. Permanecem adiados: tipo inválido, fila cheia, falhas de persistência, manifesto de dependências, desempenho/ergonomia/refinamento do editor e fallback destrutivo específico. P1-TF13 permanece inconclusivo/diferido.
## 7. Limitações e riscos remanescentes

### Itens deliberadamente adiados para fase posterior

- P1-TF13 (parâmetro obrigatório ausente): INCONCLUSIVO / DIFERIDO; precisa de feedback visível e garantia de bloqueio antes do runner.
- Validação de tipo inválido, fila cheia e falhas de persistência.
- Testes do manifesto de dependências e eventual pacote Python real incluído explicitamente no build.
- Desempenho do editor; ergonomia e refinamento do realce lexical encaminhados à fase de UI/UX.
- Confirmação isolada do fallback destrutivo Room em atualização incompatível.

Esses itens não foram aprovados; foram mantidos como pendências explícitas por decisão de escopo do usuário em 25/11/2026 e não bloqueiam o encerramento desta P1. Consulte o checklist operacional para a matriz detalhada e evidências.

- `fallbackToDestructiveMigration()` pode apagar scripts, versões e execuções locais quando o app for atualizado de uma versão de banco sem migração compatível. Esse comportamento é intencional durante o desenvolvimento e deve ser informado antes da instalação/validação. A abertura da biblioteca no Moto G06 confirma inicialização funcional do banco usado pelo APK, mas não documenta isoladamente qual caminho de fallback ocorreu nem quais dados foram descartados.
- A UI ainda não possui validação física suficiente de teclado, numeração de linhas, realce, rolagem, acessibilidade ou desempenho.
- Realce lexical não substitui parser Python, autocomplete, lint ou depurador.
- Importação suporta arquivos de texto UTF-8 de até 1 MiB; não há importação de projetos/arquivos auxiliares.
- Manifesto de dependências aceita nomes e restrições simples de versão. A compatibilidade real depende de cada pacote e não é inferida pelo validador.
- Pacotes não são instalados dinamicamente no Android.
- Execuções permanecem no mesmo UID do aplicativo; não existe sandbox.
- Execução em segundo plano, agendamento, acessibilidade e notificações continuam fora da P1.

## 8. Critério para encerramento

P1 só poderá ser considerada concluída após todos os critérios de aceite do checklist. Testes automatizados e build local foram executados; a validação física está em andamento e já cobre P1-TF01–TF12. Ainda faltam os cenários de erro de parâmetros, importação/exportação, operações restantes da biblioteca, editor, dependências e a confirmação isolada do fallback destrutivo. Não há requisito de preservar dados P0. PR e merge são etapas de integração, não substituem validação funcional.

## 9. Integração

- Branch: feat/p1-script-library.
- Diff: 34 arquivos; 2.091 adições e 568 remoções no último comparativo.
- Base: main.
- PR #2: https://github.com/Samuel-Francisco-AS/Androide-PY/pull/2 — aberta em draft, aguardando testes/build/validação.
- Squash Merge: não realizado; requer revisão final e autorização explícita para a P1.
