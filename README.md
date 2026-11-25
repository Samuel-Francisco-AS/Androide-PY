# Androide PY

**Uma plataforma local de automação Android programável em Python.**

Androide PY é um projeto experimental de aplicativo Android que permite criar, editar, organizar, executar e orquestrar automações Python capazes de interagir com recursos do dispositivo e, mediante autorizações apropriadas, com interfaces de outros aplicativos.

O projeto adota uma arquitetura *Python-first* com integração Android nativa: Python concentra a lógica de automação e orquestração; Kotlin fornece a integração com o sistema operacional, os serviços Android e a interface do aplicativo.

> **Status:** P0 e P1 concluídas e integradas em `main`. A P1 foi validada por testes host/JVM, build APK e validação física do escopo acordado no Moto G06. Consulte o checklist e o relatório P1 para pendências explicitamente adiadas e limitações conhecidas.

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
- **Persistência:** Room 2.8.5 sobre SQLite, com repositórios Kotlin para scripts, versões e execuções; migração Room 1 → 2 preparada na P1; nenhum acesso Python direto ao banco.
- **Integração:** Android Bridge com `mobile.device.get_info()`, `mobile.apps.launch(package_name)` e catálogo `mobile.capabilities`; Accessibility e Shizuku permanecem em fases posteriores.
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

## Estado atual

A P0 está integrada em `main`. O usuário reportou build APK, instalação e abertura no Moto G06, execução Python, stdout/stderr, tratamento de exceções, leitura de informações do aparelho, abertura de Configurações e persistência após reabertura. Os testes host Python (10) e JVM/Kotlin (31) foram reportados como aprovados. Consulte o [relatório P0](docs/operations/P0_IMPLEMENTATION_REPORT.md) para distinguir evidências automatizadas e relatos físicos.

A P1 está sendo desenvolvida em `feat/p1-script-library`: biblioteca CRUD, editor com numeração/realce lexical, metadados, parâmetros, versões, histórico de execução, importação/exportação e declaração de dependências. A implementação ainda precisa de build, testes executados e validação física; acompanhe o [checklist P1](docs/operations/P1_EXECUTION_CHECKLIST.md).

Os critérios históricos do P0 foram demonstrados. O gate P1 exige, além dos testes automatizados:

1. Criar, importar, editar, renomear, duplicar, exportar e excluir scripts.
2. Preservar scripts existentes na migração Room da P0.
3. Salvar metadados, parâmetros e snapshots de versão.
4. Executar com parâmetros e consultar histórico de resultados.
5. Validar o fluxo completo no Moto G06.

## Build e testes

O projeto usa Gradle Wrapper 8.14.8, Compose BOM 2026.06.01 e Python host 3.13. Não é necessário instalar Gradle globalmente nem usar ADB.

Para P0, consulte [Build e testes no Fedora](docs/operations/P0_BUILD_AND_TEST.md). Para sincronizar e validar esta entrega P1, siga a seção de comandos do [checklist operacional P1](docs/operations/P1_EXECUTION_CHECKLIST.md). O APK será transferido ao Moto G06 pelo fluxo manual já utilizado no My-CT.

## Segurança e limitações

Automações executam código e podem acessar recursos concedidos ao aplicativo. Scripts devem ser tratados como código confiável do usuário nesta primeira etapa. Acessibilidade, notificações, armazenamento, alarmes, execução em segundo plano, Shizuku e root possuem requisitos e limitações próprios. Nenhum mecanismo concede controle irrestrito sobre o Android.

Consulte a seção de segurança em [ARCHITECTURE.md](docs/ARCHITECTURE.md) antes de implementar novos adaptadores.

## Licença

A licença do projeto ainda não foi definida.
