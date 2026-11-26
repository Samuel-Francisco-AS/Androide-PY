# Androide PY

**Uma plataforma local de automação Android programável em Python.**

Androide PY é um projeto experimental de aplicativo Android que permite criar, editar, organizar, executar e orquestrar automações Python capazes de interagir com recursos do dispositivo e, mediante autorizações apropriadas, com interfaces de outros aplicativos.

O projeto adota uma arquitetura *Python-first* com integração Android nativa: Python concentra a lógica de automação e orquestração; Kotlin fornece a integração com o sistema operacional, os serviços Android e a interface do aplicativo.

> **Status:** P0 e P1 concluídas e integradas em `main`. A P1 teve testes host/JVM, build APK e validação física do escopo acordado reportados como aprovados no Moto G06; itens diferidos permanecem documentados. A P2.0 documental e a P2.1-A (fundação nativa e build debug) foram concluídas na branch de trabalho. A implementação inicial da P2.1-B (autorização e integração) está publicada, com build/testes pendentes. A validação física integrada permanece para P2.1-C.

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
- **Integração:** Android Bridge atualmente implementa `mobile.device.get_info()` e `mobile.apps.launch(package_name)`; `mobile.screen` está especificada para a P2, mas ainda não implementada. Shizuku/ADB/root permanecem opcionais e fora do núcleo.
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

## Estado atual

A P0 está integrada em `main`. O usuário reportou build APK, instalação e abertura no Moto G06, execução Python, stdout/stderr, tratamento de exceções, leitura de informações do aparelho, abertura de Configurações e persistência após reabertura. Os testes host Python (10) e JVM/Kotlin (31) foram reportados como aprovados. Consulte o [relatório P0](docs/operations/P0_IMPLEMENTATION_REPORT.md) para distinguir evidências automatizadas e relatos físicos.

A P1 foi integrada em `main` por Squash Merge da PR #2 em 25/11/2026. O escopo acordado foi validado no Moto G06; P1-TF13 e cenários adicionais de tipo inválido, fila cheia, falhas de persistência, dependências e fallback destrutivo específico permanecem diferidos, sem serem declarados aprovados. Consulte o [checklist P1](docs/operations/P1_EXECUTION_CHECKLIST.md) e o [relatório P1](docs/operations/P1_IMPLEMENTATION_REPORT.md).

**P2 — estado atual:** P2.0.1–P2.0.10 concluídas documentalmente. P2.1-A — Fundação nativa — teve a implementação inicial publicada e revisada estaticamente; build, testes e validação física permanecem pendentes. P2.1-B e P2.1-C ainda não foram iniciadas. A próxima subetapa após o gate P2.0 é P2.1-A — Fundação nativa. A P2 prioriza A (interação e Mapa de Coordenadas), B (consulta/coleta) e C (leitura/identificação), conforme o [roadmap](docs/ROADMAP.md) e o [checklist P2](docs/operations/P2_EXECUTION_CHECKLIST.md).

## Build e testes

O projeto usa Gradle Wrapper 8.14.8, Compose BOM 2026.06.01 e Python host 3.13. Não é necessário instalar Gradle globalmente nem usar ADB.

Para P0, consulte [Build e testes no Fedora](docs/operations/P0_BUILD_AND_TEST.md). Para consultar evidências e pendências da P1, siga o [checklist operacional P1](docs/operations/P1_EXECUTION_CHECKLIST.md). Para a futura implementação P2, use o checklist P2; builds e validações físicas continuam sob responsabilidade do usuário no Fedora/Moto G06. O APK será transferido ao Moto G06 pelo fluxo manual já utilizado no My-CT.

## Segurança e limitações

Automações executam código e podem acessar recursos concedidos ao aplicativo. Scripts devem ser tratados como código confiável do usuário nesta primeira etapa. Acessibilidade, notificações, armazenamento, alarmes, execução em segundo plano, Shizuku e root possuem requisitos e limitações próprios. Nenhum mecanismo concede controle irrestrito sobre o Android.

Consulte a seção de segurança em [ARCHITECTURE.md](docs/ARCHITECTURE.md) antes de implementar novos adaptadores.

## Licença

A licença do projeto ainda não foi definida.
