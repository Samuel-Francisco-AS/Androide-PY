# Androide PY

**Uma plataforma local de automação Android programável em Python.**

Androide PY é um projeto experimental de aplicativo Android que permite criar, editar, organizar, executar e orquestrar automações Python capazes de interagir com recursos do dispositivo e, mediante autorizações apropriadas, com interfaces de outros aplicativos.

O projeto adota uma arquitetura *Python-first* com integração Android nativa: Python concentra a lógica de automação e orquestração; Kotlin fornece a integração com o sistema operacional, os serviços Android e a interface do aplicativo.

> **Status:** P0 e P1 concluídas e integradas em `main`; itens diferidos da P1 permanecem documentados. P2.0 documental e P2.1-A/B/C concluídas no escopo acordado. P2.2-A implementada, com testes automatizados/build concluídos e validação física parcial (4 PASS; `type_text()` adiado). P2.2-B1 e B2 concluídas no escopo de implementação/revisão/testes JVM/build; B2 passou nos 63 testes JVM da suíte (16 específicos) e no build debug em 2026-11-28. Validação física integrada e B3 pendentes. Nenhuma capacidade `screen.*` é anunciada no catálogo.

## Objetivos

- Oferecer um ambiente local para criação e gerenciamento de scripts Python.
- Executar automações sob demanda, por agendamento ou em resposta a eventos.
- Disponibilizar uma biblioteca Python própria para recursos Android.
- Compor scripts em fluxos com dependências, condições e tratamento de falhas.
- Registrar execuções, logs e arquivos produzidos.
- Evoluir por módulos, mantendo permissões e capacidades explícitas.

## Arquitetura em poucas palavras

- **Android:** Kotlin e Jetpack Compose.
- **Runtime Python:** Chaquopy 17.2.0 configurado para CPython 3.13; execução validada no Moto G06 durante P0.
- **Persistência:** Room 2.8.5 sobre SQLite, com repositórios Kotlin para scripts, versões e execuções; schema P1 versão 2; fallback destrutivo de desenvolvimento pode descartar dados locais, conforme ADR-0007; nenhum acesso Python direto ao banco.
- **Integração:** Android Bridge atualmente implementa `mobile.device.get_info()` e `mobile.apps.launch(package_name)`; a fachada `mobile.screen` está implementada na branch P2, mas o catálogo ainda não anuncia capacidades `screen.*` até a aprovação dos testes correspondentes; a sessão de captura B2 possui somente ponto de entrada nativo interno, sem API pública nesta etapa. Shizuku/ADB/root permanecem opcionais e fora do núcleo.
- **Execução:** executor Python serial, parâmetros via `params`, console por execução e histórico persistido; execução em segundo plano permanece em fase posterior.

A arquitetura completa, suas fronteiras e suas limitações estão documentadas em [docs/ARCHITECTURE.md](docs/ARCHITECTURE.md).

## Documentação

| Documento | Conteúdo |
|---|---|
| [Visão do projeto](docs/PROJECT_VISION.md) | Problema, proposta, objetivos, escopo e princípios |
| [Arquitetura](docs/ARCHITECTURE.md) | Componentes, responsabilidades, comunicação e segurança |
| [Roadmap](docs/ROADMAP.md) | Marcos, entregas e critérios de validação |
| [Decisões arquiteturais](docs/adr/README.md) | Índice dos Architecture Decision Records |
| [Checklist operacional P0](docs/operations/P0_EXECUTION_CHECKLIST.md) | Fases, subetapas e evidências |
| [Build e testes](docs/operations/P0_BUILD_AND_TEST.md) | Sincronização e build sem ADB |
| [Plano de integração](docs/operations/P0_INTEGRATION_TEST_PLAN.md) | Testes host/JVM e roteiro físico |
| [Relatório da implementação P0](docs/operations/P0_IMPLEMENTATION_REPORT.md) | Entregas, evidências e validações da fundação |
| [Checklist operacional P1](docs/operations/P1_EXECUTION_CHECKLIST.md) | Subetapas, critérios de aceite e evidências da biblioteca de scripts |
| [Relatório da implementação P1](docs/operations/P1_IMPLEMENTATION_REPORT.md) | Decisões, implementação, testes e limitações da P1 |
| [Checklist operacional P2](docs/operations/P2_EXECUTION_CHECKLIST.md) | Decomposição, prioridades, contratos e evidências da P2 |
| [Relatório da implementação P2](docs/operations/P2_IMPLEMENTATION_REPORT.md) | Estado, decisões, riscos e validações da P2 |
| [Contrato Python P2](docs/operations/P2_PYTHON_API.md) | API planejada `mobile.screen` e Mapas de Coordenadas |
| [Protocolo assíncrono P2](docs/operations/P2_ASYNC_PROTOCOL.md) | Comunicação interna Python/Kotlin/AccessibilityService |
| [Revisão de segurança P2](docs/operations/P2_SECURITY_REVIEW.md) | Consentimento, minimização, retenção e riscos P2 |
| [Contrato da sessão B2](docs/operations/P2_2B2_CAPTURE_SESSION_CONTRACT.md) | Sessão de captura, overlay, timer, lifecycle e critérios de aceite |

