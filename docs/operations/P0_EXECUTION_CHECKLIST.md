# P0 — Checklist Operacional de Implementação

**Projeto:** Androide PY  
**Branch de trabalho:** `feat/p0-runtime-bridge`  
**Base:** `main`  
**Responsável pela implementação remota:** assistente, via integração GitHub  
**Responsável por builds e validação física:** Sam, no Fedora e no Moto G06  
**Última atualização:** 2026-11-25  
**Estado geral:** 🟢 Governança operacional concluída; revisão técnica P0 ainda não iniciada.

> Este arquivo é a fonte operacional de verdade para o progresso da P0. Um item só pode ser marcado como concluído depois de sua execução real e da conferência da evidência correspondente. Criar um arquivo ou escrever código não equivale, por si só, a validar seu funcionamento.

## Regras de atualização

1. Trabalhar em uma única subetapa por vez.
2. Antes de cada subetapa, reler este checklist e os documentos relevantes indicados em `AGENTS.md`.
3. Implementar somente o escopo da subetapa atual.
4. Conferir o diff/arquivos criados, coerência arquitetural, referências e testes aplicáveis.
5. Registrar evidências concretas: caminhos, commits, comandos, resultados de testes ou validação relatada pelo usuário.
6. Só então marcar a subetapa como concluída e publicar a atualização do checklist.
7. Reler o checklist atualizado e selecionar a próxima subetapa não concluída.
8. Nunca declarar build local, instalação ou teste físico como concluído sem evidência fornecida pelo ambiente de execução ou pelo usuário.
9. Se houver bloqueio ou decisão arquitetural nova, registrar o motivo e interromper a sequência dependente. Não contornar silenciosamente um ADR.

### Legenda

- ⬜ Não iniciada
- 🟡 Em andamento
- ☑️ Concluída com evidência registrada
- ⏸ Bloqueada — depende de decisão, acesso ou correção

---

## OP-0 — Governança operacional

- [x] **OP-0.1** Criar este checklist operacional — publicado e conferido no GitHub.
- [x] **OP-0.2** Criar `AGENTS.md` com a rotina obrigatória de leitura, implementação, conferência e atualização — publicado e conferido no GitHub.
- [x] **OP-0.3** Conferir no GitHub o conteúdo dos dois arquivos e confirmar que estão na branch de trabalho.
- [x] **OP-0.4** Relê-los e confirmar que os caminhos e instruções são coerentes com o repositório.

**Evidências:**
- OP-0.1: commit `f901ef3f1fc30ffd28e16797dd32572978e2307f`; arquivo conferido no GitHub (blob `48ce49b2eb327c80031e1647f41777821a2358c5`).
- OP-0.2: commit `2fd00ed1991f194a1593b0b64f7e366987b3df9c`; arquivo conferido no GitHub (blob `370ce8503642878ddaba3fa1d1d0ab3e148ea988`).
- OP-0.3/OP-0.4: ambos os arquivos recuperados da branch `feat/p0-runtime-bridge`; verificadas as instruções do ciclo obrigatório, referências ao checklist e decomposição das fases P0.

## P0-1 — Revisão documental e decisões técnicas

- [ ] **P0-1.1** Ler integralmente README, visão do projeto, arquitetura, roadmap e ADRs 0001–0004.
- [ ] **P0-1.2** Extrair os requisitos e critérios de aceite da P0, distinguindo escopo obrigatório e itens fora de escopo.
- [ ] **P0-1.3** Verificar compatibilidade oficial da matriz Gradle / AGP / Kotlin / Compose / Chaquopy / Python / SDK.
- [ ] **P0-1.4** Definir e registrar as versões exatas e repositórios necessários; não presumir que versões mencionadas em conversas anteriores estejam corretas.
- [ ] **P0-1.5** Registrar ADR adicional ou atualizar ADR existente se a implementação exigir mudança arquitetural relevante.

**Gate P0-1:** decisões técnicas rastreáveis e sem conflito conhecido com os ADRs aceitos.

## P0-2 — Fundação Android

