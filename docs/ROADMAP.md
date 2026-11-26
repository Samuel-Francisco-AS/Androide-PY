# Roadmap de Desenvolvimento

**Projeto:** Androide PY  
**Status:** P0 e P1 concluídas e integradas em `main`; P2.0 documental e P2.1-A (implementação e build debug local) concluídas na branch `feat/p2-accessibility-automation`; validação física pendente para P2.1-C.  
**Estratégia:** entregas incrementais, testes automatizados e validação física.

## Diretrizes

- Cada marco deve produzir um resultado demonstrável.
- Não avançar com base apenas em compilação: validar o comportamento em dispositivo físico.
- Registrar decisões técnicas em ADRs.
- Manter o escopo pequeno o suficiente para diagnosticar falhas.
- Tratar capacidades Android como adaptadores incrementais.
- Revisar arquitetura e documentação quando evidências invalidarem uma premissa.

## P0 — Runtime Python e Android Bridge mínima

**Objetivo:** provar que um APK Android consegue executar Python e invocar capacidades Android por uma API própria.

### Escopo
- Projeto Android nativo com Kotlin e Jetpack Compose.
- Integração inicial do Chaquopy.
- Tela simples com editor/campo de código, Executar e console.
- Execução fora da thread principal.
- Captura de stdout, stderr e exceções.
- API Python inicial `mobile.device.get_info()`.
- API Python inicial `mobile.apps.launch(package_name)`.
- Persistência local do script de teste.

### Critérios de aceite
1. APK instala e abre no dispositivo de validação.
2. Runtime Python inicializa.
3. Um script Python é executado a partir da interface.
4. stdout e stderr aparecem no console.
5. Exceção Python é apresentada sem encerrar o aplicativo.
6. Python consulta informações básicas do dispositivo.
7. Python inicia um aplicativo instalado por uma API Android autorizada.
8. Script permanece salvo após fechar e reabrir a aplicação.

### Fora do P0
- Editor profissional completo.
- Execução em segundo plano.
- Acessibilidade.
- Shizuku/ADB/root.
- Múltiplos workers.
- Workflow visual.
- Sandbox para scripts não confiáveis.

