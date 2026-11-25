# P1 — Relatório operacional de implementação

**Projeto:** Androide PY  
**Branch:** feat/p1-script-library  
**Base:** main  
**Marco:** P1 — Biblioteca e gerenciamento de scripts  
**Data do registro:** 2026-11-25  
**Estado:** implementação publicada e revisão estática em andamento; testes, build e validação física P1 pendentes.

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
2. **Migração:** versão 1 → 2 explícita, sem fallback destrutivo; scripts P0 são preservados e ganham versão inicial sob demanda.
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
- AppDatabase implementa MIGRATION_1_2 com criação de tabelas/índices e colunas novas.
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

Essas correções foram revisadas estaticamente; ainda precisam de testes executados.

## 6. Testes e validações

Nenhum teste P1 foi executado neste estado do relatório. A existência de testes no repositório não comprova aprovação.

| Categoria | Estado |
|---|---|
| Revisão estática de código | Em andamento |
| Testes Python host | Escritos; execução pendente |
| Testes JVM/Kotlin | Escritos; execução pendente |
| Teste de migração Room | Pendente |
| Build APK P1 | Pendente |
| Validação física Moto G06 | Pendente |
| PR/integração em main | Pendente |

### Testes adicionados/atualizados

- ScriptRepositoryTest: script default, snapshots, ausência de versões duplicadas, duplicação, metadados e validação de manifestos.
- ScriptExecutionRepositoryTest: fila, início, resultado, stdout/stderr e erro estruturado.
- ScriptParameterResolverTest: required/default, tipos incorretos e valores não declarados.
- test_runner.py: dicionário params e payload de parâmetros inválido, além dos testes anteriores.
- test_mobile_api.py: versão e catálogo de capacidades.

## 7. Limitações e riscos remanescentes

- A migração Room foi escrita, mas precisa ser executada sobre banco P0 real para comprovar preservação dos dados. A branch remota não contém os JSONs de schema Room exportados: o schema v1 histórico precisa ser recuperado/gerado da baseline P0, e o schema v2 deve ser gerado pelo build P1 e ambos versionados. Sem esses artefatos, ainda não há base completa para um teste automatizado com MigrationTestHelper.
- A UI não possui ainda validação física de teclado, rolagem, acessibilidade ou desempenho.
- Realce lexical não substitui parser Python, autocomplete, lint ou depurador.
- Importação suporta arquivos de texto UTF-8 de até 1 MiB; não há importação de projetos/arquivos auxiliares.
- Manifesto de dependências aceita nomes e restrições simples de versão. A compatibilidade real depende de cada pacote e não é inferida pelo validador.
- Pacotes não são instalados dinamicamente no Android.
- Execuções permanecem no mesmo UID do aplicativo; não existe sandbox.
- Execução em segundo plano, agendamento, acessibilidade e notificações continuam fora da P1.

## 8. Critério para encerramento

P1 só poderá ser considerada concluída após todos os critérios de aceite do checklist, testes automatizados executados, build local, migração de dados P0 e validação física do ciclo principal no Moto G06. PR e merge são etapas de integração, não substituem validação funcional.

## 9. Integração

- Branch: feat/p1-script-library.
- Base: main.
- PR: ainda não criada.
- Squash Merge: não autorizado nesta fase até confirmação explícita do usuário.