- [ ] **P0-2.1** Criar estrutura Gradle multi-módulo mínima com módulo `:app` e Gradle Wrapper 8.14.3.
- [ ] **P0-2.2** Configurar AGP, Kotlin, Compose e SDK conforme matriz verificada.
- [ ] **P0-2.3** Configurar identificador de aplicação, manifesto, atividade principal e recursos mínimos.
- [ ] **P0-2.4** Configurar ABI(s) de destino de forma explícita e compatível com o dispositivo-alvo; justificar qualquer restrição.
- [ ] **P0-2.5** Revisar arquivos Gradle, manifesto e estrutura; remover configurações copiadas do My-CT que não sejam necessárias.
- [ ] **P0-2.6** Publicar a fundação Android sem declarar build aprovado antes da execução real.

**Gate P0-2:** estrutura Android coerente, revisada e pronta para integração do runtime.

## P0-3 — Runtime Python (Chaquopy)

- [ ] **P0-3.1** Integrar a versão de Chaquopy verificada na matriz técnica.
- [ ] **P0-3.2** Configurar Python embarcado 3.13 e o interpretador host exigido pelo processo de build.
- [ ] **P0-3.3** Criar módulo Python inicial e uma interface Kotlin que encapsule inicialização/acesso ao runtime.
- [ ] **P0-3.4** Garantir inicialização fora da thread principal e comportamento seguro em chamadas repetidas.
- [ ] **P0-3.5** Criar teste mínimo de integração Kotlin/Python e documentação de execução.
- [ ] **P0-3.6** Revisar dependências, inicialização, tratamento de falhas e limites conhecidos do Chaquopy.

**Gate P0-3:** integração implementada e teste automatizado pertinente criado; execução real no dispositivo permanece pendente até validação física.

## P0-4 — Execução de scripts e console

- [ ] **P0-4.1** Definir contrato de execução e resultado (identificador, estado, saída, erro e timestamps essenciais).
- [ ] **P0-4.2** Implementar executor inicial com política serial (uma execução ativa), sem prometer isolamento ou cancelamento forçado.
- [ ] **P0-4.3** Capturar stdout e stderr separadamente, preservando ordem/limites razoáveis e evitando bloquear a UI.
- [ ] **P0-4.4** Capturar exceções Python e convertê-las em erro estruturado sem encerrar o aplicativo.
- [ ] **P0-4.5** Implementar estados mínimos QUEUED, RUNNING, SUCCEEDED, FAILED e CANCELLED, quando suportados pelo escopo inicial.
- [ ] **P0-4.6** Criar testes unitários/integrados para sucesso, stderr e exceção.
- [ ] **P0-4.7** Revisar concorrência, lifecycle, memória e apresentação do console.

**Gate P0-4:** execução básica e caminhos de erro cobertos por testes automatizados.

## P0-5 — Android Bridge mínima

- [ ] **P0-5.1** Definir contratos Kotlin/Python e erros previsíveis para a Bridge.
- [ ] **P0-5.2** Implementar `mobile.device.get_info()` com informações não sensíveis e justificadas.
- [ ] **P0-5.3** Implementar `mobile.apps.launch(package_name)` por API Android pública, respeitando visibilidade de pacotes e restrições de intents.
- [ ] **P0-5.4** Expor resultados/erros da Bridge ao script Python sem acoplá-lo a classes Android.
- [ ] **P0-5.5** Criar testes para dispositivo, aplicativo inexistente e falhas de inicialização quando viável.
- [ ] **P0-5.6** Documentar permissões, package visibility, limites e comportamento esperado.

**Gate P0-5:** ambas as capacidades implementadas e cobertas por testes apropriados.

## P0-6 — Persistência local mínima

- [ ] **P0-6.1** Implementar persistência de metadados com Room/SQLite conforme ADR-0003.
- [ ] **P0-6.2** Definir entidade/DAO/repositório mínimos para o script de teste e seu conteúdo.
- [ ] **P0-6.3** Integrar leitura e gravação ao fluxo de edição/execução.
- [ ] **P0-6.4** Tratar primeira inicialização, ausência de registro e falhas de persistência.
- [ ] **P0-6.5** Criar testes de persistência e recuperação.
- [ ] **P0-6.6** Revisar fronteira: Python não acessa diretamente tabelas Room.

**Gate P0-6:** script salvo e recuperável por repositório Kotlin, com testes de persistência.

## P0-7 — Interface mínima integrada

