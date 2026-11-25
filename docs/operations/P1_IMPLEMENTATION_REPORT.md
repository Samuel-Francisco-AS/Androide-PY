# P1 — Relatório operacional de implementação

**Projeto:** Androide PY  
**Branch:** `feat/p1-script-library`  
**Base:** `main`  
**Marco:** P1 — Biblioteca e gerenciamento de scripts  
**Estado:** preparação publicada; implementação ainda não validada.

## 1. Objetivo

Evoluir a prova de conceito P0 para um ambiente local de desenvolvimento e execução de scripts Python, com biblioteca organizada, editor utilizável, parâmetros, versões, histórico de execuções e gestão explícita de dependências.

## 2. Baseline conferida

A branch `main` foi conferida no commit `a68ea3bca2e5c8c8fa8fed0c7bba768acb21821f`.

Componentes existentes identificados:
- Kotlin + Jetpack Compose.
- Chaquopy 17.2.0 / Python 3.13.
- Room 2.8.5 e KSP 2.3.12.
- `ScriptEntity`, `ScriptDao` e `ScriptRepository` com script default.
- Executor Python serial com fila limitada e captura de stdout/stderr/exceções.
- Tela Compose inicial com editor simples e console.
- Biblioteca Python `mobile` com `configure`, `device.get_info()` e `apps.launch()`.
- Testes host Python e JVM/Kotlin da P0.

A implementação existente não contém ainda uma biblioteca completa, histórico de versões, entidade de execução persistida, parâmetros de script, importação/exportação, editor com realce/linhas ou gerenciador de dependências.

## 3. Decisões de implementação P1

1. **Persistência:** Room permanece a fonte de verdade dos metadados. Python não acessa diretamente Room/SQLite.
2. **Migração:** preservar dados P0; migração explícita e testada, sem `fallbackToDestructiveMigration`.
3. **Scripts:** metadados e código-fonte permanecem locais. Versões são snapshots imutáveis criados em salvamentos relevantes.
4. **Execuções:** cada execução referencia script e versão; histórico armazena status, horários, parâmetros, stdout, stderr e erro estruturado.
5. **Parâmetros:** contrato declarativo e tipado, resolvido no host antes de invocar o runner; o script recebe um dicionário `params`.
6. **Editor:** editor nativo Compose com numeração e realce lexical básico; não adicionar WebView nem dependência de IDE completa.
7. **Importação/exportação:** arquivos Python de texto, com validação de tamanho, extensão e UTF-8; sem executar automaticamente scripts importados.
8. **Dependências:** manifesto declarativo por script e validador local. Chaquopy não será apresentado como ambiente pip genérico em runtime; dependências Android compatíveis exigem inclusão controlada no build e validação por ABI/Python.
9. **Biblioteca mobile:** versão pública e introspecção de capacidades efetivamente implementadas. Módulos futuros não serão expostos como se funcionassem.
10. **Segurança:** scripts continuam sendo código confiável executado com permissões do app; importação não cria sandbox.

## 4. Rotina de execução

Cada subetapa deve seguir o ciclo CONFERE → IMPLEMENTA → CONFERE → MARCA COM EVIDÊNCIA → CONFERE. O checklist `P1_EXECUTION_CHECKLIST.md` é a fonte de verdade do progresso.

## 5. Registro de alterações

| Data | Alteração | Commit/evidência |
|---|---|---|
| 2026-11-25 | Branch P1 criada a partir de main | `feat/p1-script-library` |
| 2026-11-25 | Checklist operacional criado | A publicar |
| 2026-11-25 | Relatório operacional criado | A publicar |

## 6. Testes e validações

Nenhum teste P1 foi executado neste estado inicial. A revisão de arquivos existentes é inspeção estática, não comprova compilação ou funcionamento de funcionalidades novas.

| Categoria | Estado |
|---|---|
| Revisão estática da baseline | Em andamento |
| Testes Python host P1 | Pendente |
| Testes JVM/Kotlin P1 | Pendente |
| Build APK P1 | Pendente |
| Validação física Moto G06 | Pendente |
| Migração de banco P0 → P1 | Pendente |

## 7. Riscos e limitações

- A migração Room precisa preservar scripts já criados na P0.
- Editor com realce lexical precisa manter desempenho aceitável em aparelho de entrada.
- Importação/exportação deve usar Storage Access Framework e não assumir acesso irrestrito ao armazenamento.
- Dependências com extensões nativas precisam de distribuição compatível com CPython 3.13 e ABI arm64-v8a.
- Instalação dinâmica de pacotes PyPI não é presumida.
- Histórico de versões e execuções pode crescer; limites e política de retenção devem ser definidos sem apagar dados silenciosamente.

## 8. Critério para encerramento

P1 só poderá ser considerada concluída após todos os critérios de aceite do checklist, testes automatizados registrados, build local, migração de dados e validação física do ciclo principal no Moto G06. PR e merge são etapas de integração, não substituem validação funcional.