**Estado:** concluída e integrada em `main` por Squash Merge (PR #1). Testes host Python 10/10 e JVM/Kotlin 31/31 foram reportados como aprovados; os critérios funcionais foram validados fisicamente no Moto G06 por relato do usuário. Consulte `docs/operations/P0_IMPLEMENTATION_REPORT.md`.

## P1 — Biblioteca e gerenciamento de scripts

**Objetivo:** transformar a prova de conceito em um ambiente utilizável de criação e execução.

Entregas:
- Biblioteca local com pesquisa e operações CRUD: criar, importar, renomear, duplicar e excluir.
- Importação/exportação de arquivos Python UTF-8 pelo Storage Access Framework, com limite de 1 MiB para importação.
- Editor Compose com numeração de linhas e realce lexical básico de Python; não é uma IDE/parser completo.
- Metadados: descrição, tags e parâmetros declarativos tipados.
- Passagem dos parâmetros resolvidos ao script pelo dicionário Python `params`.
- Histórico inicial de versões imutáveis e restauração como nova versão.
- Registro persistente de execuções, status, parâmetros, horários, saída e falhas.
- Console atual e histórico consultável por script.
- Manifesto de dependências por script, com validação sintática local e política explícita de compatibilidade Chaquopy.
- Biblioteca `mobile` versionada e catálogo de capacidades implementadas, sem anunciar módulos futuros como disponíveis.
- Schema Room versão 2; durante o desenvolvimento, fallback destrutivo em versões incompatíveis conforme ADR-0007 (dados locais podem ser descartados).

**Limites:** não há instalação dinâmica genérica de pacotes PyPI no aparelho. Dependências adicionais precisam de distribuição compatível com Chaquopy/Python 3.13/ABI arm64-v8a e inclusão controlada no build. O editor oferece realce lexical, não análise semântica, autocomplete ou depuração.

**Estado:** concluída e integrada em `main` por Squash Merge da PR #2 em 25/11/2026. Testes host Python 13/13, testes JVM, build APK e validação física do escopo acordado no Moto G06 foram reportados como aprovados. P1-TF13 e cenários adicionais (tipo inválido, fila cheia, falhas de persistência, manifesto/dependências, desempenho do editor e fallback destrutivo específico) permanecem diferidos, não aprovados nem bloqueadores por decisão explícita do usuário. Consulte o checklist e o relatório P1.

**Gate:** ciclo completo de criar/importar, editar, salvar, executar com parâmetros, consultar versões e histórico validado no Moto G06; comportamento do banco após atualização incompatível documentado; testes automatizados e build aprovados.

## P2 — Automação de interfaces Android

**Objetivo:** permitir automações geométricas e semânticas por meio de um AccessibilityService nativo, habilitado explicitamente pelo usuário, mantendo Python como API pública e Kotlin como integração Android.

**Estado atual:** preparação documental P2.0.1–P2.0.10 concluída. A P2.1 foi agrupada em três subetapas: A — fundação nativa; B — autorização e integração; C — testes e validação. A implementação da P2.1-A está publicada, revisada estaticamente e compilada em build debug local. Testes automatizados e validação física permanecem pendentes. Nenhuma operação de automação está pronta. A branch de trabalho é `feat/p2-accessibility-automation`, baseada em `main`.

### P2.0 — Preparação, baseline e contratos
- Confirmar código/branch, SDKs e dispositivo real.
- Definir protocolo interno assíncrono Python → Kotlin → AccessibilityService → resultado.
- Definir API pública síncrona Python `mobile.screen`, incluindo `ScreenError`, estados, operações geométricas e Mapas de Coordenadas.
- Revisar privacidade, consentimento, retenção e riscos residuais.
- Formalizar ADR-0008 (serviço, gateway, lifecycle e Bridge) e ADR-0009 (consentimento sensível e retenção).
- Harmonizar visão, arquitetura, roadmap, índices e contratos sem declarar funcionalidade futura como implementada.

### P2.1 — Fundação nativa e autorização

A execução está limitada a três subetapas:
- **P2.1-A — Fundação nativa:** classe AccessibilityService, XML e manifesto, gateway application-scoped e lifecycle gerenciado pelo Android. Implementação inicial publicada; build pendente.
- **P2.1-B — Autorização e integração:** explicação e instruções para configurações Android, estados `not_enabled`/`disconnected`/`connected`, política mínima de eventos e erros estáveis.
- **P2.1-C — Testes e validação:** testes JVM/host, revisão integrada, build local e validação de habilitação/revogação/reconexão no Moto G06.

A fundação inicial recebe somente `typeWindowStateChanged`; não declara capacidade de recuperar conteúdo nem de executar gestos. A tela de autorização e os estados públicos ainda não foram implementados.

### P2.2 — Prioridade A: interação e Mapa de Coordenadas
- Implementar `tap`, `type_text`, `swipe` e `scroll` conforme contrato Python.
- Serializar gestos e aplicar timeouts que incluam fila e execução.
- Implementar captura visual com espera configurável, overlay `TYPE_ACCESSIBILITY_OVERLAY` temporário/cancelável e múltiplos pontos nomeados.
- Persistir mapas mínimos via Room/repositórios Kotlin, permitir listar/obter/excluir e reutilizar pontos com validação de contexto.
- Não solicitar `SYSTEM_ALERT_WINDOW`; o overlay intercepta intencionalmente os toques durante a seleção.

### P2.3 — Prioridade B: consulta e coleta
- Consultar conteúdo comum sob demanda, limitado à operação e ao mínimo necessário.
- Converter resultados em estruturas simples serializáveis para Python e console.
- Tratar campos sensíveis como bloqueados por padrão, com consentimentos independentes de leitura e preenchimento/alteração, concedidos pela UI e limitados à execução e ao app.
- Definir/implementar saída para arquivo somente após contrato explícito de API, destino, autorização e limites; não presumir API Python de arquivos existente.

### P2.4 — Prioridade C: leitura e identificação
- Expandir leitura controlada da árvore de acessibilidade, com limites de profundidade/quantidade/tamanho.
- Localizar elementos por atributos e ações que o app de destino efetivamente exponha.
- Manter referências de nós temporárias, sem persistir snapshots/árvores.
- Documentar apps/componentes que não expõem conteúdo ou recusam ações.

### P2.5 — Integração, testes e validação
- Anunciar capacidades apenas depois de implementação e testes correspondentes.
- Executar testes host/JVM, build APK e validação física no Moto G06, registrando cada categoria separadamente.
- Validar consentimento, revogação, logs, retenção, mapas, backup/transferência e limitações.
- Atualizar documentação e revisar o diff antes de propor integração em `main`.

**Decisões transversais P2:** a API dos scripts é síncrona, Kotlin é assíncrono internamente e a espera ocorre somente no worker serial Python; conteúdo comum é consultado sob demanda; campos sensíveis são bloqueados por padrão; consentimentos de leitura e escrita são independentes; saídas de execuções com acesso sensível não são persistidas por padrão e exigem consentimento separado para retenção; scripts não possuem sandbox e podem usar rede; screenshots, OCR e notificações ficam fora do escopo inicial; mapas são mínimos, locais e não exportados/transmitidos automaticamente. Consulte ADR-0008, ADR-0009 e os contratos operacionais P2.

**Gate P2:** nenhuma capacidade é considerada concluída apenas por existir no contrato. Exigir implementação, testes, build e validação física conforme critérios da subetapa; documentar limitações por app/versão Android.

## P3 — Notificações e eventos

**Objetivo:** permitir automações acionadas por eventos selecionados.

Entregas:
- NotificationListenerService.
- Registro e revogação da autorização.
- Filtros por pacote e condições.
- Normalização de eventos.
- API Python `mobile.notifications`.
- Roteamento de eventos para automações inscritas.

**Gate:** evento autorizado recebido, filtrado e processado sem depender da interface aberta.

## P4 — Agendamento e execução em segundo plano

**Objetivo:** executar trabalhos locais programados com mecanismos Android apropriados.

Entregas:
- Integração com WorkManager.
- Integração com AlarmManager quando aplicável.
- Foreground Service para tarefas prolongadas elegíveis.
- Recorrências e execução única.
- Política de retries.
- Recuperação de estado após reinicialização quando suportada.
- Interface de estado e diagnóstico de agendamentos.

**Gate:** cenários de agendamento validados com documentação de atrasos, permissões e restrições por versão do Android/fabricante.

## P5 — Workflow Engine

**Objetivo:** compor automações em fluxos reutilizáveis.

Entregas:
- Modelos de workflow e etapas.
- Dependências sequenciais.
- Condições de sucesso/falha.
- Passagem de dados entre etapas.
- Repetição configurável.
- Cancelamento e tratamento de falhas.
- Logs por etapa.
- Visualização inicial do fluxo.

**Gate:** fluxos sequenciais e condicionais testados, inclusive em cenários de falha e retomada.

## P6 — Integrações avançadas opcionais

**Objetivo:** ampliar capacidades em dispositivos e ambientes que ofereçam mecanismos adicionais.

Entregas possíveis:
- Adaptador Shizuku.
- Operações ADB autorizadas.
- Detecção de disponibilidade e estado de conexão.
- Adaptador root, somente se houver justificativa e ambiente de teste.
- Catálogo de capacidades e alternativas.

**Gate:** cada adaptador deve ter documentação de pré-requisitos, permissões, riscos e testes próprios. O núcleo deve continuar funcional sem eles.

## P7 — Segurança, robustez e refinamento

**Objetivo:** preparar a plataforma para uso mais confiável e demonstração técnica.

Entregas:
- Modelo de confiança para scripts importados.
- Controles de recursos e execução.
- Estratégia de isolamento avaliada.
- Proteção de credenciais.
- Backup e restauração.
- Observabilidade e diagnóstico.
- Otimização de bateria e memória.
- Testes de regressão e compatibilidade.
- Documentação de instalação e demonstração.

**Gate:** riscos conhecidos documentados, testes críticos aprovados e limitações transparentes.

## Dependências entre marcos

```text
P0 → P1 → P2 → P3 → P4 → P5
                  │           │
                  └──── P6 ───┘
                         │
                         P7
```

A sequência é orientativa. P6 pode ser desenvolvido em paralelo após a estabilização das interfaces da Android Bridge. P7 é transversal e deve receber melhorias desde os primeiros marcos, mesmo que seu gate final ocorra mais tarde.

## Política de conclusão

Um marco só será considerado concluído quando:
- o escopo estiver implementado;
- os critérios de aceite forem verificados;
- os testes relevantes estiverem registrados;
- a validação física necessária estiver concluída;
- a documentação refletir o comportamento real;
- limitações e débitos conhecidos estiverem registrados.