## Estado atual

A P0 está integrada em `main`. O usuário reportou build APK, instalação e abertura no Moto G06, execução Python, stdout/stderr, tratamento de exceções, leitura de informações do aparelho, abertura de Configurações e persistência após reabertura. Os testes host Python (10) e JVM/Kotlin (31) foram reportados como aprovados. Consulte o [relatório P0](docs/operations/P0_IMPLEMENTATION_REPORT.md) para distinguir evidências automatizadas e relatos físicos.

A P1 foi integrada em `main` por Squash Merge da PR #2 em 25/11/2026. O escopo acordado foi validado no Moto G06; P1-TF13 e cenários adicionais de tipo inválido, fila cheia, falhas de persistência, dependências e fallback destrutivo específico permanecem diferidos, sem serem declarados aprovados. Consulte o [checklist P1](docs/operations/P1_EXECUTION_CHECKLIST.md) e o [relatório P1](docs/operations/P1_IMPLEMENTATION_REPORT.md).

**P2 — estado atual:** P2.1-C permanece concluída no escopo validado. P2.2-A está implementada, testada/build concluído e com validação física parcial (4 PASS; `type_text()` adiado). P2.2-B1 e B2 estão implementadas, revisadas, testadas em JVM (63/63, incluindo 16 testes B2) e compiladas. O catálogo continua sem anunciar operações `screen.*`; B2 não publica API Python e B3 permanece pendente. A validação física do mapa/overlay está reservada à bateria integrada final da B3. Consulte o [roadmap](docs/ROADMAP.md), o [checklist P2](docs/operations/P2_EXECUTION_CHECKLIST.md) e o [relatório P2](docs/operations/P2_IMPLEMENTATION_REPORT.md).

## Build e testes

O projeto usa Gradle Wrapper 8.14.8, Compose BOM 2026.06.01 e Python host 3.13. Não é necessário instalar Gradle globalmente nem usar ADB.

Para P0, consulte [Build e testes no Fedora](docs/operations/P0_BUILD_AND_TEST.md). Para consultar evidências e pendências da P1, siga o [checklist operacional P1](docs/operations/P1_EXECUTION_CHECKLIST.md). Para a futura implementação P2, use o checklist P2; builds e validações físicas continuam sob responsabilidade do usuário no Fedora/Moto G06. O APK será transferido ao Moto G06 pelo fluxo manual já utilizado no My-CT.

## Segurança e limitações

Automações executam código e podem acessar recursos concedidos ao aplicativo. Scripts devem ser tratados como código confiável do usuário nesta primeira etapa. Acessibilidade, notificações, armazenamento, alarmes, execução em segundo plano, Shizuku e root possuem requisitos e limitações próprios. Nenhum mecanismo concede controle irrestrito sobre o Android.

Consulte a seção de segurança em [ARCHITECTURE.md](docs/ARCHITECTURE.md) antes de implementar novos adaptadores.

## Licença

A licença do projeto ainda não foi definida.