- [ ] **P0-7.1** Criar tela Compose com editor/campo de código, ação Executar e console.
- [ ] **P0-7.2** Carregar e salvar o script de teste pela camada de persistência.
- [ ] **P0-7.3** Conectar a ação Executar ao executor Python.
- [ ] **P0-7.4** Apresentar stdout, stderr, exceções e estado da execução com distinção visual clara.
- [ ] **P0-7.5** Disponibilizar acionamento demonstrável das duas capacidades da Bridge, sem sobrecarregar a tela.
- [ ] **P0-7.6** Tratar loading, execução concorrente, erros e mudanças de configuração/lifecycle.
- [ ] **P0-7.7** Revisar acessibilidade básica, layout em tela móvel e responsividade.

**Gate P0-7:** fluxo completo de editar, salvar, executar e observar resultado conectado às camadas reais.

## P0-8 — Qualidade, documentação e pacote

- [ ] **P0-8.1** Adicionar testes unitários Kotlin e Python compatíveis com a arquitetura.
- [ ] **P0-8.2** Adicionar testes de integração para runtime, executor, Bridge e persistência.
- [ ] **P0-8.3** Executar revisão estática do código, imports, permissões, manifesto e dependências.
- [ ] **P0-8.4** Atualizar README e documentação técnica com instruções reproduzíveis de build e limites reais.
- [ ] **P0-8.5** Conferir que nenhum segredo, dado pessoal, caminho absoluto da máquina de desenvolvimento ou configuração local foi publicado.
- [ ] **P0-8.6** Revisar o diff completo e registrar o commit final da implementação remota.
- [ ] **P0-8.7** Fornecer ao usuário os comandos de sincronização e build local; não afirmar que foram executados.

**Gate P0-8:** código e documentação revisados; testes que podem ser executados no ambiente remoto/CI registrados com resultados reais.

## P0-9 — Validação local e física (dependente do usuário)

- [ ] **P0-9.1** Usuário sincroniza a branch no Fedora.
- [ ] **P0-9.2** Usuário executa build debug e compartilha o resultado.
- [ ] **P0-9.3** Corrigir falhas de build com base nos logs reais; repetir até obter build aprovado.
- [ ] **P0-9.4** Usuário transfere o APK ao Moto G06 pelo fluxo já utilizado no My-CT; ADB/USB não é requisito do projeto.
- [ ] **P0-9.5** Usuário instala e valida abertura, inicialização Python, edição, execução, stdout, stderr e exceções.
- [ ] **P0-9.6** Usuário valida `mobile.device.get_info()` e `mobile.apps.launch(package_name)`.
- [ ] **P0-9.7** Usuário fecha/reabre o app e confirma persistência do script.
- [ ] **P0-9.8** Registrar modelo, versão Android, resultados, limitações e evidências relatadas.

**Gate P0-9:** todos os oito critérios do roadmap demonstrados no dispositivo físico.

## Critérios oficiais de aceite da P0

A P0 só poderá ser declarada concluída quando todos forem demonstrados:

1. APK instala e abre no dispositivo de validação.
2. Runtime Python inicializa.
3. Script Python executa pela interface.
4. stdout e stderr aparecem no console.
5. Exceção Python é apresentada sem encerrar o aplicativo.
6. Python consulta informações básicas do dispositivo.
7. Python inicia aplicativo instalado por API Android autorizada.
8. Script permanece salvo após fechar e reabrir o aplicativo.

## Registro de evidências

| Data | Subetapa | Evidência (commit, teste, comando ou relato) | Resultado |
|---|---|---|---|
| 2026-11-25 | Preparação do ambiente local | Fedora 44, JDK 21.0.10, Python 3.13.18, SDK 36 e Gradle Wrapper 8.14.3 reportados pelo usuário | Confirmado pelo usuário; não equivale a build Android |
| 2026-11-25 | Clone remoto/local | Clone SSH do repositório concluído e sincronizado, informado pelo usuário | Confirmado pelo usuário |

## Histórico de atualização

- 2026-11-25 — checklist e `AGENTS.md` publicados e conferidos na branch `feat/p0-runtime-bridge`; OP-0.1 a OP-0.4 concluídos com evidências. Nenhum código de produção foi iniciado.
