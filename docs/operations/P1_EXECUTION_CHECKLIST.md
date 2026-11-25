# P1 — Checklist operacional de implementação

**Branch:** `feat/p1-script-library`  
**Base:** `main`  
**Marco:** Biblioteca e gerenciamento de scripts  
**Estado inicial:** planejamento operacional publicado; implementação pendente.

Este documento é a fonte de verdade do progresso da P1. Marcar uma tarefa como concluída exige conferência posterior e evidência concreta. Publicação no GitHub não equivale a build, teste ou validação física.

## Regras de execução

Para cada subetapa, seguir obrigatoriamente:

1. **CONFERE:** reler este item, inspecionar código/documentos/dependências e definir verificação.
2. **IMPLEMENTA:** alterar somente o escopo selecionado e dependências necessárias.
3. **CONFERE:** revisar arquivos e diff; executar testes disponíveis; registrar limitações.
4. **MARCA:** atualizar este checklist somente com evidência.
5. **CONFERE:** reler o estado atualizado antes de avançar.

Estados: `PENDENTE`, `EM ANDAMENTO`, `BLOQUEADO`, `IMPLEMENTADO — TESTE PENDENTE`, `TESTADO`, `VALIDADO FISICAMENTE`.

## Escopo P1

### P1.0 — Preparação e contratos

- [x] Criar branch dedicada a partir de `main`. Evidência: branch `feat/p1-script-library`.
- [x] Criar este checklist operacional.
- [x] Criar o relatório operacional P1.
- [ ] Revisar estado atual do banco, executor, interface, testes e documentação.
- [ ] Definir contrato de scripts, parâmetros, versões, execuções e dependências.
- [ ] Definir política de migração Room sem perda dos scripts P0.
- [ ] Definir limites de importação, exportação e validação de arquivos.
- [ ] Definir limites explícitos do suporte a dependências Python no Android.

### P1.1 — Modelo de dados e persistência

- [ ] Evoluir entidade de script com descrição, tags, parâmetros, dependências e metadados.
- [ ] Criar entidade de versões imutáveis do script.
- [ ] Criar entidade de execuções com parâmetros, status, horários, saída e erro.
- [ ] Implementar DAOs e repositórios com operações transacionais.
- [ ] Migrar o banco sem descartar o script default nem scripts existentes.
- [ ] Adicionar testes de repositório, histórico, exclusão e migração.

### P1.2 — Biblioteca de scripts

- [ ] Listar scripts persistidos e selecionar script.
- [ ] Criar script.
- [ ] Renomear e editar metadados.
- [ ] Duplicar script com novo identificador.
- [ ] Excluir script com confirmação e regra para histórico.
- [ ] Pesquisar/filtrar por nome e tags.
- [ ] Importar arquivo Python com validação de extensão/tamanho/encoding.
- [ ] Exportar script para arquivo compartilhável.
- [ ] Preservar o script default legado na migração.

### P1.3 — Editor e parâmetros

- [ ] Implementar editor com numeração de linhas.
- [ ] Implementar realce lexical básico de Python sem dependência pesada ou editor web.
- [ ] Preservar cursor/seleção e rolagem ao editar.
- [ ] Editar descrição, tags e parâmetros.
- [ ] Validar nomes/tipos/defaults de parâmetros.
- [ ] Resolver parâmetros antes da execução e passar valores estruturados ao runner.
- [ ] Documentar que realce lexical não é parser/IDE completo.

### P1.4 — Execução e histórico

- [ ] Associar cada execução ao script e à versão de origem.
- [ ] Persistir QUEUED/RUNNING/terminal com timestamps.
- [ ] Persistir parâmetros, stdout, stderr, erro e duração.
- [ ] Exibir console separado por execução.
- [ ] Consultar histórico de execuções de um script.
- [ ] Reabrir detalhes de uma execução anterior.
- [ ] Cobrir falha de persistência sem esconder resultado da execução.

### P1.5 — Biblioteca Python mobile

- [ ] Definir versão e contrato público da biblioteca.
- [ ] Adicionar introspecção de capacidades realmente disponíveis.
- [ ] Documentar módulos implementados versus planejados.
- [ ] Testar contrato host Python com adaptador falso.
- [ ] Não criar APIs fictícias para screen, notifications, files, system, sensors, network ou integrations.
- [ ] Não introduzir dependência de Room/SQLite no código Python.

### P1.6 — Dependências Python compatíveis

- [ ] Definir manifesto de dependências por script.
- [ ] Validar nomes/versões e restrições de compatibilidade.
- [ ] Exibir dependências declaradas e status conhecido.
- [ ] Documentar fluxo de inclusão no build/runtime Chaquopy.
- [ ] Não prometer instalação dinâmica de qualquer pacote PyPI.
- [ ] Testar parser/validador sem rede e sem instalar pacotes no dispositivo.

### P1.7 — Revisão, testes e gate

- [ ] Revisar diff completo da branch.
- [ ] Executar testes Python host.
- [ ] Executar testes JVM/Kotlin.
- [ ] Executar build debug localmente pelo usuário.
- [ ] Validar ciclo biblioteca → editor → salvar → executar → histórico no Moto G06.
- [ ] Validar importação/exportação no aparelho.
- [ ] Validar migração de dados P0 no aparelho.
- [ ] Registrar resultados físicos como relato do usuário, sem atribuir logs não fornecidos.
- [ ] Atualizar README, docs/README, roadmap, arquitetura e contrato mobile.
- [ ] Criar PR para revisão.
- [ ] Fazer Squash Merge somente após revisão final e autorização explícita.

## Critérios de aceite P1

1. Scripts podem ser criados, importados, renomeados, duplicados, editados, exportados e excluídos.
2. Scripts e metadados persistem após encerrar/reabrir o aplicativo.
3. Editor apresenta numeração de linhas e realce lexical básico, sem alegar recursos de IDE completa.
4. Parâmetros declarados podem ser fornecidos e recebidos pelo script como valores Python.
5. Cada execução fica associada ao script/versão e possui histórico consultável com saída e falhas.
6. Dependências são declaradas e validadas com limites claros de compatibilidade Chaquopy; nenhuma instalação genérica ou automática é prometida.
7. Biblioteca `mobile` possui contrato versionado e introspecção honesta de capacidades implementadas.
8. Migração preserva scripts criados na P0.
9. Testes automatizados relevantes passam e o ciclo principal é validado no Moto G06.

## Evidências

| Item | Evidência |
|---|---|
| Branch | `feat/p1-script-library` criada a partir de `main` |
| Base inicial | `main` em `a68ea3bca2e5c8c8fa8fed0c7bba768acb21821f` |
| Testes/build P1 | Pendentes |
| Validação física P1 | Pendente |
| PR/merge | Pendente |

## Bloqueios e desvios

Registrar aqui qualquer impedimento, decisão alterada ou critério que precise ser renegociado. Não apagar evidências históricas; corrigir por nova anotação datada.
